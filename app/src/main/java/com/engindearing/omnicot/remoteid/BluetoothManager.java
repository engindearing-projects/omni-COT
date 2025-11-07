package com.engindearing.omnicot.remoteid;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages Bluetooth connection to the gyb_detect device.
 * Handles device discovery, connection, and data reception.
 */
public class BluetoothManager {

    private static final String TAG = "BluetoothManager";
    private static final String DEVICE_NAME_PREFIX = "gyb_detect";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private final Context context;
    private final Handler mainHandler;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private BluetoothDevice connectedDevice;
    private Thread readerThread;
    private volatile boolean isConnected = false;
    private volatile boolean shouldRun = false;

    private List<DataListener> dataListeners = new ArrayList<>();
    private List<ConnectionListener> connectionListeners = new ArrayList<>();
    private List<DiscoveryListener> discoveryListeners = new ArrayList<>();

    private BroadcastReceiver discoveryReceiver;
    private boolean isDiscovering = false;
    private List<BluetoothDevice> discoveredDevices = new ArrayList<>();

    /**
     * Listener for received data
     */
    public interface DataListener {
        void onDeviceInfo(RemoteIdParser.DeviceInfo info);
        void onBatteryStatus(RemoteIdParser.BatteryStatus status);
        void onRemoteIdData(RemoteIdData data);
    }

    /**
     * Listener for connection state changes
     */
    public interface ConnectionListener {
        void onConnecting(String deviceName);
        void onConnected(String deviceName);
        void onDisconnected();
        void onError(String error);
    }

    /**
     * Listener for device discovery
     */
    public interface DiscoveryListener {
        void onDiscoveryStarted();
        void onDeviceFound(BluetoothDevice device);
        void onDiscoveryFinished();
        void onPairingRequested(BluetoothDevice device);
        void onPairingSuccess(BluetoothDevice device);
        void onPairingFailed(BluetoothDevice device);
    }

    public BluetoothManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Register a data listener
     */
    public void addDataListener(DataListener listener) {
        if (!dataListeners.contains(listener)) {
            dataListeners.add(listener);
        }
    }

    /**
     * Unregister a data listener
     */
    public void removeDataListener(DataListener listener) {
        dataListeners.remove(listener);
    }

    /**
     * Register a connection listener
     */
    public void addConnectionListener(ConnectionListener listener) {
        if (!connectionListeners.contains(listener)) {
            connectionListeners.add(listener);
        }
    }

    /**
     * Unregister a connection listener
     */
    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    /**
     * Register a discovery listener
     */
    public void addDiscoveryListener(DiscoveryListener listener) {
        if (!discoveryListeners.contains(listener)) {
            discoveryListeners.add(listener);
        }
    }

    /**
     * Unregister a discovery listener
     */
    public void removeDiscoveryListener(DiscoveryListener listener) {
        discoveryListeners.remove(listener);
    }

    /**
     * Check if Bluetooth is supported and enabled
     */
    public boolean isBluetoothAvailable() {
        if (bluetoothAdapter == null) {
            return false;
        }
        return bluetoothAdapter.isEnabled();
    }

