# OmniCOT Plugin Architecture Overview

## 1. PROJECT STRUCTURE

### Directory Layout
```
/home/user/omni-COT/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/engindearing/omnicot/
│   │   │   │   ├── OmniCOTPlugin.java              [Entry point - AbstractPlugin]
│   │   │   │   ├── OmniCOTTool.java                [Toolbar integration]
│   │   │   │   ├── OmniCOTMapComponent.java        [Lifecycle management]
│   │   │   │   ├── OmniCOTDropDownReceiver.java    [UI controller & event handling]
│   │   │   │   ├── DashboardActivity.java          [Dashboard UI & stats]
│   │   │   │   ├── CotAffiliationListener.java     [CoT message listener]
│   │   │   │   ├── AffiliationManager.java         [Data persistence]
│   │   │   │   ├── AffiliationData.java            [Data model]
│   │   │   │   ├── AOIItem.java                    [AOI data model]
│   │   │   │   ├── AOIAdapter.java                 [AOI list UI adapter]
│   │   │   │   ├── AlertConfigDialog.java          [Geofence alert config]
│   │   │   │   └── PluginNativeLoader.java         [Native library loading]
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── omnicot_dashboard.xml       [Main dashboard UI]
│   │   │   │   │   ├── main_layout.xml             [COT & AOI management UI]
│   │   │   │   │   ├── aoi_list_item.xml           [AOI list item UI]
│   │   │   │   │   └── aoi_alert_config.xml        [Alert config dialog UI]
│   │   │   │   ├── drawable/                       [Icons & button styles]
│   │   │   │   └── values/
│   │   │   │       ├── strings.xml
│   │   │   │       ├── styles.xml
│   │   │   │       ├── colors.xml
│   │   │   │       └── dimen.xml
│   │   │   └── AndroidManifest.xml
│   │   ├── test/java/                              [Unit tests]
│   │   └── gov/res/                                [GOV variant resources]
│   └── build.gradle                                [Build configuration]
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
└── docs/

### Key Specifications
- Plugin Version: 0.1
- ATAK Version Compatibility: 5.4.0-5.5.0 (CIV, MIL, GOV)
- Min SDK: 21 (Android 5.0 Lollipop)
- Target SDK: 34
- Java Version: 17
- Supported Architectures: armeabi-v7a, arm64-v8a, x86 (debug only)

### Dependencies
- androidx.recyclerview:recyclerview:1.3.2
- androidx.annotation:annotation:1.8.2
- ATAK SDK (via takrepo)


## 2. PLUGIN ARCHITECTURE & LIFECYCLE

### Entry Point Hierarchy
```
OmniCOTPlugin (extends AbstractPlugin)
├── OmniCOTTool (extends AbstractPluginTool)
│   └── Creates toolbar button with action: com.engindearing.omnicot.SHOW_PLUGIN
│
└── OmniCOTMapComponent (extends DropDownMapComponent)
    ├── Manages plugin lifecycle (onCreate, onDestroy)
    ├── Registers OmniCOTDropDownReceiver for intent handling
    └── Registers CotAffiliationListener for CoT message monitoring
```

### Constructor Chain
```
OmniCOTPlugin(IServiceController)
  → Initializes with:
    1. OmniCOTTool (context-based toolbar integration)
    2. OmniCOTMapComponent (lifecycle manager)
