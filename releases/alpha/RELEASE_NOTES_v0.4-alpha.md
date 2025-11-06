# OmniCOT v0.4-alpha Release Notes

Release Date: November 5, 2025
Status: Pre-release (Alpha)
TAK Pipeline Submission: `omni-COT-pipeline-20251105-121833.zip`

## Changes in v0.4-alpha

### Bug Fixes
- Fixed Bluetooth device discovery dialog crash (BadTokenException)
- Changed dialog context from plugin context to Activity context via MapView.getContext()
- Bluetooth discovery and pairing now works without crashing ATAK

### Features Added (from v0.3-alpha)
- In-app Bluetooth device discovery and scanning
- Device selection dialog showing paired and unpaired devices
- In-app pairing with gyb_detect devices
- Progress indicators during scanning and pairing
- Auto-connect after successful pairing

## Modified Files
- `app/src/main/java/com/engindearing/omnicot/DashboardActivity.java` - Fixed dialog context

## Testing Status
- Dialog opens without crash
- Device scanning functionality works
- Paired device display works
- Pending: Complete pairing flow with real gyb_detect device

## Features from Previous Releases

### Remote ID Detection (v0.3-alpha)
- Bluetooth integration with gyb_detect ESP32 device
- Real-time drone detection on ATAK map
- Remote ID metadata display (serial, location, speed, altitude)
- Multi-protocol support (WiFi Beacon, WiFi NaN, Bluetooth Legacy)

### Core Features (v0.1-alpha)
- CoT marker affiliation management
- AOI detection and management
- Dashboard with activity tracking
- Alert system foundation

## Installation
1. Upload submission zip to https://tak.gov/third-party-plugins
2. Download signed APK from artifacts
3. Install on ATAK 5.4.0+
4. Grant Bluetooth and Location permissions

## Support
- GitHub: https://github.com/engindearing-projects/omni-COT
- Issues: https://github.com/engindearing-projects/omni-COT/issues
- Email: j@engindearing.soy
