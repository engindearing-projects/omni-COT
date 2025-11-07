package com.engindearing.omnicot;

import android.content.Context;
import android.view.View;
import android.widget.Button;
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
import com.engindearing.omnicot.remoteid.BluetoothDeviceDialog;
import com.engindearing.omnicot.remoteid.BluetoothManager;
import com.engindearing.omnicot.remoteid.RemoteIdParser;

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

    // Bluetooth UI Components
    private TextView txtBluetoothStatus;
    private TextView txtBluetoothDevice;
    private TextView txtDronesDetected;
    private TextView txtBatteryLevel;
    private Button btnBluetoothConnect;
    private Button btnBluetoothDisconnect;
    private ImageButton btnBluetoothRefresh;

    // Bluetooth Manager
    private BluetoothManager bluetoothManager;

    // Activity tracking
    private static int cotModifiedCount = 0;
    private static int dronesDetectedCount = 0;
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

        // Bluetooth UI components
        txtBluetoothStatus = dashboardView.findViewById(R.id.txtBluetoothStatus);
        txtBluetoothDevice = dashboardView.findViewById(R.id.txtBluetoothDevice);
        txtDronesDetected = dashboardView.findViewById(R.id.txtDronesDetected);
        txtBatteryLevel = dashboardView.findViewById(R.id.txtBatteryLevel);
        btnBluetoothConnect = dashboardView.findViewById(R.id.btnBluetoothConnect);
        btnBluetoothDisconnect = dashboardView.findViewById(R.id.btnBluetoothDisconnect);
        btnBluetoothRefresh = dashboardView.findViewById(R.id.btnBluetoothRefresh);

        setupListeners();
        initializeBluetooth();
    }

    private void setupListeners() {
        // Quick action cards
        cardCOTManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticFeedbackHelper.performLightClick(v);
                onCOTManagementClick();
            }
        });

        cardAOIManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticFeedbackHelper.performLightClick(v);
                onAOIManagementClick();
            }
        });

        cardCreateAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticFeedbackHelper.performLightClick(v);
                onCreateAlertClick();
            }
        });

        cardViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticFeedbackHelper.performLightClick(v);
                onViewHistoryClick();
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticFeedbackHelper.performMediumClick(v);
                Toast.makeText(context, "Settings - Coming Soon", Toast.LENGTH_SHORT).show();
            }
        });

        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticFeedbackHelper.performMediumClick(v);
                showHelp();
            }
        });

        // Bluetooth listeners
        btnBluetoothConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticFeedbackHelper.performMediumClick(v);
                onBluetoothConnectClick();
            }
        });

        btnBluetoothDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticFeedbackHelper.performMediumClick(v);
                onBluetoothDisconnectClick();
            }
        });

        btnBluetoothRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticFeedbackHelper.performLightClick(v);
                onBluetoothRefreshClick();
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

        // Drones detected count
        txtDronesDetected.setText(String.valueOf(dronesDetectedCount));

        Log.d(TAG, "Dashboard stats updated - AOIs: " + aoiCount + ", Alerts: " + alertCount +
                ", COT: " + cotModifiedCount + ", Drones: " + dronesDetectedCount);
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
                "• View History - See recent activities\n" +
                "• Remote ID Detection - Connect to gyb_detect device\n\n" +
                "Stats show active AOIs, alerts, modified COT markers, and detected drones.";

        Toast.makeText(context, helpText, Toast.LENGTH_LONG).show();
    }

    // ========== Bluetooth Methods ==========

    private void initializeBluetooth() {
        bluetoothManager = new BluetoothManager(context);

        // Set up data listeners
        bluetoothManager.addDataListener(new BluetoothManager.DataListener() {
            @Override
            public void onDeviceInfo(RemoteIdParser.DeviceInfo info) {
                txtBluetoothDevice.setText(info.toString());
                addActivity("gyb_detect connected: " + info.model);
            }

            @Override
            public void onBatteryStatus(RemoteIdParser.BatteryStatus status) {
                txtBatteryLevel.setText(status.getPercentage() + "%");
            }

            @Override
            public void onRemoteIdData(com.engindearing.omnicot.remoteid.RemoteIdData data) {
                handleDroneDetection(data);
            }
        });

        // Set up connection listeners
        bluetoothManager.addConnectionListener(new BluetoothManager.ConnectionListener() {
            @Override
            public void onConnecting(String deviceName) {
                txtBluetoothStatus.setText("Bluetooth: Connecting...");
                btnBluetoothConnect.setEnabled(false);
                addActivity("Connecting to " + deviceName);
            }

            @Override
            public void onConnected(String deviceName) {
                txtBluetoothStatus.setText("Bluetooth: Connected");
                txtBluetoothDevice.setText(deviceName);
                btnBluetoothConnect.setEnabled(false);
                btnBluetoothDisconnect.setEnabled(true);
                addActivity("Connected to " + deviceName);
                Toast.makeText(context, "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDisconnected() {
                txtBluetoothStatus.setText("Bluetooth: Not Connected");
                txtBluetoothDevice.setText("gyb_detect device");
                txtBatteryLevel.setText("--");
                btnBluetoothConnect.setEnabled(true);
                btnBluetoothDisconnect.setEnabled(false);
                addActivity("Bluetooth disconnected");
            }

            @Override
            public void onError(String error) {
                txtBluetoothStatus.setText("Bluetooth: Error");
                btnBluetoothConnect.setEnabled(true);
                btnBluetoothDisconnect.setEnabled(false);
                Toast.makeText(context, "Bluetooth error: " + error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Bluetooth error: " + error);
            }
        });

        // Check initial state
        if (!bluetoothManager.isBluetoothAvailable()) {
            txtBluetoothStatus.setText("Bluetooth: Unavailable");
            btnBluetoothConnect.setEnabled(false);
            Toast.makeText(context, "Bluetooth is not available or not enabled", Toast.LENGTH_LONG).show();
        }
    }

    private void onBluetoothConnectClick() {
        if (!bluetoothManager.isBluetoothAvailable()) {
            Toast.makeText(context, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show device selection dialog with discovery and pairing
        BluetoothDeviceDialog dialog = new BluetoothDeviceDialog(
                mapView.getContext(),
                bluetoothManager,
                device -> {
                    // Connect to selected device
                    bluetoothManager.connect(device);
                }
        );
        dialog.show();
    }

    private void onBluetoothDisconnectClick() {
        bluetoothManager.disconnect();
    }

    private void onBluetoothRefreshClick() {
        if (bluetoothManager.isConnected()) {
            Toast.makeText(context, "Already connected", Toast.LENGTH_SHORT).show();
        } else {
            onBluetoothConnectClick();
        }
    }

    private void handleDroneDetection(com.engindearing.omnicot.remoteid.RemoteIdData data) {
        Log.d(TAG, "Drone detected: " + data.toString());

        // Increment counter
        dronesDetectedCount++;
        updateStats();

        // Add to activity log
        String activity = "Drone detected: " + data.getUniqueId() +
                " at " + String.format("%.6f", data.getUasLat()) + ", " +
                String.format("%.6f", data.getUasLon());
        addActivity(activity);

        // Send to receiver for CoT conversion and dispatch
        if (receiver != null) {
            receiver.handleRemoteIdDetection(data);
        }
    }

    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public static void incrementDronesDetected() {
        dronesDetectedCount++;
    }

    public void dispose() {
        if (bluetoothManager != null) {
            bluetoothManager.shutdown();
        }
    }
}
