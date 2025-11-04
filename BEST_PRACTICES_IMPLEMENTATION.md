# ATAK Map Event Handling Best Practices Implementation

## Overview

This document describes the implementation of ATAK best practices for Map Event Handling in the OmniCOT plugin. The plugin now follows all recommended patterns from the official ATAK development guidelines.

## Changes Made

### 1. Created EventHandlerManager Class

**File**: `app/src/main/java/com/engindearing/omnicot/EventHandlerManager.java`

A centralized event management system that implements all ATAK best practices:

#### Key Features:

- **Proper Listener Stack Management**: Uses `pushListeners()` and `popListeners()` for temporary event handling
- **Type-Specific Subscriptions**: Efficient event filtering using specific event types
- **Persistent Event Monitoring**: Continuous monitoring of COT and AOI changes
- **Lifecycle-Aware Cleanup**: Proper disposal of all listeners and resources
- **Callback-Based Architecture**: Decoupled communication using callback interfaces

#### Event Coverage:

| Event Type | Purpose | Implementation |
|------------|---------|----------------|
| `ITEM_ADDED` | Track new COT markers | Auto-track in AffiliationManager |
| `ITEM_REMOVED` | Cleanup removed COT markers | Log removal, maintain history |
| `ITEM_DRAG_DROPPED` | Monitor COT repositioning | Update location, log movement |
| `GROUP_ADDED` | Detect new AOI shapes | Notify callbacks, refresh UI |
| `GROUP_REMOVED` | Track deleted AOI shapes | Notify callbacks, refresh UI |
| Geofence Breach | AOI entry/exit detection | Alert user, track in dashboard |

### 2. Refactored OmniCOTDropDownReceiver

**File**: `app/src/main/java/com/engindearing/omnicot/OmniCOTDropDownReceiver.java`

#### Changes:

1. **Integrated EventHandlerManager**
   - Replaced manual listener management
   - Added persistent listeners for real-time monitoring
   - Implemented callback interfaces for event handling

2. **Improved COT Selection**
   ```java
   // Before: Manual listener with potential cleanup issues
   mapView.getMapEventDispatcher().addMapEventListener(MapEvent.ITEM_CLICK, listener);

   // After: Proper stack management with automatic cleanup
   eventHandlerManager.startCotSelection(callback);
   ```

3. **Added Real-Time Monitoring**
   - Dashboard auto-updates when COT markers added/removed
   - AOI list auto-refreshes when shapes added/removed
   - Movement tracking for COT markers

4. **Proper Lifecycle Management**
   - Added cleanup in `disposeImpl()`
   - Cancel COT selection on dropdown close
   - Clear all references on disposal

## Best Practices Implemented

### 1. Listener Stack Management

**Best Practice**: Use `pushListeners()` and `popListeners()` to temporarily suppress event processing.

**Implementation**:
```java
// When starting COT selection
eventDispatcher.pushListeners();          // Save current state
eventDispatcher.clearListeners(MapEvent.ITEM_CLICK);  // Clear conflicting handlers
eventDispatcher.addMapEventListener(MapEvent.ITEM_CLICK, ourListener);  // Add our handler

// When completing/cancelling selection
eventDispatcher.popListeners();           // Restore previous state
```

**Benefits**:
- Prevents other event handlers from interfering
- Automatically restores previous behavior
- No risk of leaving listeners in wrong state

### 2. Type-Specific Subscriptions

**Best Practice**: Use `addMapEventListener(String, MapEventDispatchListener)` for efficient event filtering.

**Implementation**:
```java
// Instead of listening to ALL events
eventDispatcher.addMapEventListener(globalListener);  // ❌ Inefficient

// Listen only to specific event types
eventDispatcher.addMapEventListener(MapEvent.ITEM_ADDED, itemAddedListener);  // ✅ Efficient
eventDispatcher.addMapEventListener(MapEvent.ITEM_REMOVED, itemRemovedListener);  // ✅ Efficient
```

**Benefits**:
- Better performance (less event processing)
- Clearer code (explicit event handling)
- Easier debugging (specific listeners for specific events)

### 3. Persistent vs. Temporary Listeners

**Best Practice**: Use persistent listeners for continuous monitoring, temporary listeners for workflows.

**Implementation**:
- **Persistent**: COT/AOI monitoring (registered once, active for plugin lifetime)
- **Temporary**: COT selection (registered during workflow, removed after completion)

### 4. Proper Cleanup

**Best Practice**: Always clean up listeners in lifecycle methods.

**Implementation**:
```java
@Override
protected void disposeImpl() {
    if (eventHandlerManager != null) {
        eventHandlerManager.dispose();  // Removes all listeners
    }
}

@Override
public void onDropDownClose() {
    eventHandlerManager.cancelCotSelection();  // Cancel active workflows
}
```

**Benefits**:
- Prevents memory leaks
- Avoids ghost event handlers
- Clean plugin shutdown

## Architecture

### Event Flow

```
User Action → EventHandlerManager → Callbacks → UI Update
                     ↓
              MapEventDispatcher
                     ↓
              ATAK Core Events
```

### Component Responsibilities

| Component | Responsibility |
|-----------|----------------|
| **EventHandlerManager** | Centralized event management, listener lifecycle |
| **OmniCOTDropDownReceiver** | UI logic, user interaction, display updates |
| **AffiliationManager** | Data persistence, affiliation tracking |
| **DashboardActivity** | Statistics display, activity logging |

## Real-Time Features

### 1. Auto-Tracking New COT Markers

When a new COT marker appears on the map:
1. `ITEM_ADDED` event fires
2. EventHandlerManager detects it's a COT marker
3. Auto-tracks with `UNKNOWN` affiliation
4. Updates dashboard statistics
5. Logs activity