```

### Component Lifecycle (OmniCOTMapComponent)

**onCreate(Context, Intent, MapView)**
- Sets ATAK theme
- Inflates dashboard layout (omnicot_dashboard.xml)
- Creates OmniCOTDropDownReceiver with MapView, Context, and dashboard view
- Registers DropDownReceiver to handle SHOW_PLUGIN intent
- Creates and registers CotAffiliationListener as CommsLogger
- Log: "OmniCOT MapComponent created"

**onDestroyImpl(Context, MapView)**
- Disposes OmniCOTDropDownReceiver
- Unregisters CotAffiliationListener
- Cleanup and logging

---

## 3. MAIN PLUGIN COMPONENTS

### 3.1 OmniCOTPlugin.java (Entry Point)
**File Path**: /home/user/omni-COT/app/src/main/java/com/engindearing/omnicot/OmniCOTPlugin.java

**Purpose**: Main plugin entry point extending AbstractPlugin

**Responsibilities**:
- Instantiate OmniCOTTool for toolbar integration
- Instantiate OmniCOTMapComponent for lifecycle management
- Register both with the AbstractPlugin framework

**Key Methods**:
```java
public OmniCOTPlugin(IServiceController serviceController)
  → serviceController.getService(PluginContextProvider.class).getPluginContext()
  → Creates OmniCOTTool and OmniCOTMapComponent
```

---

### 3.2 OmniCOTTool.java (Toolbar Integration)
**File Path**: /home/user/omni-COT/app/src/main/java/com/engindearing/omnicot/OmniCOTTool.java

**Purpose**: Integrates plugin as toolbar button in ATAK

**Responsibilities**:
- Create toolbar button with icon and label
- Trigger dashboard display when tapped
- Resource management

**Key Features**:
- Extends AbstractPluginTool
- Implements Disposable interface
- Intent Action: `com.engindearing.omnicot.SHOW_PLUGIN`
- Icon: R.drawable.ic_launcher
- Label: app_name from strings.xml

**UI Elements**:
- Toolbar button that broadcasts SHOW_PLUGIN intent

---

### 3.3 OmniCOTMapComponent.java (Lifecycle Manager)
**File Path**: /home/user/omni-COT/app/src/main/java/com/engindearing/omnicot/OmniCOTMapComponent.java

**Purpose**: Manages plugin lifecycle and core component registration

**Responsibilities**:
- Initialize and teardown plugin resources
- Register UI event handlers
- Monitor CoT messages
- Manage theme and layout inflation

**Key Methods**:
```java
onCreate(Context, Intent, MapView)
  → Inflate dashboard view
  → Create and register OmniCOTDropDownReceiver
  → Create and register CotAffiliationListener

onDestroyImpl(Context, MapView)
  → Dispose of receivers and listeners
```

**Registered Listeners**:
1. **OmniCOTDropDownReceiver** - Intent filter: SHOW_PLUGIN
2. **CotAffiliationListener** - Registered as CommsLogger with CommsMapComponent

**Theme**:
- Applies R.style.ATAKPluginTheme

---

### 3.4 OmniCOTDropDownReceiver.java (UI Controller & Event Handler)
**File Path**: /home/user/omni-COT/app/src/main/java/com/engindearing/omnicot/OmniCOTDropDownReceiver.java

**Purpose**: Main UI controller handling dashboard and management screens

**Responsibilities**:
- Display dashboard and management UI
- Handle map click events for CoT selection
- Process COT affiliation updates
- Manage AOI list and refresh
- Dispatch COT events to federation
- Coordinate with other components

**Key Classes**:
- Extends DropDownReceiver
- Implements DropDown.OnStateListener

**Major Features**:

1. **Dashboard Management** (via DashboardActivity)
   - Shows real-time stats (AOI count, alerts, COT modifications)
   - Quick action cards
   - Activity feed

2. **COT Selection & Affiliation**
   - Map event listener for ITEM_CLICK events
   - Parses CoT type string (format: "a-[affiliation]-[dimension]-...")
   - Displays current affiliation/dimension spinners
   - Stores custom affiliation data (Unknown, Assumed Friendly, Assumed Hostile, Pending)
   - Broadcasts updates via CotDispatcher

3. **AOI Management**
   - Discovers shapes in "Drawing Objects" MapGroup
   - RecyclerView-based list with AOIAdapter
   - Zoom-to-AOI functionality
   - Alert configuration per AOI

**Key Event Handling**:

```java
// Map Event Listener for COT Selection
final MapEventDispatcher.MapEventDispatchListener clickListener = 
  new MapEventDispatcher.MapEventDispatchListener() {
    @Override
    public void onMapEvent(MapEvent event) {
      if (isSelectingCot) {
        MapItem item = event.getItem();
        if (item != null) {
          onCotSelected(item);
          mapView.getMapEventDispatcher().removeMapEventListener(
            MapEvent.ITEM_CLICK, this);
        }
      }
    }
  };

