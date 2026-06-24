# Direct Android APK Updates

This setup publishes the `selfHosted` APK flavor to the VPS and exposes update
metadata as a static JSON manifest:

```text
https://apiexpenserabbit.hearo.support/azkary/releases/update.json
```

The app checks that manifest on startup and resume. If the manifest advertises a
higher `versionCode`, the app downloads the APK, verifies its SHA-256 checksum,
and opens Android's package installer.

The stable share URL is:

```text
https://apiexpenserabbit.hearo.support/azkary/releases/Azkary-latest-selfHosted-release.apk
```

## VPS One-Time Setup

1. Create the release directory:

```bash
mkdir -p /root/services/caddy/data/site/azkary/releases
chmod -R 755 /root/services/caddy/data/site/azkary
```

2. Add the handles from `ops/app-updates/caddy-azkary-releases.caddy` inside the
existing site block in `/root/services/caddy/data/Caddyfile`, before any
catch-all `handle`.

3. Validate and reload Caddy:

```bash
docker exec caddy caddy validate --config /etc/caddy/Caddyfile
docker exec caddy caddy reload --config /etc/caddy/Caddyfile
```

## Build the Self-Hosted APK

Use the same signing key as the currently installed Azkary package so Android can
install it as an update:

```powershell
.\gradlew.bat assembleSelfHostedRelease `
  -Pandroid.injected.signing.store.file=C:\path\to\keystore.jks `
  -Pandroid.injected.signing.store.password=$env:KEYSTORE_PASSWORD `
  -Pandroid.injected.signing.key.alias=$env:KEY_ALIAS `
  -Pandroid.injected.signing.key.password=$env:KEY_PASSWORD
```

By default the `selfHosted` flavor checks:

```text
https://apiexpenserabbit.hearo.support/azkary/releases/update.json
```

Override it at build time if needed:

```powershell
.\gradlew.bat assembleSelfHostedRelease `
  -PselfHostedUpdateManifestUrl=https://your-domain.example/azkary/releases/update.json
```

## Publish a Release

From Windows, after building the signed APK:

```powershell
.\scripts\publish_android_update.ps1 `
  -ApkPath .\app\build\outputs\apk\selfHosted\release\app-selfHosted-release.apk `
  -VersionCode 21 `
  -VersionName 3.1.3 `
  -VpsTarget root@188.245.185.63 `
  -ReleaseNotes "Improved prayer-time refresh|Fixed update checks"
```

The script uploads:

- `Azkary-v<versionName>-<versionCode>-selfHosted-release.apk`
- matching `.sha256` files
- `Azkary-latest-selfHosted-release.apk`
- `update.json`

If `-ReleaseNotes` is omitted, the script tries to read bullet points from the
matching `CHANGELOG.md` section.

## Manifest Format

```json
{
  "enabled": true,
  "latestVersionCode": 21,
  "latestVersionName": "3.1.3",
  "minSupportedVersionCode": 20,
  "apkUrl": "https://apiexpenserabbit.hearo.support/azkary/releases/Azkary-v3.1.3-21-selfHosted-release.apk",
  "sha256": "<sha256>",
  "mandatory": false,
  "releaseNotes": [
    "Improved prayer-time refresh",
    "Fixed update checks"
  ]
}
```

Set `mandatory` to `true`, or set `minSupportedVersionCode` higher than the
installed build, to block app usage until the update installer is opened.
