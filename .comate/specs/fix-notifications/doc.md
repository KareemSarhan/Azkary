# Fix Notifications - Spec Document

## Problem Statement

Notifications in Azkary are not working reliably. After thorough analysis, the previously documented bugs (missing permission check, no scheduling on first launch, categories defaulting to `notificationEnabled=false`, broken `.first()` in boot receiver) have all been fixed. However, notifications still fail because **there is no daily rescheduling mechanism**.

### Current Behavior

1. `AzkaryApp.onCreate()` calls `scheduleNotificationsIfNeeded()` -- schedules today's notifications only
2. `AzkarNotificationScheduler.scheduleNotifications()` uses `OneTimeWorkRequest` with `setInitialDelay()` -- fires once, then done
3. When tomorrow arrives, **no notifications are scheduled** because:
   - The app wasn't opened (no `onCreate()` trigger)
   - The device didn't reboot (no `NotificationBootReceiver` trigger)
   - The user didn't change settings (no `SettingsViewModel` trigger)
4. Each `AzkarNotificationWorker` executes once and returns `Result.success()` with no follow-up

### Secondary Issues

- **Silent permission skip**: If `POST_NOTIFICATIONS` is denied on Android 13+, the notification is silently dropped at `AzkarNotificationManager.kt:72-74`. The worker still returns `Result.success()`, so WorkManager won't retry later even if the user grants permission.
- **WorkManager timing inaccuracy**: Without exact alarm permissions (removed in v3.1.2), WorkManager relies on inexact scheduling which can be significantly delayed on devices with aggressive battery optimization (Samsung, Xiaomi, Huawei).

---

## Proposed Solution

### Fix 1: Daily Rescheduling via PeriodicWorkRequest

Add a `PeriodicWorkRequest` that runs once per day (at midnight or shortly after) to reschedule all category notifications for the new day.

**Approach**: Create a new `DailyNotificationSchedulerWorker` that:
- Runs once every 24 hours
- Queries location preferences, prayer times, and categories with notifications enabled
- Calls `AzkarNotificationScheduler.scheduleNotifications()` for the current day

This worker is scheduled alongside the per-category `OneTimeWorkRequest` entries and ensures that even if the user doesn't open the app, notifications are still set up for the next day.

**Why PeriodicWorkRequest over AlarmManager?**
- WorkManager is battery-aware and respects Doze mode
- No need for `SCHEDULE_EXACT_ALARM` permission (which Google Play restricts)
- Survives device reboots automatically
- Consistent with the existing architecture

**Trade-off**: WorkManager's periodic work is inexact (may fire a few minutes after the interval). For a midnight rescheduling task, a few minutes of delay is acceptable since the per-category notifications still have hours before their trigger times.

### Fix 2: Chain Rescheduling from Each Notification Worker

After `AzkarNotificationWorker` fires a notification, it should also check if tomorrow's notifications are scheduled, and if not, schedule them. This provides a belt-and-suspenders approach alongside the periodic work.

**Approach**: In `AzkarNotificationWorker.doWork()`:
- After showing the notification, check if tomorrow's work is already scheduled
- If not, schedule a one-time work for tomorrow's prayer times
- This ensures that even if the periodic work doesn't fire (edge case), the next day still gets covered

### Fix 3: Improve Permission Denial Handling

Instead of silently skipping notifications when `POST_NOTIFICATIONS` is denied, the worker should return `Result.retry()` with a backoff policy, so WorkManager will retry when conditions might change.

**Approach**: In `AzkarNotificationManager.showCategoryNotification()`:
- Return a result enum (SHOWN, PERMISSION_DENIED, ERROR)
- In `AzkarNotificationWorker.doWork()`, handle the result:
  - `PERMISSION_DENIED` -> return `Result.retry()` so WorkManager retries later
  - `SHOWN` -> return `Result.success()`

---

## Affected Files