mapView.getMapEventDispatcher().addMapEventListener(
  MapEvent.ITEM_CLICK, clickListener);
```

**COT Update Process**:
```java
private void updateCotAffiliation()
  → Get selected affiliation and dimension from spinners
  → Create CotEvent manually:
     - Set UID, Type, How, Time, Start, Stale
     - Add location from PointMapItem
     - Add custom affiliation detail (__omnicot_affiliation)
  → Dispatch via CotDispatcher.dispatch(cotEvent)
  → Store locally in AffiliationManager
  → Update dashboard counter
  → Broadcast intent with affiliation update
```

**UI State Management**:
- `showingDashboard`: Boolean flag for current view
- `isSelectingCot`: Boolean flag for map interaction mode
- `selectedCotItem`: Reference to currently selected map item

**Intent Actions**:
- `com.engindearing.omnicot.SHOW_PLUGIN`: Broadcasts to trigger dashboard display

**View Management**:
- `templateView`: Dashboard layout
- `managementView`: COT and AOI management layout
- Uses PluginLayoutInflater for resource inflation

---

## 4. MAP EVENT HANDLING

### Current Implementation

**MapEventDispatcher Usage**:

Location: `OmniCOTDropDownReceiver.java`

```java
// Add listener for map clicks during COT selection
mapView.getMapEventDispatcher().addMapEventListener(
  MapEvent.ITEM_CLICK, clickListener);

// Remove listener after selection
mapView.getMapEventDispatcher().removeMapEventListener(
  MapEvent.ITEM_CLICK, this);
```

**Event Type**:
- `MapEvent.ITEM_CLICK`: Fired when user taps a map item

**Listener Implementation**:
- Implements `MapEventDispatcher.MapEventDispatchListener`
- Handles single-use listener (auto-removes after first item selected)
- Calls `onCotSelected(MapItem)` with the clicked item

**MapEventDispatcher Access**:
```java
mapView.getMapEventDispatcher()  // Via MapView reference passed to constructor
```

**Current Event Listeners**:
1. **ITEM_CLICK** - For COT marker selection during affiliation management
   - Single-use listener added dynamically
   - Automatically removed after selection

**Limitations of Current Approach**:
- Limited to single map click event
- No real-time monitoring of map item changes
- No listener for AOI entry/exit events
- No persistent map interaction handlers

---

## 5. COT MESSAGE HANDLING & FEDERATION

### CotAffiliationListener.java (CommsLogger Integration)
**File Path**: /home/user/omni-COT/app/src/main/java/com/engindearing/omnicot/CotAffiliationListener.java

**Purpose**: Monitor and process incoming/outgoing CoT messages

**Responsibilities**:
- Implement CommsLogger interface to hook into ATAK's CoT pipeline
- Extract affiliation information from CoT event details
- Store and update affiliation data
- Track CoT messages from other team members

**Key Methods**:

```java
// Incoming CoT events
public void logReceive(CotEvent event, String rxid, String server)
  → Extract UID from event
  → Look for __omnicot_affiliation detail child
  → Extract: affiliation, markedBy, notes attributes
  → Update or create AffiliationData in AffiliationManager
  → Create default UNKNOWN affiliation for new CoT items

// Outgoing CoT events
public void logSend(CotEvent event, String destination)
public void logSend(CotEvent event, String[] toUIDs)
  → Log affiliation updates being sent
