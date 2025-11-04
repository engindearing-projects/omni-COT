
package com.engindearing.omnicot;

import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.PluginContextProvider;
import gov.tak.api.plugin.IServiceController;

public class OmniCOTPlugin extends AbstractPlugin {

    public OmniCOTPlugin(IServiceController serviceController) {
        super(serviceController,
              new OmniCOTTool(serviceController.getService(PluginContextProvider.class).getPluginContext()),
              new OmniCOTMapComponent());
    }
}
