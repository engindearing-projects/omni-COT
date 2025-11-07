# COT Radial Menu Implementation

## Overview

This implementation adds radial menu support for COT (Cursor on Target) items in the OmniCOT plugin. Users can now tap on any COT marker on the map to see a radial menu with affiliation update options.

## Architecture

The implementation follows the ATAK MapMenuFactory pattern as described in the ATAK SDK documentation:

1. **Factory Pattern**: `CotMenuFactory` implements `MapMenuFactory` interface
2. **Menu Resolution**: XML menu assets define menu structure and actions
3. **Factory Chain**: Factories are visited in reverse registration order
4. **Fallback**: Returns null to delegate to default factory if not a COT item

## Components Created

### 1. XML Menu Assets

**Location**: `/home/user/omni-COT/app/src/main/assets/menus/cot_affiliation_menu.xml`

Defines the radial menu structure with buttons for:
- Unknown affiliation
- Assumed Friendly
- Assumed Hostile
- Pending Review
- Show Details (opens existing dropdown UI)

Each menu item broadcasts an Intent with action `com.engindearing.omnicot.UPDATE_AFFILIATION` and an extra containing the affiliation value.

### 2. Menu Filters

**Location**: `/home/user/omni-COT/app/src/main/assets/filters/menu_filters.xml`

Maps COT item types (regex pattern `a-.*`) to the affiliation menu XML resource. This ensures any COT item (types starting with "a-") will show the custom affiliation menu.

### 3. CotMenuFactory

**Location**: `/home/user/omni-COT/app/src/main/java/com/engindearing/omnicot/CotMenuFactory.java`

Implements `MapMenuFactory` interface to create radial menus for COT items:
- Checks if MapItem is a COT item (type starts with "a-")
- Returns null for non-COT items to allow default factory handling
- Uses `MenuResourceFactory` to resolve menus from XML assets
- Loads menu filters to associate COT types with menu resources
- Builds `ConfigEnvironment` with MapItem resolver for metadata access

### 4. CotAffiliationMenuReceiver

**Location**: `/home/user/omni-COT/app/src/main/java/com/engindearing/omnicot/CotAffiliationMenuReceiver.java`

Broadcast receiver that handles menu button clicks:
- Listens for `UPDATE_AFFILIATION` action
- Retrieves the selected MapItem from `MapMenuReceiver.getMenuSubject()`
- Updates COT type based on new affiliation
- Stores affiliation data locally via `AffiliationManager`
- Federates changes to team members via `CotDispatcher`
- Updates dashboard statistics
- Shows confirmation toast to user

**Key Method**: `updateAffiliation(MapItem, Affiliation)` - Core logic matching the existing `OmniCOTDropDownReceiver.updateCotAffiliation()` pattern

### 5. CotMenuEventListener

**Location**: `/home/user/omni-COT/app/src/main/java/com/engindearing/omnicot/CotMenuEventListener.java`

Implements `MapMenuEventListener` for menu lifecycle tracking:
- `onShowMenu(MapItem)` - Called before menu display, logs COT item info
- `onHideMenu(MapItem)` - Called when menu dismissed
- Updates dashboard activity log
- Returns false to allow menu display (true would block it)

### 6. OmniCOTMapComponent Updates

**Location**: `/home/user/omni-COT/app/src/main/java/com/engindearing/omnicot/OmniCOTMapComponent.java`

**Changes**:
- Added import for `MapMenuReceiver`
- Added member variables for menu components
- New method: `registerRadialMenuComponents(MapView)` - Registers factory, listener, and receiver
- New method: `unregisterRadialMenuComponents()` - Cleanup in onDestroy
- Integrated into `onCreate()` and `onDestroyImpl()` lifecycle

**Registration Order**:
1. `MapMenuReceiver.getInstance().registerMapMenuFactory(cotMenuFactory)`
2. `MapMenuReceiver.getInstance().addEventListener(cotMenuEventListener)`
3. `AtakBroadcast.getInstance().registerReceiver(cotMenuReceiver, menuFilter)`

## Data Flow

### When User Taps a COT Item:

1. **Menu Factory Invocation**
   - ATAK MapMenuReceiver calls registered factories in reverse order
   - `CotMenuFactory.create(MapItem)` is called
   - Factory checks if item type starts with "a-"
   - If yes, returns `MapMenuWidget` from XML resource
   - If no, returns null to delegate to default factory