```

**Affiliation Detail Tag**:
- Custom tag: `__omnicot_affiliation`
- Nested CotDetail with attributes:
  - `affiliation`: Enum value (unknown, assumedFriendly, assumedHostile, pending)
  - `markedBy`: Callsign of user who set affiliation
  - `timestamp`: Milliseconds when set
  - `notes`: Optional notes field

**Integration**:
```java
// Registered in OmniCOTMapComponent.onCreate()
affiliationListener = new CotAffiliationListener(pluginContext);
CommsMapComponent.getInstance().registerCommsLogger(affiliationListener);

// Unregistered in onDestroyImpl()
CommsMapComponent.getInstance().unregisterCommsLogger(affiliationListener);
```

---

## 6. COT DISPATCHING

### CotDispatcher Usage
**Location**: `OmniCOTDropDownReceiver.java`

```java
// Get dispatcher
cotDispatcher = com.atakmap.android.cot.CotMapComponent.getInternalDispatcher();

// Dispatch affiliation update
cotEvent.setUID(uid);
cotEvent.setType(newType);  // Format: "a-f-G-E-V" (f=friendly, G=ground, E=equipment, V=vehicle)
cotEvent.setHow("h-e");     // how="h-e" (human entered)
cotEvent.setTime(new CoordinatedTime());
cotEvent.setStart(new CoordinatedTime());
cotEvent.setStale(new CoordinatedTime(now.getMilliseconds() + 30 * 60 * 1000));
cotEvent.setPoint(new CotPoint(geoPoint));

// Add detail with affiliation information
CotDetail detail = new CotDetail();
CotDetail affiliationDetail = new CotDetail(CotAffiliationListener.getAffiliationDetailTag());
affiliationDetail.setAttribute("affiliation", customAffiliation.getValue());
affiliationDetail.setAttribute("markedBy", localCallsign);
affiliationDetail.setAttribute("timestamp", String.valueOf(System.currentTimeMillis()));
detail.addChild(affiliationDetail);
cotEvent.setDetail(detail);

