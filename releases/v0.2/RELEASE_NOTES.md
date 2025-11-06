# OmniCOT v0.2 Release Notes

Release Date: November 6, 2025
ATAK Compatibility: 5.4.0+
Signing: TAK Official Pipeline

## Downloads

APK: `ATAK-Plugin-omnicot-0.2--5.4.0-civ-release.apk` (602 KB)

## Changes in v0.2

### Build Infrastructure
- Fixed GitHub Actions workflow to produce valid TAK pipeline submissions
- Added `--prefix=omni-COT/` for proper zip structure
- Resolved pipeline submission failures that prevented artifact downloads

### Signing
- Signed with TAK's standard plugin signing certificate
- Passes Fortify security scan

## Features

- Dashboard interface with real-time CoT statistics
- CoT marker affiliation management (Friendly/Neutral/Hostile/Unknown)
- Battle dimension support (Point/Air/Ground/Sea/Subsurface)
- AOI detection and management from map shapes
- Activity tracking for recent operations
- Alert system foundation (geofence monitoring not yet implemented)

## Technical Details

- Package: `com.engindearing.omnicot`
- ATAK Version: 5.4.0
- Min SDK: 21 (Android 5.0)
- Target SDK: 34 (Android 14)
- ProGuard: Enabled with repackaging to `atakplugin.omnicot`

## Installation

1. Download the APK from this release
2. Transfer to your Android device
3. Install (allow unknown sources if needed)
4. Launch ATAK - plugin loads automatically
5. Access via OmniCOT toolbar button

## Testing Status

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

## Upgrade Notes

If upgrading from v0.1 alpha:
- Uninstall the old version first
- Install this signed version
- Plugin data is stored in ATAK's map (no data loss)

## Documentation

- README.md - Main documentation
- PLUGIN_SUMMARY.md - Feature overview
- SUBMISSION_README.md - Technical details

## Support

- GitHub Issues: https://github.com/engindearing-projects/omni-COT/issues
- Email: j@engindearing.soy
- ATAK Log Tags: `OmniCOTMapComponent`, `OmniCOTDropDownReceiver`, `DashboardActivity`
