package com.engindearing.omnicot;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Data model for storing CoT affiliation information
 */
public class AffiliationData {

    public enum Affiliation {
        UNKNOWN("unknown"),
        ASSUMED_FRIENDLY("assumedFriendly"),
        ASSUMED_HOSTILE("assumedHostile"),
        PENDING("pending");

        private final String value;

        Affiliation(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Affiliation fromString(String value) {
            for (Affiliation affiliation : values()) {
                if (affiliation.value.equalsIgnoreCase(value)) {
                    return affiliation;
                }
            }
            return UNKNOWN;
        }
    }

    private String uid;
    private Affiliation affiliation;
    private String markedBy;
    private long timestamp;
    private String serverConnection;
    private String notes;

    public AffiliationData(String uid, Affiliation affiliation, String markedBy, String serverConnection) {
        this.uid = uid;
        this.affiliation = affiliation;
        this.markedBy = markedBy;
        this.timestamp = System.currentTimeMillis();
        this.serverConnection = serverConnection;
        this.notes = "";
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Affiliation getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(Affiliation affiliation) {
        this.affiliation = affiliation;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMarkedBy() {
        return markedBy;
    }

    public void setMarkedBy(String markedBy) {
        this.markedBy = markedBy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getServerConnection() {
        return serverConnection;
    }

    public void setServerConnection(String serverConnection) {
        this.serverConnection = serverConnection;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // JSON Serialization
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uid", uid);
        json.put("affiliation", affiliation.getValue());
        json.put("markedBy", markedBy);
        json.put("timestamp", timestamp);
        json.put("serverConnection", serverConnection);
        json.put("notes", notes);
        return json;
    }

    public static AffiliationData fromJson(JSONObject json) throws JSONException {
        String uid = json.getString("uid");
        Affiliation affiliation = Affiliation.fromString(json.getString("affiliation"));
        String markedBy = json.optString("markedBy", "");
        String serverConnection = json.optString("serverConnection", "");

        AffiliationData data = new AffiliationData(uid, affiliation, markedBy, serverConnection);
        data.timestamp = json.optLong("timestamp", System.currentTimeMillis());
        data.notes = json.optString("notes", "");

        return data;
    }

    @Override
    public String toString() {
        return "AffiliationData{" +
                "uid='" + uid + '\'' +
                ", affiliation=" + affiliation +
                ", markedBy='" + markedBy + '\'' +
                ", timestamp=" + timestamp +
                ", serverConnection='" + serverConnection + '\'' +
                '}';
    }
}
