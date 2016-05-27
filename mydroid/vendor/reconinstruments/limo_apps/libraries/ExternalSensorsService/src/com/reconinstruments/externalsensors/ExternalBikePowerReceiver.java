package com.reconinstruments.externalsensors;
import android.content.Context;
import android.content.Intent;
public class ExternalBikePowerReceiver extends ExternalSensorReceiver {
    public ExternalBikePowerReceiver(Context c, IExternalBikePowerListener icl) {
	super(c);
	mExternalSensorListener = icl;
    }
    @Override
    public void start() {
	start("com.reconinstruments.externalsensors.bikepower");
    }
    @Override
    public void onReceive(Context c, Intent i) {
        int power = i.getIntExtra("Power", -1);
        if (power != -1) {
            ((IExternalBikePowerListener)mExternalSensorListener)
            .onPowerChanged(power);
        }
        else {
            ((IExternalBikePowerListener)mExternalSensorListener).
            onSensorDisconnected();
        }
    }
}