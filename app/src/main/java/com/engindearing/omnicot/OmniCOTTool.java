package com.engindearing.omnicot;

import android.content.Context;

import com.atak.plugins.impl.AbstractPluginTool;
import com.atakmap.coremap.log.Log;
import gov.tak.api.util.Disposable;

public class OmniCOTTool extends AbstractPluginTool implements Disposable {

    private static final String TAG = OmniCOTTool.class.getSimpleName();

    public OmniCOTTool(final Context context) {
        super(context,
                context.getString(R.string.app_name),
                context.getString(R.string.app_name),
                context.getResources().getDrawable(R.drawable.ic_launcher),
                OmniCOTDropDownReceiver.SHOW_PLUGIN);

        Log.d(TAG, "OmniCOTTool initialized");
    }

    @Override
    public void dispose() {
        // Cleanup if needed
    }
}
