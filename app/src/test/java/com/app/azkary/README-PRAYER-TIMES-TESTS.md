# Prayer Times API Testing

This directory contains comprehensive tests for the Aladhan API integration and Prayer Times functionality.

## Test Structure

### 1. Contract Tests (`AladhanLiveContractTest.kt`)
Live API tests that verify the Aladhan API contract remains stable.

**Key Tests:**
- Basic response structure validation
- Timezone format validation
- Prayer times format validation
- Full month data completeness
- Multiple location testing
- Different calculation methods
- Juristic schools comparison
- Prayer times chronological order
- Error response handling
- Extreme latitude handling

**Run with:**
```bash
./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.app.azkary.AladhanLiveContractTest
```

### 2. Mock Server Tests (`AladhanMockServerTest.kt`)
Comprehensive tests using MockWebServer to test various API scenarios without network dependency.

**Key Tests:**
- Success response parsing
- Query parameter validation
- Full month handling (28/30/31 days)
- Error responses (400, 401, 403, 404, 500, 503)
- Rate limiting (429) with/without Retry-After
- Connection timeout handling
- Malformed JSON responses
- Empty response bodies
- Missing data fields
- Extra fields handling
- Timezone offset in times

**Run with:**
```bash
./gradlew :app:testDebugUnitTest --tests "com.app.azkary.AladhanMockServerTest"
```

### 3. Network Repository Tests (`PrayerTimesNetworkRepositoryTest.kt`)
Unit tests for the network repository layer.

**Key Tests:**
- Success response parsing
- Full month response handling
- Parameter passing validation
- Default parameter usage
- Error code mapping
- Exception types verification
- Network error handling
- Malformed response handling

**Run with:**
```bash
./gradlew :app:testDebugUnitTest --tests "com.app.azkary.PrayerTimesNetworkRepositoryTest"
```

### 4. Offline-First Tests (`PrayerTimesRepositoryOfflineTest.kt`)
Tests for the offline-first caching behavior using Robolectric.

**Key Tests:**
- Fresh cache usage
- Stale cache refresh
- Stale cache fallback on network failure
- Offline mode with cache
- Offline mode without cache
- Day-level cache queries
- Cache corruption detection
- Manual refresh behavior
- Cache population verification
- Old cache cleanup
- Range queries with/without cache

**Run with:**
```bash
./gradlew :app:testDebugUnitTest --tests "com.app.azkary.PrayerTimesRepositoryOfflineTest"
```

### 5. E2E Tests (`PrayerTimesE2ETest.kt`)
End-to-end user flow tests simulating real app usage.

**Key Tests:**
- First-time user flow
- Cached data usage
- Today's prayer times view
- Specific date view
- Current Azkar window calculation
- Morning/Night/Sleep window determination
- Week view prayer times
- Month boundary spanning
- Manual refresh flow
- Islamic date calculation

**Run with:**
```bash
./gradlew :app:testDebugUnitTest --tests "com.app.azkary.PrayerTimesE2ETest"
```

### 6. Test Utilities (`testutil/PrayerTimesTestData.kt`)
Helper functions and test data factories for creating mock responses.

**Utilities:**
- Calendar response generators
- Prayer day DTO builders
- Error response creators
- Sample locations
- Calculation method constants
- Juristic school constants
- Assertion extensions

## Running All Tests

### Unit Tests
```bash
./gradlew :app:testDebugUnitTest
```

### Instrumented Tests
```bash
./gradlew :app:connectedAndroidTest
```

### All Tests
```bash
./gradlew :app:test :app:connectedAndroidTest
```

## CI/CD Considerations

### For CI Pipelines:

1. **Unit Tests Only** (Fast, no emulator needed):
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```

2. **Contract Tests** (Require network, use sparingly):
   ```bash
   ./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.app.azkary.AladhanLiveContractTest
   ```
   Note: Consider running contract tests on a schedule (e.g., daily) rather than on every build to avoid flakiness.

3. **Mock Tests** (Recommended for PR validation):
   ```bash
   ./gradlew :app:testDebugUnitTest --tests "com.app.azkary.*MockServerTest" --tests "com.app.azkary.*RepositoryTest" --tests "com.app.azkary.*OfflineTest" --tests "com.app.azkary.*E2ETest"
   ```

### Test Reports:
- Unit test reports: `app/build/reports/tests/testDebugUnitTest/index.html`
- Coverage reports: `app/build/reports/jacoco/index.html` (if Jacoco configured)

## Test Dependencies

```kotlin
// Already configured in build.gradle.kts
testImplementation(libs.junit)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.mockwebserver)
testImplementation(libs.robolectric)
testImplementation(libs.androidx.core)
testImplementation(libs.mockk)

androidTestImplementation(libs.androidx.junit)
androidTestImplementation(libs.androidx.espresso.core)
androidTestImplementation(libs.androidx.ui.test.junit4)
```

## Best Practices

1. **MockWebServer vs Live Tests**
   - Use MockWebServer for PR validation and fast feedback
   - Use Live Contract Tests for detecting API changes (run periodically)

2. **Offline Testing**
   - Use Robolectric for offline behavior tests
   - Mock ConnectivityManager for network state simulation

3. **Test Data**
   - Use `PrayerTimesTestData` factory for consistent test data
   - Vary test data to cover edge cases (28/30/31 day months)

4. **Error Scenarios**
   - Test all HTTP error codes (400, 401, 403, 404, 429, 500, 503)
   - Test network failures (timeout, disconnect)
   - Test malformed responses

5. **Performance**
   - Keep unit tests fast (< 1 second each)
   - Use `runTest` for coroutine testing
   - Avoid real network calls in unit tests

## Coverage Areas

- ✅ API contract validation
- ✅ Response parsing (success/error)
- ✅ HTTP error handling
- ✅ Network failure handling
- ✅ Offline-first behavior
- ✅ Cache management
- ✅ Cache corruption detection
- ✅ Prayer time calculations
- ✅ Azkar window calculations
- ✅ Multi-day operations
- ✅ Different locations/timezones
- ✅ Different calculation methods
- ✅ Juristic schools
