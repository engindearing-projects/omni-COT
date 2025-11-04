package com.engindearing.omnicot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import com.atakmap.android.gui.PluginSpinner;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.geofence.component.GeoFenceComponent;
import com.atakmap.android.geofence.data.GeoFence;
import com.atakmap.android.geofence.data.GeoFenceConstants;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

public class AlertConfigDialog {

    private static final String TAG = AlertConfigDialog.class.getSimpleName();

    private final Context context;
    private final MapView mapView;
    private final AOIItem aoiItem;
    private AlertDialog dialog;
    private BroadcastReceiver breachReceiver;

    public AlertConfigDialog(Context context, MapView mapView, AOIItem aoiItem) {
        this.context = context;
        this.mapView = mapView;
        this.aoiItem = aoiItem;
    }

    public void show() {
        // Use plugin context for inflating layout (to access plugin resources)
        // But get activity context for the dialog itself
        View dialogView = LayoutInflater.from(context).inflate(R.layout.aoi_alert_config, null);

        // Get Activity context from MapView for the dialog
        Context mvContext = mapView.getContext();
        final Context dialogContext;

        if (mvContext instanceof Activity) {
            dialogContext = mvContext;
        } else {
            // Fallback to mapView context
            dialogContext = mvContext;
            Log.w(TAG, "MapView context is not an Activity, dialog may fail to show");
        }

        // Initialize UI components
        TextView alertAoiName = dialogView.findViewById(R.id.alertAoiName);
        CheckBox checkEnableAlert = dialogView.findViewById(R.id.checkEnableAlert);
        PluginSpinner spinnerTriggerType = dialogView.findViewById(R.id.spinnerTriggerType);
        PluginSpinner spinnerMonitoredTypes = dialogView.findViewById(R.id.spinnerMonitoredTypes);
        EditText editAlertDuration = dialogView.findViewById(R.id.editAlertDuration);
        Button btnSaveAlert = dialogView.findViewById(R.id.btnSaveAlert);
        Button btnCancelAlert = dialogView.findViewById(R.id.btnCancelAlert);

        // Set current values
        alertAoiName.setText("AOI: " + aoiItem.getName());
        checkEnableAlert.setChecked(aoiItem.isAlertEnabled());
        editAlertDuration.setText(String.valueOf(aoiItem.getDurationHours()));

        // Setup spinners
        ArrayAdapter<CharSequence> triggerAdapter = ArrayAdapter.createFromResource(
                context, R.array.trigger_types, android.R.layout.simple_spinner_item);
        triggerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTriggerType.setAdapter(triggerAdapter);

        ArrayAdapter<CharSequence> monitoredAdapter = ArrayAdapter.createFromResource(
                context, R.array.monitored_types, android.R.layout.simple_spinner_item);
        monitoredAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonitoredTypes.setAdapter(monitoredAdapter);

        // Set spinner selections based on current config
        setTriggerTypeSelection(spinnerTriggerType, aoiItem.getTriggerType());
        setMonitoredTypeSelection(spinnerMonitoredTypes, aoiItem.getMonitoredType());

        // Create dialog with activity context
        AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);
        builder.setView(dialogView);
        dialog = builder.create();

