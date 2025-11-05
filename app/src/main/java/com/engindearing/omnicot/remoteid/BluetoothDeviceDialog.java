package com.engindearing.omnicot.remoteid;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.engindearing.omnicot.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog to show available Bluetooth devices and initiate pairing/connection
 */
public class BluetoothDeviceDialog {

    public interface DeviceSelectionListener {
        void onDeviceSelected(BluetoothDevice device);
    }

    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final DeviceSelectionListener listener;
    private AlertDialog dialog;
    private DeviceAdapter adapter;
    private ProgressBar progressBar;
    private TextView statusText;
    private Button scanButton;

    public BluetoothDeviceDialog(Context context, BluetoothManager bluetoothManager,
                                  DeviceSelectionListener listener) {
        this.context = context;
        this.bluetoothManager = bluetoothManager;
        this.listener = listener;
    }

    public void show() {
        // Create custom view
        View view = LayoutInflater.from(context).inflate(
                android.R.layout.select_dialog_item, null);

        // Create a simple linear layout programmatically since we don't have a custom layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        // Status text
        statusText = new TextView(context);
        statusText.setText("Searching for gyb_detect devices...");
        statusText.setPadding(10, 10, 10, 10);
        layout.addView(statusText);

        // Progress bar
        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        layout.addView(progressBar);

        // Device list
        ListView listView = new ListView(context);
        listView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 400));
        adapter = new DeviceAdapter(context, new ArrayList<>());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, v, position, id) -> {
            BluetoothDevice device = adapter.getItem(position);
            if (device != null) {
                handleDeviceClick(device);
            }
        });
        layout.addView(listView);

        // Scan button
        scanButton = new Button(context);
        scanButton.setText("Scan for Devices");
        scanButton.setOnClickListener(v -> startScan());
        layout.addView(scanButton);

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select gyb_detect Device");
        builder.setView(layout);
        builder.setNegativeButton("Cancel", (d, which) -> {
            bluetoothManager.stopDiscovery();
            d.dismiss();
        });

        dialog = builder.create();

        // Set up discovery listener
        bluetoothManager.addDiscoveryListener(new BluetoothManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted() {
                progressBar.setVisibility(View.VISIBLE);
                statusText.setText("Scanning for Bluetooth devices...");
                scanButton.setEnabled(false);
                adapter.clear();
            }

            @Override
            public void onDeviceFound(BluetoothDevice device) {
                // Only show gyb_detect devices or allow all for debugging
                String name = getDeviceName(device);
                if (name.startsWith("gyb_detect") || name.contains("gyb")) {
                    adapter.add(device);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onDiscoveryFinished() {
                progressBar.setVisibility(View.GONE);
                scanButton.setEnabled(true);

                if (adapter.getCount() == 0) {
                    statusText.setText("No gyb_detect devices found. Make sure the device is powered on.");
                } else {
                    statusText.setText("Found " + adapter.getCount() + " device(s). Tap to pair and connect.");
                }
            }

            @Override
            public void onPairingRequested(BluetoothDevice device) {
                Toast.makeText(context, "Pairing with " + getDeviceName(device) + "...",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPairingSuccess(BluetoothDevice device) {
                Toast.makeText(context, "Paired with " + getDeviceName(device),
                        Toast.LENGTH_SHORT).show();
                // Auto-connect after pairing
                if (listener != null) {
                    listener.onDeviceSelected(device);
                }
                dialog.dismiss();
            }

            @Override
            public void onPairingFailed(BluetoothDevice device) {
                Toast.makeText(context, "Pairing failed with " + getDeviceName(device),
                        Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();

        // Start initial scan
        startScan();
    }

    private void startScan() {
        // Add already paired devices first
        List<BluetoothDevice> paired = bluetoothManager.findGybDevices();
        adapter.clear();
        for (BluetoothDevice device : paired) {
            adapter.add(device);
        }
        adapter.notifyDataSetChanged();

        if (!paired.isEmpty()) {
            statusText.setText("Found " + paired.size() + " paired device(s). Scanning for more...");
        }

        // Start discovery
        bluetoothManager.startDiscovery();
    }

    private void handleDeviceClick(BluetoothDevice device) {
        // Check if already paired
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            // Already paired, just connect
            if (listener != null) {
                listener.onDeviceSelected(device);
            }
            dialog.dismiss();
        } else {
            // Need to pair first
            statusText.setText("Initiating pairing with " + getDeviceName(device) + "...");
            bluetoothManager.pairDevice(device);
        }
    }

    private String getDeviceName(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED) {
            String name = device.getName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        return device.getAddress();
    }

    /**
     * Simple adapter for Bluetooth devices
     */
    private class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {

        public DeviceAdapter(Context context, List<BluetoothDevice> devices) {
            super(context, android.R.layout.simple_list_item_2, android.R.id.text1, devices);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            BluetoothDevice device = getItem(position);
            if (device != null) {
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                String name = getDeviceName(device);
                text1.setText(name);

                String status = device.getBondState() == BluetoothDevice.BOND_BONDED
                        ? "Paired - Tap to connect"
                        : "Not paired - Tap to pair";
                text2.setText(device.getAddress() + " (" + status + ")");
            }

            return view;
        }
    }
}
