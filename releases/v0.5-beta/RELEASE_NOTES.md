# OmniCOT v0.5-beta Release Notes

Release Date: November 6, 2025
Status: Beta Release (First)
ATAK Compatibility: 5.4.0+
Signing: TAK Official Pipeline

## Downloads

APK: `ATAK-Plugin-omnicot-0.5--5.4.0-civ-release.apk` (602 KB)

## Release Information

First beta release following alpha testing phase. This release includes build infrastructure fixes and TAK pipeline signing.

## Changes in v0.5-beta

### Build Infrastructure
- Fixed GitHub Actions workflow to produce valid TAK pipeline submissions
- Added `--prefix=omni-COT/` for proper zip structure
- Resolved pipeline submission failures that prevented artifact downloads
- Automated release artifact generation

### Signing
- Signed with TAK's standard plugin signing certificate
- Passes Fortify security scan

## Features

### CoT Management
- Dashboard interface with real-time CoT statistics
- CoT marker affiliation management (Friendly/Neutral/Hostile/Unknown)
- Battle dimension support (Point/Air/Ground/Sea/Subsurface)
- Standard ATAK CoT dispatcher integration

### AOI Management
- Automatic detection of areas of interest from map shapes
- Area calculation and display
- Center coordinate extraction

### Activity Tracking
- Monitor recent plugin operations
- Activity feed display

### Remote ID Detection (Alpha feature from v0.2-v0.4-alpha)
- Bluetooth integration with gyb_detect ESP32 device
- Real-time drone detection on ATAK map
- Full Remote ID metadata display
- Multi-protocol support (WiFi Beacon, WiFi NaN, Bluetooth Legacy)
- In-app Bluetooth device discovery and pairing

### Alert System
- Foundation infrastructure for geofence-based notifications
- Ready for full implementation

## Technical Details

Package: `com.engindearing.omnicot`
ATAK Version: 5.4.0
Min SDK: 21 (Android 5.0)
Target SDK: 34 (Android 14)
ProGuard: Enabled with repackaging to `atakplugin.omnicot`

## Installation

1. Download the APK from this release
2. Transfer to your Android device
3. Install (allow unknown sources if needed)
4. Launch ATAK - plugin loads automatically
5. Access via OmniCOT toolbar button

## Testing Status

Tested features:
- Plugin loads in ATAK CIV 5.4.0+
- Dashboard displays correctly
- CoT marker modifications work
- Affiliation changes federate correctly
- AOI detection functions properly
- Security scan passed

## Known Limitations

- Alert system foundation only - full geofence monitoring not implemented
- Export/import functionality not implemented
- Historical activity logging is in-memory only (not persisted)
- No permission controls - any user can modify any marker
- Remote ID features require gyb_detect hardware device

## Version History

This beta release follows the alpha testing series:
- v0.1-alpha - Initial release with core features
- v0.2-alpha - Remote ID integration
- v0.3-alpha - Bluetooth device discovery
- v0.4-alpha - Bluetooth dialog fixes
- v0.5-beta - First beta with infrastructure fixes (this release)

## Upgrade Notes

If upgrading from any alpha version:
- Uninstall the old version first
- Install this beta version
- Plugin data is stored in ATAK's map (no data loss)

## Documentation

- README.md - Main documentation
- PLUGIN_SUMMARY.md - Feature overview
- SUBMISSION_README.md - Technical details
- REMOTE_ID_INTEGRATION.md - Remote ID setup guide

## Support

GitHub: https://github.com/engindearing-projects/omni-COT
Issues: https://github.com/engindearing-projects/omni-COT/issues
Email: j@engindearing.soy
ATAK Log Tags: `OmniCOTMapComponent`, `OmniCOTDropDownReceiver`, `DashboardActivity`
