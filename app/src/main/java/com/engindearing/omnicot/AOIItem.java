package com.engindearing.omnicot;

import com.atakmap.android.maps.Shape;

public class AOIItem {
    private final Shape shape;
    private boolean alertEnabled;
    private String triggerType;
    private String monitoredType;
    private int durationHours;

    public AOIItem(Shape shape) {
        this.shape = shape;
        this.alertEnabled = false;
        this.triggerType = "Both";
        this.monitoredType = "All";
        this.durationHours = 24;
    }

    public Shape getShape() {
        return shape;
    }

    public String getName() {
        return shape.getTitle() != null ? shape.getTitle() : "Unnamed AOI";
    }

    public String getType() {
        return shape.getClass().getSimpleName().replace("Drawing", "");
    }

    public String getUID() {
        return shape.getUID();
    }

    public boolean isAlertEnabled() {
        return alertEnabled;
    }

    public void setAlertEnabled(boolean alertEnabled) {
        this.alertEnabled = alertEnabled;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getMonitoredType() {
        return monitoredType;
    }

    public void setMonitoredType(String monitoredType) {
        this.monitoredType = monitoredType;
    }

    public int getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(int durationHours) {
        this.durationHours = durationHours;
    }

    public String getAlertStatus() {
        if (alertEnabled) {
            return "Alert: " + triggerType + " (" + monitoredType + ")";
        } else {
            return "No alerts configured";
        }
    }
}
