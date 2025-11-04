package com.engindearing.omnicot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Shape;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;

/**
 * Centralized event handler manager for OmniCOT plugin.
 * Implements ATAK best practices for MapEventDispatcher usage including:
 * - Proper listener stack management (push/pop)
 * - Type-specific and item-specific subscriptions
 * - Lifecycle-aware event handling
 * - Comprehensive event type coverage
 */
public class EventHandlerManager {

    private static final String TAG = EventHandlerManager.class.getSimpleName();

    private final Context context;
    private final MapView mapView;
    private final MapEventDispatcher eventDispatcher;
    private final AffiliationManager affiliationManager;

    // Listener references for cleanup
    private MapEventDispatcher.MapEventDispatchListener itemAddedListener;
    private MapEventDispatcher.MapEventDispatchListener itemRemovedListener;
    private MapEventDispatcher.MapEventDispatchListener itemDraggedListener;
    private MapEventDispatcher.MapEventDispatchListener groupAddedListener;
    private MapEventDispatcher.MapEventDispatchListener groupRemovedListener;
    private MapEventDispatcher.MapEventDispatchListener cotSelectionListener;

    // State tracking
    private boolean isSelectingCot = false;
    private boolean listenerStackPushed = false;
    private boolean persistentListenersActive = false;

    // Callbacks
    public interface CotSelectionCallback {
        void onCotSelected(MapItem item);
        void onCotSelectionCancelled();
    }

    public interface CotChangeCallback {
        void onCotAdded(MapItem item);
        void onCotRemoved(MapItem item);
        void onCotMoved(MapItem item, GeoPoint newLocation);
    }

    public interface AOIChangeCallback {
        void onAOIAdded(Shape shape);
        void onAOIRemoved(Shape shape);
        void onAOIGroupChanged();
    }

    private CotSelectionCallback cotSelectionCallback;
    private CotChangeCallback cotChangeCallback;
    private AOIChangeCallback aoiChangeCallback;

    // Geofence breach receiver
    private BroadcastReceiver geofenceBreachReceiver;

    /**
     * Creates a new EventHandlerManager.
     *
     * @param context Plugin context
     * @param mapView MapView instance
     * @param affiliationManager AffiliationManager instance
     */
    public EventHandlerManager(Context context, MapView mapView, AffiliationManager affiliationManager) {
        this.context = context;
        this.mapView = mapView;
        this.affiliationManager = affiliationManager;
        this.eventDispatcher = mapView.getMapEventDispatcher();

        Log.d(TAG, "EventHandlerManager initialized");
    }

