# Remote ID Integration for omni-COT

## Overview

This integration adds Bluetooth connectivity to the omni-COT ATAK plugin, enabling it to receive drone Remote ID detections from the **gyb_detect** ESP32 device and display them as CoT (Cursor on Target) events on the ATAK map.

## Features

- **Bluetooth Serial Connection**: Connects to gyb_detect device via Bluetooth Serial (SPP)
- **Real-time Drone Detection**: Receives and displays drone detections on ATAK map
- **Remote ID Parsing**: Parses OpenDroneID and French Remote ID formats
- **CoT Conversion**: Converts drone detections to ATAK-compatible CoT events
- **Dashboard Integration**: Shows connection status, battery level, and drone count
- **Automatic Mapping**: Detected drones appear as air tracks on the ATAK map with full metadata

## Architecture

### New Components

1. **RemoteIdData.java** (`com.engindearing.omnicot.remoteid.RemoteIdData`)
   - Data model for Remote ID drone detections
   - Includes drone location, speed, altitude, operator info, and metadata

2. **RemoteIdParser.java** (`com.engindearing.omnicot.remoteid.RemoteIdParser`)
   - Parses JSON messages from gyb_detect device
   - Handles device info, battery status, and drone detections

3. **BluetoothManager.java** (`com.engindearing.omnicot.remoteid.BluetoothManager`)
   - Manages Bluetooth connection lifecycle
   - Scans for and connects to gyb_detect devices
   - Streams JSON data and notifies listeners

4. **RemoteIdToCotConverter.java** (`com.engindearing.omnicot.remoteid.RemoteIdToCotConverter`)
   - Converts Remote ID detections to CoT events
   - Maps drone data to MIL-STD-2525D symbols
   - Includes Remote ID metadata in CoT details

### Modified Components

1. **AndroidManifest.xml**
   - Added Bluetooth permissions (BLUETOOTH, BLUETOOTH_CONNECT, BLUETOOTH_SCAN)
   - Added location permission for Bluetooth scanning

2. **DashboardActivity.java**
   - Added Bluetooth UI components (status, connect/disconnect buttons)
   - Added battery level and drone detection counter
   - Integrated BluetoothManager with listeners

3. **OmniCOTDropDownReceiver.java**
   - Added `handleRemoteIdDetection()` method
   - Converts detections to CoT and dispatches to ATAK

4. **omnicot_dashboard.xml**
   - Added "Remote ID Detection" section to dashboard
   - Displays connection status, battery level, and drone count

## Data Flow

```
gyb_detect Device (ESP32)
    ↓ Bluetooth Serial (JSON)
BluetoothManager
    ↓ RemoteIdParser
RemoteIdData
    ↓ DashboardActivity
OmniCOTDropDownReceiver
    ↓ RemoteIdToCotConverter
CotEvent
    ↓ CotDispatcher
ATAK Map (Air Track Display)
```

## Usage Instructions

### 1. Pair the gyb_detect Device

Before using the plugin, you need to pair your Android device with the gyb_detect device:

1. Turn on the gyb_detect device
2. Go to Android **Settings** → **Bluetooth**
3. Look for a device named **`gyb_detect-XXXXXX`** (where XXXXXX is the device serial)
4. Tap to pair with the device

### 2. Connect via omni-COT Dashboard

1. Open ATAK and launch the **OmniCOT** plugin
2. The dashboard will show a **"Remote ID Detection"** section
3. Click the **Connect** button
4. The plugin will automatically find and connect to the paired gyb_detect device
5. Connection status will update to **"Bluetooth: Connected"**
6. Battery level will display the gyb_detect device battery percentage

### 3. View Drone Detections

Once connected:

- **Automatic Display**: Detected drones automatically appear on the ATAK map
- **Air Track Symbols**: Drones show as unknown air tracks (yellow rotary wing symbols)
- **Real-time Updates**: Drone positions update every second while detected
- **Metadata**: Tap on a drone marker to view:
  - Serial number
  - Operator ID
  - Altitude (MSL and AGL)
  - Speed and heading
  - Detection method (WiFi/Bluetooth)
  - Signal strength (RSSI)
  - Operator location (if available)

### 4. Dashboard Statistics

The dashboard displays:
- **Drones Detected**: Total count of unique drones detected
- **Battery**: gyb_detect device battery percentage
- **Connection Status**: Current Bluetooth connection state

### 5. Disconnect

Click the **Disconnect** button to close the Bluetooth connection.

## CoT Event Structure

Drone detections are converted to CoT events with the following structure:

### CoT Type
- **Type**: `a-u-A-M-F-Q-r` (Unknown Air Track, Rotary Wing)
- **UID**: `DRONE-{serial_number/mac}`
- **How**: `h-s` (Sensor detected)