// Dispatch to all peers
cotDispatcher.dispatch(cotEvent);
```

**Broadcasting Destinations**:
- All connected TAK servers
- All peer-to-peer connections
- All team members on the network

---

## 7. COT AND AFFILIATION DATA MANAGEMENT

### AffiliationData.java (Data Model)
**File Path**: /home/user/omni-COT/app/src/main/java/com/engindearing/omnicot/AffiliationData.java

**Purpose**: Represent team affiliation tracking data

**Enum: Affiliation**
```java
enum Affiliation {
    UNKNOWN("unknown"),
    ASSUMED_FRIENDLY("assumedFriendly"),
    ASSUMED_HOSTILE("assumedHostile"),
    PENDING("pending");
}
```

**Properties**:
- `uid`: CoT marker unique identifier
- `affiliation`: Team affiliation status
- `markedBy`: Callsign of person who set affiliation
- `timestamp`: When affiliation was set (milliseconds)
- `serverConnection`: Server connection identifier
- `notes`: Optional notes field

**Serialization**:
- JSON serialization via toJson() and fromJson()
- Stored in SharedPreferences via AffiliationManager

---

### AffiliationManager.java (Data Persistence)
**File Path**: /home/user/omni-COT/app/src/main/java/com/engindearing/omnicot/AffiliationManager.java

**Purpose**: Persistent storage of CoT affiliation data

**Pattern**: Singleton

**Storage Backend**: SharedPreferences (prefs_name="omnicot_affiliations")

**Key Methods**:
```java
getInstance(Context context)           // Singleton accessor
setAffiliation(AffiliationData data)   // Store affiliation
getAffiliation(String uid)             // Retrieve affiliation
hasAffiliation(String uid)             // Check existence
removeAffiliation(String uid)          // Delete affiliation
getAllAffiliations()                   // Get all stored data
clearAll()                             // Clear all data
getAffiliationCount()                  // Count stored items
updateAffiliation(uid, newAff, markedBy)  // Update existing
```

**Storage Format**: JSON strings in SharedPreferences with prefix "affiliation_"

---

## 8. AOI (AREAS OF INTEREST) MANAGEMENT

### AOIItem.java (Data Model)
**File Path**: /home/user/omni-COT/app/src/main/java/com/engindearing/omnicot/AOIItem.java

**Purpose**: Represent an Area of Interest (shape on map)

**Properties**:
- `shape`: Reference to ATAK Shape object
- `alertEnabled`: Boolean flag for alert status
- `triggerType`: "Entry", "Exit", or "Both"
- `monitoredType`: "All", "Friendly", "Hostile", or "Unknown"
- `durationHours`: Alert duration in hours

**Methods**:
```java
getName()             // Shape title
getType()             // Shape class name (Polygon, Circle, etc.)
getUID()              // Shape unique identifier
isAlertEnabled()
getAlertStatus()      // Human-readable alert status
```

**Discovery**:
- Found in "Drawing Objects" MapGroup
- Filters for instances of Shape class

---

### AOIAdapter.java (RecyclerView Adapter)
**File Path**: /home/user/omni-COT/app/src/main/java/com/engindearing/omnicot/AOIAdapter.java

**Purpose**: Display list of AOIs in RecyclerView

**Layout Item**: aoi_list_item.xml

**ViewHolder Features**:
- AOI name and type display
- Alert status indicator
- "Zoom To AOI" button - calculates bounds and pans/zooms map
- "Configure Alert" button - opens AlertConfigDialog

**Zoom Logic**:
```java
GeoBounds bounds = item.getShape().getBounds(null);
GeoPoint center = bounds.getCenter(null);
double distance = GeoCalculations.distanceTo(southwest, northeast);
double scale = Math.max(distance * 1.5, 100.0);
mapView.getMapController().panTo(center, true);
mapView.getMapController().zoomTo(scale, true);
```

---

### AlertConfigDialog.java (Geofence Configuration)
**File Path**: /home/user/omni-COT/app/src/main/java/com/engindearing/omnicot/AlertConfigDialog.java

**Purpose**: Configure geofence alerts for AOIs

**Features**:
- Enable/disable alerts
- Trigger type selection (Entry, Exit, Both)
- Monitored types (All, Friendly, Hostile, Unknown)
- Duration configuration
- GeoFence integration with GeoFenceComponent

**GeoFence Creation**:
```java
GeoFence geoFence = new GeoFence(
    aoiItem.getShape(),
    true,  // enabled
    trigger,  // Entry, Exit, or Both
    monitoredTypes,  // Which markers to monitor
    (int) durationMillis
);

GeoFenceComponent.getInstance().dispatch(geoFence, aoiItem.getShape());
```

**Breach Event Monitoring**:
- Registers BroadcastReceiver for "com.atakmap.android.geofence.BREACH_EVENT"
- Displays toast notifications on geofence breaches
- Logs breach details: fence UID, item UID, breach type

**Intent Filter**:
```java
DocumentedIntentFilter filter = 
  new DocumentedIntentFilter("com.atakmap.android.geofence.BREACH_EVENT");
AtakBroadcast.getInstance().registerSystemReceiver(breachReceiver, filter);
```

---

## 9. DASHBOARD & UI STATE MANAGEMENT

### DashboardActivity.java
**File Path**: /home/user/omni-COT/app/src/main/java/com/engindearing/omnicot/DashboardActivity.java

**Purpose**: Display statistics and quick action cards

**UI Components**:
```
Dashboard View (omnicot_dashboard.xml)
├── Status Metrics Row
│   ├── Active AOIs (text display)
│   ├── Active Alerts (text display)
│   └── COT Modified (text display)
├── Quick Action Cards
│   ├── COT Management Card → receiver.showCOTManagement()
│   ├── AOI Management Card → receiver.showAOIManagement()
│   ├── Create Alert Card → Toast message
│   └── View History Card → Toast with recent activities
├── Recent Activity Feed (RecyclerView)
└── Header Buttons
    ├── Settings Button (future)
    └── Help Button