2. **Menu Display**
   - `CotMenuEventListener.onShowMenu(MapItem)` is called
   - Listener logs the COT item details
   - Dashboard activity log is updated
   - Returns false to allow menu display
   - Radial menu appears on screen with affiliation options

3. **User Selects Affiliation**
   - User taps a menu button (e.g., "Assumed Friendly")
   - Menu button broadcasts Intent with action `UPDATE_AFFILIATION`
   - Intent contains extra "affiliation" = "ASSUMED_FRIENDLY"

4. **Affiliation Update**
   - `CotAffiliationMenuReceiver.onReceive(Intent)` is called
   - Receiver extracts affiliation value from intent
   - Retrieves MapItem via `MapMenuReceiver.getMenuSubject()`
   - Calls `updateAffiliation(mapItem, affiliation)`

5. **Update Processing**
   - Parse current COT type to extract dimension (P/A/G/S/U)
   - Map affiliation enum to COT character (f/h/u/p)
   - Build new type: `a-{affiliation}-{dimension}`
   - Update MapItem: `mapItem.setType(newType)`
   - Store affiliation in `AffiliationManager` (SharedPreferences)
   - Create COT event with affiliation detail tag
   - Dispatch via `CotDispatcher` to federate to team
   - Update dashboard statistics
   - Show toast confirmation

6. **Menu Dismissal**
   - User closes menu or menu auto-closes
   - `CotMenuEventListener.onHideMenu(MapItem)` is called
   - Listener logs closure

## Integration with Existing Code

The implementation integrates seamlessly with existing OmniCOT functionality:

### Reuses Existing Classes:
- **AffiliationManager** - For persistent storage (SharedPreferences)
- **AffiliationData** - Data model with enum values
- **CotAffiliationListener** - For receiving affiliation details in COT events
- **DashboardActivity** - Statistics tracking and activity log
- **CotDispatcher** - For federating changes to team members

### Maintains Compatibility:
- Existing dropdown UI (`OmniCOTDropDownReceiver`) still works
- Manual COT selection workflow unchanged
- All existing affiliation tracking features preserved
- Federation protocol identical to dropdown approach

### Adds New Capability:
- **Quick access** - Single tap on COT marker opens menu
- **Contextual** - Menu appears at marker location
- **Efficient** - No need to open dropdown, select COT, update, close
- **Standard UX** - Follows ATAK radial menu patterns

## Menu Lifecycle

### Registration (Plugin Load):
```
OmniCOTMapComponent.onCreate()
  → registerRadialMenuComponents()
    → MapMenuReceiver.registerMapMenuFactory(cotMenuFactory)
    → MapMenuReceiver.addEventListener(cotMenuEventListener)
    → AtakBroadcast.registerReceiver(cotMenuReceiver)
```

### Menu Display (User Taps COT):
```
User taps COT marker
  → MapMenuReceiver calls factories in reverse order
  → CotMenuFactory.create(mapItem)
    → Check if COT item (type starts with "a-")
    → MenuResourceFactory.create(mapItem)
      → Resolve menu from filters (a-.* → cot_affiliation_menu.xml)
      → Parse XML, create MapMenuWidget hierarchy
      → Return widget
  → CotMenuEventListener.onShowMenu(mapItem)
    → Log event, update dashboard
    → Return false (allow display)
  → MapMenuWidget added to MenuLayoutWidget
  → Radial menu rendered on screen
```

### Menu Action (User Selects Option):
```
User taps "Assumed Friendly" button
  → Button broadcasts Intent
    → Action: "com.engindearing.omnicot.UPDATE_AFFILIATION"
    → Extra: "affiliation" = "ASSUMED_FRIENDLY"
  → CotAffiliationMenuReceiver.onReceive(intent)
    → Get MapItem from MapMenuReceiver.getMenuSubject()
    → Parse affiliation value
    → updateAffiliation(mapItem, ASSUMED_FRIENDLY)
      → Update COT type (a-f-{dimension})
      → Store in AffiliationManager
      → Create and dispatch CotEvent
      → Update dashboard statistics
      → Show toast confirmation
```

### Menu Dismissal:
```
User closes menu or menu auto-closes
  → CotMenuEventListener.onHideMenu(mapItem)
    → Log event
  → MapMenuWidget removed from layout
```

### Cleanup (Plugin Unload):
```
OmniCOTMapComponent.onDestroyImpl()
  → unregisterRadialMenuComponents()
    → MapMenuReceiver.unregisterMapMenuFactory(cotMenuFactory)
    → MapMenuReceiver.removeEventListener(cotMenuEventListener)
    → AtakBroadcast.unregisterReceiver(cotMenuReceiver)
```