### 2. Auto-Refreshing AOI List

When AOI shapes change:
1. `ITEM_ADDED` or `ITEM_REMOVED` event fires
2. EventHandlerManager detects it's a Shape
3. Triggers AOI callback
4. UI auto-refreshes if AOI management is visible

### 3. Movement Tracking

When COT markers move:
1. `ITEM_DRAG_DROPPED` event fires
2. EventHandlerManager captures new location
3. Updates affiliation manager timestamp
4. Logs movement activity

### 4. Geofence Monitoring

When COT enters/exits AOI:
1. Android broadcasts geofence breach event
2. EventHandlerManager receives broadcast
3. Displays alert to user
4. Increments dashboard counter
5. Logs activity

## Testing Recommendations

### Manual Testing

1. **COT Selection**
   - Open COT management
   - Click "Select COT to Modify"
   - Tap a COT marker
   - Verify other click handlers don't interfere
   - Close dropdown mid-selection → verify cleanup

2. **Real-Time Monitoring**
   - Open dashboard
   - Add new COT marker (via ATAK)
   - Verify dashboard updates automatically
   - Remove COT marker
   - Verify dashboard updates

3. **AOI Tracking**
   - Open AOI management
   - Draw new shape (via ATAK tools)
   - Verify list auto-refreshes
   - Delete shape
   - Verify list auto-refreshes

4. **Geofence Alerts**
   - Create AOI with geofence alert
   - Move COT marker into AOI
   - Verify alert displays
   - Move COT marker out of AOI
   - Verify exit alert

### Code Review Checklist

- ✅ All listeners registered have corresponding unregister calls
- ✅ Push/pop calls are balanced (every push has a pop)
- ✅ disposeImpl() cleans up all resources
- ✅ No memory leaks (all references cleared)
- ✅ Callbacks are null-checked before invocation
- ✅ Event types are specific (not global subscription)

## Migration Guide

For other ATAK plugins wanting to adopt these practices:

### Step 1: Create Event Manager

Create a centralized event management class similar to `EventHandlerManager.java`.

### Step 2: Identify Event Needs

Categorize events into:
- **Persistent**: Continuous monitoring (ITEM_ADDED, ITEM_REMOVED)
- **Temporary**: Workflow-specific (ITEM_CLICK during selection)

### Step 3: Use Push/Pop Stack

For temporary workflows:
```java
dispatcher.pushListeners();
dispatcher.clearListeners(eventType);
dispatcher.addMapEventListener(eventType, yourListener);
// ... workflow ...
dispatcher.popListeners();
```

### Step 4: Add Proper Cleanup

In component lifecycle methods:
```java
@Override
protected void onDestroyImpl() {
    if (eventManager != null) {
        eventManager.dispose();
    }
}
```

## Benefits Achieved

### 1. Robustness
- ✅ No listener leaks
- ✅ Proper cleanup on all paths
- ✅ No interference between handlers

### 2. Performance
- ✅ Type-specific subscriptions (efficient filtering)
- ✅ Minimal event processing overhead

### 3. Maintainability
- ✅ Centralized event logic
- ✅ Clear separation of concerns
- ✅ Easy to add new event handlers

### 4. User Experience
- ✅ Real-time updates
- ✅ No manual refresh needed
- ✅ Immediate feedback on map changes

### 5. Open Source Ready
- ✅ Well-documented code
- ✅ Clear best practices
- ✅ Suitable for civilian ATAK use

## Event Type Reference

### Map Events (Not Currently Used)
- `MAP_CLICK`: Map clicked (possibly double tap)
- `MAP_CONFIRMED_CLICK`: Map clicked (confirmed not double tap)
- `MAP_MOVED`: Map moved programmatically or by user
- `MAP_LONG_PRESS`: Map long pressed

### Item Events (Used in Plugin)
- `ITEM_ADDED`: ✅ MapItem added to map
- `ITEM_REMOVED`: ✅ MapItem removed from map
- `ITEM_CLICK`: ✅ MapItem clicked (used for COT selection)
- `ITEM_DRAG_DROPPED`: ✅ MapItem drag completed

### Group Events (Monitored via ITEM_ADDED/REMOVED)
- `GROUP_ADDED`: Deprecated (use ITEM_ADDED with instanceof Shape)
- `GROUP_REMOVED`: Deprecated (use ITEM_REMOVED with instanceof Shape)

## Future Enhancements

Potential improvements for future versions:

1. **Long Press COT Selection**: Use `ITEM_LONG_PRESS` for alternative selection method
2. **Map Movement Tracking**: Use `MAP_MOVED` to detect when user pans to new area
3. **Double Tap Actions**: Use `ITEM_DOUBLE_TAP` for quick actions
4. **Drag Preview**: Use `ITEM_DRAG_CONTINUED` for live preview during drag

## Conclusion

The OmniCOT plugin now implements all ATAK best practices for Map Event Handling, making it:
- **Robust**: Proper lifecycle management, no leaks
- **Efficient**: Type-specific subscriptions, minimal overhead
- **Maintainable**: Centralized logic, clear architecture
- **Real-time**: Automatic updates, no manual refresh
- **Open Source Ready**: Well-documented, suitable for civilian use

This implementation serves as a reference for other ATAK plugin developers looking to follow best practices.

## References

- ATAK Plugin Development Documentation
- MapEventDispatcher API
- ATAK Best Practices Guide

---

**Plugin Version**: OmniCOT v1.0
**ATAK Version**: Compatible with ATAK 4.10+
**Last Updated**: 2025-11-04