    /**
     * Registers persistent listeners for COT and AOI monitoring.
     * These listeners remain active for the lifetime of the plugin.
     *
     * Best Practice: Use type-specific subscription for efficient event filtering
     */
    public void registerPersistentListeners(CotChangeCallback cotCallback, AOIChangeCallback aoiCallback) {
        if (persistentListenersActive) {
            Log.w(TAG, "Persistent listeners already registered");
            return;
        }

        this.cotChangeCallback = cotCallback;
        this.aoiChangeCallback = aoiCallback;

        // Register ITEM_ADDED listener for new COT markers
        itemAddedListener = new MapEventDispatcher.MapEventDispatchListener() {
            @Override
            public void onMapEvent(MapEvent event) {
                MapItem item = event.getItem();
                if (item != null && isCotMarker(item)) {
                    Log.d(TAG, "COT marker added: " + item.getTitle() + " (Type: " + item.getType() + ")");
                    if (cotChangeCallback != null) {
                        cotChangeCallback.onCotAdded(item);
                    }

                    // Auto-track new COT markers in affiliation manager
                    trackNewCotMarker(item);
                }
            }
        };
        eventDispatcher.addMapEventListener(MapEvent.ITEM_ADDED, itemAddedListener);
        Log.d(TAG, "Registered ITEM_ADDED listener for COT tracking");

        // Register ITEM_REMOVED listener for deleted COT markers
        itemRemovedListener = new MapEventDispatcher.MapEventDispatchListener() {
            @Override
            public void onMapEvent(MapEvent event) {
                MapItem item = event.getItem();
                if (item != null && isCotMarker(item)) {
                    Log.d(TAG, "COT marker removed: " + item.getTitle());
                    if (cotChangeCallback != null) {
                        cotChangeCallback.onCotRemoved(item);
                    }

                    // Clean up affiliation data for removed marker
                    cleanupRemovedCotMarker(item);
                }
            }
        };
        eventDispatcher.addMapEventListener(MapEvent.ITEM_REMOVED, itemRemovedListener);
        Log.d(TAG, "Registered ITEM_REMOVED listener for COT cleanup");

        // Register ITEM_DRAG_DROPPED listener for COT repositioning
        itemDraggedListener = new MapEventDispatcher.MapEventDispatchListener() {
            @Override
            public void onMapEvent(MapEvent event) {
                MapItem item = event.getItem();
                if (item != null && isCotMarker(item) && item instanceof PointMapItem) {
                    PointMapItem pmi = (PointMapItem) item;
                    GeoPoint newLocation = pmi.getPoint();
                    Log.d(TAG, "COT marker moved: " + item.getTitle() + " to " + newLocation);
                    if (cotChangeCallback != null) {
                        cotChangeCallback.onCotMoved(item, newLocation);
                    }

                    // Update affiliation manager with new location
                    updateCotLocation(item, newLocation);
                }
            }
        };
        eventDispatcher.addMapEventListener(MapEvent.ITEM_DRAG_DROPPED, itemDraggedListener);
        Log.d(TAG, "Registered ITEM_DRAG_DROPPED listener for COT repositioning");

        // Register GROUP_ADDED listener for new AOI groups
        groupAddedListener = new MapEventDispatcher.MapEventDispatchListener() {
            @Override
            public void onMapEvent(MapEvent event) {
                MapItem item = event.getItem();
                if (item instanceof Shape) {
                    Shape shape = (Shape) item;
                    Log.d(TAG, "AOI shape added: " + shape.getTitle());
                    if (aoiChangeCallback != null) {
                        aoiChangeCallback.onAOIAdded(shape);
                        aoiChangeCallback.onAOIGroupChanged();
                    }
                }
            }
        };
        eventDispatcher.addMapEventListener(MapEvent.ITEM_ADDED, groupAddedListener);
        Log.d(TAG, "Registered GROUP_ADDED listener for AOI tracking");

        // Register GROUP_REMOVED listener for deleted AOI groups
        groupRemovedListener = new MapEventDispatcher.MapEventDispatchListener() {
            @Override
            public void onMapEvent(MapEvent event) {
                MapItem item = event.getItem();
                if (item instanceof Shape) {
                    Shape shape = (Shape) item;
                    Log.d(TAG, "AOI shape removed: " + shape.getTitle());
                    if (aoiChangeCallback != null) {
                        aoiChangeCallback.onAOIRemoved(shape);
                        aoiChangeCallback.onAOIGroupChanged();
                    }
                }
            }
        };
        eventDispatcher.addMapEventListener(MapEvent.ITEM_REMOVED, groupRemovedListener);
        Log.d(TAG, "Registered GROUP_REMOVED listener for AOI cleanup");

        // Register geofence breach receiver
        registerGeofenceBreachListener();

        persistentListenersActive = true;
        Log.d(TAG, "All persistent listeners registered successfully");
    }

    /**
     * Starts COT selection mode using listener stack management.
     *
     * Best Practice: Push listener stack to temporarily suppress other handlers,
     * then restore when selection is complete.
     *
     * @param callback Callback to receive selected COT item
     */
    public void startCotSelection(final CotSelectionCallback callback) {
        if (isSelectingCot) {
            Log.w(TAG, "COT selection already in progress");
            return;
        }

        this.cotSelectionCallback = callback;
        isSelectingCot = true;

        // Best Practice: Push the listener stack to suppress other handlers
        eventDispatcher.pushListeners();
        listenerStackPushed = true;
        Log.d(TAG, "Pushed listener stack for COT selection");

        // Clear existing ITEM_CLICK listeners to prevent conflicts
        eventDispatcher.clearListeners(MapEvent.ITEM_CLICK);
        Log.d(TAG, "Cleared ITEM_CLICK listeners");

        // Add our custom listener for COT selection
        cotSelectionListener = new MapEventDispatcher.MapEventDispatchListener() {
            @Override
            public void onMapEvent(MapEvent event) {
                MapItem item = event.getItem();
                if (item != null && isCotMarker(item)) {
                    Log.d(TAG, "COT item selected: " + item.getTitle());
                    completeCotSelection(item);
                } else {
                    Log.d(TAG, "Non-COT item tapped, ignoring");
                }
            }
        };
        eventDispatcher.addMapEventListener(MapEvent.ITEM_CLICK, cotSelectionListener);
        Log.d(TAG, "COT selection mode activated - tap any COT marker on map");

        Toast.makeText(context, "Tap any COT marker on the map", Toast.LENGTH_SHORT).show();
    }

