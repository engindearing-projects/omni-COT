package com.engindearing.omnicot;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.atakmap.android.gui.PluginSpinner;
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
import com.engindearing.omnicot.remoteid.RemoteIdData;
import com.engindearing.omnicot.remoteid.RemoteIdToCotConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

public class OmniCOTDropDownReceiver extends DropDownReceiver implements DropDown.OnStateListener {

    public static final String TAG = OmniCOTDropDownReceiver.class.getSimpleName();
    public static final String SHOW_PLUGIN = "com.engindearing.omnicot.SHOW_PLUGIN";

    // Navigation constants
    private static final String DASHBOARD = "dashboard";
    private static final String COT_MANAGEMENT = "cot_management";
    private static final String AOI_MANAGEMENT = "aoi_management";

    private final Context pluginContext;
    private final MapView mapView;
    private final View templateView;
    private View managementView;
    private DashboardActivity dashboardActivity;

    // UI Components - COT Affiliation
    private Button btnSelectCot;
    private LinearLayout cotAffiliationSection;
    private TextView selectedCotInfo;
    private PluginSpinner spinnerAffiliation;
    private PluginSpinner spinnerDimension;
    private PluginSpinner spinnerCustomAffiliation;
    private TextView txtAffiliationInfo;
    private Button btnUpdateAffiliation;

    // UI Components - AOI
    private Button btnRefreshAoi;
    private RecyclerView aoiRecyclerView;
    private Button btnCreateAoi;
    private AOIAdapter aoiAdapter;

    // State
    private MapItem selectedCotItem;
    private CotDispatcher cotDispatcher;
    private AffiliationManager affiliationManager;
    private boolean isSelectingCot = false;
    private boolean showingDashboard = true;
    private List<MapItem> recentCOTItems = new ArrayList<>();

    // Navigation state
    private Stack<String> navigationStack = new Stack<>();
    private String currentScreen = DASHBOARD;
    private ImageButton btnBack;

    public OmniCOTDropDownReceiver(final MapView mapView, final Context context, View templateView) {
        super(mapView);
        this.pluginContext = context;
        this.mapView = mapView;
        this.templateView = templateView;

        // Get COT dispatcher for federating changes
        // Use external dispatcher to send updates over the network to team members
        cotDispatcher = com.atakmap.android.cot.CotMapComponent.getExternalDispatcher();
        if (cotDispatcher == null) {
            Log.e(TAG, "WARNING: CotDispatcher is NULL during initialization!");
            Log.e(TAG, "CotMapComponent may not be fully initialized yet.");
            Log.e(TAG, "Drone detections will attempt to re-initialize dispatcher.");
        } else {
            Log.d(TAG, "CotDispatcher initialized successfully");
        }

        // Initialize affiliation manager
        Log.d(TAG, "Initializing AffiliationManager with context: " + (pluginContext != null ? "valid" : "NULL"));
        affiliationManager = AffiliationManager.getInstance(pluginContext);
        if (affiliationManager == null) {
            Log.e(TAG, "Failed to initialize AffiliationManager");
        }

        // Initialize dashboard
        dashboardActivity = new DashboardActivity(pluginContext, mapView, templateView, this);

        initializeUI();
    }

    private void initializeUI() {
        // Inflate the management view
        managementView = android.view.LayoutInflater.from(pluginContext)
                .inflate(R.layout.main_layout, null);

        // Initialize management UI components
        initializeManagementComponents();

        Log.d(TAG, "Dashboard and management views initialized");
    }

