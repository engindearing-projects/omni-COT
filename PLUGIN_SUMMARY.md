# OmniCOT Plugin for ATAK

## Overview
OmniCOT is an advanced tactical coordination plugin that enhances ATAK's situational awareness capabilities through a modern dashboard interface, CoT marker management, and AOI detection.

## Features

### Modern Dashboard
- **Real-time Status Metrics**: View active AOIs, alerts, and CoT modifications at a glance
- **Quick Action Cards**: Fast access to key functions
  - CoT Management
  - AOI Management
  - Create Alert
  - View History
- **Activity Feed**: Track recent plugin operations
- **Advanced Controls**: Data export, import, and management

### CoT Marker Management
- **Change Affiliations**: Modify marker affiliations with federation to all team members
  - Friendly (Blue)
  - Neutral (Green)
  - Hostile (Red)
  - Unknown (Yellow)
- **Change Dimensions**: Update battle space dimensions
  - Point (P)
  - Air (A)
  - Ground (G)
  - Sea Surface (S)
  - Subsurface (U)
- **Real-time Broadcasting**: All changes immediately broadcast via CoT dispatcher

### AOI Detection
- Automatic detection of Areas of Interest from map shapes
- Real-time count of active AOIs
- Foundation for geofence-based alerts

## How to Use

### Installation
1. Install ATAK on your device
2. Install the OmniCOT plugin APK
3. Start ATAK
4. Locate the OmniCOT button in the toolbar

### Accessing the Dashboard
1. **Tap** the OmniCOT toolbar button (plugin icon)
2. The dashboard will slide down from the top
3. View current status metrics and recent activity

### Changing CoT Marker Affiliation
1. From the dashboard, **tap** "CoT Management"
2. **Tap** any CoT marker on the map
3. The marker details will appear showing current type
4. Use the **Affiliation** dropdown to select new affiliation:
   - Friend (f)
   - Neutral (n)
   - Hostile (h)
   - Unknown (u)
5. Use the **Dimension** dropdown to select battle space:
   - Point (P)
   - Air (A)
   - Ground (G)
   - Sea Surface (S)
   - Subsurface (U)
6. **Tap** "Update Affiliation" to apply
7. Changes are immediately broadcasted to all team members

### Managing Areas of Interest
1. Create shapes on the map using ATAK's drawing tools
2. From the dashboard, **tap** "AOI Management"
3. View list of detected AOIs
4. (Future) Configure geofence alerts for each AOI

## Technical Details

### Files Created/Modified
1. **PluginTemplate.java** - Main plugin lifecycle
2. **OmniCOTTool.java** - Toolbar integration
3. **OmniCOTMapComponent.java** - Map component lifecycle
4. **OmniCOTDropDownReceiver.java** - Dashboard UI controller
5. **DashboardActivity.java** - Statistics and state management
6. **omnicot_dashboard.xml** - Modern dashboard layout
7. **Icon resources** - Plugin icon and UI elements

### CoT Broadcasting
The plugin uses `CotMapComponent.getInternalDispatcher()` to broadcast CoT events to:
- All connected TAK servers
- All peer-to-peer connections
- All team members on the same network

### Affiliation Encoding
Based on **CoT Event Schema** and **MIL-STD-2525D**, affiliations are encoded in the CoT type string at position 2:
- `a-f-G-E-V` = Friendly ground equipment vehicle
- `a-n-G-E-V` = Neutral ground equipment vehicle
- `a-h-G-E-V` = Hostile ground equipment vehicle
- `a-u-G-E-V` = Unknown ground equipment vehicle

The plugin preserves all other CoT type information (function, status modifiers, etc.).

## Build Information
- **Plugin Version**: 0.1
- **ATAK Version**: 5.5.0 (CIV, MIL, GOV compatible)
- **Package**: com.engindearing.omnicot
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34

## Future Enhancements
1. **Geofence Alerts**: Entry/exit notifications for AOIs
2. **Export/Import**: Save and load AOI configurations
3. **Historical Logging**: Persistent activity history
4. **Batch Operations**: Modify multiple markers at once
5. **Permission Controls**: Restrict who can modify markers
6. **Custom Alert Types**: Configurable alert conditions

## Testing
To test the plugin across multiple devices:
1. Install on multiple ATAK devices on the same network
2. Create a marker on device A
3. Open OmniCOT dashboard and change the marker's affiliation on device A
4. Verify the affiliation change appears on device B
5. Check ATAK logs for "COT affiliation updated and federated" messages

## Troubleshooting

### Dashboard Doesn't Appear
- Verify plugin is enabled in ATAK settings
- Check ATAK logs for "OmniCOTMapComponent created" message
- Restart ATAK

### CoT Changes Don't Broadcast
- Verify network connectivity between devices
- Check that CoT dispatcher is active
- Review ATAK logs for tags: OmniCOTDropDownReceiver

### AOIs Not Detected
- Ensure shapes are created in the "Drawing Objects" map group
- Refresh the AOI list from the dashboard
- Check that shapes are visible on the map

## Support
- **GitHub**: https://github.com/engindearing-projects/omni-COT
- **Issues**: https://github.com/engindearing-projects/omni-COT/issues
- **Email**: j@engindearing.soy

## Standards Compliance
- **CoT Event Schema**: DoD INST (January 2025)
- **MIL-STD-2525D**: Joint Military Symbology
- **ATAK Plugin Architecture**: Standard AbstractPlugin pattern

---
**Version**: 0.1
**Last Updated**: October 20, 2025
**License**: MIT
