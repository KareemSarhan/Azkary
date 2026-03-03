# Contributing to Azkary

Thank you for your interest in contributing to Azkary! This document provides guidelines and instructions for contributing to this project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Commit Messages](#commit-messages)
- [Reporting Bugs](#reporting-bugs)
- [Requesting Features](#requesting-features)

## Code of Conduct

This project adheres to a [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to [azkary@hearo.support](mailto:azkary@hearo.support).

## How Can I Contribute?

### Reporting Bugs

Before creating a bug report, please check the existing issues to see if the problem has already been reported. When you are creating a bug report, please include as many details as possible:

- **Use a clear and descriptive title**
- **Describe the exact steps to reproduce the problem**
- **Provide specific examples to demonstrate the steps**
- **Describe the behavior you observed and what behavior you expected**
- **Include screenshots or recordings if applicable**
- **Include your device information** (Android version, device model, app version)

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, please include:

- **Use a clear and descriptive title**
- **Provide a step-by-step description of the suggested enhancement**
- **Provide specific examples to demonstrate the enhancement**
- **Explain why this enhancement would be useful**

### Pull Requests

1. Fork the repository
2. Create a new branch from `main` for your feature or bug fix
3. Make your changes
4. Ensure your code follows our coding standards
5. Test your changes thoroughly
6. Update documentation if necessary
7. Submit a pull request

## Development Setup

### Prerequisites

- Android Studio Ladybug | 2024.2.1 or newer
- JDK 17
- Android SDK 36
- Git

### Building the Project

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/azkary.git
cd azkary

# Open in Android Studio or build from command line
./gradlew assembleDebug
```

## Pull Request Process

1. Update the README.md with details of changes to the interface, if applicable
2. Ensure all tests pass (when test suite is added)
3. Update the version numbers following [SemVer](https://semver.org/):
   - MAJOR version for incompatible API changes
   - MINOR version for backwards-compatible functionality additions
   - PATCH version for backwards-compatible bug fixes
4. Your pull request will be reviewed by maintainers who may request changes
5. Once approved, your pull request will be merged

## Coding Standards

### Kotlin Style Guide

We follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with these additions:

- **Indentation**: 4 spaces (no tabs)
- **Maximum line length**: 120 characters
- **Naming**:
  - Classes and objects: PascalCase
  - Functions and properties: camelCase
  - Constants: UPPER_SNAKE_CASE
  - Private properties: _camelCase with leading underscore

### Android Specific

- Use ViewModel for UI-related data
- Use Repository pattern for data operations
- Use Hilt for dependency injection
- Follow MVVM architecture
- Use Jetpack Compose for UI
- Support both RTL and LTR layouts

### Code Organization

```
app/src/main/java/com/app/azkary/
├── data/           # Data layer (repositories, database, API)
├── di/             # Dependency injection modules
├── domain/         # Domain models and business logic
├── ui/             # UI layer (screens, viewmodels)
└── util/           # Utility classes
```

## Commit Messages

Use clear and meaningful commit messages. Follow these guidelines:

- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters or less
- Reference issues and pull requests liberally after the first line

Example:
```
Add haptic feedback to counter button

- Implement vibration on increment
- Add user preference to toggle feedback
- Fixes #123
```

## Reporting Bugs

When reporting bugs, please use the bug report template and include:

### Template

```markdown
**Description:**
Clear description of the bug

**Steps to Reproduce:**
1. Go to '...'
2. Click on '...'
3. Scroll down to '...'
4. See error

**Expected Behavior:**
What you expected to happen

**Actual Behavior:**
What actually happened

**Screenshots:**
If applicable, add screenshots

**Device Information:**
- Device: [e.g., Samsung Galaxy S23]
- OS: [e.g., Android 14]
- App Version: [e.g., 2.0]
- Language: [e.g., Arabic]
```

## Requesting Features

When requesting features, please describe:

- The use case or problem you're trying to solve
- Your proposed solution
- Any alternative solutions you've considered
- Whether you'd be willing to implement this feature yourself

## Questions?

Feel free to open a [Discussion](https://github.com/KareemSarhan/Azkary/discussions) if you have questions about contributing.

Thank you for contributing to Azkary!
