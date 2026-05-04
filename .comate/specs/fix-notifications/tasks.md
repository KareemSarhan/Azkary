# Fix Notifications - Task Plan

- [x] Task 1: Add `NotificationResult` enum and update `AzkarNotificationManager.showCategoryNotification()` return type
    - 1.1: Add `NotificationResult` enum (SHOWN, PERMISSION_DENIED) inside `AzkarNotificationManager.kt`
    - 1.2: Change `showCategoryNotification()` return type from `Unit` to `NotificationResult`
    - 1.3: Return `NotificationResult.PERMISSION_DENIED` when permission check fails; return `NotificationResult.SHOWN` after `notify()` succeeds

- [x] Task 2: Create `DailyNotificationSchedulerWorker`
    - 2.1: Create new file `notification/DailyNotificationSchedulerWorker.kt`
    - 2.2: Inject `AzkarNotificationScheduler`, `UserPreferencesRepository`, `PrayerTimesRepository`, `AzkarRepository` via Hilt
    - 2.3: Implement `doWork()`: fetch location prefs, prayer times, categories with notifications, and call `notificationScheduler.scheduleNotifications()`
    - 2.4: Return `Result.success()` on all early-exit paths (no location, no categories, no prayer times) to avoid infinite retries

- [x] Task 3: Add daily periodic scheduling to `AzkarNotificationScheduler`
    - 3.1: Add `DAILY_SCHEDULER_WORK_NAME` constant to companion object
    - 3.2: Create `scheduleDailyRescheduling()` method that enqueues a `PeriodicWorkRequestBuilder<DailyNotificationSchedulerWorker>` with 24h repeat interval and 15min flex
    - 3.3: Use `ExistingPeriodicWorkPolicy.KEEP` to avoid replacing already-scheduled work
    - 3.4: Add `WORK_TAG` to the periodic request for consistent cancellation

- [x] Task 4: Update `AzkarNotificationWorker` to handle permission result and ensure daily rescheduling
    - 4.1: Capture the `NotificationResult` from `showCategoryNotification()`
    - 4.2: Return `Result.retry()` when result is `PERMISSION_DENIED` (WorkManager will retry with backoff)
    - 4.3: After successful notification, call `notificationScheduler.scheduleDailyRescheduling()` as a safety net

- [x] Task 5: Schedule daily rescheduling on app start in `AzkaryApp.onCreate()`
    - 5.1: Add `notificationScheduler.scheduleDailyRescheduling()` call before `scheduleNotificationsIfNeeded()` in `onCreate()` coroutine

- [x] Task 6: Schedule daily rescheduling on device boot in `NotificationBootReceiver`
    - 6.1: Add `notificationScheduler.scheduleDailyRescheduling()` call at the end of `rescheduleNotifications()`

- [x] Task 7: Build and verify
    - 7.1: Run `./gradlew assembleDebug` to ensure compilation succeeds
    - 7.2: Run `./gradlew test` to ensure existing tests pass