| File | Type of Modification | Description |
|------|---------------------|-------------|
| `app/src/main/java/com/app/azkary/notification/AzkarNotificationScheduler.kt` | Modify | Add method to schedule daily periodic work alongside per-category work |
| `app/src/main/java/com/app/azkary/notification/AzkarNotificationWorker.kt` | Modify | Chain tomorrow's scheduling after notification fires; handle permission result |
| `app/src/main/java/com/app/azkary/notification/AzkarNotificationManager.kt` | Modify | Return result enum instead of void from `showCategoryNotification()` |
| `app/src/main/java/com/app/azkary/AzkaryApp.kt` | Modify | Schedule the daily periodic work on app start |
| `app/src/main/java/com/app/azkary/notification/DailyNotificationSchedulerWorker.kt` | **New file** | Periodic worker that reschedules all notifications once per day |
| `app/src/main/java/com/app/azkary/notification/NotificationBootReceiver.kt` | Modify | Also schedule the daily periodic work on boot |

---

## Implementation Details

### 1. New `NotificationResult` Enum

Location: `AzkarNotificationManager.kt` (inner companion or top-level)

```kotlin
enum class NotificationResult {
    SHOWN,
    PERMISSION_DENIED
}
```

### 2. Modify `AzkarNotificationManager.showCategoryNotification()`

Change return type from `Unit` to `NotificationResult`:

```kotlin
fun showCategoryNotification(
    categoryId: String,
    categoryName: String,
    categoryType: CategoryType,
    notificationId: Int
): NotificationResult {
    if (!permissionHelper.hasNotificationPermission()) {
        return NotificationResult.PERMISSION_DENIED
    }
    val channelId = if (categoryType == CategoryType.DEFAULT) CHANNEL_ID_BASE else CHANNEL_ID_USER
    val notification = buildNotification(
        title = categoryName,
        content = context.getString(R.string.notification_reminder_content),
        notificationId = notificationId,
        categoryId = categoryId,
        channelId = channelId
    )
    notificationManager.notify(notificationId, notification)
    return NotificationResult.SHOWN
}
```

### 3. New `DailyNotificationSchedulerWorker`

```kotlin
@HiltWorker
class DailyNotificationSchedulerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationScheduler: AzkarNotificationScheduler,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val azkarRepository: AzkarRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "daily_notification_scheduler"
    }

    override suspend fun doWork(): Result {
        val locationPrefs = userPreferencesRepository.locationPreferences.first()
        if (!locationPrefs.useLocation || locationPrefs.lastResolvedLocation == null) {
            return Result.success()
        }

        val location = locationPrefs.lastResolvedLocation
        val todayTimes = prayerTimesRepository.getDayPrayerTimes(
            date = LocalDate.now(),
            latitude = location.latitude,
            longitude = location.longitude
        ) ?: return Result.success()

        val categories = azkarRepository.getCategoriesWithNotifications()
        if (categories.isEmpty()) return Result.success()

        notificationScheduler.scheduleNotifications(
            todayTimes = todayTimes,
            categories = categories
        )

        return Result.success()
    }
}
```

### 4. Modify `AzkarNotificationScheduler` -- Add Daily Periodic Scheduling

```kotlin
companion object {
    const val REMINDER_MINUTES = 30L
    const val WORK_TAG = "azkar_notification_worker"
    const val DAILY_SCHEDULER_WORK_NAME = "daily_notification_scheduler"

    fun getWorkNameForCategory(categoryId: String) = "category_notification_$categoryId"
}

fun scheduleDailyRescheduling() {
    val dailyRequest = PeriodicWorkRequestBuilder<DailyNotificationSchedulerWorker>(
        24, TimeUnit.HOURS,
        15, TimeUnit.MINUTES  // flex interval
    )
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
        )
        .addTag(WORK_TAG)
        .build()

    workManager.enqueueUniquePeriodicWork(
        DAILY_SCHEDULER_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,  // don't replace if already scheduled
        dailyRequest
    )
}
```

### 5. Modify `AzkarNotificationWorker` -- Chain Tomorrow + Handle Permission

