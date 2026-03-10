# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [3.1.2] - 2025-03-10

### Fixed
- Removed unnecessary alarm permissions (`USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM`) — the app no longer requests permissions it doesn't need, improving your privacy and reducing the permission footprint
- Completed and cleaned up Azkar content for both Arabic and English — several remembrances now display the correct full text and repetition counts
- Sleep Surah recitations are now listed as individual items instead of grouped together, making it easier to track each one separately

### Internal
- Updated release automation to correctly handle Play Store and F-Droid build variants

## [3.1.0] - 2025-02-XX

### Added
- GitHub Actions automated release workflow
- CI workflow for pull requests

## [3.0.0] - 2024-XX-XX

### Added
- Comprehensive project documentation (README, CONTRIBUTING, CODE_OF_CONDUCT, SECURITY)
- F-Droid metadata structure
- GitHub release automation
- Automated CI/CD pipeline

### Changed
- Bumped version to 3.0.0
- Updated build configuration for automated signing

## [2.0.0] - Previous Release

### Added
- Initial release with core Azkar functionality
- Prayer times integration
- Progress tracking
- Multi-language support (Arabic/English)
- Custom categories

[Unreleased]: https://github.com/KareemSarhan/Azkary/compare/v3.1.2...HEAD
[3.1.2]: https://github.com/KareemSarhan/Azkary/compare/v3.1.0...v3.1.2
[3.1.0]: https://github.com/KareemSarhan/Azkary/compare/v3.0.0...v3.1.0
[3.0.0]: https://github.com/KareemSarhan/Azkary/compare/v2.0.0...v3.0.0
[2.0.0]: https://github.com/KareemSarhan/Azkary/releases/tag/v2.0.0
