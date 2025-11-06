# OmniCOT v0.3-alpha Release Notes

Release Date: November 5, 2025
Status: Pre-release (Alpha)
TAK Pipeline Submission: `omni-COT-pipeline-20251105-101959.zip`

## Changes in v0.3-alpha

### New Features: Bluetooth Device Discovery
- In-app Bluetooth device discovery and scanning
- Device selection dialog showing paired and unpaired gyb_detect devices
- In-app pairing without leaving ATAK
- Progress indicators during scanning and pairing
- Auto-connect after successful pairing

### Modified Files
- `app/src/main/java/com/engindearing/omnicot/remoteid/BluetoothDeviceDialog.java` (new)
- `app/src/main/java/com/engindearing/omnicot/DashboardActivity.java` - Added discovery UI

## Features from Previous Releases

### Remote ID Integration (v0.2-alpha)
- Bluetooth connectivity with gyb_detect ESP32 device via Bluetooth Serial (SPP)
- Drone detection display as air tracks on ATAK map
- Real-time 1 Hz position updates for detected drones
- Full metadata: serial numbers, operator IDs, altitude, speed, heading
- Multi-protocol support: WiFi Beacon, WiFi NaN, Bluetooth Legacy detection

### Drone Data Display
- Basic info: Serial number, MAC address, callsign, timestamp
- Location: GPS coordinates, altitude MSL, height AGL, takeoff height
- Motion: Heading, horizontal/vertical speed, speed accuracy
- Detection: Method (WiFi/Bluetooth), signal strength (RSSI), accuracy
- Operator: ID, location, altitude, location type (if available)
- Optional: Description, CAA registration, UTM ID, session ID

### CoT Event Format
- Type: `a-u-A-M-F-Q-r` (Unknown Air Track, Rotary Wing)
- Yellow rotary wing air track symbol
- 30 second stale time with 1 second updates
- Full metadata in `__remoteid` detail tag

### Dashboard Features
- Remote ID connection section
- Connection management controls
- Battery level monitoring for gyb_detect device
- Drone detection counter
- Activity feed with recent detections

### Technical Implementation
- RemoteIdData model for drone detections
- RemoteIdParser for JSON message parsing
- BluetoothManager for connection handling
- RemoteIdToCotConverter for CoT event generation
- GPS coordinate validation

### Core Features (v0.1-alpha)
- CoT marker affiliation management
- AOI detection and management
- Dashboard with activity tracking
- Alert system foundation

## Requirements

Hardware:
- gyb_detect ESP32 device
- Android device with Bluetooth

Software:
- ATAK 5.4.0 or later
- Android with Bluetooth enabled
- Location permissions granted

## Installation
1. Pair gyb_detect device in Android Bluetooth settings
2. Open OmniCOT plugin in ATAK
3. Click "Scan" to discover devices
4. Select gyb_detect device to connect
5. Drones appear automatically when detected

## Known Limitations
- One gyb_detect device connection at a time
- Drones disappear after 30 seconds if not re-detected
- No drone track history persistence

## Support
- GitHub: https://github.com/engindearing-projects/omni-COT
- Issues: https://github.com/engindearing-projects/omni-COT/issues
- Documentation: REMOTE_ID_INTEGRATION.md
- Email: j@engindearing.soy
