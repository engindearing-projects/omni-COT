package com.engindearing.omnicot;

import android.view.HapticFeedbackConstants;
import android.view.View;

public class HapticFeedbackHelper {

    public static void performLightClick(View view) {
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    public static void performMediumClick(View view) {
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    public static void performHeavyClick(View view) {
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
    }
}