## Menu Parenting

As documented in ATAK SDK:
- Upon creation, submenu parents point to the MapMenuButtonWidget that contains them
- Full menu hierarchy is resolved at creation time
- When displayed, top-level MapMenuWidget is added to MenuLayoutWidget
- MenuLayoutWidget becomes the parent
- Original parent relationships become invalid after display
- To walk the hierarchy, examine MenuLayoutWidget children

This implementation doesn't currently use submenus (single-level menu), so parenting complexity is minimal.

## MenuResourceFactory Implementation

The implementation uses ATAK's MenuResourceFactory which handles menu resolution from XML assets:

```java
// Create the menu resource factory for resolving menus from XML
menuResourceFactory = new MenuResourceFactory(mapView, mapView.getMapData(), mapAssets, adapter);

// Later, when creating the menu widget:
MapMenuWidget menuWidget = menuResourceFactory.create(mapItem);
```

The MenuResourceFactory automatically resolves the appropriate menu from XML based on the menu filters and the MapItem type. In ATAK 5.4.0, the PhraseParser and ConfigEnvironment approach from earlier versions is no longer needed - the MenuResourceFactory constructor takes the required parameters directly.

## Required Icons

The menu XML references the following drawable resources that need to be created:

1. **ic_menu_unknown** - Icon for "Unknown" affiliation button
2. **ic_menu_friendly** - Icon for "Assumed Friendly" button
3. **ic_menu_hostile** - Icon for "Assumed Hostile" button
4. **ic_menu_pending** - Icon for "Pending Review" button
5. **ic_menu_info** - Icon for "Show Details" button

**Location**: `/home/user/omni-COT/app/src/main/res/drawable/`

**Recommendations**:
- Use vector drawables (XML) for scalability
- Follow ATAK icon style guidelines
- Consider color-coding: blue for friendly, red for hostile, gray for unknown, yellow for pending
- Size: 24dp x 24dp for menu icons

## Testing Checklist

### Functional Testing:

- [ ] **Menu Display**
  - [ ] Tap on COT marker shows radial menu
  - [ ] Menu contains 5 buttons (Unknown, Friendly, Hostile, Pending, Details)
  - [ ] Icons display correctly
  - [ ] Menu appears at marker location

- [ ] **Affiliation Updates**
  - [ ] Tap "Unknown" updates item to Unknown affiliation
  - [ ] Tap "Assumed Friendly" updates item to Friendly
  - [ ] Tap "Assumed Hostile" updates item to Hostile
  - [ ] Tap "Pending Review" updates item to Pending
  - [ ] COT type changes correctly (a-{affiliation}-{dimension})
  - [ ] Toast confirmation appears

- [ ] **Data Persistence**
  - [ ] Affiliation stored in SharedPreferences
  - [ ] Reload plugin - affiliation persists
  - [ ] Affiliation retrieved correctly for subsequent menu opens

- [ ] **Federation**
  - [ ] COT event dispatched when affiliation updated
  - [ ] Team members receive affiliation update
  - [ ] Affiliation detail tag includes correct attributes:
    - [ ] affiliation value
    - [ ] markedBy (callsign)
    - [ ] timestamp
    - [ ] notes (empty for quick menu updates)

- [ ] **Dashboard Integration**
  - [ ] COT Modified counter increments
  - [ ] Activity log shows "Updated affiliation: {title} -> {value}"
  - [ ] Activity log shows "Opened menu for: {title}"

- [ ] **Show Details**
  - [ ] Tap "Show Details" opens existing dropdown UI
  - [ ] Dropdown shows full affiliation info
  - [ ] Selected COT is pre-populated in dropdown

- [ ] **Non-COT Items**
  - [ ] Tap on non-COT marker shows default menu
  - [ ] Factory returns null for non-COT items
  - [ ] No errors in logs

- [ ] **Lifecycle**
  - [ ] Menu event listener logs menu show/hide
  - [ ] No memory leaks
  - [ ] Clean unregistration on plugin unload

### Edge Cases:

- [ ] **Null MapItem**
  - [ ] Factory handles null MapItem gracefully
  - [ ] Returns null to delegate to default factory

- [ ] **Invalid COT Type**
  - [ ] Items with malformed types (e.g., "a-x") handled gracefully
  - [ ] Defaults to "Unknown" dimension if type parsing fails

