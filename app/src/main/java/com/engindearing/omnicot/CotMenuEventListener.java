package com.engindearing.omnicot;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.menu.MapMenuEventListener;
import com.atakmap.coremap.log.Log;

/**
 * Event listener for COT radial menu lifecycle events.
 * This listener is notified when menus are shown or hidden, allowing for
 * custom behavior such as logging, analytics, or UI updates.
 */
public class CotMenuEventListener implements MapMenuEventListener {

    private static final String TAG = CotMenuEventListener.class.getSimpleName();

    /**
     * Called before a menu is displayed.
     * If this method returns true, no more listeners will be called and the
     * menu creation process will not continue.
     *
     * @param mapItem The MapItem for which the menu will be shown, may be null
     * @return true to prevent menu display, false to allow it
     */
    @Override
    public boolean onShowMenu(MapItem mapItem) {
        if (mapItem != null) {
            String itemType = mapItem.getType();
            String itemTitle = mapItem.getTitle();
            String uid = mapItem.getUID();

            // Check if this is a COT item
            if (itemType != null && itemType.startsWith("a-")) {
                Log.d(TAG, "Showing radial menu for COT item: " + itemTitle +
                           " (type: " + itemType + ", uid: " + uid + ")");

                // Update dashboard activity log
                DashboardActivity.addActivity("Opened menu for: " + itemTitle);

                // Allow menu to be shown
                return false;
            } else {
                Log.d(TAG, "Not a COT item, allowing default menu handling");
            }
        } else {
            Log.d(TAG, "MapItem is null, showing default menu");
        }

        // Allow menu to be shown by default
        return false;
    }

    /**
     * Called when a menu is hidden/dismissed.
     * This allows for cleanup or logging when the user closes the menu.
     *
     * @param mapItem The MapItem for which the menu was shown, may be null
     */
    @Override
    public void onHideMenu(MapItem mapItem) {
        if (mapItem != null) {
            String itemType = mapItem.getType();
            String itemTitle = mapItem.getTitle();

            // Check if this was a COT item menu
            if (itemType != null && itemType.startsWith("a-")) {
                Log.d(TAG, "Menu hidden for COT item: " + itemTitle +
                           " (type: " + itemType + ")");
            }
        }
    }
}
