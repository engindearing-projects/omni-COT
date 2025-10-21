package com.engindearing.omnicot;

import android.content.Context;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Shape;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DashboardActivity {

    private static final String TAG = DashboardActivity.class.getSimpleName();

    private final Context context;
    private final MapView mapView;
    private final View dashboardView;
    private final OmniCOTDropDownReceiver receiver;

    // UI Components
    private TextView txtActiveAOIs;
    private TextView txtActiveAlerts;
    private TextView txtCOTModified;
    private LinearLayout cardCOTManagement;
    private LinearLayout cardAOIManagement;
    private LinearLayout cardCreateAlert;
    private LinearLayout cardViewHistory;
    private RecyclerView recentActivityRecyclerView;
    private ImageButton btnSettings;
    private ImageButton btnHelp;

    // Activity tracking
    private static int cotModifiedCount = 0;
    private static List<String> recentActivities = new ArrayList<>();

    public DashboardActivity(Context context, MapView mapView, View dashboardView, OmniCOTDropDownReceiver receiver) {
        this.context = context;
        this.mapView = mapView;
        this.dashboardView = dashboardView;
        this.receiver = receiver;

        initializeUI();
        updateStats();
    }

    private void initializeUI() {
        // Status metrics
        txtActiveAOIs = dashboardView.findViewById(R.id.txtActiveAOIs);
        txtActiveAlerts = dashboardView.findViewById(R.id.txtActiveAlerts);
        txtCOTModified = dashboardView.findViewById(R.id.txtCOTModified);

        // Quick action cards
        cardCOTManagement = dashboardView.findViewById(R.id.cardCOTManagement);
        cardAOIManagement = dashboardView.findViewById(R.id.cardAOIManagement);
        cardCreateAlert = dashboardView.findViewById(R.id.cardCreateAlert);
        cardViewHistory = dashboardView.findViewById(R.id.cardViewHistory);

        // Recent activity
        recentActivityRecyclerView = dashboardView.findViewById(R.id.recentActivityRecyclerView);
        recentActivityRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        // Header buttons
        btnSettings = dashboardView.findViewById(R.id.btnSettings);
        btnHelp = dashboardView.findViewById(R.id.btnHelp);

        setupListeners();
    }

    private void setupListeners() {
        // Quick action cards
        cardCOTManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCOTManagementClick();
            }
        });

        cardAOIManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAOIManagementClick();
            }
        });

        cardCreateAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateAlertClick();
            }
        });

        cardViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onViewHistoryClick();
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Settings - Coming Soon", Toast.LENGTH_SHORT).show();
            }
        });

        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHelp();
            }
        });
    }

    public void updateStats() {
        // Count active AOIs
        int aoiCount = getAOICount();
        txtActiveAOIs.setText(String.valueOf(aoiCount));

        // Count active alerts
        int alertCount = getActiveAlertCount();
        txtActiveAlerts.setText(String.valueOf(alertCount));

        // COT modified count
        txtCOTModified.setText(String.valueOf(cotModifiedCount));

        Log.d(TAG, "Dashboard stats updated - AOIs: " + aoiCount + ", Alerts: " + alertCount + ", COT: " + cotModifiedCount);
    }

    private int getAOICount() {
        try {
            // Get the Drawing Objects group where shapes are stored
            MapGroup drawingGroup = mapView.getRootGroup().findMapGroup("Drawing Objects");
            if (drawingGroup == null) {
                Log.d(TAG, "Drawing Objects group not found");
                return 0;
            }

            int count = 0;
            Collection<com.atakmap.android.maps.MapItem> items = drawingGroup.deepFindItems("type", "shape");
            if (items != null) {
                for (com.atakmap.android.maps.MapItem item : items) {
                    if (item instanceof Shape) {
                        count++;
                        Log.d(TAG, "Found AOI: " + item.getTitle());
                    }
                }
            }

            Log.d(TAG, "Total AOIs found: " + count);
            return count;
        } catch (Exception e) {
            Log.e(TAG, "Error counting AOIs", e);
            return 0;
        }
    }

    private int getActiveAlertCount() {
        // Count AOIs with alerts enabled
        // This would need to track which AOIs have alerts configured
        return 0; // Placeholder
    }

    public static void incrementCOTModified() {
        cotModifiedCount++;
        addActivity("COT marker affiliation modified");
    }

    public static void addActivity(String activity) {
        recentActivities.add(0, activity);
        if (recentActivities.size() > 10) {
            recentActivities.remove(recentActivities.size() - 1);
        }
    }

    private void onCOTManagementClick() {
        Log.d(TAG, "COT Management clicked");
        if (receiver != null) {
            receiver.showCOTManagement();
        }
    }

    private void onAOIManagementClick() {
        Log.d(TAG, "AOI Management clicked");
        if (receiver != null) {
            receiver.showAOIManagement();
        }
    }

    private void onCreateAlertClick() {
        Toast.makeText(context, "Create Alert - Select an AOI first", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Create Alert clicked");
    }

    private void onViewHistoryClick() {
        StringBuilder history = new StringBuilder("Recent Activity:\n\n");
        if (recentActivities.isEmpty()) {
            history.append("No recent activity");
        } else {
            for (int i = 0; i < Math.min(5, recentActivities.size()); i++) {
                history.append("• ").append(recentActivities.get(i)).append("\n");
            }
        }
        Toast.makeText(context, history.toString(), Toast.LENGTH_LONG).show();
        Log.d(TAG, "View History clicked");
    }

    private void showHelp() {
        String helpText = "OmniCOT Dashboard Help:\n\n" +
                "• COT Management - Modify marker affiliations\n" +
                "• AOI Management - Manage areas of interest\n" +
                "• Create Alert - Set up geofence alerts\n" +
                "• View History - See recent activities\n\n" +
                "Stats show active AOIs, alerts, and modified COT markers.";

        Toast.makeText(context, helpText, Toast.LENGTH_LONG).show();
    }
}
