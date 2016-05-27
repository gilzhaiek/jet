package com.reconinstruments.externalsensors;
import android.content.Context;
import android.content.Intent;
public class ExternalCadenceReceiver extends ExternalSensorReceiver {
    public ExternalCadenceReceiver(Context c, IExternalCadenceListener icl) {
	super(c);
	mExternalSensorListener = icl;
    }
    @Override
    public void start() {
	start("com.reconinstruments.externalsensors.cadence");
    }
    @Override
    public void onReceive(Context c, Intent i) {
        int cadence = i.getIntExtra("computedBikeCadence",255);
        if (cadence != 255) {
            ((IExternalCadenceListener)mExternalSensorListener)
            .onCadenceChanged(cadence);
        }
        else {
            ((IExternalCadenceListener)mExternalSensorListener).
            onSensorDisconnected();
        }
    }
}