```

**Statistics Tracking**:
- `cotModifiedCount`: Static counter for COT affiliation changes
- `recentActivities`: Static list of 10 most recent activities

**Methods**:
```java
updateStats()              // Refresh all counters from map
getAOICount()             // Count Shape objects in Drawing Objects
getActiveAlertCount()     // Placeholder (returns 0)
incrementCOTModified()    // Called after COT update
addActivity(String)       // Add to activity feed
```

**Data Binding**:
- Links to UI TextViews and Buttons via findViewById
- Sets up click listeners for card navigation

---

## 10. EXISTING MAPDISPATCHER/EVENT HANDLING PATTERNS

### Current Usage Summary

**Single Map Click Event Handler** (in OmniCOTDropDownReceiver)
- Event: `MapEvent.ITEM_CLICK`
- Listener Type: `MapEventDispatcher.MapEventDispatchListener`
- Scope: Single-use, auto-removes after selection
- Purpose: Select CoT marker for affiliation update

### MapEventDispatcher Access Pattern
```java
mapView.getMapEventDispatcher()  // Primary access pattern in plugin
  .addMapEventListener(eventType, listener)
  .removeMapEventListener(eventType, listener)
```

### Integration Points
1. **Direct Integration**: OmniCOTDropDownReceiver uses MapView reference
2. **Indirect Integration**: CotAffiliationListener via CommsLogger
3. **Callback Pattern**: Alert dialogs register BroadcastReceivers

---

## 11. WIDGET & TOOL IMPLEMENTATIONS

### OmniCOTTool (Toolbar Widget)
**Type**: AbstractPluginTool
**Icon**: ic_launcher drawable
**Label**: app_name from strings
**Action**: com.engindearing.omnicot.SHOW_PLUGIN
**Handler**: OmniCOTDropDownReceiver

### DropDown Receivers
**OmniCOTDropDownReceiver** (DropDownReceiver + OnStateListener)
- Shows/hides drop-down views
- Handles SHOW_PLUGIN intent
- Manages state transitions between Dashboard and Management views

### Dialog Implementations
**AlertConfigDialog**
- Custom Alert Dialog for geofence configuration
- Integrates with Android AlertDialog.Builder
- Registers BroadcastReceiver for breach notifications

---

## 12. ANDROID MANIFEST & PERMISSIONS

**File Path**: /home/user/omni-COT/app/src/main/AndroidManifest.xml

**Key Metadata**:
- `plugin-api`: Dynamic (set per flavor via ${atakApiVersion})
- `app_desc`: From strings.xml

**Activity Declaration** (Plugin Discovery):
```xml
<activity android:name="com.atakmap.app.component"
    android:exported="true"
    tools:ignore="MissingClass">
    <intent-filter android:label="@string/app_name">
        <action android:name="com.atakmap.app.component" />
    </intent-filter>
</activity>
```

**Theme**:
- Application theme: @style/AppTheme
- Component theme: R.style.ATAKPluginTheme (applied in OmniCOTMapComponent)

**Backup Configuration**:
- `android:allowBackup="false"` - No backup allowed

---

## 13. BUILD CONFIGURATION

**File Path**: /home/user/omni-COT/app/build.gradle

**Key Features**:
- Multi-flavor support: CIV, MIL, GOV, XYZ
- Gradle version: 8.8.2
- Compile SDK: 35
- Android Plugin: atak-takdev-plugin
- Keystore signing: debug and release configurations

**Build Types**:
- Debug: debuggable=true
- Release: minifyEnabled=true, ProGuard obfuscation

**Packaging**:
- Native libraries: useLegacyPackaging=true
- JNI Libs support for native code
- ABI filters: armeabi-v7a, arm64-v8a, x86 (debug)

---

## 14. COMPLETE COMPONENT FLOW

### Dashboard Activation Flow
```
User taps toolbar button
  ↓
