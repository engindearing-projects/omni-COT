package com.engindearing.omnicot;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manager for storing and retrieving CoT affiliation data
 * Uses SharedPreferences for persistent storage
 */
public class AffiliationManager {
    private static final String TAG = "AffiliationManager";
    private static final String PREFS_NAME = "omnicot_affiliations";
    private static final String KEY_PREFIX = "affiliation_";

    private static AffiliationManager instance;
    private final SharedPreferences prefs;
    private final Context context;

    private AffiliationManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.context = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AffiliationManager getInstance(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot create AffiliationManager with null context");
            return null;
        }
        if (instance == null) {
            instance = new AffiliationManager(context);
        }
        return instance;
    }

    /**
     * Store affiliation data for a CoT UID
     */
    public void setAffiliation(AffiliationData data) {
        try {
            String key = KEY_PREFIX + data.getUid();
            String jsonString = data.toJson().toString();
            prefs.edit().putString(key, jsonString).apply();
            Log.d(TAG, "Stored affiliation for UID: " + data.getUid() + " -> " + data.getAffiliation());
        } catch (JSONException e) {
            Log.e(TAG, "Error storing affiliation data", e);
        }
    }

    /**
     * Retrieve affiliation data for a CoT UID
     * @return AffiliationData or null if not found
     */
    public AffiliationData getAffiliation(String uid) {
        String key = KEY_PREFIX + uid;
        String jsonString = prefs.getString(key, null);

        if (jsonString != null) {
            try {
                JSONObject json = new JSONObject(jsonString);
                return AffiliationData.fromJson(json);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing affiliation data for UID: " + uid, e);
            }
        }

        return null;
    }

    /**
     * Check if affiliation exists for a UID
     */
    public boolean hasAffiliation(String uid) {
        String key = KEY_PREFIX + uid;
        return prefs.contains(key);
    }

    /**
     * Remove affiliation data for a CoT UID
     */
    public void removeAffiliation(String uid) {
        String key = KEY_PREFIX + uid;
        prefs.edit().remove(key).apply();
        Log.d(TAG, "Removed affiliation for UID: " + uid);
    }

    /**
     * Get all stored affiliations
     */
    public List<AffiliationData> getAllAffiliations() {
        List<AffiliationData> affiliations = new ArrayList<>();
        Map<String, ?> allPrefs = prefs.getAll();

        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            if (entry.getKey().startsWith(KEY_PREFIX)) {
                try {
                    String jsonString = (String) entry.getValue();
                    if (jsonString != null) {
                        JSONObject json = new JSONObject(jsonString);
                        affiliations.add(AffiliationData.fromJson(json));
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing affiliation data", e);
                }
            }
        }

        return affiliations;
    }

    /**
     * Clear all affiliation data
     */
    public void clearAll() {
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> allPrefs = prefs.getAll();

        for (String key : allPrefs.keySet()) {
            if (key.startsWith(KEY_PREFIX)) {
                editor.remove(key);
            }
        }

        editor.apply();
        Log.d(TAG, "Cleared all affiliation data");
    }

    /**
     * Get count of stored affiliations
     */
    public int getAffiliationCount() {
        int count = 0;
        Map<String, ?> allPrefs = prefs.getAll();

        for (String key : allPrefs.keySet()) {
            if (key.startsWith(KEY_PREFIX)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Update affiliation for existing data
     */
    public void updateAffiliation(String uid, AffiliationData.Affiliation newAffiliation, String markedBy) {
        AffiliationData existingData = getAffiliation(uid);

        if (existingData != null) {
            existingData.setAffiliation(newAffiliation);
            existingData.setMarkedBy(markedBy);
            setAffiliation(existingData);
        } else {
            // Create new affiliation data
            AffiliationData newData = new AffiliationData(uid, newAffiliation, markedBy, "");
            setAffiliation(newData);
        }
    }
}
