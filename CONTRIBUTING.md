# Contributing to OmniCOT

Thank you for your interest in contributing to OmniCOT! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

- [Development Setup](#development-setup)
- [Building the Plugin](#building-the-plugin)
- [Code Style](#code-style)
- [Submitting Changes](#submitting-changes)
- [Testing](#testing)
- [Issue Reporting](#issue-reporting)

## Development Setup

### Prerequisites

1. **ATAK-CIV SDK**
   - Download ATAK-CIV SDK 5.5.0 or later from [TAK.gov](https://tak.gov)
   - Extract the SDK to a known location

2. **Development Tools**
   - Android Studio (latest stable version recommended)
   - Java Development Kit (JDK) 17
   - Git

3. **Android SDK**
   - Android SDK 21+ (Android 5.0)
   - Target SDK 34
   - Build tools compatible with Gradle 8.13

### Initial Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/engindearing-projects/omni-COT.git
   cd omni-COT
   ```

2. Create `local.properties` in the project root:
   ```properties
   sdk.dir=/path/to/Android/Sdk
   takdev.plugin=/path/to/ATAK-CIV-SDK/atak-gradle-takdev.jar
   ```

   Replace paths with your actual SDK locations.

3. Open the project in Android Studio:
   - File → Open → Select the `omnicot` folder
   - Wait for Gradle sync to complete

## Building the Plugin

### Debug Build

For development and testing:

```bash
./gradlew assembleCivDebug
```

Output: `app/build/outputs/apk/civ/debug/ATAK-Plugin-omnicot-0.1--5.5.0-civ-debug.apk`

### Release Build

For production:

```bash
./gradlew assembleCivRelease
```

Output: `app/build/outputs/apk/civ/release/ATAK-Plugin-omnicot-0.1--5.5.0-civ-release.apk`

### Installing to Device

```bash
adb install -r app/build/outputs/apk/civ/debug/ATAK-Plugin-omnicot-*.apk
```

## Code Style

### Java Conventions

- Follow standard Java naming conventions
- Use meaningful variable and method names
- Keep methods focused and concise
- Add Javadoc comments for public APIs

### Example

```java
/**
 * Updates the dashboard statistics display.
 * Refreshes AOI count, alert count, and COT modification count.
 */
public void updateStats() {
    int aoiCount = getAOICount();
    txtActiveAOIs.setText(String.valueOf(aoiCount));
    // ...
}
```

### XML Formatting

- Use 4-space indentation
- Keep attributes on separate lines for readability
- Use descriptive IDs: `@+id/btnRefreshAoi` not `@+id/button1`

### File Organization

```
app/src/main/java/com/engindearing/omnicot/
├── PluginTemplate.java          # Plugin lifecycle
├── OmniCOTTool.java             # Toolbar integration
├── OmniCOTMapComponent.java     # Map component lifecycle
├── OmniCOTDropDownReceiver.java # UI controller
├── DashboardActivity.java       # Dashboard management
├── AlertConfigDialog.java       # Alert configuration
├── AOIAdapter.java              # RecyclerView adapter
└── AOIItem.java                 # Data model
```

## Submitting Changes

### Pull Request Process

1. **Fork the repository** and create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes** following the code style guidelines

3. **Test thoroughly**:
   - Build succeeds without errors
   - Plugin loads in ATAK
   - Features work as expected
   - No regressions in existing functionality

4. **Commit with clear messages**:
   ```bash
   git commit -m "Add geofence entry/exit notifications

   - Implement GeofenceMonitor service
   - Add notification preferences
   - Update AlertConfigDialog UI
   - Add tests for geofence detection"
   ```

5. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Create a Pull Request**:
   - Provide clear description of changes
   - Reference any related issues
   - Include screenshots for UI changes
   - List testing performed

### Commit Message Guidelines

- Use present tense: "Add feature" not "Added feature"
- First line summary (50 chars or less)
- Blank line, then detailed description
- Reference issues: "Fixes #123" or "Relates to #456"

## Testing

### Manual Testing

Before submitting, verify:

1. **Plugin Installation**
   - Plugin installs without errors
   - Icon appears in ATAK toolbar

2. **Dashboard**
   - Dashboard opens when button clicked
   - Statistics display correctly
   - Quick action cards respond

3. **CoT Management**
   - Map selection works
   - Affiliation changes apply
   - Changes federate to other devices

4. **AOI Detection**
   - Shapes detected from map
   - Count updates correctly
   - List displays properly

### Testing Checklist

- [ ] Builds successfully (debug and release)
- [ ] No compilation warnings
- [ ] Plugin loads in ATAK
- [ ] UI renders correctly on different screen sizes
- [ ] No crashes during normal operation
- [ ] Features work as documented
- [ ] Changes don't break existing features

## Issue Reporting

### Bug Reports

Include:

- **Environment**: ATAK version, Android version, device model
- **Steps to reproduce**: Detailed, numbered steps
- **Expected behavior**: What should happen
- **Actual behavior**: What actually happened
- **Logs**: Relevant logcat output (use `adb logcat`)
- **Screenshots**: If applicable

### Feature Requests

Include:

- **Use case**: Why this feature is needed
- **Proposed solution**: How it could work
- **Alternatives**: Other approaches considered
- **Additional context**: Any relevant information

### Example Bug Report

```markdown
**Environment**
- ATAK-CIV 5.5.0
- Android 12
- Samsung Galaxy S21

**Steps to Reproduce**
1. Open OmniCOT dashboard
2. Tap "COT Management"
3. Select a marker on the map
4. Change affiliation to "Hostile"

**Expected**: Marker affiliation changes and federates
**Actual**: App crashes with NullPointerException

**Logs**
```
10-20 16:47:46.479 E/OmniCOT: NullPointerException at line 123
...
```
```

## Questions?

- Check existing [Issues](../../issues)
- Review [Documentation](README.md)
- Contact: j@engindearing.soy

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
