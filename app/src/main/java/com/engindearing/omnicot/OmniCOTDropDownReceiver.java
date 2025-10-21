package com.engindearing.omnicot;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Shape;
import com.atakmap.comms.CotDispatcher;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OmniCOTDropDownReceiver extends DropDownReceiver implements DropDown.OnStateListener {

    public static final String TAG = OmniCOTDropDownReceiver.class.getSimpleName();
    public static final String SHOW_PLUGIN = "com.engindearing.omnicot.SHOW_PLUGIN";

    private final Context pluginContext;
    private final MapView mapView;
    private final View templateView;
    private DashboardActivity dashboardActivity;

    // UI Components - COT Affiliation
    private Button btnSelectCot;
    private LinearLayout cotAffiliationSection;
    private TextView selectedCotInfo;
    private Spinner spinnerAffiliation;
    private Spinner spinnerDimension;
    private Button btnUpdateAffiliation;

    // UI Components - AOI
    private Button btnRefreshAoi;
    private RecyclerView aoiRecyclerView;
    private Button btnCreateAoi;
    private AOIAdapter aoiAdapter;

    // State
    private MapItem selectedCotItem;
    private CotDispatcher cotDispatcher;
    private boolean isSelectingCot = false;

    public OmniCOTDropDownReceiver(final MapView mapView, final Context context, View templateView) {
        super(mapView);
        this.pluginContext = context;
        this.mapView = mapView;
        this.templateView = templateView;

        // Get COT dispatcher for federating changes
        cotDispatcher = com.atakmap.android.cot.CotMapComponent.getInternalDispatcher();

        // Initialize dashboard
        dashboardActivity = new DashboardActivity(pluginContext, mapView, templateView);

        initializeUI();
    }

    private void initializeUI() {
        // Note: Dashboard UI is handled by DashboardActivity
        // The dashboard replaces the old main_layout.xml

        Log.d(TAG, "Dashboard initialized");
        // Old UI components removed - using dashboard now
        // Setup button listeners for dashboard handled in DashboardActivity
        setupButtonListeners();
    }

    private void setupButtonListeners() {
        // Button listeners now handled by DashboardActivity
        // Keeping methods for future integration
    }

    private void startCotSelection() {
        isSelectingCot = true;
        btnSelectCot.setText("Tap a COT marker on map...");
        btnSelectCot.setEnabled(false);

        Toast.makeText(pluginContext, "Tap any COT marker on the map", Toast.LENGTH_SHORT).show();

        // Add map click listener
        final MapEventDispatcher.MapEventDispatchListener clickListener = new MapEventDispatcher.MapEventDispatchListener() {
            @Override
            public void onMapEvent(MapEvent event) {
                if (isSelectingCot) {
                    MapItem item = event.getItem();
                    if (item != null) {
                        onCotSelected(item);
                        mapView.getMapEventDispatcher().removeMapEventListener(MapEvent.ITEM_CLICK, this);
                    }
                }
            }
        };
        mapView.getMapEventDispatcher().addMapEventListener(MapEvent.ITEM_CLICK, clickListener);
    }

    private void onCotSelected(MapItem item) {
        isSelectingCot = false;
        selectedCotItem = item;

        btnSelectCot.setText("Select COT to Modify");
        btnSelectCot.setEnabled(true);
        cotAffiliationSection.setVisibility(View.VISIBLE);

        String itemType = item.getType();
        String itemTitle = item.getTitle();
        selectedCotInfo.setText("Selected: " + itemTitle + " (Type: " + itemType + ")");

        // Parse current affiliation and dimension
        if (itemType != null && itemType.startsWith("a-")) {
            String[] parts = itemType.split("-");
            if (parts.length >= 3) {
                String affiliation = parts[1];
                String dimension = parts[2];

                // Set spinner selections
                setSpinnerByAffiliation(affiliation);
                setSpinnerByDimension(dimension);
            }
        }

        Log.d(TAG, "COT selected: " + itemTitle + " (" + itemType + ")");
    }

    private void setSpinnerByAffiliation(String affiliation) {
        int position = 0;
        switch (affiliation) {
            case "f": position = 0; break;
            case "n": position = 1; break;
            case "h": position = 2; break;
            case "u": position = 3; break;
        }
        spinnerAffiliation.setSelection(position);
    }

    private void setSpinnerByDimension(String dimension) {
        int position = 0;
        switch (dimension) {
            case "P": position = 0; break;
            case "A": position = 1; break;
            case "G": position = 2; break;
            case "S": position = 3; break;
            case "U": position = 4; break;
        }
        spinnerDimension.setSelection(position);
    }

    private void updateCotAffiliation() {
        if (selectedCotItem == null) {
            Toast.makeText(pluginContext, "No COT selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected affiliation and dimension
        int affiliationPos = spinnerAffiliation.getSelectedItemPosition();
        int dimensionPos = spinnerDimension.getSelectedItemPosition();

        char[] affiliations = {'f', 'n', 'h', 'u'};
        char[] dimensions = {'P', 'A', 'G', 'S', 'U'};

        String newType = "a-" + affiliations[affiliationPos] + "-" + dimensions[dimensionPos];

        // Update the COT item
        selectedCotItem.setType(newType);

        // Federate the change by dispatching a COT event
        try {
            // Create CotEvent manually from MapItem
            CotEvent cotEvent = new CotEvent();
            cotEvent.setUID(selectedCotItem.getUID());
            cotEvent.setType(newType);
            cotEvent.setHow("h-e");

            CoordinatedTime now = new CoordinatedTime();
            cotEvent.setTime(now);
            cotEvent.setStart(now);
            cotEvent.setStale(new CoordinatedTime(now.getMilliseconds() + 30 * 60 * 1000)); // 30 minutes from now

            // Set point for PointMapItem
            if (selectedCotItem instanceof PointMapItem) {
                PointMapItem pmi = (PointMapItem) selectedCotItem;
                GeoPoint gp = pmi.getPoint();
                cotEvent.setPoint(new CotPoint(gp));
            }

            // Set detail
            CotDetail detail = new CotDetail();
            cotEvent.setDetail(detail);

            cotDispatcher.dispatch(cotEvent);
            Toast.makeText(pluginContext, "COT affiliation updated and federated!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "COT affiliation updated: " + selectedCotItem.getTitle() + " -> " + newType);
        } catch (Exception e) {
            Log.e(TAG, "Error federating COT update", e);
            Toast.makeText(pluginContext, "Updated locally, federation may have failed", Toast.LENGTH_SHORT).show();
        }

        // Update display
        selectedCotInfo.setText("Selected: " + selectedCotItem.getTitle() + " (Type: " + newType + ")");
    }

    private void refreshAOIList() {
        List<AOIItem> aoiItems = getAOIsFromMap();
        aoiAdapter.updateData(aoiItems);
        Toast.makeText(pluginContext, "Found " + aoiItems.size() + " AOIs", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Refreshed AOI list: " + aoiItems.size() + " items");
    }

    private List<AOIItem> getAOIsFromMap() {
        List<AOIItem> aoiItems = new ArrayList<>();

        // Get the Drawing Objects group where shapes are stored
        MapGroup drawingGroup = mapView.getRootGroup().findMapGroup("Drawing Objects");
        if (drawingGroup != null) {
            Collection<MapItem> items = drawingGroup.getItems();
            for (MapItem item : items) {
                if (item instanceof Shape) {
                    AOIItem aoiItem = new AOIItem((Shape) item);
                    aoiItems.add(aoiItem);
                    Log.d(TAG, "Found AOI: " + item.getTitle() + " (" + item.getClass().getSimpleName() + ")");
                }
            }
        }

        return aoiItems;
    }

    private void createNewAOI() {
        Toast.makeText(pluginContext, "Use ATAK's drawing tools to create shapes", Toast.LENGTH_LONG).show();
        // Note: ATAK has built-in drawing tools accessible from the main toolbar
        // We just need to detect and list them, which is done by refreshAOIList()
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive called with intent: " + intent);
        final String action = intent.getAction();
        if (action == null) {
            Log.d(TAG, "Action is null");
            return;
        }

        if (action.equals(SHOW_PLUGIN)) {
            // Check if already open
            if (!isClosed()) {
                unhideDropDown();
                return;
            }

            showDropDown(templateView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, this);
            setAssociationKey("omniCOTPreference");

            // Update dashboard stats
            if (dashboardActivity != null) {
                dashboardActivity.updateStats();
            }
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

    @Override
    protected void disposeImpl() {
    }
}
