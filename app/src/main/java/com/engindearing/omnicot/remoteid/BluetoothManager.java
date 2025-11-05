package com.engindearing.omnicot.remoteid;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        if (socket == null) return;

        readerThread = new Thread(() -> {
            try {
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder jsonBuffer = new StringBuilder();
                int braceCount = 0;
                boolean inJson = false;

                Log.d(TAG, "Started reading data");

                while (shouldRun && isConnected) {
                    int ch = reader.read();
                    if (ch == -1) {
                        Log.w(TAG, "End of stream reached");
                        break;
                    }

                    char c = (char) ch;

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
                                processJsonData(jsonString);
                                inJson = false;
                                jsonBuffer.setLength(0);
                            }
                        }
                    } else if (inJson) {
                        jsonBuffer.append(c);
                    }
                }

            } catch (IOException e) {
                if (shouldRun) {
                    Log.e(TAG, "Error reading data", e);
                    notifyError("Connection lost: " + e.getMessage());
                }
            } finally {
                cleanup();
                if (shouldRun) {
                    notifyDisconnected();
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
    private void cleanup() {
        isConnected = false;
        shouldRun = false;

        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket", e);
            }
            socket = null;
        }

        connectedDevice = null;
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

    /**
     * Release all resources
     */
    public void shutdown() {
        disconnect();
        dataListeners.clear();
        connectionListeners.clear();
    }
}
