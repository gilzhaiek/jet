package com.reconinstruments.externalsensors;
import android.content.Context;
import android.content.Intent;
public class ExternalHeartrateReceiver extends ExternalSensorReceiver {
    public ExternalHeartrateReceiver(Context c, IExternalHeartrateListener ihrl) {
	super(c);
	mExternalSensorListener = ihrl;
    }
    @Override
    public void start() {
	start("com.reconinstruments.externalsensors.heartrate");
    }
    @Override
    public void onReceive(Context c, Intent i) {
	int heartrate = i.getIntExtra("computedHeartRate",255);
	if (heartrate != 255) {
	    ((IExternalHeartrateListener)mExternalSensorListener)
	    .onHeartrateChanged(heartrate);
	}
	else {
	    ((IExternalHeartrateListener)mExternalSensorListener).
		onSensorDisconnected();
	}
    }
}