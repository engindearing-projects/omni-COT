# OmniCOT v0.2-alpha Release Notes

Release Date: November 4, 2025
Status: Pre-release (Alpha)
TAK Pipeline Submission: `omni-COT-pipeline-20251104-161202.zip`

## Changes in v0.2-alpha

### Major Feature: Remote ID Drone Detection
Complete Bluetooth integration with gyb_detect ESP32 device for real-time drone detection in ATAK without requiring separate apps.

### New Components
- RemoteIdData.java - Data model for drone detections (239 lines)
- RemoteIdParser.java - JSON parsing for device messages (185 lines)
- BluetoothManager.java - Bluetooth connection handling (375 lines)
- RemoteIdToCotConverter.java - Converts drone data to CoT events (293 lines)
- REMOTE_ID_INTEGRATION.md - Comprehensive documentation

### Modified Files
- AndroidManifest.xml - Added Bluetooth permissions
- DashboardActivity.java - Bluetooth UI and handlers
- OmniCOTDropDownReceiver.java - CoT dispatch integration
- omnicot_dashboard.xml - Remote ID UI section

### Total Changes
- 9 files changed
- 1,735 insertions
- 3 deletions

## Features

### Bluetooth Connectivity
- Connect to gyb_detect devices via Bluetooth Serial (SPP)
- Real-time 1 Hz position updates
- Battery level monitoring
- Connection status management

### Drone Detection Display
- Drones appear as air tracks on ATAK map
- Full metadata: serial numbers, operator IDs, altitude, speed, heading
- Multi-protocol support: WiFi Beacon, WiFi NaN, Bluetooth Legacy detection
- GPS coordinate validation before display

### Data Displayed Per Drone

Basic Info:
- Serial number or MAC address
- Auto-generated callsign
- Detection timestamp

Location Data:
- GPS coordinates (lat/lon)
- Altitude MSL (meters)
- Height AGL (above ground level)
- Height above takeoff point

Motion Data:
- Heading (degrees)
- Horizontal speed (m/s)
- Vertical speed (m/s)
- Speed accuracy

Detection Info:
- Detection method (WiFi/Bluetooth)
- Signal strength (RSSI in dBm)
- Horizontal/vertical accuracy

Operator Info (if available):
- Operator ID
- Operator location
- Operator altitude
- Location type (takeoff/live/fixed)

Optional Fields:
- Description text
- CAA Registration ID
- UTM ID
- Session ID

### CoT Event Format
- Type: `a-u-A-M-F-Q-r` (Unknown Air Track, Rotary Wing)
- Yellow rotary wing air track symbol
- 30 second stale time
- 1 second update interval while detected
- Full metadata in `__remoteid` detail tag

### Dashboard Enhancements
- Remote ID connection section
- Easy connect/disconnect controls
- Real-time battery display
- Drone detection counter
- Activity feed with recent detections

## Requirements

Hardware:
- gyb_detect ESP32 device
- Android device with Bluetooth

Software:
- ATAK 5.4.0 or later
- Android with Bluetooth enabled
- Location permissions granted

## Setup
1. Pair gyb_detect device in Android Bluetooth settings
2. Open OmniCOT plugin in ATAK
3. Click "Connect" in Remote ID Detection section
4. Drones appear automatically when detected

## Known Limitations
- One gyb_detect device connection at a time
- Manual pairing required in Android settings
- Drones disappear after 30 seconds if not re-detected
- No drone track history persistence

## Previous Features (v0.1-alpha)
- CoT marker affiliation modification
- AOI (Area of Interest) management
- Dashboard with activity tracking
- Team affiliation marking
- CoT event federation

## Build Information
- Submission: omni-COT-pipeline-20251104-161202.zip
- Package Size: 5.8 MB
- Build Target: ATAK 5.4.0
- takdevVersion: 2.+
- Commit: 05162e7

## Installation
1. Upload submission zip to https://tak.gov/third-party-plugins
2. Download signed APK from artifacts
3. Install on ATAK device
4. Grant Bluetooth and Location permissions

## Support
- GitHub: https://github.com/engindearing-projects/omni-COT
- Documentation: REMOTE_ID_INTEGRATION.md
- Issues: https://github.com/engindearing-projects/omni-COT/issues
- Email: j@engindearing.soy
