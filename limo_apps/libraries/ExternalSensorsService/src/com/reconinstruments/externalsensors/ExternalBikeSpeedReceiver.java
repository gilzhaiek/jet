package com.reconinstruments.externalsensors;
import android.content.Context;
import android.content.Intent;
public class ExternalBikeSpeedReceiver extends ExternalSensorReceiver {
    public ExternalBikeSpeedReceiver(Context c, IExternalBikeSpeedListener icl) {
	super(c);
	mExternalSensorListener = icl;
    }
    @Override
    public void start() {
	start("com.reconinstruments.externalsensors.bikespeed");
    }
    @Override
    public void onReceive(Context c, Intent i) {
        int speed = i.getIntExtra("computedBikeSpeed", -1);
        if (speed != -1) {
            ((IExternalBikeSpeedListener)mExternalSensorListener)
            .onBikeSpeedChanged(speed);
        }
        else {
            ((IExternalBikeSpeedListener)mExternalSensorListener).
            onSensorDisconnected();
        }
    }
}