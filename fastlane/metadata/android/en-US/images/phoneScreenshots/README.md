# Screenshots for F-Droid

Place app screenshots in this directory for F-Droid display.

## Naming Convention

Name screenshots descriptively to indicate the feature shown:
- `01_home.png` - Main/home screen
- `02_azkar_morning.png` - Morning azkar screen
- `03_azkar_evening.png` - Evening azkar screen
- `04_progress.png` - Progress tracking
- `05_settings.png` - Settings screen
- `06_dark_mode.png` - Dark mode example

## Requirements

- Format: PNG preferred (JPG accepted)
- Resolution: 320px minimum width, 3840px maximum width
- Aspect ratio: Keep consistent across all screenshots
- Content: Show actual app content (not mockups)
- Language: English screenshots for en-US locale

## Screenshot Sizes

F-Droid recommends:
- Phone screenshots: 1080x1920 or similar portrait ratio
- 7-inch tablet: 1200x1920
- 10-inch tablet: 1600x2560

## Tips

1. Use clean device frames or no frames
2. Show the app in light and dark modes
3. Highlight key features
4. Keep text legible
5. Avoid sensitive/personal information

## Generating Screenshots

You can generate screenshots automatically using Fastlane:

```bash
fastlane screengrab
```

Or manually:
1. Build and install the debug APK
2. Navigate to each screen
3. Take screenshots
4. Copy to this directory