- [ ] **No Network**
  - [ ] Local update succeeds
  - [ ] Federation fails gracefully with toast message
  - [ ] "Updated locally, federation may have failed" shown

- [ ] **Concurrent Updates**
  - [ ] Multiple users updating same item
  - [ ] Last-write-wins behavior
  - [ ] No race conditions

### Performance:

- [ ] **Menu Creation**
  - [ ] Menu displays quickly (< 500ms)
  - [ ] No lag when tapping markers

- [ ] **Memory**
  - [ ] MenuResourceFactory doesn't leak
  - [ ] MapAssets properly managed
  - [ ] No retained references after menu close

- [ ] **XML Parsing**
  - [ ] Menu filters loaded once at startup
  - [ ] Not reloaded on every menu display

### Log Verification:

Check logcat for expected messages:

```
CotMenuFactory: Loaded menu filters successfully
CotMenuFactory: CotMenuFactory initialized
OmniCOTMapComponent: Registered CotMenuFactory with MapMenuReceiver
OmniCOTMapComponent: Registered CotMenuEventListener for menu lifecycle events
OmniCOTMapComponent: Registered CotAffiliationMenuReceiver for menu actions

[When user taps COT marker]
CotMenuFactory: Creating radial menu for COT item: {title} (type: {type})
CotMenuFactory: Successfully created menu widget for COT item
CotMenuEventListener: Showing radial menu for COT item: {title} (type: {type}, uid: {uid})

[When user selects affiliation]
CotAffiliationMenuReceiver: Updating affiliation for {title} to {affiliation}
CotAffiliationMenuReceiver: Federated affiliation update for {uid}
CotAffiliationMenuReceiver: Successfully updated affiliation: {title} -> {type} (Custom: {affiliation})

[When menu closes]
CotMenuEventListener: Menu hidden for COT item: {title} (type: {type})
```

## Known Limitations

1. **ATAK SDK Version Dependencies**
   - Implementation assumes ATAK 4.5+ with MapMenuFactory API
   - May need adjustments for older ATAK versions
   - PhraseParser and ConfigEnvironment API may vary

2. **Icon Resources**
   - Menu icons not included in this implementation
   - Placeholder references in XML need actual drawables
   - Menu will show text labels without icons until added

3. **Network Compilation**
   - Build not verified due to sandbox network restrictions
   - ATAK SDK class paths assumed based on documentation
   - May need package import adjustments

4. **Submenu Support**
   - Current implementation is single-level menu
   - No submenus for advanced options
   - Could be extended for dimension selection, etc.

5. **Menu Customization**
   - Menu structure fixed in XML
   - No runtime menu item addition/removal
   - Advanced customization would require MenuResourceFactory override

## Future Enhancements

### Potential Improvements:

1. **Dynamic Menu Items**
   - Show/hide buttons based on current affiliation
   - Disable current affiliation button
   - Add recently used affiliations

2. **Submenus**
   - Affiliation submenu → dimension selection
   - Advanced options submenu
   - Notes/comments submenu

3. **Visual Feedback**
   - Highlight current affiliation with icon badge
   - Show timestamp of last update
   - Display who marked the affiliation

4. **Batch Operations**
   - Multi-select mode for bulk affiliation updates
   - Apply affiliation to all items in AOI
   - Filter view by affiliation

5. **Contextual Menus**
   - Different menus for different COT dimensions
   - Airborne items show altitude options
   - Ground items show terrain options

6. **Integration**
   - Link to remote ID drone data
   - Auto-affiliation based on AOI rules
   - AI/ML affiliation suggestions

## API Compatibility

### Backwards Compatible:
- Existing dropdown UI workflow unchanged
- All current API methods still available
- No breaking changes to public interfaces

### New APIs:
- Radial menu appears automatically on COT tap
- Users don't need to know about menu system
- Transparent integration with existing features

### Migration Path:
- No migration required
- Both dropdown and radial menu work simultaneously
- Users can choose preferred workflow

## Conclusion

This implementation provides a streamlined, efficient way for users to update COT affiliation directly from the map view. By following the ATAK MapMenuFactory pattern, it integrates seamlessly with the existing OmniCOT plugin architecture while adding powerful new capability.

The factory pattern ensures proper fallback behavior, menu lifecycle events enable tracking and analytics, and broadcast receivers handle actions in a decoupled manner. The implementation reuses existing affiliation management and federation code, ensuring consistency and reliability.

Testing in a live ATAK environment with real COT items will validate the implementation and may reveal minor adjustments needed for ATAK SDK class paths or API variations.
