package com.engindearing.omnicot;

import android.content.Context;
import android.util.Log;

import com.atakmap.android.maps.MapView;
import com.atakmap.comms.CommsLogger;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

/**
 * CoT listener that monitors incoming CoT messages for affiliation information
 * Implements CommsLogger to hook into ATAK's CoT processing pipeline
 */
public class CotAffiliationListener implements CommsLogger {
    private static final String TAG = "CotAffiliationListener";
    private static final String AFFILIATION_DETAIL_TAG = "__omnicot_affiliation";

    private final Context context;
    private final AffiliationManager affiliationManager;
    private final String localCallsign;

    public CotAffiliationListener(Context context) {
        this.context = context;
        this.affiliationManager = AffiliationManager.getInstance(context);

        // Get local callsign from ATAK
        MapView mapView = MapView.getMapView();
        if (mapView != null) {
            this.localCallsign = mapView.getDeviceCallsign();
        } else {
            this.localCallsign = "Unknown";
        }

        Log.d(TAG, "CotAffiliationListener initialized with callsign: " + localCallsign);
    }

    @Override
    public void logReceive(CotEvent event, String rxid, String server) {
        if (event == null) {
            return;
        }

        try {
            String uid = event.getUID();
            if (uid == null || uid.isEmpty()) {
                return;
            }

            // Check if the CoT event contains affiliation detail
            CotDetail detail = event.getDetail();
            if (detail != null) {
                CotDetail affiliationDetail = detail.getFirstChildByName(0, AFFILIATION_DETAIL_TAG);

                if (affiliationDetail != null) {
                    // Extract affiliation information from the detail
                    String affiliationValue = affiliationDetail.getAttribute("affiliation");
                    String markedBy = affiliationDetail.getAttribute("markedBy");
                    String notes = affiliationDetail.getAttribute("notes");

                    if (affiliationValue != null) {
                        AffiliationData.Affiliation affiliation =
                            AffiliationData.Affiliation.fromString(affiliationValue);

                        // Check if we already have this affiliation stored
                        AffiliationData existingData = affiliationManager.getAffiliation(uid);

                        if (existingData != null) {
                            // Update existing affiliation if it's different
                            boolean affiliationChanged = existingData.getAffiliation() != affiliation;
                            boolean markerChanged = markedBy != null && !markedBy.equals(existingData.getMarkedBy());

                            if (affiliationChanged || markerChanged) {
                                existingData.setAffiliation(affiliation);
                                if (markedBy != null) {
                                    existingData.setMarkedBy(markedBy);
                                }
                                if (server != null) {
                                    existingData.setServerConnection(server);
                                }
                                if (notes != null && !notes.isEmpty()) {
                                    existingData.setNotes(notes);
                                }

                                affiliationManager.setAffiliation(existingData);

                                Log.d(TAG, "Updated affiliation for " + uid + ": " + affiliation +
                                      " (marked by " + markedBy + ")");
                            }
                        } else {
                            // Create new affiliation entry
                            AffiliationData newData = new AffiliationData(
                                uid, affiliation, markedBy, server
                            );
                            if (notes != null && !notes.isEmpty()) {
                                newData.setNotes(notes);
                            }

                            affiliationManager.setAffiliation(newData);

                            Log.d(TAG, "New affiliation received for " + uid + ": " + affiliation +
                                  " (marked by " + markedBy + ")");
                        }
                    }
                }
            }

            // Also check if we have existing affiliation data for this UID
            // This helps maintain affiliation even when the CoT doesn't include the detail
            if (!affiliationManager.hasAffiliation(uid)) {
                // Create default UNKNOWN affiliation for new CoT items
                AffiliationData defaultData = new AffiliationData(
                    uid,
                    AffiliationData.Affiliation.UNKNOWN,
                    "System",
                    server
                );
                affiliationManager.setAffiliation(defaultData);

                Log.d(TAG, "Created default UNKNOWN affiliation for new CoT: " + uid);
            } else {
                // Update server connection for existing affiliation
                AffiliationData existingData = affiliationManager.getAffiliation(uid);
                if (existingData != null && server != null &&
                    !server.equals(existingData.getServerConnection())) {
                    existingData.setServerConnection(server);
                    affiliationManager.setAffiliation(existingData);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing received CoT event", e);
        }
    }

    @Override
    public void logSend(CotEvent event, String destination) {
        // We can track outgoing affiliation updates here if needed
        if (event == null) {
            return;
        }

        try {
            CotDetail detail = event.getDetail();
            if (detail != null) {
                CotDetail affiliationDetail = detail.getFirstChildByName(0, AFFILIATION_DETAIL_TAG);
                if (affiliationDetail != null) {
                    String uid = event.getUID();
                    String affiliationValue = affiliationDetail.getAttribute("affiliation");

                    Log.d(TAG, "Sending affiliation update for " + uid + ": " +
                          affiliationValue + " to " + destination);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing sent CoT event", e);
        }
    }

    @Override
    public void logSend(CotEvent event, String[] toUIDs) {
        // Track sends to multiple UIDs
        if (event == null || toUIDs == null || toUIDs.length == 0) {
            return;
        }

        try {
            CotDetail detail = event.getDetail();
            if (detail != null) {
                CotDetail affiliationDetail = detail.getFirstChildByName(0, AFFILIATION_DETAIL_TAG);
                if (affiliationDetail != null) {
                    String uid = event.getUID();
                    String affiliationValue = affiliationDetail.getAttribute("affiliation");

                    Log.d(TAG, "Sending affiliation update for " + uid + ": " +
                          affiliationValue + " to " + toUIDs.length + " recipients");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing sent CoT event to UIDs", e);
        }
    }

    @Override
    public void dispose() {
        Log.d(TAG, "CotAffiliationListener disposed");
        // Cleanup if needed
    }

    /**
     * Get the detail tag name used for affiliation information
     */
    public static String getAffiliationDetailTag() {
        return AFFILIATION_DETAIL_TAG;
    }
}