    private void initializeManagementComponents() {
        // Back button
        btnBack = managementView.findViewById(R.id.btnBack);

        // COT Management components
        btnSelectCot = managementView.findViewById(R.id.btnSelectCot);
        cotAffiliationSection = managementView.findViewById(R.id.cotAffiliationSection);
        selectedCotInfo = managementView.findViewById(R.id.selectedCotInfo);
        spinnerAffiliation = managementView.findViewById(R.id.spinnerAffiliation);
        spinnerDimension = managementView.findViewById(R.id.spinnerDimension);
        spinnerCustomAffiliation = managementView.findViewById(R.id.spinnerCustomAffiliation);
        txtAffiliationInfo = managementView.findViewById(R.id.txtAffiliationInfo);
        btnUpdateAffiliation = managementView.findViewById(R.id.btnUpdateAffiliation);

        // AOI Management components
        btnRefreshAoi = managementView.findViewById(R.id.btnRefreshAoi);
        aoiRecyclerView = managementView.findViewById(R.id.aoiRecyclerView);
        btnCreateAoi = managementView.findViewById(R.id.btnCreateAoi);

        // Setup spinners
        setupSpinners();

        // Setup RecyclerView
        aoiAdapter = new AOIAdapter(pluginContext, mapView, new ArrayList<>());
        aoiRecyclerView.setLayoutManager(new LinearLayoutManager(pluginContext));
        aoiRecyclerView.setAdapter(aoiAdapter);

        // Setup button listeners
        setupButtonListeners();
    }

    private void setupSpinners() {
        // Affiliation spinner
        String[] affiliations = {"Friendly (f)", "Neutral (n)", "Hostile (h)", "Unknown (u)"};
        ArrayAdapter<String> affiliationAdapter = new ArrayAdapter<>(
                pluginContext, android.R.layout.simple_spinner_item, affiliations);
        affiliationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAffiliation.setAdapter(affiliationAdapter);

        // Dimension spinner
        String[] dimensions = {"Point (P)", "Air (A)", "Ground (G)", "Sea (S)", "Subsurface (U)"};
        ArrayAdapter<String> dimensionAdapter = new ArrayAdapter<>(
                pluginContext, android.R.layout.simple_spinner_item, dimensions);
        dimensionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDimension.setAdapter(dimensionAdapter);

        // Custom Affiliation spinner (for team tracking)
        String[] customAffiliations = {"Unknown", "Assumed Friendly", "Assumed Hostile", "Pending"};
        ArrayAdapter<String> customAffiliationAdapter = new ArrayAdapter<>(
                pluginContext, android.R.layout.simple_spinner_item, customAffiliations);
        customAffiliationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCustomAffiliation.setAdapter(customAffiliationAdapter);
    }

