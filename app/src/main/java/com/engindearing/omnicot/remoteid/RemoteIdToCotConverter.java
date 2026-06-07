package com.engindearing.omnicot.remoteid;

import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.ArrayList;
import java.util.List;

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
    // Operator/pilot marker: unknown ground. ATAK maps a-u-G to a default ground icon.
    private static final String COT_TYPE_OPERATOR_UNKNOWN = "a-u-G";

    /**
     * Convert a Remote ID detection to all relevant CoT events.
     *
     * <p>A single RID broadcast can yield up to two markers:
     * <ul>
     *   <li>a DRONE marker at the aircraft's lat/lon (only when the drone GPS is valid), and</li>
     *   <li>an OPERATOR/PILOT marker at the operator's lat/lon (whenever that is valid).</li>
     * </ul>
     * The drone firmware frequently omits the aircraft fix until its GPS locks, but the RID
     * System message still carries the operator location, so the pilot marker keeps a detection
     * visible in ATAK even when the drone itself has no fix yet.
     *
     * @return a (possibly empty) list of CoT events to dispatch; never {@code null}.
     */
    public static List<CotEvent> convertToCotEvents(RemoteIdData data) {
        List<CotEvent> events = new ArrayList<>(2);
        if (data == null) {
            return events;
        }

        boolean droneValid = data.isValidLocation();

        if (droneValid) {
            CotEvent drone = buildDroneCotEvent(data);
            if (drone != null) {
                events.add(drone);
            }
        }

        if (isValidOperatorLocation(data)) {
            CotEvent operator = buildOperatorCotEvent(data, droneValid);
            if (operator != null) {
                events.add(operator);
            }
        }

        return events;
    }

    /**
     * Backwards-compatible single-event entry point. Returns the drone CoT when the drone GPS
     * is valid, otherwise the operator CoT (if available), otherwise {@code null}.
     *
     * @deprecated use {@link #convertToCotEvents(RemoteIdData)} which can emit both markers.
     */
    @Deprecated
    public static CotEvent convertToCotEvent(RemoteIdData data) {
        List<CotEvent> events = convertToCotEvents(data);
        return events.isEmpty() ? null : events.get(0);
    }

    /**
     * Validate operator (pilot) location: present, not NaN, not (0,0), within bounds.
     */
    private static boolean isValidOperatorLocation(RemoteIdData data) {
        double lat = data.getOpLat();
        double lon = data.getOpLon();
        if (Double.isNaN(lat) || Double.isNaN(lon)) return false;
        if (lat == 0.0 && lon == 0.0) return false;
        if (lat < -90 || lat > 90) return false;
        if (lon < -180 || lon > 180) return false;
        return true;
    }

    /**
     * Build the DRONE CoT event (airborne UAS). Caller must ensure the drone location is valid.
     */
    private static CotEvent buildDroneCotEvent(RemoteIdData data) {
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
     * Build the OPERATOR/PILOT CoT event (unknown ground) at the operator's reported location.
     *
     * @param data        the parsed Remote ID data (operator location assumed valid by caller)
     * @param droneValid  whether the drone aircraft itself currently has a valid GPS fix; when
     *                    false the remarks note that the drone GPS has not yet been acquired.
     */
    private static CotEvent buildOperatorCotEvent(RemoteIdData data, boolean droneValid) {
        try {
            CotEvent cotEvent = new CotEvent();

            // UID derived from the drone UID with an -OP suffix so the two markers never collide
            // and remain stably keyed to the same physical detection across updates.
            String uid = "RID-OP-" + data.getUniqueId();
            cotEvent.setUID(uid);

            // Unknown ground - ATAK renders a default ground icon for a-u-G.
            cotEvent.setType(COT_TYPE_OPERATOR_UNKNOWN);

            // Sensor-detected, same as the drone marker.
            cotEvent.setHow("h-s");

            CoordinatedTime now = new CoordinatedTime();
            cotEvent.setTime(now);
            cotEvent.setStart(now);
            // Operators move less than drones; keep the marker a bit longer (2 min).
            cotEvent.setStale(new CoordinatedTime(now.getMilliseconds() + 120 * 1000));

            GeoPoint geoPoint = new GeoPoint(
                data.getOpLat(),
                data.getOpLon(),
                data.getOpHae(), // operator altitude MSL in meters
                GeoPoint.AltitudeReference.HAE
            );
            cotEvent.setPoint(new CotPoint(geoPoint));

            CotDetail detail = new CotDetail();

            // Contact / callsign
            CotDetail contact = new CotDetail("contact");
            contact.setAttribute("callsign", generateOperatorCallsign(data));
            detail.addChild(contact);

            // Remote ID specific details (mirror the drone marker so this marker is self-describing)
            CotDetail remoteIdDetail = new CotDetail("__remoteid");
            remoteIdDetail.setAttribute("markerRole", "operator");
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
            remoteIdDetail.setAttribute("rssi", String.valueOf(data.getRssi()));
            remoteIdDetail.setAttribute("recvMethod", data.getRecvMethodString());
            remoteIdDetail.setAttribute("opLat", String.valueOf(data.getOpLat()));
            remoteIdDetail.setAttribute("opLon", String.valueOf(data.getOpLon()));
            remoteIdDetail.setAttribute("opAlt", String.format("%.1f", data.getOpHae()));
            remoteIdDetail.setAttribute("opLocType", getOperatorLocationTypeString(data.getOpLocationType()));
            remoteIdDetail.setAttribute("droneGpsValid", String.valueOf(droneValid));
            remoteIdDetail.setAttribute("detectedBy", "gyb_detect");
            remoteIdDetail.setAttribute("timestamp", String.valueOf(data.getTimestamp()));
            detail.addChild(remoteIdDetail);

            // Precision location source
            CotDetail precisionLocation = new CotDetail("precisionlocation");
            precisionLocation.setAttribute("altsrc", "GPS");
            precisionLocation.setAttribute("geopointsrc", "GPS");
            detail.addChild(precisionLocation);

            // Remarks
            CotDetail remarks = new CotDetail("remarks");
            remarks.setInnerText(generateOperatorRemarks(data, droneValid));
            detail.addChild(remarks);

            cotEvent.setDetail(detail);

            return cotEvent;

        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to convert RemoteIdData to operator CotEvent", e);
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
     * Generate a callsign for the operator/pilot marker.
     * Mirrors the drone callsign id but with a PILOT- prefix.
     */
    private static String generateOperatorCallsign(RemoteIdData data) {
        if (data.getSerialNumber() != null && !data.getSerialNumber().isEmpty()) {
            return "PILOT-" + data.getSerialNumber().substring(0, Math.min(8, data.getSerialNumber().length()));
        }
        if (data.getUasId() != null && !data.getUasId().isEmpty()) {
            String mac = data.getUasId().replace(":", "");
            if (mac.length() >= 4) {
                return "PILOT-" + mac.substring(mac.length() - 4);
            }
        }
        if (data.getRemoteId() != null && !data.getRemoteId().isEmpty()) {
            return "PILOT-" + data.getRemoteId();
        }
        return "PILOT-" + System.currentTimeMillis() % 10000;
    }

    /**
     * Generate human-readable remarks for the operator/pilot marker.
     */
    private static String generateOperatorRemarks(RemoteIdData data, boolean droneValid) {
        StringBuilder remarks = new StringBuilder();
        remarks.append("Remote ID Operator/Pilot Location\n");

        if (!droneValid) {
            remarks.append("Drone GPS not yet acquired\n");
        }

        if (data.getSerialNumber() != null && !data.getSerialNumber().isEmpty()) {
            remarks.append("Drone S/N: ").append(data.getSerialNumber()).append("\n");
        }
        if (data.getDescription() != null && !data.getDescription().isEmpty()) {
            remarks.append("Desc: ").append(data.getDescription()).append("\n");
        }

        remarks.append("Operator: ").append(String.format("%.6f", data.getOpLat()))
               .append(", ").append(String.format("%.6f", data.getOpLon())).append("\n");
        remarks.append("Loc type: ").append(getOperatorLocationTypeString(data.getOpLocationType())).append("\n");

        remarks.append("Detection: ").append(data.getRecvMethodString());
        remarks.append(" (RSSI: ").append(data.getRssi()).append("dBm)\n");

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
