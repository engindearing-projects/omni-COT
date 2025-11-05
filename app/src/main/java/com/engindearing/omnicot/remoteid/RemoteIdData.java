package com.engindearing.omnicot.remoteid;

/**
 * Data model representing a Remote ID detection from the gyb_detect device.
 * Corresponds to the JSON format sent by the ESP32 device.
 */
public class RemoteIdData {

    // Device identification
    private String uasId;           // MAC address of the drone
    private String remoteId;        // Operator ID
    private String serialNumber;    // Drone serial number
    private String caaRegId;        // CAA registration ID
    private String description;     // Self-ID description text
    private String opId;            // Operator ID string

    // Reception metadata
    private int rssi;               // Signal strength
    private int recvMethod;         // 1=WiFi Beacon, 2=WiFi NaN, 16=Bluetooth
    private long timestamp;         // Detection timestamp

    // Drone type and status
    private int uasType;            // UAV type (0-15)
    private int sessionId;          // Session ID
    private String utmId;           // UTM UUID
    private int opStatus;           // Operational status (0-4)

    // Drone location
    private double uasLat;          // Drone latitude
    private double uasLon;          // Drone longitude
    private float uasHeading;       // Heading in degrees (0-360)
    private float uasHSpeed;        // Horizontal speed (m/s)
    private float uasHSpeedError;   // Horizontal speed error
    private float uasVSpeed;        // Vertical speed (m/s)
    private float uasVSpeedError;   // Vertical speed error
    private float uasHae;           // Altitude MSL (meters)
    private float uasHag;           // Height above ground (meters)
    private float uasHat;           // Height above takeoff (meters)
    private float uasHorizontalError; // Horizontal accuracy (meters)
    private float uasVerticalError;   // Vertical accuracy (meters)
    private float uasBaroPressure;    // Barometric altitude
    private float uasBaroPressureAcc; // Baro accuracy

    // Operator location
    private double opLat;           // Operator latitude
    private double opLon;           // Operator longitude
    private float opHae;            // Operator altitude MSL
    private int opLocationType;     // 0=takeoff, 1=live, 2=fixed

    // Constructors
    public RemoteIdData() {
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUasId() { return uasId; }
    public void setUasId(String uasId) { this.uasId = uasId; }

    public String getRemoteId() { return remoteId; }
    public void setRemoteId(String remoteId) { this.remoteId = remoteId; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getCaaRegId() { return caaRegId; }
    public void setCaaRegId(String caaRegId) { this.caaRegId = caaRegId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOpId() { return opId; }
    public void setOpId(String opId) { this.opId = opId; }

    public int getRssi() { return rssi; }
    public void setRssi(int rssi) { this.rssi = rssi; }

    public int getRecvMethod() { return recvMethod; }
    public void setRecvMethod(int recvMethod) { this.recvMethod = recvMethod; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getUasType() { return uasType; }
    public void setUasType(int uasType) { this.uasType = uasType; }

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public String getUtmId() { return utmId; }
    public void setUtmId(String utmId) { this.utmId = utmId; }

    public int getOpStatus() { return opStatus; }
    public void setOpStatus(int opStatus) { this.opStatus = opStatus; }

    public double getUasLat() { return uasLat; }
    public void setUasLat(double uasLat) { this.uasLat = uasLat; }

    public double getUasLon() { return uasLon; }
    public void setUasLon(double uasLon) { this.uasLon = uasLon; }

    public float getUasHeading() { return uasHeading; }
    public void setUasHeading(float uasHeading) { this.uasHeading = uasHeading; }

    public float getUasHSpeed() { return uasHSpeed; }
    public void setUasHSpeed(float uasHSpeed) { this.uasHSpeed = uasHSpeed; }

    public float getUasHSpeedError() { return uasHSpeedError; }
    public void setUasHSpeedError(float uasHSpeedError) { this.uasHSpeedError = uasHSpeedError; }

    public float getUasVSpeed() { return uasVSpeed; }
    public void setUasVSpeed(float uasVSpeed) { this.uasVSpeed = uasVSpeed; }

    public float getUasVSpeedError() { return uasVSpeedError; }
    public void setUasVSpeedError(float uasVSpeedError) { this.uasVSpeedError = uasVSpeedError; }

    public float getUasHae() { return uasHae; }
    public void setUasHae(float uasHae) { this.uasHae = uasHae; }

    public float getUasHag() { return uasHag; }
    public void setUasHag(float uasHag) { this.uasHag = uasHag; }

    public float getUasHat() { return uasHat; }
    public void setUasHat(float uasHat) { this.uasHat = uasHat; }

    public float getUasHorizontalError() { return uasHorizontalError; }
    public void setUasHorizontalError(float uasHorizontalError) { this.uasHorizontalError = uasHorizontalError; }

    public float getUasVerticalError() { return uasVerticalError; }
    public void setUasVerticalError(float uasVerticalError) { this.uasVerticalError = uasVerticalError; }

    public float getUasBaroPressure() { return uasBaroPressure; }
    public void setUasBaroPressure(float uasBaroPressure) { this.uasBaroPressure = uasBaroPressure; }

    public float getUasBaroPressureAcc() { return uasBaroPressureAcc; }
    public void setUasBaroPressureAcc(float uasBaroPressureAcc) { this.uasBaroPressureAcc = uasBaroPressureAcc; }

    public double getOpLat() { return opLat; }
    public void setOpLat(double opLat) { this.opLat = opLat; }

    public double getOpLon() { return opLon; }
    public void setOpLon(double opLon) { this.opLon = opLon; }

    public float getOpHae() { return opHae; }
    public void setOpHae(float opHae) { this.opHae = opHae; }

    public int getOpLocationType() { return opLocationType; }
    public void setOpLocationType(int opLocationType) { this.opLocationType = opLocationType; }

    /**
     * Validates that essential location data is present and valid
     */
    public boolean isValidLocation() {
        // Check for valid GPS coordinates
        if (uasLat == 0.0 && uasLon == 0.0) return false;
        if (Double.isNaN(uasLat) || Double.isNaN(uasLon)) return false;
        if (uasLat < -90 || uasLat > 90) return false;
        if (uasLon < -180 || uasLon > 180) return false;
        return true;
    }

    /**
     * Get a human-readable description of the receive method
     */
    public String getRecvMethodString() {
        switch (recvMethod) {
            case 1: return "WiFi Beacon";
            case 2: return "WiFi NaN";
            case 16: return "Bluetooth";
            default: return "Unknown (" + recvMethod + ")";
        }
    }

    /**
     * Get a unique identifier for this drone
     */
    public String getUniqueId() {
        if (serialNumber != null && !serialNumber.isEmpty()) {
            return serialNumber;
        }
        if (uasId != null && !uasId.isEmpty()) {
            return uasId;
        }
        if (remoteId != null && !remoteId.isEmpty()) {
            return remoteId;
        }
        return "UNKNOWN-" + System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "RemoteIdData{" +
                "uasId='" + uasId + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", lat=" + uasLat +
                ", lon=" + uasLon +
                ", alt=" + uasHae +
                ", heading=" + uasHeading +
                ", speed=" + uasHSpeed +
                ", recvMethod=" + getRecvMethodString() +
                ", rssi=" + rssi +
                '}';
    }
}
