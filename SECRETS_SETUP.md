# GitHub Secrets Setup Guide

This guide explains how to set up the required GitHub Secrets for automated releases.

## Required Secrets

The following secrets must be added to your GitHub repository for the release workflow to function:

| Secret Name | Description |
|-------------|-------------|
| `KEYSTORE_BASE64` | Base64-encoded keystore file |
| `KEYSTORE_PASSWORD` | Password for the keystore file |
| `KEY_ALIAS` | Alias of the signing key |
| `KEY_PASSWORD` | Password for the signing key |

## Setup Instructions

### Step 1: Encode Your Keystore

Open a terminal (Git Bash, PowerShell, or WSL) and run:

```bash
# Navigate to your keystore directory
cd C:\Dev\keys

# Encode the keystore to base64 (Git Bash / WSL)
base64 -w 0 AndroidKsKeyStore > keystore_base64.txt

# Or using PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\Dev\keys\AndroidKsKeyStore")) | Out-File -Encoding ASCII keystore_base64.txt
```

Copy the contents of `keystore_base64.txt` — this is your `KEYSTORE_BASE64` secret.

### Step 2: Add Secrets to GitHub

1. Go to your GitHub repository: https://github.com/KareemSarhan/Azkary
2. Click on **Settings** tab
3. In the left sidebar, click **Secrets and variables** → **Actions**
4. Click **New repository secret**
5. Add each secret one by one:

#### Secret 1: KEYSTORE_BASE64
- **Name**: `KEYSTORE_BASE64`
- **Value**: Paste the base64-encoded keystore content from Step 1

#### Secret 2: KEYSTORE_PASSWORD
- **Name**: `KEYSTORE_PASSWORD`
- **Value**: Your keystore password

#### Secret 3: KEY_ALIAS
- **Name**: `KEY_ALIAS`
- **Value**: Your key alias (e.g., `key0` or `upload`)

#### Secret 4: KEY_PASSWORD
- **Name**: `KEY_PASSWORD`
- **Value**: Your key password (may be the same as keystore password)

### Step 3: Verify Setup

After adding all secrets, you can verify the workflow is configured correctly by:

1. Going to **Actions** tab in your repository
2. Click on **Release** workflow
3. Click **Run workflow** → **Run workflow** (manual trigger)
4. Or push a tag: `git tag v3.0.0 && git push origin v3.0.0`

## Finding Your Keystore Details

If you don't remember your key alias or passwords, you can check:

### From Android Studio:
1. Open Android Studio
2. **Build** → **Generate Signed Bundle/APK**
3. The dialog will show your keystore path and alias

### Using Command Line:
```bash
# List aliases in keystore
keytool -list -v -keystore C:\Dev\keys\AndroidKsKeyStore
```

## Security Notes

- ⚠️ **Never commit your keystore file to git**
- ⚠️ **Never share your keystore passwords**
- ✅ GitHub Secrets are encrypted and only available to workflow runs
- ✅ The keystore is decoded only during the build process and immediately deleted

## Troubleshooting

### "Keystore file not found" error
- Verify `KEYSTORE_BASE64` is correctly encoded
- Check that the base64 string doesn't have newlines

### "Failed to read key" error
- Verify `KEY_ALIAS` matches exactly (case-sensitive)
- Verify `KEY_PASSWORD` is correct

### "Wrong password" error
- Verify `KEYSTORE_PASSWORD` is correct
- Try using the same password for both keystore and key

## Need Help?

If you encounter issues:
1. Check the workflow logs in **Actions** tab
2. Verify all secrets are set correctly
3. Ensure the keystore file works locally: `./gradlew assembleRelease`