OmniCOTTool broadcasts SHOW_PLUGIN intent
  ↓
AtakBroadcast delivers to OmniCOTMapComponent receivers
  ↓
OmniCOTDropDownReceiver.onReceive(SHOW_PLUGIN)
  ↓
showDropDown(templateView) displays dashboard
  ↓
DashboardActivity.updateStats() populates UI
  ↓
Dashboard shows: AOI count, alert count, COT modifications
```

### COT Affiliation Update Flow
```
User clicks "Select COT to Modify"
  ↓
MapEventDispatcher listener registered for ITEM_CLICK
  ↓
User taps CoT marker on map
  ↓
MapEvent.ITEM_CLICK fired with MapItem
  ↓
onCotSelected(MapItem) called
  ↓
Parse current affiliation from item.getType()
  ↓
Display current spinners and custom affiliation
  ↓
User selects new values and clicks "Update and Federate"
  ↓
updateCotAffiliation() executed:
  ├─ Create CotEvent with new type and details
  ├─ Add __omnicot_affiliation detail with metadata
  ├─ Dispatch via CotDispatcher (federation)
  ├─ Store in AffiliationManager (local persistence)
  ├─ Update MapItem.setType() (local UI update)
  ├─ Increment dashboard counter
  └─ Add activity to feed
  ↓
CotAffiliationListener.logSend() logs outgoing update
  ↓
Other devices receive CoT event
  ↓
CotAffiliationListener.logReceive() on remote device
  ↓
Update AffiliationManager with received affiliation
```

### AOI Management & Geofence Alert Flow
```
User clicks "AOI Management"
  ↓
showAOIManagement() refreshes shape list
  ↓
getAOIsFromMap() queries "Drawing Objects" MapGroup
  ↓
AOIAdapter populates RecyclerView
  ↓
User clicks "Configure Alert" on AOI item
  ↓
AlertConfigDialog.show() displays configuration
  ↓
User configures trigger, monitored types, duration
  ↓
saveAlertConfiguration() called:
  ├─ Create GeoFence object
  ├─ Dispatch to GeoFenceComponent
  ├─ Register BroadcastReceiver for BREACH_EVENT
  └─ Store alert config in AOIItem
  ↓
Geofence monitoring active
  ↓
When geofence breach detected
  ↓
BREACH_EVENT broadcast received
  ↓
BroadcastReceiver.onReceive() shows alert notification
```

---

## 15. DATA FLOW ARCHITECTURE

```
┌─────────────────────────────────────────────────────────────┐
│                    ATAK MAP VIEW                             │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ MapEventDispatcher (Click Events)                      │  │
│  │ CommsMapComponent (CoT Message Pipeline)               │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────┬──────────────────────────┬────────────────────┘
               │                          │
               ↓                          ↓
    ┌──────────────────────┐   ┌──────────────────────┐
    │ OmniCOTDropDown      │   │ CotAffiliation       │
    │ Receiver             │   │ Listener             │
    │ (UI Controller)      │   │ (CommsLogger)        │
    │                      │   │                      │
    │ • COT Selection      │   │ • Receive CoT Msgs   │
    │ • AOI Management     │   │ • Extract Affiliation│
    │ • Geofence Config    │   │ • Update Manager     │
    │ • Dashboard          │   │ • Monitor Peers      │
    └──────────┬───────────┘   └──────────┬───────────┘
               │                          │
               ↓                          ↓
         ┌─────────────────────────────────────┐
         │    AffiliationManager (Singleton)   │
         │    SharedPreferences Storage        │
         │                                     │
         │ • setAffiliation(data)              │
         │ • getAffiliation(uid)               │
         │ • getAllAffiliations()              │
         └──────────────┬──────────────────────┘
                        │
                        ↓
              ┌──────────────────────┐
              │   SharedPreferences  │
              │ "omnicot_affiliations"│
              │                      │
              │ affiliation_[uid]    │
              │ → JSON data          │
              └──────────────────────┘

         ┌──────────────────────────┐
         │  CotDispatcher           │
         │  (ATAK CoT Pipeline)     │
         │                          │
         │ dispatch(CotEvent)       │
         │ → Federation             │
         └──────────────┬───────────┘
                        │
                        ↓
         ┌──────────────────────────┐
         │  TAK Server / P2P Network│
         │  → Other Devices         │
         └──────────────────────────┘
