# Fix Notifications - Summary

## Problem
Notifications in the Azkary app were only scheduled for the current day using `OneTimeWorkRequest`. Once a notification fired, nothing scheduled the next day's notifications. This caused all azkar reminders to stop working after the first day unless the user manually reopened the app or the device rebooted.

## Root Cause
The notification scheduling pipeline (`AzkarNotificationScheduler.scheduleNotifications()`) created one-shot `OneTimeWorkRequest` instances per category. There was no periodic or chained mechanism to reschedule notifications for subsequent days. The only entry points that triggered scheduling were:
- App open (`AzkaryApp.onCreate()`)
- Device boot (`NotificationBootReceiver`)

Neither of these runs daily on their own.

## Solution (3 parts)

### 1. Daily Periodic Rescheduling
- Created `DailyNotificationSchedulerWorker` - a `CoroutineWorker` that runs every ~24 hours via `PeriodicWorkRequest`
- It fetches the user's location, computes today's prayer times, gets categories with notifications enabled, and calls `AzkarNotificationScheduler.scheduleNotifications()`
- Uses `ExistingPeriodicWorkPolicy.KEEP` to avoid duplicate periodic work
- Scheduled from `AzkaryApp.onCreate()` and `NotificationBootReceiver.rescheduleNotifications()`

### 2. Permission-Aware Retry
- Added `NotificationResult` enum (`SHOWN`, `PERMISSION_DENIED`) to `AzkarNotificationManager`
- `showCategoryNotification()` now returns `NotificationResult.PERMISSION_DENIED` instead of silently skipping when `POST_NOTIFICATIONS` permission is missing
- `AzkarNotificationWorker` returns `Result.retry()` on permission denial, allowing WorkManager to retry with backoff

### 3. Safety-Net Chaining
- `AzkarNotificationWorker` calls `notificationScheduler.scheduleDailyRescheduling()` after each successful notification delivery
- This ensures the periodic work is enrolled even if the app was opened before the `AzkaryApp.onCreate()` scheduling was added

## Files Changed

| File | Change |
|------|--------|
| `notification/AzkarNotificationManager.kt` | Added `NotificationResult` enum; changed `showCategoryNotification()` return type |
| `notification/DailyNotificationSchedulerWorker.kt` | **New file** - periodic worker that reschedules all notifications daily |
| `notification/AzkarNotificationScheduler.kt` | Added `scheduleDailyRescheduling()` with `PeriodicWorkRequestBuilder` |
| `notification/AzkarNotificationWorker.kt` | Injected `AzkarNotificationScheduler`; added `Result.retry()` on permission denial; added safety-net `scheduleDailyRescheduling()` call |
| `AzkaryApp.kt` | Added `scheduleDailyRescheduling()` call in `onCreate` coroutine |
| `notification/NotificationBootReceiver.kt` | Added `scheduleDailyRescheduling()` call in `rescheduleNotifications()` |

## Build Status
BUILD SUCCESSFUL - all 84 tasks completed, 0 test failures.

## Commit
`4c4efa4` on branch `fix/notifications` in worktree `.worktrees/fix-notifications`