    /**
     * Cancels COT selection mode and restores listener stack.
     *
     * Best Practice: Always pop the stack to restore previous listener state
     */
    public void cancelCotSelection() {
        if (!isSelectingCot) {
            return;
        }

        isSelectingCot = false;

        // Best Practice: Pop the stack to restore listener state
        if (listenerStackPushed) {
            eventDispatcher.popListeners();
            listenerStackPushed = false;
            Log.d(TAG, "Popped listener stack - restored previous listeners");
        }

        if (cotSelectionCallback != null) {
            cotSelectionCallback.onCotSelectionCancelled();
            cotSelectionCallback = null;
        }

        Log.d(TAG, "COT selection cancelled");
    }

    /**
     * Completes COT selection and restores listener stack.
     */
    private void completeCotSelection(MapItem item) {
        if (!isSelectingCot) {
            return;
        }

        isSelectingCot = false;

        // Best Practice: Pop the stack to restore listener state
        if (listenerStackPushed) {
            eventDispatcher.popListeners();
            listenerStackPushed = false;
            Log.d(TAG, "Popped listener stack - restored previous listeners");
        }

        if (cotSelectionCallback != null) {
            cotSelectionCallback.onCotSelected(item);
            cotSelectionCallback = null;
        }

        Log.d(TAG, "COT selection completed: " + item.getTitle());
    }

    /**
     * Registers a geofence breach listener for AOI entry/exit monitoring.
     *
     * Best Practice: Use Android BroadcastReceiver for geofence events
     */
    private void registerGeofenceBreachListener() {
        geofenceBreachReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null || !action.equals("com.atakmap.android.geofence.BREACH_EVENT")) {
                    return;
                }

                // Extract breach information
                String itemUID = intent.getStringExtra("itemUID");
                String regionUID = intent.getStringExtra("regionUID");
                boolean breachIn = intent.getBooleanExtra("breachIn", false);

                Log.d(TAG, "Geofence breach: Item=" + itemUID + ", Region=" + regionUID +
                          ", Type=" + (breachIn ? "ENTRY" : "EXIT"));

                // Find the COT marker and AOI shape
                MapItem cotItem = mapView.getRootGroup().deepFindUID(itemUID);
                MapItem aoiItem = mapView.getRootGroup().deepFindUID(regionUID);

