package com.cyanogenmod.settings.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Startup extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent bootintent) {
        Sweep2WakeSwitch.restore(context);
        Sweep2WakeStroke.restore(context);
        Sweep2WakeMinLength.restore(context);
        BacklightDisable.restore(context);
        BacklightNotificationSwitch.restore(context);
        SmartDimmerSwitch.restore(context);
        BacklightBrightness.restore(context);
        SlowBlinkBrightnessLimit.restore(context);
        DoubleTap2WakeSwitch.restore(context);
        DoubleTap2WakeDuration.restore(context);
        CalibrationControl.restore(context);
    }
}