    private void setupButtonListeners() {
        // COT Management buttons
        btnSelectCot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticFeedbackHelper.performMediumClick(v);
                startCotSelection();
            }
        });

        btnUpdateAffiliation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticFeedbackHelper.performHeavyClick(v);
                updateCotAffiliation();
            }
        });

        // AOI Management buttons
        btnRefreshAoi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticFeedbackHelper.performLightClick(v);
                refreshAOIList();
            }
        });

        btnCreateAoi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticFeedbackHelper.performMediumClick(v);
                createNewAOI();
            }
        });
    }

    private void setupBackButton() {
        if (btnBack != null) {
            btnBack.setVisibility(View.VISIBLE);
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigateBack();
                }
            });
        }
    }

    private void navigateBack() {
        HapticFeedbackHelper.performLightClick(btnBack);
        if (!navigationStack.isEmpty()) {
            String previousScreen = navigationStack.pop();
            if (DASHBOARD.equals(previousScreen)) {
                showDashboard();
            }
        }
    }

    public void showCOTManagement() {
        if (navigationStack.isEmpty() || !DASHBOARD.equals(navigationStack.peek())) {
            navigationStack.push(DASHBOARD);
        }
        currentScreen = COT_MANAGEMENT;
        showingDashboard = false;
        setRetain(true);
        closeDropDown();
        showDropDown(managementView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, this);
        setupBackButton();
        Log.d(TAG, "Switched to COT Management view");
    }

    public void showAOIManagement() {
        if (navigationStack.isEmpty() || !DASHBOARD.equals(navigationStack.peek())) {
            navigationStack.push(DASHBOARD);
        }
        currentScreen = AOI_MANAGEMENT;
        showingDashboard = false;
        setRetain(true);
        closeDropDown();
        showDropDown(managementView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, this);
        setupBackButton();
        // Automatically refresh the AOI list when opening
        refreshAOIList();
        Log.d(TAG, "Switched to AOI Management view");
    }

    public void showDashboard() {
        currentScreen = DASHBOARD;
        navigationStack.clear();
        showingDashboard = true;
        setRetain(true);
        closeDropDown();
        showDropDown(templateView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, this);
        if (dashboardActivity != null) {
            dashboardActivity.updateStats();
        }
        // Hide back button on dashboard
        if (btnBack != null) {
            btnBack.setVisibility(View.GONE);
        }
        Log.d(TAG, "Switched to Dashboard view");
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

    private void addToRecentCOT(MapItem item) {
        recentCOTItems.remove(item); // Remove if already exists
        recentCOTItems.add(0, item); // Add to front
        if (recentCOTItems.size() > 5) {
            recentCOTItems.remove(recentCOTItems.size() - 1);
        }
        Log.d(TAG, "Added to recent COT: " + item.getTitle() + " (Total recent: " + recentCOTItems.size() + ")");
    }

    private void onCotSelected(MapItem item) {
        isSelectingCot = false;
        selectedCotItem = item;
        addToRecentCOT(item);

        btnSelectCot.setText("Select COT to Modify");
        btnSelectCot.setEnabled(true);
        cotAffiliationSection.setVisibility(View.VISIBLE);

        String itemType = item.getType();
        String itemTitle = item.getTitle();
        String uid = item.getUID();
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

        // Check for stored affiliation data
        AffiliationData storedAffiliation = affiliationManager.getAffiliation(uid);
        if (storedAffiliation != null) {
            // Set custom affiliation spinner
            setCustomAffiliationSpinner(storedAffiliation.getAffiliation());

            // Display affiliation info
            String infoText = "Team Affiliation: " + storedAffiliation.getAffiliation().getValue();
            if (storedAffiliation.getMarkedBy() != null && !storedAffiliation.getMarkedBy().isEmpty()) {
                infoText += "\nMarked by: " + storedAffiliation.getMarkedBy();
            }
            if (storedAffiliation.getNotes() != null && !storedAffiliation.getNotes().isEmpty()) {
                infoText += "\nNotes: " + storedAffiliation.getNotes();
            }
            txtAffiliationInfo.setText(infoText);
            txtAffiliationInfo.setVisibility(View.VISIBLE);
        } else {
            // No stored affiliation, set to default
            spinnerCustomAffiliation.setSelection(0); // Unknown
            txtAffiliationInfo.setText("No team affiliation data");
            txtAffiliationInfo.setVisibility(View.VISIBLE);
        }

        Log.d(TAG, "COT selected: " + itemTitle + " (" + itemType + ")");
    }

    private void setCustomAffiliationSpinner(AffiliationData.Affiliation affiliation) {
        int position = 0;
        switch (affiliation) {
            case UNKNOWN: position = 0; break;
            case ASSUMED_FRIENDLY: position = 1; break;
            case ASSUMED_HOSTILE: position = 2; break;
            case PENDING: position = 3; break;
        }
        spinnerCustomAffiliation.setSelection(position);
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
        int customAffiliationPos = spinnerCustomAffiliation.getSelectedItemPosition();

        char[] affiliations = {'f', 'n', 'h', 'u'};
        char[] dimensions = {'P', 'A', 'G', 'S', 'U'};

        String newType = "a-" + affiliations[affiliationPos] + "-" + dimensions[dimensionPos];

        // Get custom affiliation
        AffiliationData.Affiliation customAffiliation;
        switch (customAffiliationPos) {
            case 1: customAffiliation = AffiliationData.Affiliation.ASSUMED_FRIENDLY; break;
            case 2: customAffiliation = AffiliationData.Affiliation.ASSUMED_HOSTILE; break;
            case 3: customAffiliation = AffiliationData.Affiliation.PENDING; break;
            default: customAffiliation = AffiliationData.Affiliation.UNKNOWN; break;
        }

        // Get local callsign
        String localCallsign = mapView.getDeviceCallsign();

        // Update the COT item
        selectedCotItem.setType(newType);

        // Store affiliation data locally
        String uid = selectedCotItem.getUID();
        String serverConnection = "";
        AffiliationData affiliationData = new AffiliationData(uid, customAffiliation, localCallsign, serverConnection);
        affiliationManager.setAffiliation(affiliationData);

        // Federate the change by dispatching a COT event
        try {
            // Create CotEvent manually from MapItem
            CotEvent cotEvent = new CotEvent();
            cotEvent.setUID(uid);
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

            // Set detail with custom affiliation information
            CotDetail detail = new CotDetail();

            // Add custom affiliation detail
            CotDetail affiliationDetail = new CotDetail(CotAffiliationListener.getAffiliationDetailTag());
            affiliationDetail.setAttribute("affiliation", customAffiliation.getValue());
            affiliationDetail.setAttribute("markedBy", localCallsign);
            affiliationDetail.setAttribute("timestamp", String.valueOf(System.currentTimeMillis()));
            affiliationDetail.setAttribute("notes", "");
            detail.addChild(affiliationDetail);

            cotEvent.setDetail(detail);

            cotDispatcher.dispatch(cotEvent);

            // Increment dashboard counter
            DashboardActivity.incrementCOTModified();
            DashboardActivity.addActivity("Updated affiliation: " + selectedCotItem.getTitle() +
                                         " -> " + customAffiliation.getValue());

            Toast.makeText(pluginContext, "COT affiliation updated and federated to team!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "COT affiliation updated: " + selectedCotItem.getTitle() + " -> " + newType +
                  " (Custom: " + customAffiliation.getValue() + ")");
        } catch (Exception e) {
            Log.e(TAG, "Error federating COT update", e);
            Toast.makeText(pluginContext, "Updated locally, federation may have failed", Toast.LENGTH_SHORT).show();
        }

        // Update display
        selectedCotInfo.setText("Selected: " + selectedCotItem.getTitle() + " (Type: " + newType + ")");
        txtAffiliationInfo.setText("Team Affiliation: " + customAffiliation.getValue() +
                                  "\nMarked by: " + localCallsign);
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

    /**
     * Handle Remote ID drone detection from gyb_detect device
     */
    public void handleRemoteIdDetection(RemoteIdData data) {
        try {
            // Check if cotDispatcher is available
            if (cotDispatcher == null) {
                Log.e(TAG, "CotDispatcher is null! Cannot dispatch drone detection.");
                Log.e(TAG, "This usually means CotMapComponent is not yet initialized.");
                // Try to re-initialize the dispatcher
                cotDispatcher = com.atakmap.android.cot.CotMapComponent.getExternalDispatcher();
                if (cotDispatcher == null) {
                    Log.e(TAG, "Failed to re-initialize CotDispatcher. Drone will not appear on map.");
                    DashboardActivity.addActivity("ERROR: Cannot display drone - CoT dispatcher unavailable");
                    return;
                }
            }

            // Convert Remote ID data to CoT event
            CotEvent cotEvent = RemoteIdToCotConverter.convertToCotEvent(data);

            if (cotEvent != null) {
                // Dispatch the CoT event to ATAK
                cotDispatcher.dispatch(cotEvent);

                Log.d(TAG, "Dispatched drone CoT event: " + cotEvent.getUID() +
                        " at " + data.getUasLat() + ", " + data.getUasLon());

                // Update dashboard
                DashboardActivity.incrementDronesDetected();
                DashboardActivity.addActivity("Drone " + data.getUniqueId() +
                        " displayed on map");
            } else {
                Log.w(TAG, "Failed to convert Remote ID data to CoT event");
                Log.w(TAG, "RemoteIdData: " + data.toString());
                DashboardActivity.addActivity("WARNING: Failed to convert drone data to CoT");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling Remote ID detection", e);
            Log.e(TAG, "Exception details: " + e.getMessage());
            e.printStackTrace();
            DashboardActivity.addActivity("ERROR: Exception while processing drone - " + e.getMessage());
        }
    }

    @Override
    public boolean onBackButtonPressed() {
        if (!DASHBOARD.equals(currentScreen)) {
            navigateBack();
            return true;
        }
        return super.onBackButtonPressed();
    }

    @Override
    protected void disposeImpl() {
        // Clean up dashboard
        if (dashboardActivity != null) {
            dashboardActivity.dispose();
        }
    }
}