```

---

## 16. KEY DESIGN PATTERNS

### 1. **Singleton Pattern**
- AffiliationManager: Single instance per application lifecycle

### 2. **Observer Pattern**
- MapEventDispatcher: Event listeners for map interactions
- CommsLogger: Hook into CoT message pipeline
- BroadcastReceiver: Listen for geofence breach events

### 3. **Factory Pattern**
- PluginLayoutInflater: Create UI layouts from resources

### 4. **MVC Pattern**
- Model: AffiliationData, AOIItem, DashboardActivity state
- View: layout XML files (dashboard, main_layout, dialogs)
- Controller: OmniCOTDropDownReceiver, DashboardActivity

### 5. **Adapter Pattern**
- AOIAdapter: Adapt Shape list to RecyclerView display

### 6. **Strategy Pattern**
- Multiple affiliation types (Friendly, Hostile, Unknown, Pending)
- Multiple trigger types (Entry, Exit, Both)

---

## 17. KEY IMPORTS & ATAK DEPENDENCIES

**Map & Event Handling**:
```java
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.MapGroup;
```

**CoT & Messaging**:
```java
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.comms.CotDispatcher;
import com.atakmap.comms.CommsLogger;
import com.atakmap.comms.CommsMapComponent;
```

**Plugin Framework**:
```java
import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.AbstractPluginTool;
import com.atak.plugins.impl.PluginContextProvider;
import com.atak.plugins.impl.PluginLayoutInflater;
```

**UI & Android**:
```java
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.geofence.component.GeoFenceComponent;
import com.atakmap.android.geofence.data.GeoFence;
import androidx.recyclerview.widget.RecyclerView;
```

**Broadcast & Intent**:
```java
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
```

---

## 18. SUMMARY OF CURRENT APPROACH

**Strengths**:
1. Clean separation of concerns with OmniCOTMapComponent managing lifecycle
2. Effective use of CommsLogger for passive CoT monitoring
3. Proper event federation via CotDispatcher
4. Persistent storage of affiliation data
5. Geofence integration for AOI alerting

**Limitations**:
1. Limited to single map click event for COT selection
2. No real-time map item change listeners
3. No persistent map event handlers
4. Manual shape discovery vs map change notifications
5. Activity feed stored only in memory (not persistent)

**Current Event Sources**:
- MapEventDispatcher: ITEM_CLICK events
- CommsMapComponent: CoT message pipeline
- BroadcastReceiver: Geofence breach events
- Button/UI click handlers: User interactions

---

## 19. FILE SIZES & METRICS

```
Component Java Files (12):
├── OmniCOTPlugin.java              16 lines
├── OmniCOTTool.java                28 lines
├── OmniCOTMapComponent.java         70 lines
├── OmniCOTDropDownReceiver.java    495 lines (main controller)
├── DashboardActivity.java          228 lines
├── CotAffiliationListener.java     190 lines
├── AffiliationManager.java         170 lines
├── AffiliationData.java            134 lines
├── AOIItem.java                     75 lines
├── AOIAdapter.java                 133 lines
├── AlertConfigDialog.java          254 lines
└── PluginNativeLoader.java          55 lines

Resource Files (4 XML layouts):
├── omnicot_dashboard.xml           409 lines (main UI)
├── main_layout.xml                 149 lines (management)
├── aoi_list_item.xml                80 lines
└── aoi_alert_config.xml            104 lines
```