### CoT Details

The CoT event includes:
- **contact**: Callsign derived from serial number or MAC
- **track**: Course (heading) and speed in m/s
- **__remoteid**: Custom detail with all Remote ID metadata:
  - `serialNumber`: Drone serial number
  - `operatorId`: Remote ID operator identifier
  - `description`: Self-ID description text
  - `rssi`: Signal strength in dBm
  - `recvMethod`: Detection method (WiFi Beacon/NaN/Bluetooth)
  - `heightAGL`: Height above ground in meters
  - `heightTakeoff`: Height above takeoff point
  - `hSpeed`, `vSpeed`: Horizontal and vertical speeds
  - `opLat`, `opLon`: Operator location coordinates
  - `detectedBy`: "gyb_detect"
- **precisionlocation**: GPS source indicators
- **remarks**: Human-readable summary of detection

## Remote ID Data Fields

The gyb_detect device sends the following JSON data:

### Device Information (on connection)
```json
{
  "manufacturer": "engindearing",
  "make": "engindearing",
  "model": "gyb_detect",
  "version": "0.1a - [date]",
  "serialNumber": "XXXXXX",
  "capabilities": 49
}
```

### Battery Status (every 5 seconds)
```json
{
  "batteryLevel": 0.85,
  "batteryVersion": "11",
  "batteryTemp": "29.0"
}
```

### Drone Detection (every 1 second per drone)
```json
{
  "remoteId": "operator_id",
  "uasId": "XX:XX:XX:XX:XX:XX",
  "serialNumber": "drone_serial",
  "rssi": -45,
  "recvMethod": 16,
  "uasLat": "37.123456",
  "uasLon": "-122.123456",
  "uasHae": 150.5,
  "uasHag": 50.2,
  "uasHeading": 180.0,
  "uasHSpeed": 5.5,
  "uasVSpeed": 0.5,
  "opLat": "37.123400",
  "opLon": "-122.123400",
  "description": "DJI Mavic",
  ...
}
```

## Reception Methods

The gyb_detect device can detect drones via:

- **WiFi Beacon (2.4 GHz)**: `recvMethod = 1`
- **WiFi NaN (Neighbor Awareness)**: `recvMethod = 2`
- **Bluetooth Legacy (BLE 4)**: `recvMethod = 16`

## Troubleshooting

### Cannot Find gyb_detect Device
- Ensure the gyb_detect device is powered on
- Check that Bluetooth is enabled on your Android device
- Pair the device in Android Bluetooth settings first

### Connection Fails
- Make sure you're within Bluetooth range (typically 10 meters)
- Try disconnecting and reconnecting
- Restart the gyb_detect device

### No Drones Appearing
- Verify the gyb_detect device is detecting drones (check serial monitor if available)
- Ensure the drone is broadcasting Remote ID (required for DJI drones in many regions)
- Check that location data is valid (non-zero coordinates)

### Permissions Errors
- Grant Bluetooth and Location permissions to ATAK
- On Android 12+, both BLUETOOTH_CONNECT and BLUETOOTH_SCAN permissions are required

## Technical Details

### Bluetooth Protocol
- **Profile**: SPP (Serial Port Profile)
- **UUID**: `00001101-0000-1000-8000-00805f9b34fb`
- **Baud Rate**: 115200 (handled automatically by Android)

### GPS Validation
The plugin validates GPS coordinates:
- Rejects (0, 0) or near-zero coordinates
- Validates latitude: -90° to +90°
- Validates longitude: -180° to +180°
- Rejects NaN values

### Performance
- **Update Rate**: 1 Hz per drone
- **Connection Latency**: ~1-2 seconds
- **Map Update**: Real-time via ATAK's CoT processing

## Building the Plugin

To build the plugin with Remote ID support:

```bash
cd omni-COT
./gradlew clean assembleRelease
```

The APK will be in `app/build/outputs/apk/release/`.

## Future Enhancements

Potential improvements:
- Support for multiple simultaneous drones
- Drone track history and path display
- Geofence alerts for drone detections
- Offline drone database for identification
- Export drone detection logs
- Integration with other Remote ID receivers

## References

- **OpenDroneID**: https://github.com/opendroneid/opendroneid-core-c
- **ASTM F3411**: Remote ID standard
- **ATAK**: https://tak.gov/
- **gyb_detect Project**: See main project README

## License

This integration maintains the same license as the parent omni-COT project.

## Contact

For issues or questions about the Remote ID integration, please open an issue in the project repository.

---

**Note**: This integration requires the gyb_detect hardware device. See the main gyb_detect project for hardware setup and firmware installation instructions.
