# Testing Guide

This document outlines how to run tests locally and the testing infrastructure in CI/CD.

## Running Tests Locally

### Unit Tests

Run all unit tests:

```bash
./gradlew testDebugUnitTest
```

Run a specific test class:

```bash
./gradlew testDebugUnitTest --tests "com.app.azkary.ExampleTest"
```

Run tests matching a pattern:

```bash
./gradlew testDebugUnitTest --tests "*RepositoryTest"
```

### Integration Tests

Integration tests are run as part of the unit test suite. To run only integration tests:

```bash
./gradlew testDebugUnitTest --tests "*Integration*"
```

### Code Coverage

Generate coverage report:

```bash
./gradlew koverHtmlReportDebug
```

Open the report:

```bash
open app/build/reports/kover/reportDebug/html/index.html
```

Verify coverage meets thresholds:

```bash
./gradlew koverVerifyDebug
```

### Lint Checks

Run Android lint:

```bash
./gradlew lintDebug
```

## CI/CD Pipeline

The GitHub Actions workflow (`.github/workflows/test.yml`) runs the following on every PR and push to main/develop:

### Jobs

1. **Unit Tests** - Runs all unit tests using Robolectric
   - Generates test reports
   - Publishes results as PR comments

2. **Integration Tests** - Runs integration tests
   - Tests API interactions and database operations
   - Uses MockWebServer for network mocking

3. **Code Coverage** - Generates and verifies coverage reports
   - Minimum overall coverage: **70%**
   - Minimum coverage for changed files: **70%**
   - Posts coverage report as PR comment

4. **Lint** - Runs static analysis
   - Kotlin lint (ktlint)
   - Android lint

### Coverage Configuration

Coverage is configured in `app/build.gradle.kts` using Kover:

- **Minimum Threshold**: 70% line coverage
- **Excluded Classes**:
  - BuildConfig
  - Hilt generated code (*Hilt_*, *_Factory, *_MembersInjector)
  - Dagger components
  - Compose previews
  - Generated binding classes

### Excluded Packages

- `dagger.hilt.internal`
- `androidx.compose`

## Writing Tests

### Unit Tests

Located in `app/src/test/java/`

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class MyViewModelTest {
    // Test implementation
}
```

### Integration Tests

Integration tests should be named with `Integration` suffix:

```kotlin
class PrayerTimesIntegrationTest {
    // Test API + Database integration
}
```

### Best Practices

1. Use `MockK` for mocking Kotlin classes
2. Use `kotlinx-coroutines-test` for testing coroutines
3. Use `Robolectric` for Android-specific tests
4. Mock external dependencies (APIs, databases)
5. Test both success and error cases
6. Keep tests isolated and independent

## Troubleshooting

### Tests fail in CI but pass locally

1. Check for environment-specific issues (Android SDK, Java version)
2. Ensure all resources are properly mocked
3. Check for race conditions in async code

### Coverage report not generated

```bash
./gradlew clean koverHtmlReportDebug
```

### Memory issues during testing

Add to `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
```
