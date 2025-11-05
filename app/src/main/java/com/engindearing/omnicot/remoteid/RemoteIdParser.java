package com.engindearing.omnicot.remoteid;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Parser for JSON data received from the gyb_detect device.
 * Handles device info, battery status, and drone detection messages.
 */
public class RemoteIdParser {

    private static final String TAG = "RemoteIdParser";

    /**
     * Device information from gyb_detect
     */
    public static class DeviceInfo {
        public String manufacturer;
        public String make;
        public String model;
        public String version;
        public String serialNumber;
        public int capabilities;

        @Override
        public String toString() {
            return model + " v" + version + " (SN: " + serialNumber + ")";
        }
    }

    /**
     * Battery status from gyb_detect
     */
    public static class BatteryStatus {
        public float level;        // 0.0 - 1.0
        public String version;
        public float temperature;  // Celsius

        public int getPercentage() {
            return Math.round(level * 100);
        }
    }

    /**
     * Parses a JSON message and returns the appropriate object type.
     * Returns DeviceInfo, BatteryStatus, or RemoteIdData depending on content.
     */
    public static Object parseMessage(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);

            // Check for device info message
            if (json.has("manufacturer")) {
                return parseDeviceInfo(json);
            }

            // Check for battery status message
            if (json.has("batteryLevel")) {
                return parseBatteryStatus(json);
            }

            // Check for drone detection message
            if (json.has("remoteId") || json.has("uasId")) {
                return parseRemoteIdData(json);
            }

            Log.w(TAG, "Unknown JSON message type: " + jsonString);
            return null;

        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSON: " + jsonString, e);
            return null;
        }
    }

    /**
     * Parses device information JSON
     */
    public static DeviceInfo parseDeviceInfo(JSONObject json) throws JSONException {
        DeviceInfo info = new DeviceInfo();
        info.manufacturer = json.optString("manufacturer", "");
        info.make = json.optString("make", "");
        info.model = json.optString("model", "");
        info.version = json.optString("version", "");
        info.serialNumber = json.optString("serialNumber", "");
        info.capabilities = json.optInt("capabilities", 0);
        return info;
    }

    /**
     * Parses battery status JSON
     */
    public static BatteryStatus parseBatteryStatus(JSONObject json) throws JSONException {
        BatteryStatus status = new BatteryStatus();
        status.level = (float) json.optDouble("batteryLevel", 0.0);
        status.version = json.optString("batteryVersion", "");
        status.temperature = (float) json.optDouble("batteryTemp", 0.0);
        return status;
    }

    /**
     * Parses Remote ID drone detection JSON
     */
    public static RemoteIdData parseRemoteIdData(JSONObject json) throws JSONException {
        RemoteIdData data = new RemoteIdData();

        // Device identification
        data.setUasId(json.optString("uasId", ""));
        data.setRemoteId(json.optString("remoteId", ""));
        data.setSerialNumber(json.optString("serialNumber", ""));
        data.setCaaRegId(json.optString("caaRegId", ""));
        data.setDescription(json.optString("description", ""));
        data.setOpId(json.optString("opId", ""));

        // Reception metadata
        data.setRssi(json.optInt("rssi", 0));
        data.setRecvMethod(json.optInt("recvMethod", 0));

        // Drone type and status
        data.setUasType(json.optInt("uasType", 0));
        data.setSessionId(json.optInt("sessionId", 0));
        data.setUtmId(json.optString("utmId", ""));
        data.setOpStatus(json.optInt("opStatus", 0));

        // Drone location - parse as strings first to handle empty strings
        data.setUasLat(parseDouble(json, "uasLat"));
        data.setUasLon(parseDouble(json, "uasLon"));
        data.setUasHeading(parseFloat(json, "uasHeading"));
        data.setUasHSpeed(parseFloat(json, "uasHSpeed"));
        data.setUasHSpeedError(parseFloat(json, "uasHSpeedError"));
        data.setUasVSpeed(parseFloat(json, "uasVSpeed"));
        data.setUasVSpeedError(parseFloat(json, "uasVSpeedError"));
        data.setUasHae(parseFloat(json, "uasHae"));
        data.setUasHag(parseFloat(json, "uasHag"));
        data.setUasHat(parseFloat(json, "uasHat"));
        data.setUasHorizontalError(parseFloat(json, "uasHorizontalError"));
        data.setUasVerticalError(parseFloat(json, "uasVerticalError"));
        data.setUasBaroPressure(parseFloat(json, "uasBaroPressure"));
        data.setUasBaroPressureAcc(parseFloat(json, "uasBaroPressureAcc"));

        // Operator location
        data.setOpLat(parseDouble(json, "opLat"));
        data.setOpLon(parseDouble(json, "opLon"));
        data.setOpHae(parseFloat(json, "opHae"));
        data.setOpLocationType(json.optInt("opLocationType", 0));

        return data;
    }

    /**
     * Safely parse a double from JSON, handling strings and null values
     */
    private static double parseDouble(JSONObject json, String key) {
        if (!json.has(key)) return 0.0;

        String value = json.optString(key, "0");
        if (value == null || value.isEmpty()) return 0.0;

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Safely parse a float from JSON, handling strings and null values
     */
    private static float parseFloat(JSONObject json, String key) {
        if (!json.has(key)) return 0.0f;

        String value = json.optString(key, "0");
        if (value == null || value.isEmpty()) return 0.0f;

        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0.0f;
        }
    }

    /**
     * Parses capabilities bitmask into human-readable list
     */
    public static String getCapabilitiesString(int capabilities) {
        StringBuilder sb = new StringBuilder();
        if ((capabilities & 0x01) != 0) sb.append("WiFi Beacon 2.4GHz, ");
        if ((capabilities & 0x02) != 0) sb.append("WiFi NaN 2.4GHz, ");
        if ((capabilities & 0x04) != 0) sb.append("WiFi Beacon 5GHz, ");
        if ((capabilities & 0x08) != 0) sb.append("WiFi NaN 5GHz, ");
        if ((capabilities & 0x10) != 0) sb.append("Bluetooth 4, ");
        if ((capabilities & 0x20) != 0) sb.append("Bluetooth 5, ");

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2); // Remove trailing ", "
        }
        return sb.toString();
    }
}
