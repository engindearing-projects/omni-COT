package com.engindearing.omnicot.remoteid;

import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;

/**
 * Converts Remote ID drone detections to CoT (Cursor on Target) events for display in ATAK.
 */
public class RemoteIdToCotConverter {

    private static final String TAG = "RemoteIdToCotConverter";

    // CoT type prefix for different drone classifications
    // Using 'u' (unknown) by default since we don't know if drone is friend or foe
    // Format: a-{affiliation}-A-{battle dimension}-{function}
    // a-u-A = Airborne, Unknown affiliation
    private static final String COT_TYPE_DRONE_UNKNOWN = "a-u-A-M-F-Q-r"; // Unknown air track, rotary wing
    private static final String COT_TYPE_DRONE_HOSTILE = "a-h-A-M-F-Q-r"; // Hostile air track, rotary wing

    /**
     * Convert a Remote ID detection to a CoT event
     */
    public static CotEvent convertToCotEvent(RemoteIdData data) {
        if (data == null || !data.isValidLocation()) {
            return null;
        }

        try {
            CotEvent cotEvent = new CotEvent();

            // Set UID - use unique identifier from drone
            String uid = "DRONE-" + data.getUniqueId();
            cotEvent.setUID(uid);

            // Set CoT type - unknown drone
            cotEvent.setType(COT_TYPE_DRONE_UNKNOWN);

            // Set how - sensor (h-s) since it's detected by sensor
            cotEvent.setHow("h-s");

            // Set timestamps
            CoordinatedTime now = new CoordinatedTime();
            cotEvent.setTime(now);
            cotEvent.setStart(now);
            // Stale after 30 seconds (drone detections are time-sensitive)
            cotEvent.setStale(new CoordinatedTime(now.getMilliseconds() + 30 * 1000));

            // Set location point with altitude
            GeoPoint geoPoint = new GeoPoint(
                data.getUasLat(),
                data.getUasLon(),
                data.getUasHae(), // altitude MSL in meters
                GeoPoint.AltitudeReference.HAE  // Height Above Ellipsoid
            );
            cotEvent.setPoint(new CotPoint(geoPoint));

            // Create detail with Remote ID metadata
            CotDetail detail = new CotDetail();

            // Add contact info
            CotDetail contact = new CotDetail("contact");
            String callsign = generateCallsign(data);
            contact.setAttribute("callsign", callsign);
            detail.addChild(contact);

            // Add track information (speed, heading)
            CotDetail track = new CotDetail("track");
            track.setAttribute("course", String.valueOf(data.getUasHeading()));
            track.setAttribute("speed", String.valueOf(data.getUasHSpeed())); // m/s
            detail.addChild(track);

            // Add Remote ID specific details
            CotDetail remoteIdDetail = new CotDetail("__remoteid");

            // Basic identification
            if (data.getSerialNumber() != null && !data.getSerialNumber().isEmpty()) {
                remoteIdDetail.setAttribute("serialNumber", data.getSerialNumber());
            }
            if (data.getRemoteId() != null && !data.getRemoteId().isEmpty()) {
                remoteIdDetail.setAttribute("operatorId", data.getRemoteId());
            }
            if (data.getOpId() != null && !data.getOpId().isEmpty()) {
                remoteIdDetail.setAttribute("opId", data.getOpId());
            }
            if (data.getDescription() != null && !data.getDescription().isEmpty()) {
                remoteIdDetail.setAttribute("description", data.getDescription());
            }
            if (data.getCaaRegId() != null && !data.getCaaRegId().isEmpty()) {
                remoteIdDetail.setAttribute("caaRegId", data.getCaaRegId());
            }

            // Reception info
            remoteIdDetail.setAttribute("rssi", String.valueOf(data.getRssi()));
            remoteIdDetail.setAttribute("recvMethod", data.getRecvMethodString());
            remoteIdDetail.setAttribute("uasType", String.valueOf(data.getUasType()));

            // Altitude data
            remoteIdDetail.setAttribute("heightAGL", String.format("%.1f", data.getUasHag()));
            remoteIdDetail.setAttribute("heightTakeoff", String.format("%.1f", data.getUasHat()));

            // Speed data
            remoteIdDetail.setAttribute("vSpeed", String.format("%.1f", data.getUasVSpeed()));
            remoteIdDetail.setAttribute("hSpeed", String.format("%.1f", data.getUasHSpeed()));

            // Accuracy data
            if (data.getUasHorizontalError() > 0) {
                remoteIdDetail.setAttribute("hAccuracy", String.format("%.1f", data.getUasHorizontalError()));
            }
            if (data.getUasVerticalError() > 0) {
                remoteIdDetail.setAttribute("vAccuracy", String.format("%.1f", data.getUasVerticalError()));
            }

            // Operator location (if available)
            if (data.getOpLat() != 0.0 && data.getOpLon() != 0.0) {
                remoteIdDetail.setAttribute("opLat", String.valueOf(data.getOpLat()));
                remoteIdDetail.setAttribute("opLon", String.valueOf(data.getOpLon()));
                remoteIdDetail.setAttribute("opAlt", String.format("%.1f", data.getOpHae()));
                remoteIdDetail.setAttribute("opLocType", getOperatorLocationTypeString(data.getOpLocationType()));
            }

            // Session info
            if (data.getSessionId() > 0) {
                remoteIdDetail.setAttribute("sessionId", String.valueOf(data.getSessionId()));
            }
            if (data.getUtmId() != null && !data.getUtmId().isEmpty()) {
                remoteIdDetail.setAttribute("utmId", data.getUtmId());
            }

            remoteIdDetail.setAttribute("detectedBy", "gyb_detect");
            remoteIdDetail.setAttribute("timestamp", String.valueOf(data.getTimestamp()));

            detail.addChild(remoteIdDetail);

            // Add precision location detail for ATAK
            CotDetail precisionLocation = new CotDetail("precisionlocation");
            precisionLocation.setAttribute("altsrc", "GPS");
            precisionLocation.setAttribute("geopointsrc", "GPS");
            detail.addChild(precisionLocation);

            // Add remarks with summary
            CotDetail remarks = new CotDetail("remarks");
            remarks.setInnerText(generateRemarks(data));
            detail.addChild(remarks);

            cotEvent.setDetail(detail);

            return cotEvent;

        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to convert RemoteIdData to CotEvent", e);
            return null;
        }
    }

