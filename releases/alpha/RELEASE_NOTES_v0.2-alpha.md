# OmniCOT v0.2-alpha - Remote ID Integration Release

**Release Date:** November 4, 2025
**TAK Pipeline Submission:** `omni-COT-pipeline-20251104-161202.zip`

## 🚀 Major New Feature: Remote ID Drone Detection

This alpha release adds complete Bluetooth integration with the **gyb_detect** ESP32 device, enabling real-time drone detection and display directly in ATAK without requiring the Drone Hone APK.

### What's New

#### 🛰️ Remote ID Integration
- **Bluetooth Connectivity**: Connect to gyb_detect devices via Bluetooth Serial (SPP)
- **Drone Detection Display**: Detected drones automatically appear as air tracks on ATAK map
- **Real-time Updates**: 1 Hz position updates for detected drones
- **Full Metadata**: Serial numbers, operator IDs, altitude, speed, heading, and more
- **Multi-Protocol Support**: WiFi Beacon, WiFi NaN, and Bluetooth Legacy detection methods

#### 📱 Dashboard Enhancements
- **Remote ID Section**: New dashboard section for gyb_detect connection
- **Connection Management**: Easy connect/disconnect controls
- **Battery Monitor**: Real-time battery level display for gyb_detect device
- **Drone Counter**: Track total number of unique drones detected
- **Activity Feed**: Recent drone detections shown in activity log

#### 🔧 Technical Implementation
- **RemoteIdData Model**: Complete data model for drone detections
- **RemoteIdParser**: JSON parsing for device messages
- **BluetoothManager**: Robust Bluetooth connection handling
- **RemoteIdToCotConverter**: Converts drone data to ATAK CoT events
- **GPS Validation**: Validates drone coordinates before display

### Data Displayed for Each Drone

When a drone is detected, the following information appears on the ATAK map:

**Basic Info:**
- Serial Number or MAC Address
- Callsign (auto-generated)
- Detection timestamp

**Location Data:**
- GPS Coordinates (lat/lon)
- Altitude MSL (meters)
- Height AGL (above ground level)
- Height above takeoff point

**Motion Data:**
- Heading (degrees)
- Horizontal speed (m/s)
- Vertical speed (m/s)
- Speed accuracy

**Detection Info:**
- Detection method (WiFi/Bluetooth)
- Signal strength (RSSI in dBm)
- Horizontal/vertical accuracy

**Operator Info (if available):**
- Operator ID
- Operator location
- Operator altitude
- Location type (takeoff/live/fixed)

**Optional Fields:**
- Description text
- CAA Registration ID
- UTM ID
- Session ID

### CoT Event Format

Drones appear as:
- **Type:** `a-u-A-M-F-Q-r` (Unknown Air Track, Rotary Wing)
- **Symbol:** Yellow rotary wing air track
- **Stale Time:** 30 seconds (updates every second while detected)
- **Detail Tag:** `__remoteid` with full metadata

### Requirements

**Hardware:**
- gyb_detect ESP32 device (see main project)
- Android device with Bluetooth

**Software:**
- ATAK 5.4.0 or later
- Android with Bluetooth enabled
- Location permissions granted

**Setup:**
1. Pair gyb_detect device in Android Bluetooth settings
2. Open OmniCOT plugin in ATAK
3. Click "Connect" in Remote ID Detection section
4. Drones will appear automatically when detected

### Files Changed

**New Files:**
- `app/src/main/java/com/engindearing/omnicot/remoteid/RemoteIdData.java` (239 lines)
- `app/src/main/java/com/engindearing/omnicot/remoteid/RemoteIdParser.java` (185 lines)
- `app/src/main/java/com/engindearing/omnicot/remoteid/BluetoothManager.java` (375 lines)
- `app/src/main/java/com/engindearing/omnicot/remoteid/RemoteIdToCotConverter.java` (293 lines)
- `REMOTE_ID_INTEGRATION.md` (comprehensive documentation)

**Modified Files:**
- `app/src/main/AndroidManifest.xml` - Added Bluetooth permissions
- `app/src/main/java/com/engindearing/omnicot/DashboardActivity.java` - Bluetooth UI and handlers
- `app/src/main/java/com/engindearing/omnicot/OmniCOTDropDownReceiver.java` - CoT dispatch
- `app/src/main/res/layout/omnicot_dashboard.xml` - Remote ID UI section

**Total Changes:**
- 9 files changed
- 1,735 insertions (+)
- 3 deletions (-)

### Documentation

Comprehensive documentation available in:
- `REMOTE_ID_INTEGRATION.md` - Complete integration guide
- `README.md` - Updated with Remote ID features
- Inline code documentation

### Known Limitations

- Only one gyb_detect device can be connected at a time
- Requires manual pairing in Android Bluetooth settings
- Drones disappear after 30 seconds if not re-detected
- No drone track history (yet)

### Testing Status

⚠️ **Alpha Release** - Awaiting TAK pipeline build

This release is submitted to the TAK third-party pipeline for:
- Official ATAK 5.4.0 build
- TAK signing with official keystore
- Compatibility verification

### Next Steps

1. **Upload to TAK Pipeline**: Submit `omni-COT-pipeline-20251104-161202.zip`
2. **Wait for Build**: ~5-10 minutes for artifacts
3. **Download Signed APK**: From TAK third-party plugins page
4. **Test Installation**: Install on ATAK device
5. **Field Testing**: Test with real gyb_detect device and drones

### Future Enhancements

Planned for future releases:
- Multiple drone tracking with persistence
- Drone track history visualization
- Geofence alerts for drone detections
- Offline drone database
- Export detection logs
- Support for multiple gyb_detect devices

### Compatibility

- **ATAK Version:** 5.4.0+ (Play Store compatible)
- **Android Version:** 5.0+ (API 21+)
- **Bluetooth:** Classic Bluetooth (SPP) required
- **gyb_detect Firmware:** v0.1a or later

### Build Information

- **Submission Package:** `omni-COT-pipeline-20251104-161202.zip`
- **Package Size:** 5.8 MB
- **Build Target:** ATAK 5.4.0
- **takdevVersion:** 2.+
- **Commit:** `05162e7` - "Add Remote ID Bluetooth integration for gyb_detect device"

### Installation

1. Upload submission zip to https://tak.gov/third-party-plugins
2. Wait for TAK pipeline to build and sign
3. Download signed APK from artifacts
4. Install APK on ATAK device
5. Grant Bluetooth and Location permissions

### Support

For issues or questions:
- See `REMOTE_ID_INTEGRATION.md` for troubleshooting
- Check main project README for gyb_detect setup
- Open issue on project repository

---

## Previous Features (from v0.1)

All features from the initial release are maintained:
- CoT marker affiliation modification
- AOI (Area of Interest) management
- Dashboard with activity tracking
- Team affiliation marking
- CoT event federation

---

**Download:** After TAK pipeline build completes
**Documentation:** `REMOTE_ID_INTEGRATION.md`
**Source:** https://github.com/engindearing-projects/omni-COT