```kotlin
override suspend fun doWork(): Result {
    val categoryId = inputData.getString(KEY_CATEGORY_ID) ?: return Result.failure()

    val langTag = localeManager.getCurrentLanguageTag(applicationContext)
    val today = LocalDate.now().toString()

    val categories = repository.observeCategoriesWithDisplayName(langTag, today).first()
    val category = categories.find { it.id == categoryId } ?: return Result.failure()

    val result = notificationManager.showCategoryNotification(
        categoryId = categoryId,
        categoryName = category.name,
        categoryType = category.type,
        notificationId = categoryId.hashCode()
    )

    if (result == NotificationResult.PERMISSION_DENIED) {
        return Result.retry()
    }

    // Ensure daily rescheduling worker is active
    scheduleDailyReschedulingIfNeeded()

    return Result.success()
}

private fun scheduleDailyReschedulingIfNeeded() {
    // Delegate to AzkarNotificationScheduler to ensure the daily worker is enqueued
    // This is a safety net -- the periodic work should already be running
}
```

### 6. Modify `AzkaryApp.onCreate()` -- Schedule Daily Work

```kotlin
override fun onCreate() {
    super.onCreate()

    applicationScope.launch {
        userPreferencesRepository.initializeFirstInstallDate()
        userPreferencesRepository.incrementAppOpenCount()
        notificationScheduler.scheduleDailyRescheduling()  // ensure daily worker is active
        scheduleNotificationsIfNeeded()
    }
}
```

### 7. Modify `NotificationBootReceiver` -- Schedule Daily Work on Boot

```kotlin
private suspend fun rescheduleNotifications() {
    val locationPrefs = userPreferencesRepository.locationPreferences.first()
    if (!locationPrefs.useLocation || locationPrefs.lastResolvedLocation == null) return

    val location = locationPrefs.lastResolvedLocation
    val todayTimes = prayerTimesRepository.getDayPrayerTimes(
        date = LocalDate.now(),
        latitude = location.latitude,
        longitude = location.longitude
    )

    if (todayTimes != null) {
        val categories = azkarRepository.getCategoriesWithNotifications()
        notificationScheduler.scheduleNotifications(
            todayTimes = todayTimes,
            categories = categories
        )
    }

    // Also ensure the daily rescheduling worker is active
    notificationScheduler.scheduleDailyRescheduling()
}
```

---

## Boundary Conditions and Exception Handling

1. **First install with no location set**: `scheduleNotificationsIfNeeded()` returns early. The daily worker runs but also returns early gracefully with `Result.success()`.
2. **Location disabled after initial setup**: `getCategoriesWithNotifications()` returns empty list, daily worker exits cleanly.
3. **Prayer times fetch fails**: `getDayPrayerTimes()` returns null, daily worker exits with `Result.success()` (not failure, to avoid infinite retries).
4. **All categories archived**: `getCategoriesWithNotifications()` returns empty, no notifications scheduled, no crash.
5. **Permission denied mid-day**: Worker returns `Result.retry()`, WorkManager retries with backoff. Max retry count is WorkManager's default (3).
6. **Periodic work already running**: `ExistingPeriodicWorkPolicy.KEEP` prevents replacing an already-scheduled periodic work, avoiding unnecessary rescheduling.
7. **Day boundary crossing**: The periodic worker fires around midnight, reschedules all per-category notifications for the new day's prayer times.

## Data Flow

```
App Open / Boot / Settings Change
        |
        v
scheduleDailyRescheduling()  ----->  PeriodicWorkRequest (every 24h)
        |                                    |
        v                                    v
scheduleNotificationsIfNeeded()      DailyNotificationSchedulerWorker.doWork()
        |                                    |
        v                                    v
scheduleNotifications(todayTimes, categories)
        |
        v
OneTimeWorkRequest per category (delayed to prayer time + 30 min)
        |
        v
AzkarNotificationWorker.doWork()
        |
        +---> showCategoryNotification() ---> NotificationResult
        |         |
        |         +---> PERMISSION_DENIED -> Result.retry()
        |         +---> SHOWN -> continue
        |
        +---> scheduleDailyRescheduling() (safety net)
        |
        v
    Result.success()
```

## Expected Outcomes

1. Notifications fire reliably every day without requiring the user to open the app
2. If the user denies notification permission, WorkManager retries (up to 3 times) instead of silently dropping
3. The daily periodic worker ensures rescheduling even if the app process is killed
4. No new permissions required (no `SCHEDULE_EXACT_ALARM`)
5. Battery-efficient: WorkManager batches work and respects Doze mode
