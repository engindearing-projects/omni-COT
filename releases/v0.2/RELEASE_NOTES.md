# OmniCOT v0.2 Release Notes

**Release Date:** November 6, 2025
**ATAK Compatibility:** 5.4.0+
**Signing:** TAK Official Pipeline

## 📦 Downloads

- **APK (Android):** `ATAK-Plugin-omnicot-0.2--5.4.0-civ-release.apk` (602 KB)

## ✨ What's New in v0.2

### 🔒 Official TAK Signing
- **First officially signed release** from TAK's third-party plugin pipeline
- Signed with TAK's standard plugin signing certificate
- Verified and approved for production use

### 🔧 Build Infrastructure
- Fixed GitHub Actions workflow to produce valid TAK pipeline submissions
- Added `--prefix=omni-COT/` for proper zip structure
- Automated release artifact generation

### 🐛 Bug Fixes
- Resolved pipeline submission failures that prevented artifact downloads
- Corrected zip structure to match TAK pipeline requirements

## 📋 Features (Carried Forward from v0.1)

- **Modern Dashboard Interface** - Card-based UI with real-time statistics
- **CoT Marker Management** - Change affiliations (Friendly/Neutral/Hostile/Unknown)
- **Battle Dimension Support** - Point/Air/Ground/Sea/Subsurface classifications
- **AOI Management** - Automatic detection and listing of Areas of Interest
- **Activity Tracking** - Monitor recent plugin operations
- **Alert System Foundation** - Infrastructure for geofence-based notifications

## 🔐 Security

- No network connections initiated by plugin
- Uses standard ATAK CoT dispatcher for all communications
- No sensitive data storage
- Passes Fortify security scan

## 📊 Technical Details

- **Package:** `com.engindearing.omnicot`
- **ATAK Version:** 5.4.0
- **Min SDK:** 21 (Android 5.0)
- **Target SDK:** 34 (Android 14)
- **ProGuard:** Enabled with repackaging to `atakplugin.omnicot`

## 🚀 Installation

1. Download the APK from this release
2. Transfer to your Android device
3. Install (allow unknown sources if needed)
4. Launch ATAK - plugin loads automatically
5. Access via OmniCOT toolbar button

## 🧪 Testing

- ✅ Plugin loads successfully in ATAK CIV 5.4.0+
- ✅ Dashboard displays correctly
- ✅ CoT marker modifications work
- ✅ Affiliation changes federate correctly
- ✅ AOI detection functions properly
- ✅ No crashes during normal operation
- ✅ Security scan passed

## 📝 Known Limitations

1. Alert system is foundation only - full geofence monitoring not yet implemented
2. Export/import functionality not yet implemented
3. Historical activity logging is in-memory only (not persisted)
4. No permission controls - any user can modify any marker

## 🔄 Upgrade Notes

This is the first official release. If you're using v0.1 alpha:
- Uninstall the old version first
- Install this new signed version
- All plugin data is stored in ATAK's map, so no data loss

## 📖 Documentation

- [README.md](../../README.md) - Main documentation
- [PLUGIN_SUMMARY.md](../../PLUGIN_SUMMARY.md) - Feature overview
- [SUBMISSION_README.md](../../SUBMISSION_README.md) - Technical details

## 💬 Support

- **GitHub Issues:** https://github.com/engindearing-projects/omni-COT/issues
- **Email:** j@engindearing.soy
- **ATAK Logs Tags:** `OmniCOTMapComponent`, `OmniCOTDropDownReceiver`, `DashboardActivity`

## 🙏 Acknowledgments

- TAK development team for the excellent SDK and pipeline
- TAK community for feedback and testing
- All contributors who helped improve this plugin

---

**Previous Release:** v0.1 (Alpha - October 20, 2025)
**Next Release:** v0.3 (Planned - Full alert system implementation)
