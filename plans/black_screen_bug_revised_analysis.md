# Black Screen Bug - Revised Analysis

## Updated Understanding

User clarified: **This is NOT a race condition** - they can wait after the gesture and still
reproduce the issue.

## Actual Issue Flow

1. User is on ReadingScreen (or CompletionScreen)
2. User swipes back using gesture navigation
3. Successfully navigates to SummaryScreen
4. User taps in the same area where the back button was (top-left corner)
5. **Screen goes black**

## Key Question

What element is being tapped on the SummaryScreen when the user taps in the top-left corner?

### SummaryScreen Layout Analysis

Looking at [`SummaryScreen.kt`](app/src/main/java/com/app/azkary/ui/summary/SummaryScreen.kt):

```kotlin
Scaffold(
    topBar = {
        TopAppBar(title = {
            Column {
                Text(today, style = MaterialTheme.typography.bodyMedium)
            }
        }, actions = {
            IconButton(onClick = { viewModel.toggleEditMode() }) {
                Icon(Icons.Default.Edit, contentDescription = null)
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, ...)
            }
        })
    }) { padding ->
        LazyColumn(...) {
            currentSession?.let { session ->
                item {
                    CurrentSessionCard(
                        category = session,
                        sessionEndTime = sessionEndTime,
                        onContinue = { onNavigateToCategory(session.id) })
                }
            }
            // ... category items
        }
    }
```

### TopAppBar Analysis

- **No navigationIcon** (no back button on SummaryScreen)
- Title is in the center (date)
- Actions (Edit, Settings) are on the **right side**

### LazyColumn Content

1. **CurrentSessionCard** (if exists) - has a "Continue" button that navigates to reading
2. **CategoryItem** cards - clickable, navigate to reading

## Possible Causes

### 1. CurrentSessionCard "Continue" Button Issue

If the first item in the LazyColumn is the CurrentSessionCard, and its "Continue" button happens to
be in the tap area, clicking it would navigate to the reading page. But this shouldn't cause a black
screen unless there's an issue with the navigation.

### 2. CategoryItem Click Issue

If a category item is in the tap area and gets clicked, it navigates to reading. Again, shouldn't
cause black screen unless there's a navigation issue.

### 3. Navigation State Corruption

The most likely cause is that something in the navigation state is corrupted after the gesture back,
and when the user taps something that triggers navigation again, it causes a black screen.

### 4. ViewModel State Issue

The ReadingViewModel might have some state that's not properly reset when navigating back, and when
navigating again, it causes issues.

## Investigation Needed

1. Check if there are any issues with the navigation setup in MainActivity
2. Check if the ReadingViewModel has any state that could cause issues on re-navigation
3. Check if there are any issues with the categoryId parameter handling

## Next Steps

1. Examine the navigation setup in MainActivity more carefully
2. Look for any potential issues with the categoryId parameter
3. Check if there are any state management issues in ReadingViewModel
4. Consider if the issue is related to the gesture navigation itself