        // Button listeners
        btnCancelAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSaveAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAlertConfiguration(
                        checkEnableAlert.isChecked(),
                        spinnerTriggerType.getSelectedItemPosition(),
                        spinnerMonitoredTypes.getSelectedItemPosition(),
                        editAlertDuration.getText().toString()
                );
            }
        });

        dialog.show();
    }

    private void setTriggerTypeSelection(PluginSpinner spinner, String triggerType) {
        int position = 0;
        if ("Entry".equals(triggerType)) position = 0;
        else if ("Exit".equals(triggerType)) position = 1;
        else if ("Both".equals(triggerType)) position = 2;
        spinner.setSelection(position);
    }

    private void setMonitoredTypeSelection(PluginSpinner spinner, String monitoredType) {
        int position = 0;
        if ("All".equals(monitoredType)) position = 0;
        else if ("Friendly".equals(monitoredType)) position = 1;
        else if ("Hostile".equals(monitoredType)) position = 2;
        else if ("Unknown".equals(monitoredType)) position = 3;
        spinner.setSelection(position);
    }

    private void saveAlertConfiguration(boolean enabled, int triggerPos, int monitoredPos, String durationStr) {
        try {
            // Parse duration
            int durationHours = Integer.parseInt(durationStr);
            long durationMillis = durationHours * 60 * 60 * 1000L;

            // Map trigger type
            GeoFence.Trigger trigger;
            String triggerName;
            switch (triggerPos) {
                case 0:
                    trigger = GeoFence.Trigger.Entry;
                    triggerName = "Entry";
                    break;
                case 1:
                    trigger = GeoFence.Trigger.Exit;
                    triggerName = "Exit";
                    break;
                case 2:
                default:
                    trigger = GeoFence.Trigger.Both;
                    triggerName = "Both";
                    break;
            }

            // Map monitored types
            GeoFence.MonitoredTypes monitoredTypes;
            String monitoredName;
            switch (monitoredPos) {
                case 1:
                    monitoredTypes = GeoFence.MonitoredTypes.Friendly;
                    monitoredName = "Friendly";
                    break;
                case 2:
                    monitoredTypes = GeoFence.MonitoredTypes.Hostile;
                    monitoredName = "Hostile";
                    break;
                case 3:
                    // Unknown - use All since Neutral/Unknown doesn't exist in API
                    monitoredTypes = GeoFence.MonitoredTypes.All;
                    monitoredName = "Unknown";
                    break;
                case 0:
                default:
                    monitoredTypes = GeoFence.MonitoredTypes.All;
                    monitoredName = "All";
                    break;
            }

            // Update AOI item
            aoiItem.setAlertEnabled(enabled);
            aoiItem.setTriggerType(triggerName);
            aoiItem.setMonitoredType(monitoredName);
            aoiItem.setDurationHours(durationHours);

            if (enabled) {
                // Create and register geofence
                GeoFence geoFence = new GeoFence(
                        aoiItem.getShape(),
                        true,  // enabled
                        trigger,
                        monitoredTypes,
                        (int) durationMillis
                );

                GeoFenceComponent.getInstance().dispatch(geoFence, aoiItem.getShape());

                // Setup breach listener
                setupBreachListener();

                Toast.makeText(mapView.getContext(), "Alert enabled for " + aoiItem.getName(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "GeoFence created: " + aoiItem.getName() + " (" + triggerName + ", " + monitoredName + ")");
            } else {
                // Disable geofence
                GeoFence geoFence = new GeoFence(
                        aoiItem.getShape(),
                        false,  // disabled
                        trigger,
                        monitoredTypes,
                        (int) durationMillis
                );

                GeoFenceComponent.getInstance().dispatch(geoFence, aoiItem.getShape());

                // Remove breach listener
                if (breachReceiver != null) {
                    AtakBroadcast.getInstance().unregisterReceiver(breachReceiver);
                    breachReceiver = null;
                }

                Toast.makeText(mapView.getContext(), "Alert disabled for " + aoiItem.getName(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "GeoFence disabled: " + aoiItem.getName());
            }

            dialog.dismiss();

        } catch (NumberFormatException e) {
            Toast.makeText(mapView.getContext(), "Invalid duration", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error parsing duration", e);
        } catch (Exception e) {
            Toast.makeText(mapView.getContext(), "Error configuring alert", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error configuring geofence", e);
        }
    }

    private void setupBreachListener() {
        if (breachReceiver != null) {
            // Already registered
            return;
        }

        breachReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String fenceUID = intent.getStringExtra("fenceUID");
                String itemUID = intent.getStringExtra("itemUID");
                String breachType = intent.getStringExtra("breachType");

                if (fenceUID != null && fenceUID.equals(aoiItem.getUID())) {
                    Log.d(TAG, "GeoFence breach detected: " + aoiItem.getName() +
                            " - Item: " + itemUID + " - Type: " + breachType);

                    // Show notification
                    Toast.makeText(mapView.getContext(),
                            "ALERT: COT " + breachType + " AOI " + aoiItem.getName(),
                            Toast.LENGTH_LONG).show();
                }
            }
        };

        DocumentedIntentFilter filter = new DocumentedIntentFilter("com.atakmap.android.geofence.BREACH_EVENT");
        AtakBroadcast.getInstance().registerSystemReceiver(breachReceiver, filter);

        Log.d(TAG, "Breach listener registered for: " + aoiItem.getName());
    }
}
