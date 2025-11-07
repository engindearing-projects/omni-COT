package com.engindearing.omnicot;

import android.content.Context;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.assets.MapAssets;
import com.atakmap.android.menu.MapMenuFactory;
import com.atakmap.android.menu.MapMenuWidget;
import com.atakmap.android.menu.MenuResourceFactory;
import com.atakmap.android.menu.MenuMapAdapter;
import com.atakmap.coremap.log.Log;

import java.io.IOException;

/**
 * Factory for creating radial menus for COT items to allow quick affiliation updates.
 * This factory is registered with MapMenuReceiver and will be consulted when a user
 * taps on a COT marker on the map.
 *
 * Factories are visited in reverse order of registration, so this factory will be
 * consulted before the default factory. If this factory returns null, the next
 * factory in the chain will be consulted.
 */
public class CotMenuFactory implements MapMenuFactory {

    private static final String TAG = CotMenuFactory.class.getSimpleName();

    private final Context context;
    private final MapView mapView;
    private final MenuResourceFactory menuResourceFactory;

    /**
     * Creates a new CotMenuFactory.
     *
     * @param context Application context for loading assets
     * @param mapView The MapView instance
     */
    public CotMenuFactory(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;

        // Initialize menu resource factory with menu filters
        MapAssets mapAssets = new MapAssets(context);
        MenuMapAdapter adapter = new MenuMapAdapter();

        try {
            // Load menu filters that associate COT types with menu resources
            adapter.loadMenuFilters(mapAssets, "filters/menu_filters.xml");
            Log.d(TAG, "Loaded menu filters successfully");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load menu filters", e);
        }

        // Create the menu resource factory for resolving menus from XML
        menuResourceFactory = new MenuResourceFactory(mapView, mapView.getMapData(), mapAssets, adapter);

        Log.d(TAG, "CotMenuFactory initialized");
    }

    /**
     * Creates a MapMenuWidget for the given MapItem.
     *
     * This method is called when a user taps on a MapItem. If the item is a COT item
     * (type starts with "a-"), this factory will provide a radial menu for updating
     * affiliation. Otherwise, it returns null to allow the next factory to handle it.
     *
     * @param mapItem The MapItem that was tapped, may be null
     * @return A MapMenuWidget for the COT item, or null if not a COT item
     */
    @Override
    public MapMenuWidget create(MapItem mapItem) {
        // If no MapItem, return null to let default factory handle it
        if (mapItem == null) {
            Log.d(TAG, "MapItem is null, returning null to delegate to next factory");
            return null;
        }

        String itemType = mapItem.getType();

        // Check if this is a COT item (type starts with "a-")
        if (itemType == null || !itemType.startsWith("a-")) {
            Log.d(TAG, "Not a COT item (type: " + itemType + "), returning null");
            return null;
        }

        // This is a COT item, create menu from our XML definition
        Log.d(TAG, "Creating radial menu for COT item: " + mapItem.getTitle() +
                   " (type: " + itemType + ")");

        try {
            // Resolve menu from XML resource
            // The menu_filters.xml will map COT types (a-.*) to cot_affiliation_menu.xml
            MapMenuWidget menuWidget = menuResourceFactory.create(mapItem);

            if (menuWidget != null) {
                Log.d(TAG, "Successfully created menu widget for COT item");
                return menuWidget;
            } else {
                Log.w(TAG, "MenuResourceFactory returned null for COT item");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error creating menu widget for COT item", e);
        }

        // If menu creation failed, return null to fall back to default factory
        return null;
    }
}
