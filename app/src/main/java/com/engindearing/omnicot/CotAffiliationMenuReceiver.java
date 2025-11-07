package com.engindearing.omnicot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.menu.MapMenuReceiver;
import com.atakmap.comms.CotDispatcher;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;

/**
 * Broadcast receiver that handles radial menu selections for COT affiliation updates.
 * This receiver listens for UPDATE_AFFILIATION intents sent by the radial menu buttons
 * and updates the selected COT item's affiliation accordingly.
 */
public class CotAffiliationMenuReceiver extends BroadcastReceiver {

    private static final String TAG = CotAffiliationMenuReceiver.class.getSimpleName();

    public static final String UPDATE_AFFILIATION = "com.engindearing.omnicot.UPDATE_AFFILIATION";

    private final Context context;
    private final MapView mapView;
    private final CotDispatcher cotDispatcher;
    private final AffiliationManager affiliationManager;

    /**
     * Creates a new CotAffiliationMenuReceiver.
     *
     * @param context Application context
     * @param mapView The MapView instance
     */
    public CotAffiliationMenuReceiver(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;

        // Get COT dispatcher for federating changes
        this.cotDispatcher = com.atakmap.android.cot.CotMapComponent.getExternalDispatcher();

        // Get affiliation manager for persistent storage
        this.affiliationManager = AffiliationManager.getInstance(context);

        Log.d(TAG, "CotAffiliationMenuReceiver initialized");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (UPDATE_AFFILIATION.equals(action)) {
            handleAffiliationUpdate(intent);
        }
    }

    /**
     * Handles affiliation update requests from the radial menu.
     *
     * @param intent The intent containing the affiliation value and MapItem reference
     */
    private void handleAffiliationUpdate(Intent intent) {
        // Get the affiliation value from the intent
        String affiliationValue = intent.getStringExtra("affiliation");
        if (affiliationValue == null) {
            Log.e(TAG, "No affiliation value in intent");
            return;
        }

        // Get the MapItem that was tapped
        // The MapMenuReceiver provides the current menu subject via getCurrentItem()
        MapItem mapItem = MapMenuReceiver.getCurrentItem();
        if (mapItem == null) {
            Log.e(TAG, "No MapItem available for affiliation update");
            Toast.makeText(context, "No COT item selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse the affiliation enum
        AffiliationData.Affiliation affiliation;
        try {
            affiliation = AffiliationData.Affiliation.valueOf(affiliationValue);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid affiliation value: " + affiliationValue, e);
            Toast.makeText(context, "Invalid affiliation", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the affiliation
        updateAffiliation(mapItem, affiliation);
    }

    /**
     * Updates the affiliation of a COT item.
     * This method:
     * 1. Updates the COT type based on the new affiliation
     * 2. Stores the affiliation data locally
     * 3. Federates the change to team members via COT event
     *
     * @param mapItem The COT MapItem to update
     * @param newAffiliation The new affiliation to set
     */
    private void updateAffiliation(MapItem mapItem, AffiliationData.Affiliation newAffiliation) {
        String uid = mapItem.getUID();
        String title = mapItem.getTitle();
        String currentType = mapItem.getType();

        Log.d(TAG, "Updating affiliation for " + title + " to " + newAffiliation.getValue());

        // Parse current COT type to extract dimension
        String dimension = "G"; // Default to Ground
        if (currentType != null && currentType.startsWith("a-") && currentType.length() >= 5) {
            String[] parts = currentType.split("-");
            if (parts.length >= 3) {
                dimension = parts[2]; // Extract dimension (P/A/G/S/U)
            }
        }

        // Map affiliation to COT type affiliation character
        char affiliationChar = getAffiliationCharacter(newAffiliation);

        // Build new COT type: a-{affiliation}-{dimension}
        String newType = "a-" + affiliationChar + "-" + dimension;

        // Update the MapItem type
        mapItem.setType(newType);

        // Get local callsign for tracking who made the change
        String localCallsign = mapView.getDeviceCallsign();

        // Store affiliation data locally
        String serverConnection = "";
        AffiliationData affiliationData = new AffiliationData(
            uid,
            newAffiliation,
            localCallsign,
            serverConnection
        );
        affiliationManager.setAffiliation(affiliationData);

        // Federate the change by dispatching a COT event
        federateAffiliationUpdate(mapItem, newType, newAffiliation, localCallsign);

        // Update dashboard statistics
        DashboardActivity.incrementCOTModified();
        DashboardActivity.addActivity("Updated affiliation: " + title +
                                     " -> " + newAffiliation.getValue());

        // Show confirmation to user
        Toast.makeText(context,
                      "Updated " + title + " to " + newAffiliation.getValue(),
                      Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Successfully updated affiliation: " + title + " -> " + newType +
                   " (Custom: " + newAffiliation.getValue() + ")");
    }

    /**
     * Federates the affiliation update to team members via COT event.
     *
     * @param mapItem The MapItem being updated
     * @param newType The new COT type
     * @param affiliation The new affiliation
     * @param markedBy Callsign of who made the change
     */
    private void federateAffiliationUpdate(MapItem mapItem, String newType,
                                          AffiliationData.Affiliation affiliation,
                                          String markedBy) {
        try {
            // Create COT event
            CotEvent cotEvent = new CotEvent();
            cotEvent.setUID(mapItem.getUID());
            cotEvent.setType(newType);
            cotEvent.setHow("h-e"); // Human entry

            CoordinatedTime now = new CoordinatedTime();
            cotEvent.setTime(now);
            cotEvent.setStart(now);
            // Set stale time to 30 minutes from now
            cotEvent.setStale(new CoordinatedTime(now.getMilliseconds() + 30 * 60 * 1000));

            // Set point for PointMapItem
            if (mapItem instanceof PointMapItem) {
                PointMapItem pmi = (PointMapItem) mapItem;
                GeoPoint gp = pmi.getPoint();
                cotEvent.setPoint(new CotPoint(gp));
            }

            // Create detail with custom affiliation information
            CotDetail detail = new CotDetail();

            // Add custom affiliation detail tag
            CotDetail affiliationDetail = new CotDetail(CotAffiliationListener.getAffiliationDetailTag());
            affiliationDetail.setAttribute("affiliation", affiliation.getValue());
            affiliationDetail.setAttribute("markedBy", markedBy);
            affiliationDetail.setAttribute("timestamp", String.valueOf(System.currentTimeMillis()));
            affiliationDetail.setAttribute("notes", "");
            detail.addChild(affiliationDetail);

            cotEvent.setDetail(detail);

            // Dispatch to federation
            cotDispatcher.dispatch(cotEvent);

            Log.d(TAG, "Federated affiliation update for " + mapItem.getUID());

        } catch (Exception e) {
            Log.e(TAG, "Error federating affiliation update", e);
            Toast.makeText(context,
                          "Updated locally, federation may have failed",
                          Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Maps an Affiliation enum to the corresponding COT type affiliation character.
     *
     * @param affiliation The affiliation enum value
     * @return The COT affiliation character (f/h/u/p)
     */
    private char getAffiliationCharacter(AffiliationData.Affiliation affiliation) {
        switch (affiliation) {
            case ASSUMED_FRIENDLY:
                return 'f'; // Friendly
            case ASSUMED_HOSTILE:
                return 'h'; // Hostile
            case PENDING:
                return 'p'; // Pending/Suspected
            case UNKNOWN:
            default:
                return 'u'; // Unknown
        }
    }
}
