# OmniCOT - TAK Third-Party Plugin Submission

## Plugin Information
- **Plugin Name**: OmniCOT
- **Version**: 0.1
- **Target ATAK Version**: 5.5.0 (CIV, MIL, GOV compatible)
- **Package Name**: com.engindearing.omnicot
- **Description**: Advanced tactical coordination plugin featuring modern dashboard UI, CoT marker management, AOI detection, and geofence-based alerting capabilities

## Build Instructions

### Prerequisites
- Java 17 (OpenJDK or equivalent)
- Android SDK
- ATAK SDK 5.5.0+ (fetched via atak-gradle-takdev plugin)

### Build Commands
```bash
# Navigate to plugin directory
cd omnicot

# Build debug version
./gradlew assembleCivDebug

# Build release version (for all flavors)
./gradlew assembleCivRelease
./gradlew assembleMilRelease
./gradlew assembleGovRelease

# Output APK locations:
# - Debug: app/build/outputs/apk/civ/debug/ATAK-Plugin-omnicot-0.1--5.5.0-civ-debug.apk
# - Release: app/build/outputs/apk/civ/release/ATAK-Plugin-omnicot-0.1--5.5.0-civ-release.apk
```

## Key Features
1. **Modern Dashboard Interface**: Card-based UI with real-time status metrics
2. **CoT Marker Management**: Change marker affiliations (Friend/Neutral/Hostile/Unknown) and dimensions (Point/Air/Ground/Sea/Subsurface)
3. **AOI Detection**: Automatic detection and listing of Areas of Interest from map shapes
4. **Alert System Foundation**: Infrastructure for geofence-based notifications
5. **Activity Tracking**: Monitor recent plugin operations

## Technical Implementation

### Source Files
- `PluginTemplate.java` - Main plugin lifecycle management (extends AbstractPlugin)
- `OmniCOTTool.java` - Toolbar button integration
- `OmniCOTMapComponent.java` - Map component lifecycle and receiver registration
- `OmniCOTDropDownReceiver.java` - Dashboard UI controller
- `DashboardActivity.java` - Dashboard statistics and state management
- `AlertConfigDialog.java` - Alert configuration interface
- `AOIAdapter.java` / `AOIItem.java` - RecyclerView adapter for AOI list

### ATAK APIs Used
- `AbstractPlugin` - Plugin lifecycle management
- `AbstractPluginTool` - Toolbar button integration
- `DropDownMapComponent` - Dropdown UI framework
- `DropDownReceiver` - Broadcast receiver for dropdown display
- `CotDispatcher` - For broadcasting CoT events
- `CotEventFactory` - Creating CoT events from map items
- `MapGroup.deepFindItems()` - AOI detection from map shapes

### CoT Type Modification
Based on **CoT Event Schema** and **MIL-STD-2525D**, the plugin modifies affiliations and dimensions in the CoT type string:

**Affiliation (position 2)**:
- `a-f-G-E-V` = Friendly ground equipment vehicle
- `a-n-G-E-V` = Neutral ground equipment vehicle
- `a-h-G-E-V` = Hostile ground equipment vehicle
- `a-u-G-E-V` = Unknown ground equipment vehicle

**Dimension (position 4)**:
- `a-f-P-E-V` = Point
- `a-f-A-E-V` = Air
- `a-f-G-E-V` = Ground
- `a-f-S-E-V` = Sea Surface
- `a-f-U-E-V` = Subsurface

All other type information is preserved during modification.

**Standards References**:
- CoT Message Standard (DoD INST) - January 2025
- MIL-STD-2525D - Joint Military Symbology

## Security Considerations
- No network connections initiated by plugin (uses ATAK's CoT dispatcher)
- Uses standard ATAK CoT dispatcher for all communications
- No sensitive data storage
- No special permissions required beyond standard ATAK plugin permissions
- All user data kept within ATAK's existing storage mechanisms

## Testing Performed
- [x] Plugin loads successfully in ATAK CIV 5.5.0
- [x] Dashboard displays correctly
- [x] CoT marker selection and modification works
- [x] Affiliation changes federate via CoT dispatcher
- [x] AOI detection from map shapes functions correctly
- [x] UI renders properly on different screen sizes
- [x] No crashes during normal operation

## Known Limitations
1. Alert system is foundation only - full geofence monitoring not yet implemented
2. Export/import functionality not yet implemented
3. Historical activity logging is in-memory only (not persisted)
4. No permission controls - any user can modify any marker

## Dependencies
- AndroidX RecyclerView 1.3.2
- AndroidX Annotation 1.8.2
All other dependencies provided by ATAK SDK. No external libraries required.

## ProGuard Configuration
The plugin uses standard ATAK ProGuard rules with repackaging to `atakplugin.omnicot`:
```
-repackageclasses atakplugin.omnicot
```

This is automatically configured in `build.gradle` via:
```groovy
afterEvaluate {
    project.file('proguard-gradle-repackage.txt').text = "-repackageclasses atakplugin.${rootProject.getName()}"
}
```

## Support
For questions or issues with this plugin submission:
- GitHub Issues: https://github.com/engindearing-projects/omni-COT/issues
- Email: j@engindearing.soy
- Check ATAK logs for tags: `OmniCOTMapComponent`, `OmniCOTDropDownReceiver`, `OmniCOTTool`, `DashboardActivity`

## Signing Instructions
Please sign with TAK standard plugin signing certificate. No special signing requirements.

---
**Submission Date**: October 20, 2025
**Built Against**: ATAK-CIV-5.5.1.6-SDK
**Tested On**: ATAK CIV 5.5.0
