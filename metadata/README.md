# F-Droid Distribution

This directory contains metadata for F-Droid distribution.

## Metadata File

`metadata/com.app.azkary.fdroid.yml` - F-Droid metadata file

## Submitting to F-Droid

### Option 1: Forum Request (Recommended)

1. Visit [F-Droid Forum - Requests](https://forum.f-droid.org/c/requests/9)
2. Create a new topic with:
   - Title: `[Request] Azkary`
   - Content: App description, link to GitHub repo, license info
3. Include a link to the metadata file in your repo

### Option 2: Merge Request

1. Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata)
2. Copy `metadata/com.app.azkary.fdroid.yml` to `metadata/com.app.azkary.fdroid.yml`
3. Run `fdroid checkupdates com.app.azkary.fdroid`
4. Run `fdroid lint com.app.azkary.fdroid`
5. Submit merge request

## Build Configuration

The app has an `fdroid` build flavor that:
- Uses `com.app.azkary.fdroid` as package name
- Excludes Google Play Services dependencies
- Excludes in-app update/review features

Build command:
```bash
./gradlew assembleFdroidRelease
```

## Requirements Checklist

- [x] FOSS License (GPL-3.0)
- [x] Source code publicly available
- [x] No proprietary dependencies in fdroid flavor
- [x] Reproducible builds (no signing config in gradle)
- [x] F-Droid metadata file

## After Inclusion

Once accepted:
- F-Droid will automatically build and sign new releases
- Updates are triggered by new Git tags (vX.X.X format)
- F-Droid signs with their own key (different from Play Store)
