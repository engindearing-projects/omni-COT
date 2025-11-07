package com.engindearing.omnicot;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.menu.MapMenuReceiver;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.log.Log;

public class OmniCOTMapComponent extends DropDownMapComponent {

    private static final String TAG = OmniCOTMapComponent.class.getSimpleName();

    private Context pluginContext;
    private OmniCOTDropDownReceiver dropDownReceiver;
    private CotAffiliationListener affiliationListener;

    // Radial menu components
    private CotMenuFactory cotMenuFactory;
    private CotMenuEventListener cotMenuEventListener;
    private CotAffiliationMenuReceiver cotMenuReceiver;

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

        // Register radial menu components for COT affiliation updates
        registerRadialMenuComponents(view);
    }

    /**
     * Registers the radial menu factory and listeners for COT affiliation updates.
     * This enables users to tap on a COT item and see a radial menu with affiliation options.
     */
    private void registerRadialMenuComponents(MapView mapView) {
        Log.d(TAG, "Registering radial menu components");

        // Create and register the COT menu factory
        // Factories are visited in reverse order, so this will be consulted before default factory
        cotMenuFactory = new CotMenuFactory(pluginContext, mapView);
        MapMenuReceiver.getInstance().registerMapMenuFactory(cotMenuFactory);
        Log.d(TAG, "Registered CotMenuFactory with MapMenuReceiver");

        // Create and register the menu event listener for lifecycle tracking
        cotMenuEventListener = new CotMenuEventListener();
        MapMenuReceiver.getInstance().addEventListener(cotMenuEventListener);
        Log.d(TAG, "Registered CotMenuEventListener for menu lifecycle events");

        // Create and register the broadcast receiver for menu button clicks
        cotMenuReceiver = new CotAffiliationMenuReceiver(pluginContext, mapView);
        DocumentedIntentFilter menuFilter = new DocumentedIntentFilter();
        menuFilter.addAction(CotAffiliationMenuReceiver.UPDATE_AFFILIATION,
                           "Update COT affiliation from radial menu");
        AtakBroadcast.getInstance().registerReceiver(cotMenuReceiver, menuFilter);
        Log.d(TAG, "Registered CotAffiliationMenuReceiver for menu actions");
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

        // Unregister radial menu components
        unregisterRadialMenuComponents();

        Log.d(TAG, "OmniCOT MapComponent destroyed");
    }

    /**
     * Unregisters the radial menu factory and listeners.
     * Called during component cleanup.
     */
    private void unregisterRadialMenuComponents() {
        Log.d(TAG, "Unregistering radial menu components");

        // Unregister menu factory
        if (cotMenuFactory != null) {
            MapMenuReceiver.getInstance().unregisterMapMenuFactory(cotMenuFactory);
            cotMenuFactory = null;
            Log.d(TAG, "Unregistered CotMenuFactory");
        }

        // Unregister menu event listener
        if (cotMenuEventListener != null) {
            MapMenuReceiver.getInstance().removeEventListener(cotMenuEventListener);
            cotMenuEventListener = null;
            Log.d(TAG, "Unregistered CotMenuEventListener");
        }

        // Unregister broadcast receiver
        if (cotMenuReceiver != null) {
            AtakBroadcast.getInstance().unregisterReceiver(cotMenuReceiver);
            cotMenuReceiver = null;
            Log.d(TAG, "Unregistered CotAffiliationMenuReceiver");
        }
    }
}
