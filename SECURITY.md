# Security Policy

## Supported Versions

The following versions of Azkary are currently supported with security updates:

| Version | Supported          |
| ------- | ------------------ |
| 2.x     | :white_check_mark: |
| 1.x     | :x:                |

## Reporting a Vulnerability

We take the security of Azkary seriously. If you discover a security vulnerability, please follow these steps:

### 1. Do Not Open a Public Issue

Please **DO NOT** create a public GitHub issue for security vulnerabilities. This could expose the vulnerability to malicious actors before we have a chance to fix it.

### 2. Contact Us Privately

Email us directly at: **azkary@hearo.support**

Please include:
- A description of the vulnerability
- Steps to reproduce the issue
- Potential impact of the vulnerability
- Any suggested fixes or mitigations

### 3. Response Timeline

- **Initial Response**: Within 48 hours
- **Assessment Complete**: Within 7 days
- **Fix Released**: As soon as possible, depending on severity

### 4. Disclosure Policy

Once the vulnerability is fixed:
- We will credit you in the release notes (if you wish)
- We will publish a security advisory on GitHub
- We will release the fix through F-Droid and GitHub Releases

## Security Measures

Azkary is designed with security and privacy in mind:

### Data Storage
- All data is stored locally on your device using Android's secure storage
- No data is transmitted to any servers
- No analytics or tracking services are used

### Permissions
Azkary requests minimal permissions:
- **Location** (optional): Only used for prayer time calculation. Can be denied without affecting core functionality.
- **Internet** (optional): Only used for fetching prayer times. Works fully offline after initial sync.

### Code Quality
- Static analysis with Android Lint
- Dependency scanning with Gradle
- Regular updates to dependencies

## Known Security Considerations

### Local Data
Azkary stores all data locally. While this ensures privacy, it also means:
- Data is accessible if the device is compromised
- No remote wipe capability
- Users should enable device encryption for additional security

### Prayer Times API
When fetching prayer times:
- Only approximate location (latitude/longitude) is sent to Aladhan API
- No personal identifiers are transmitted
- HTTPS is used for all network requests

## Best Practices for Users

To ensure the security of your Azkary data:

1. **Keep your device updated** with the latest Android security patches
2. **Enable device encryption** in Android settings
3. **Use a strong lock screen** (PIN, pattern, or password)
4. **Download only from official sources**:
   - F-Droid (recommended)
   - GitHub Releases
5. **Verify APK signatures** when sideloading

## Security-Related Configuration

### Disabling Location
If you prefer not to share location data:
- Go to Settings → Prayer Times
- Toggle off "Use location for prayer times"
- The app will work normally without location data

### Offline Mode
Azkary works completely offline:
- Prayer times can be cached locally
- All Azkar content is bundled with the app
- No internet connection is ever required

## Third-Party Dependencies

Azkary uses the following security-sensitive dependencies:

| Dependency | Purpose | Security Tracking |
|------------|---------|-------------------|
| AndroidX Security | Encrypted preferences | Google Security Bulletins |
| Room | Local database | Google Security Bulletins |
| Retrofit/OkHttp | Network requests | Square Security Advisories |

All dependencies are regularly updated to their latest secure versions.

## Acknowledgments

We thank the following individuals for responsibly disclosing security issues:

*No vulnerabilities have been reported yet. Be the first to help us improve!*

---

Last updated: 2024
