# F-Droid Metadata

This directory contains metadata used by F-Droid to list and display the app.

## Structure

```
fastlane/metadata/android/en-US/
├── title.txt              # App title (max 50 chars)
├── short_description.txt  # Short description (max 80 chars)
├── full_description.txt   # Full description (HTML allowed)
├── changelogs/            # Changelogs per versionCode
│   ├── 10.txt
│   └── ...
└── images/                # Screenshots and graphics
    ├── featureGraphic.png
    ├── icon.png
    └── phoneScreenshots/
        ├── 01_home.png
        ├── 02_azkar.png
        └── ...
```

## F-Droid Configuration Notes

### Auto-Update
F-Droid automatically checks for new versions by:
1. Looking at the `versionCode` in `app/build.gradle.kts`
2. Finding the corresponding changelog in `changelogs/<versionCode>.txt`
3. Building from the source code

### Build Process
F-Droid builds the APK from source, so:
- No need to upload APKs to F-Droid
- Ensure `versionCode` is incremented for each release
- Ensure changelog exists for each new versionCode

### Metadata Updates
Changes to metadata (screenshots, descriptions) are picked up by F-Droid
on their next metadata update cycle (typically within a few days).

## Links
- F-Droid Documentation: https://f-droid.org/docs/
- Fastlane Supply: https://docs.fastlane.tools/actions/supply/