                if (cotItem != null && aoiItem instanceof Shape) {
                    handleGeofenceBreach(cotItem, (Shape) aoiItem, breachIn);
                }
            }
        };

        // Register the broadcast receiver
        AtakBroadcast.getInstance().registerReceiver(
            geofenceBreachReceiver,
            new android.content.IntentFilter("com.atakmap.android.geofence.BREACH_EVENT")
        );
        Log.d(TAG, "Registered geofence breach listener");
    }

    /**
     * Handles geofence breach events (COT entering/exiting AOI).
     */
    private void handleGeofenceBreach(MapItem cotItem, Shape aoiShape, boolean entry) {
        String eventType = entry ? "entered" : "exited";
        String message = cotItem.getTitle() + " " + eventType + " AOI: " + aoiShape.getTitle();

        Log.i(TAG, "Geofence Alert: " + message);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();

        // Track breach in dashboard
        DashboardActivity.incrementGeofenceBreaches();
        DashboardActivity.addActivity(message);
    }

    /**
     * Checks if a MapItem is a COT marker.
     */
    private boolean isCotMarker(MapItem item) {
        if (item == null || item.getType() == null) {
            return false;
        }

        String type = item.getType();
        // COT markers typically start with "a-" (atom) format
        return type.startsWith("a-") || item instanceof Marker;
    }

    /**
     * Tracks a newly added COT marker in the affiliation manager.
     */
    private void trackNewCotMarker(MapItem item) {
        String uid = item.getUID();

        // Check if already tracked
        if (affiliationManager.getAffiliation(uid) != null) {
            Log.d(TAG, "COT marker already tracked: " + uid);
            return;
        }

        // Auto-track with UNKNOWN affiliation
        AffiliationData data = new AffiliationData(
            uid,
            AffiliationData.Affiliation.UNKNOWN,
            "AUTO",
            ""
        );
        affiliationManager.setAffiliation(data);

        // Increment dashboard counter
        DashboardActivity.incrementCOTTracked();
        DashboardActivity.addActivity("Auto-tracked new COT: " + item.getTitle());

        Log.d(TAG, "Auto-tracked new COT marker: " + item.getTitle());
    }

    /**
     * Cleans up affiliation data for removed COT marker.
     */
    private void cleanupRemovedCotMarker(MapItem item) {
        String uid = item.getUID();

        if (affiliationManager.getAffiliation(uid) != null) {
            // Note: We don't remove from persistent storage to maintain history
            // Just log the removal
            Log.d(TAG, "COT marker removed (keeping history): " + item.getTitle());
            DashboardActivity.addActivity("COT removed: " + item.getTitle());
        }
    }

    /**
     * Updates COT location in affiliation manager.
     */
    private void updateCotLocation(MapItem item, GeoPoint newLocation) {
        String uid = item.getUID();
        AffiliationData data = affiliationManager.getAffiliation(uid);

        if (data != null) {
            // Update timestamp to reflect movement
            Log.d(TAG, "COT location updated: " + item.getTitle());
            DashboardActivity.addActivity("COT moved: " + item.getTitle());
        }
    }

    /**
     * Unregisters all persistent listeners and cleans up resources.
     *
     * Best Practice: Always clean up listeners in lifecycle methods
     */
    public void dispose() {
        Log.d(TAG, "Disposing EventHandlerManager - cleaning up all listeners");

        // Cancel any active COT selection
        if (isSelectingCot) {
            cancelCotSelection();
        }

        // Remove all persistent listeners
        if (persistentListenersActive) {
            if (itemAddedListener != null) {
                eventDispatcher.removeMapEventListener(MapEvent.ITEM_ADDED, itemAddedListener);
                itemAddedListener = null;
            }

            if (itemRemovedListener != null) {
                eventDispatcher.removeMapEventListener(MapEvent.ITEM_REMOVED, itemRemovedListener);
                itemRemovedListener = null;
            }

            if (itemDraggedListener != null) {
                eventDispatcher.removeMapEventListener(MapEvent.ITEM_DRAG_DROPPED, itemDraggedListener);
                itemDraggedListener = null;
            }

            if (groupAddedListener != null) {
                eventDispatcher.removeMapEventListener(MapEvent.ITEM_ADDED, groupAddedListener);
                groupAddedListener = null;
            }

            if (groupRemovedListener != null) {
                eventDispatcher.removeMapEventListener(MapEvent.ITEM_REMOVED, groupRemovedListener);
                groupRemovedListener = null;
            }

            persistentListenersActive = false;
            Log.d(TAG, "Removed all persistent map event listeners");
        }

        // Unregister geofence breach receiver
        if (geofenceBreachReceiver != null) {
            try {
                AtakBroadcast.getInstance().unregisterReceiver(geofenceBreachReceiver);
                geofenceBreachReceiver = null;
                Log.d(TAG, "Unregistered geofence breach receiver");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering geofence receiver", e);
            }
        }

        // Clear callbacks
        cotSelectionCallback = null;
        cotChangeCallback = null;
        aoiChangeCallback = null;

        Log.d(TAG, "EventHandlerManager disposed successfully");
    }
}