    /**
     * Generate a callsign for the drone
     */
    private static String generateCallsign(RemoteIdData data) {
        // Try to use serial number first
        if (data.getSerialNumber() != null && !data.getSerialNumber().isEmpty()) {
            return "DRONE-" + data.getSerialNumber().substring(0, Math.min(8, data.getSerialNumber().length()));
        }

        // Try MAC address
        if (data.getUasId() != null && !data.getUasId().isEmpty()) {
            // Extract last 4 chars of MAC
            String mac = data.getUasId().replace(":", "");
            if (mac.length() >= 4) {
                return "DRONE-" + mac.substring(mac.length() - 4);
            }
        }

        // Fallback to timestamp
        return "DRONE-" + System.currentTimeMillis() % 10000;
    }

    /**
     * Generate human-readable remarks for the drone detection
     */
    private static String generateRemarks(RemoteIdData data) {
        StringBuilder remarks = new StringBuilder();
        remarks.append("Remote ID Drone Detection\n");

        if (data.getDescription() != null && !data.getDescription().isEmpty()) {
            remarks.append("Desc: ").append(data.getDescription()).append("\n");
        }

        if (data.getSerialNumber() != null && !data.getSerialNumber().isEmpty()) {
            remarks.append("S/N: ").append(data.getSerialNumber()).append("\n");
        }

        remarks.append("Alt: ").append(String.format("%.0f", data.getUasHae())).append("m MSL, ");
        remarks.append(String.format("%.0f", data.getUasHag())).append("m AGL\n");

        remarks.append("Speed: ").append(String.format("%.1f", data.getUasHSpeed())).append("m/s");
        if (data.getUasVSpeed() != 0) {
            remarks.append(" (V: ").append(String.format("%.1f", data.getUasVSpeed())).append("m/s)");
        }
        remarks.append("\n");

        remarks.append("Heading: ").append(String.format("%.0f", data.getUasHeading())).append("°\n");

        remarks.append("Detection: ").append(data.getRecvMethodString());
        remarks.append(" (RSSI: ").append(data.getRssi()).append("dBm)\n");

        if (data.getOpLat() != 0.0 && data.getOpLon() != 0.0) {
            remarks.append("Operator: ").append(String.format("%.6f", data.getOpLat()))
                   .append(", ").append(String.format("%.6f", data.getOpLon())).append("\n");
        }

        return remarks.toString();
    }

    /**
     * Get operator location type as string
     */
    private static String getOperatorLocationTypeString(int type) {
        switch (type) {
            case 0: return "takeoff";
            case 1: return "live";
            case 2: return "fixed";
            default: return "unknown";
        }
    }

    /**
     * Get UAV type as human-readable string based on Remote ID spec
     */
    public static String getUavTypeString(int uasType) {
        switch (uasType) {
            case 0: return "None/Undeclared";
            case 1: return "Aeroplane";
            case 2: return "Helicopter/Multirotor";
            case 3: return "Gyroplane";
            case 4: return "Hybrid Lift";
            case 5: return "Ornithopter";
            case 6: return "Glider";
            case 7: return "Kite";
            case 8: return "Free Balloon";
            case 9: return "Captive Balloon";
            case 10: return "Airship";
            case 11: return "Free Fall/Parachute";
            case 12: return "Rocket";
            case 13: return "Tethered Powered";
            case 14: return "Ground Obstacle";
            case 15: return "Other";
            default: return "Unknown (" + uasType + ")";
        }
    }

    /**
     * Get operational status as string
     */
    public static String getOperationalStatusString(int status) {
        switch (status) {
            case 0: return "Undeclared";
            case 1: return "Ground";
            case 2: return "Airborne";
            case 3: return "Emergency";
            case 4: return "Remote ID Failure";
            default: return "Unknown (" + status + ")";
        }
    }
}
