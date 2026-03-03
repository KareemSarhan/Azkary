# Fastlane Configuration

This directory contains Fastlane configuration for automating Android deployments.

## Setup

### Prerequisites

1. Install Ruby (2.5 or newer)
2. Install Bundler: `gem install bundler`
3. Install dependencies: `bundle install`

### Quick Start

```bash
# Build debug APK
fastlane build_debug

# Build release APK
fastlane build_apk

# Prepare F-Droid release
fastlane fdroid_release

# Validate metadata
fastlane validate_fdroid_metadata
```

## Available Lanes

### Build Lanes

- `build_debug` - Build debug APK
- `build_apk` - Build signed release APK
- `build_aab` - Build signed release AAB (Google Play)
- `fdroid_build` - Build F-Droid compatible APK

### F-Droid Lanes

- `fdroid_prepare` - Prepare F-Droid release metadata
- `validate_fdroid_metadata` - Validate metadata structure
- `fdroid_build` - Build and verify F-Droid APK
- `fdroid_release` - Complete F-Droid release process
- `generate_changelog` - Generate changelog from git history

### Utility Lanes

- `check` - Run lint and tests

## Configuration Files

- `Appfile` - App configuration (package name, etc.)
- `Fastfile` - Lane definitions
- `metadata/` - Store listing metadata

## F-Droid Integration

F-Droid automatically reads metadata from:
- `fastlane/metadata/android/en-US/title.txt`
- `fastlane/metadata/android/en-US/short_description.txt`
- `fastlane/metadata/android/en-US/full_description.txt`
- `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
- `fastlane/metadata/android/en-US/images/`

No manual submission needed - F-Droid polls the repository for updates.

## Environment Variables

For signed builds, set these environment variables:

```bash
export KEYSTORE_PATH="path/to/keystore.jks"
export KEYSTORE_PASSWORD="keystore_password"
export KEY_ALIAS="key_alias"
export KEY_PASSWORD="key_password"
```

## GitHub Actions Integration

Three workflows are configured:

1. **CI** (`.github/workflows/ci.yml`) - Lint and build on PR
2. **Release** (`.github/workflows/release.yml`) - Build and create GitHub releases
3. **F-Droid** (`.github/workflows/fdroid.yml`) - Validate F-Droid metadata

## Documentation

- [Fastlane Documentation](https://docs.fastlane.tools/)
- [F-Droid Fastlane Integration](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/)
- [F-Droid Build Metadata](https://f-droid.org/docs/Build_Metadata_Reference/)
