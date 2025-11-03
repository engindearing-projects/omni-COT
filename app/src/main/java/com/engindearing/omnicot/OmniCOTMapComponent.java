package com.engindearing.omnicot;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.log.Log;

public class OmniCOTMapComponent extends DropDownMapComponent {

    private static final String TAG = OmniCOTMapComponent.class.getSimpleName();

    private Context pluginContext;
    private OmniCOTDropDownReceiver dropDownReceiver;
    private CotAffiliationListener affiliationListener;

    public OmniCOTMapComponent() {
        Log.d(TAG, "OmniCOTMapComponent constructor called");
    }

    public void onCreate(final Context context, Intent intent, final MapView view) {
        Log.d(TAG, "onCreate() called - START");
        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;

        Log.d(TAG, "OmniCOT MapComponent created");

        // Inflate the dashboard layout
        View dashboardView = PluginLayoutInflater.inflate(pluginContext, R.layout.omnicot_dashboard, null);

        // Create and register the drop-down receiver
        dropDownReceiver = new OmniCOTDropDownReceiver(view, pluginContext, dashboardView);

        Log.d(TAG, "Registering OmniCOT DropDownReceiver: " + OmniCOTDropDownReceiver.SHOW_PLUGIN);
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(OmniCOTDropDownReceiver.SHOW_PLUGIN, "Show the OmniCOT Dashboard");
        registerDropDownReceiver(dropDownReceiver, ddFilter);
        Log.d(TAG, "Registered OmniCOT DropDownReceiver successfully");

        // Register CoT affiliation listener
        affiliationListener = new CotAffiliationListener(pluginContext);
        CommsMapComponent.getInstance().registerCommsLogger(affiliationListener);
        Log.d(TAG, "Registered CotAffiliationListener for monitoring CoT messages");
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);

        if (dropDownReceiver != null) {
            dropDownReceiver.dispose();
        }

        // Unregister CoT affiliation listener
        if (affiliationListener != null) {
            CommsMapComponent.getInstance().unregisterCommsLogger(affiliationListener);
            affiliationListener.dispose();
            Log.d(TAG, "Unregistered CotAffiliationListener");
        }

        Log.d(TAG, "OmniCOT MapComponent destroyed");
    }
}