    /**
     * Scan for paired gyb_detect devices
     */
    public List<BluetoothDevice> findGybDevices() {
        List<BluetoothDevice> gybDevices = new ArrayList<>();

        if (!isBluetoothAvailable()) {
            Log.w(TAG, "Bluetooth not available");
            return gybDevices;
        }

        // Check permissions
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted");
            return gybDevices;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                String name = device.getName();
                if (name != null && name.startsWith(DEVICE_NAME_PREFIX)) {
                    gybDevices.add(device);
                    Log.d(TAG, "Found gyb_detect device: " + name + " (" + device.getAddress() + ")");
                }
            }
        }

        return gybDevices;
    }

    /**
     * Start discovering nearby Bluetooth devices
     */
    public void startDiscovery() {
        if (!isBluetoothAvailable()) {
            notifyDiscoveryError("Bluetooth not available");
            return;
        }

        // Check permissions
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            notifyDiscoveryError("BLUETOOTH_SCAN permission not granted");
            return;
        }

        if (isDiscovering) {
            Log.w(TAG, "Discovery already in progress");
            return;
        }

        // Clear previous results
        discoveredDevices.clear();

        // Set up discovery receiver
        if (discoveryReceiver == null) {
            discoveryReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device != null) {
                            handleDeviceFound(device);
                        }
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                        handleDiscoveryFinished();
                    } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                        handleBondStateChanged(device, bondState);
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            context.registerReceiver(discoveryReceiver, filter);
        }

        // Start discovery
        if (bluetoothAdapter.startDiscovery()) {
            isDiscovering = true;
            notifyDiscoveryStarted();
            Log.d(TAG, "Started Bluetooth discovery");
        } else {
            notifyDiscoveryError("Failed to start discovery");
        }
    }

    /**
     * Stop discovering Bluetooth devices
     */
    public void stopDiscovery() {
        if (!isDiscovering) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.cancelDiscovery();
        }

        isDiscovering = false;
        Log.d(TAG, "Stopped Bluetooth discovery");
    }

    /**
     * Get list of discovered devices
     */
    public List<BluetoothDevice> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices);
    }

    /**
     * Initiate pairing with a device
     */
    public boolean pairDevice(BluetoothDevice device) {
        if (device == null) {
            return false;
        }

        // Check if already paired
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            Log.d(TAG, "Device already paired: " + getDeviceName(device));
            return true;
        }

        // Check permissions
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            notifyDiscoveryError("BLUETOOTH_CONNECT permission not granted");
            return false;
        }

        // Initiate pairing
        try {
            boolean result = device.createBond();
            if (result) {
                notifyPairingRequested(device);
                Log.d(TAG, "Pairing requested for: " + getDeviceName(device));
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initiate pairing", e);
            return false;
        }
    }

    /**
     * Handle device found during discovery
     */
    private void handleDeviceFound(BluetoothDevice device) {
        if (device == null) return;

        // Avoid duplicates
        for (BluetoothDevice existing : discoveredDevices) {
            if (existing.getAddress().equals(device.getAddress())) {
                return;
            }
        }

        discoveredDevices.add(device);
        notifyDeviceFound(device);

        String name = getDeviceName(device);
        Log.d(TAG, "Found device: " + name + " (" + device.getAddress() + ")");
    }

    /**
     * Handle discovery finished
     */
    private void handleDiscoveryFinished() {
        isDiscovering = false;
        notifyDiscoveryFinished();
        Log.d(TAG, "Discovery finished. Found " + discoveredDevices.size() + " devices");
    }

    /**
     * Handle bond state changes (pairing)
     */
    private void handleBondStateChanged(BluetoothDevice device, int bondState) {
        if (device == null) return;

        String name = getDeviceName(device);
        switch (bondState) {
            case BluetoothDevice.BOND_BONDED:
                Log.d(TAG, "Device paired: " + name);
                notifyPairingSuccess(device);
                break;
            case BluetoothDevice.BOND_NONE:
                Log.d(TAG, "Device unpaired: " + name);
                notifyPairingFailed(device);
                break;
            case BluetoothDevice.BOND_BONDING:
                Log.d(TAG, "Device pairing in progress: " + name);
                break;
        }
    }

    /**
     * Get device name safely
     */
    private String getDeviceName(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED) {
            String name = device.getName();
            return name != null ? name : device.getAddress();
        }
        return device.getAddress();
    }

    /**
     * Connect to a specific device
     */
    public void connect(BluetoothDevice device) {
        if (isConnected) {
            Log.w(TAG, "Already connected, disconnect first");
            return;
        }

        // Check permissions
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            notifyError("BLUETOOTH_CONNECT permission not granted");
            return;
        }

        String deviceName = device.getName();
        notifyConnecting(deviceName);

        new Thread(() -> {
            try {
                Log.d(TAG, "Connecting to " + deviceName);
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();

                connectedDevice = device;
                isConnected = true;
                shouldRun = true;

                Log.i(TAG, "Connected to " + deviceName);
                notifyConnected(deviceName);

                // Start reading data
                startReading();

            } catch (IOException e) {
                Log.e(TAG, "Failed to connect to " + deviceName, e);
                notifyError("Connection failed: " + e.getMessage());
                cleanup();
            }
        }).start();
    }

    /**
     * Connect to the first available gyb_detect device
     */
    public void connectToFirstDevice() {
        List<BluetoothDevice> devices = findGybDevices();
        if (devices.isEmpty()) {
            notifyError("No paired gyb_detect devices found");
            return;
        }
        connect(devices.get(0));
    }

    /**
     * Disconnect from the current device
     */
    public void disconnect() {
        shouldRun = false;
        cleanup();
        notifyDisconnected();
    }

    /**
     * Check if currently connected
     */
    public boolean isConnected() {
        return isConnected && socket != null && socket.isConnected();
    }

    /**
     * Get the connected device name
     */
    public String getConnectedDeviceName() {
        if (connectedDevice != null && ActivityCompat.checkSelfPermission(context,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            return connectedDevice.getName();
        }
        return null;
    }

    /**
     * Start reading data from the device
     */
    private void startReading() {
        if (socket == null) {
            Log.w(TAG, "Cannot start reading - socket is null");
            return;
        }

        if (!socket.isConnected()) {
            Log.w(TAG, "Cannot start reading - socket is not connected");
            return;
        }

        readerThread = new Thread(() -> {
            try {
                // Double-check socket is still valid when thread starts
                if (socket == null || !socket.isConnected()) {
                    Log.w(TAG, "Socket closed before reader thread started");
                    return;
                }

                InputStream inputStream;
                try {
                    inputStream = socket.getInputStream();
                } catch (IOException e) {
                    // Socket was closed before we could get the stream
                    if (e.getMessage() != null && e.getMessage().contains("Socket is closed")) {
                        Log.w(TAG, "Socket was closed before starting read - ignoring");
                        return; // Exit gracefully without notifying as error
                    } else {
                        throw e; // Re-throw other IOExceptions
                    }
                }

                // Use larger buffer size (8KB) for better performance with burst traffic
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 8192);
                StringBuilder jsonBuffer = new StringBuilder();
                int braceCount = 0;
                boolean inJson = false;

                Log.d(TAG, "Started reading data with buffered array reads");

                // Use char array for more efficient reading
                char[] buffer = new char[1024];

                while (shouldRun && isConnected) {
                    try {
                        // Check if data is available before blocking read
                        if (!reader.ready()) {
                            Thread.sleep(10); // Small sleep to prevent busy-wait
                            continue;
                        }

                        int numRead = reader.read(buffer, 0, buffer.length);
                        if (numRead == -1) {
                            Log.w(TAG, "End of stream reached");
                            break;
                        }

                        Log.d(TAG, "Read " + numRead + " characters from stream");

                        // Process buffer content
                        for (int i = 0; i < numRead; i++) {
                            char c = buffer[i];

                            // JSON parsing state machine
                            if (c == '{') {
                                if (!inJson) {
                                    inJson = true;
                                    jsonBuffer.setLength(0);
                                }
                                braceCount++;
                                jsonBuffer.append(c);
                            } else if (c == '}') {
                                if (inJson) {
                                    jsonBuffer.append(c);
                                    braceCount--;

                                    if (braceCount == 0) {
                                        // Complete JSON object received
                                        String jsonString = jsonBuffer.toString();
                                        Log.d(TAG, "Received complete JSON: " + jsonString.substring(0, Math.min(100, jsonString.length())) + (jsonString.length() > 100 ? "..." : ""));
                                        try {
                                            processJsonData(jsonString);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error processing JSON data: " + jsonString, e);
                                            e.printStackTrace();
                                            // Don't break the read loop - continue reading
                                        }
                                        inJson = false;
                                        jsonBuffer.setLength(0);
                                    }
                                }
                            } else if (inJson) {
                                jsonBuffer.append(c);
                            }
                        }

                    } catch (InterruptedIOException | InterruptedException e) {
                        // Thread was interrupted - normal shutdown
                        Log.d(TAG, "Reader thread interrupted, shutting down");
                        break;
                    } catch (IOException e) {
                        // IOException on read - connection problem
                        if (shouldRun) {
                            Log.e(TAG, "IOException while reading data", e);
                            mainHandler.post(() -> notifyError("Connection lost: " + e.getMessage()));
                        }
                        break;
                    } catch (Exception e) {
                        // Unexpected exception - log but don't break connection
                        Log.e(TAG, "Unexpected error in read loop", e);
                        e.printStackTrace();
                        // Continue reading - don't break the connection for non-IO errors
                    }
                }

                Log.d(TAG, "Read loop exited");

            } catch (IOException e) {
                if (shouldRun) {
                    Log.e(TAG, "Error initializing stream reader", e);
                    mainHandler.post(() -> notifyError("Connection lost: " + e.getMessage()));
                }
            } finally {
                // Only cleanup if we're still the owner of the connection
                if (isConnected) {
                    Log.d(TAG, "Read thread ending, calling cleanup");
                    cleanup();
                    mainHandler.post(() -> notifyDisconnected());
                } else {
                    Log.d(TAG, "Read thread ending, already disconnected");
                }
            }
        });
        readerThread.start();
    }

    /**
     * Process received JSON data
     */
    private void processJsonData(String jsonString) {
        Object result = RemoteIdParser.parseMessage(jsonString);

        if (result == null) {
            return;
        }

        // Notify listeners on main thread
        mainHandler.post(() -> {
            if (result instanceof RemoteIdParser.DeviceInfo) {
                for (DataListener listener : dataListeners) {
                    listener.onDeviceInfo((RemoteIdParser.DeviceInfo) result);
                }
            } else if (result instanceof RemoteIdParser.BatteryStatus) {
                for (DataListener listener : dataListeners) {
                    listener.onBatteryStatus((RemoteIdParser.BatteryStatus) result);
                }
            } else if (result instanceof RemoteIdData) {
                RemoteIdData data = (RemoteIdData) result;
                if (data.isValidLocation()) {
                    for (DataListener listener : dataListeners) {
                        listener.onRemoteIdData(data);
                    }
                } else {
                    Log.d(TAG, "Ignoring detection with invalid location");
                }
            }
        });
    }

    /**
     * Clean up resources
     */
    private synchronized void cleanup() {
        // Prevent double cleanup
        if (!isConnected && socket == null) {
            Log.d(TAG, "cleanup() called but already cleaned up, skipping");
            return;
        }

        Log.d(TAG, "Starting cleanup");
        isConnected = false;
        shouldRun = false;

        // Wait for reader thread to finish
        if (readerThread != null) {
            readerThread.interrupt();
            try {
                readerThread.join(2000); // Wait up to 2 seconds
                Log.d(TAG, "Reader thread joined successfully");
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while waiting for reader thread to finish");
            }
            readerThread = null;
        }

        // Now safe to close socket
        if (socket != null) {
            try {
                socket.close();
                Log.d(TAG, "Socket closed successfully");
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket", e);
            }
            socket = null;
        }

        connectedDevice = null;
        Log.d(TAG, "Cleanup completed");
    }

    /**
     * Notify listeners of connection state changes
     */
    private void notifyConnecting(String deviceName) {
        mainHandler.post(() -> {
            for (ConnectionListener listener : connectionListeners) {
                listener.onConnecting(deviceName);
            }
        });
    }

    private void notifyConnected(String deviceName) {
        mainHandler.post(() -> {
            for (ConnectionListener listener : connectionListeners) {
                listener.onConnected(deviceName);
            }
        });
    }

    private void notifyDisconnected() {
        mainHandler.post(() -> {
            for (ConnectionListener listener : connectionListeners) {
                listener.onDisconnected();
            }
        });
    }

    private void notifyError(String error) {
        mainHandler.post(() -> {
            for (ConnectionListener listener : connectionListeners) {
                listener.onError(error);
            }
        });
    }

    private void notifyDiscoveryStarted() {
        mainHandler.post(() -> {
            for (DiscoveryListener listener : discoveryListeners) {
                listener.onDiscoveryStarted();
            }
        });
    }

    private void notifyDeviceFound(BluetoothDevice device) {
        mainHandler.post(() -> {
            for (DiscoveryListener listener : discoveryListeners) {
                listener.onDeviceFound(device);
            }
        });
    }

    private void notifyDiscoveryFinished() {
        mainHandler.post(() -> {
            for (DiscoveryListener listener : discoveryListeners) {
                listener.onDiscoveryFinished();
            }
        });
    }

    private void notifyPairingRequested(BluetoothDevice device) {
        mainHandler.post(() -> {
            for (DiscoveryListener listener : discoveryListeners) {
                listener.onPairingRequested(device);
            }
        });
    }

    private void notifyPairingSuccess(BluetoothDevice device) {
        mainHandler.post(() -> {
            for (DiscoveryListener listener : discoveryListeners) {
                listener.onPairingSuccess(device);
            }
        });
    }

    private void notifyPairingFailed(BluetoothDevice device) {
        mainHandler.post(() -> {
            for (DiscoveryListener listener : discoveryListeners) {
                listener.onPairingFailed(device);
            }
        });
    }

    private void notifyDiscoveryError(String error) {
        mainHandler.post(() -> {
            for (ConnectionListener listener : connectionListeners) {
                listener.onError(error);
            }
        });
    }

    /**
     * Release all resources
     */
    public void shutdown() {
        stopDiscovery();
        disconnect();

        // Unregister discovery receiver
        if (discoveryReceiver != null) {
            try {
                context.unregisterReceiver(discoveryReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering discovery receiver", e);
            }
            discoveryReceiver = null;
        }

        dataListeners.clear();
        connectionListeners.clear();
        discoveryListeners.clear();
    }
}
