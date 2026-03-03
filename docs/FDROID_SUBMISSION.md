# F-Droid Submission Guide

This document provides instructions for submitting Azkary to F-Droid.

## Prerequisites

- [ ] App is fully FOSS (no proprietary dependencies in F-Droid flavor)
- [ ] Source code is publicly available on GitHub
- [ ] Release tags are created for each version
- [ ] Reproducible builds are set up
- [ ] Fastlane metadata is complete

## FOSS Compliance Status

### Dependencies Review

| Dependency | F-Droid Compatible | Notes |
|------------|-------------------|-------|
| Kotlin stdlib | ✅ Yes | Apache 2.0 |
| AndroidX libraries | ✅ Yes | Apache 2.0 |
| Material Design 3 | ✅ Yes | Apache 2.0 |
| Jetpack Compose | ✅ Yes | Apache 2.0 |
| Room (SQLite) | ✅ Yes | Apache 2.0 |
| DataStore | ✅ Yes | Apache 2.0 |
| Hilt/Dagger | ✅ Yes | Apache 2.0 |
| Retrofit | ✅ Yes | Apache 2.0 |
| OkHttp | ✅ Yes | Apache 2.0 |
| Kotlinx Serialization | ✅ Yes | Apache 2.0 |
| **Play Services Location** | ❌ No (proprietary) | Excluded via `fdroid` product flavor |

### Product Flavors

The app uses two product flavors to handle the proprietary dependency:

- **`fdroid`**: FOSS-only flavor using Android's `LocationManager` for location
- **`play`**: Play Store flavor using Google Play Services for location

## Build Verification

Before submitting to F-Droid, verify the build:

```bash
# Build F-Droid flavor
./gradlew assembleFdroidRelease

# Verify APK location
ls -la app/build/outputs/apk/fdroid/release/

# Check APK for proprietary libraries (should return empty)
unzip -l app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk | grep -i "play"
```

## F-Droid Metadata File

A metadata template is provided at `metadata/com.app.azkary.fdroid.txt`. This file will be submitted to the [fdroiddata repository](https://gitlab.com/fdroid/fdroiddata).

## Submission Steps

### Step 1: Create a Release

1. Update `versionCode` and `versionName` in `app/build.gradle.kts`
2. Create a git tag: `git tag v3.0.0`
3. Push the tag: `git push origin v3.0.0`
4. GitHub Actions will build and create a release

### Step 2: Submit to F-Droid Data

1. Fork the [fdroiddata repository](https://gitlab.com/fdroid/fdroiddata)
2. Copy `metadata/com.app.azkary.fdroid.txt` to `metadata/com.app.azkary.fdroid.yml` (convert to YAML format)
3. Create a merge request to fdroiddata
4. Wait for F-Droid maintainers to review and merge

### Step 3: Metadata Format (YAML)

The final submission should be in YAML format:

```yaml
Categories:
  - Religion
License: GPL-3.0-only
AuthorName: Kareem Sarhan
AuthorWebSite: https://github.com/KareemSarhan
WebSite: https://github.com/KareemSarhan/Azkary
SourceCode: https://github.com/KareemSarhan/Azkary
IssueTracker: https://github.com/KareemSarhan/Azkary/issues
Changelog: https://github.com/KareemSarhan/Azkary/releases

AutoName: Azkary
Summary: Islamic remembrance (Azkar) app with prayer times
Description: |-
    Azkary is a privacy-focused Islamic remembrance app...

RepoType: git
Repo: https://github.com/KareemSarhan/Azkary

Builds:
  - versionName: 3.0.0
    versionCode: 10
    commit: v3.0.0
    subdir: app
    gradle:
      - fdroid
    output: build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
CurrentVersion: 3.0.0
CurrentVersionCode: 10
```

## Verification Checklist

Before submitting:

- [ ] F-Droid flavor builds successfully
- [ ] No proprietary libraries in F-Droid APK
- [ ] Fastlane metadata complete (en-US, ar)
- [ ] Changelogs exist for current versionCode
- [ ] Screenshots are available (or will be provided)
- [ ] LICENSE file is present (GPL-3.0)
- [ ] Source code matches release tags
- [ ] App functions correctly without Google Play Services

## Timeline

1. **Preparation**: Complete (this PR)
2. **Submission**: After PR is merged and tagged release
3. **Review**: F-Droid maintainers review (typically 1-2 weeks)
4. **Publication**: Available on F-Droid after merge

## Resources

- [F-Droid Inclusion Policy](https://f-droid.org/en/docs/Inclusion_Policy/)
- [F-Droid Build Metadata Reference](https://f-droid.org/en/docs/Build_Metadata_Reference/)
- [F-Droid Fastlane Structure](https://f-droid.org/en/docs/All_About_Descriptions_Graphics_and_Screenshots/)
