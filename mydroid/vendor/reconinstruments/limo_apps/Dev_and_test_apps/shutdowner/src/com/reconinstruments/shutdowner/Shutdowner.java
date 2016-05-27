package com.reconinstruments.shutdowner;

import android.app.Activity;
import android.os.Bundle;
import android.os.PowerManager;
import android.content.Context;
import android.provider.Settings;
public class Shutdowner extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	Settings.Secure.putInt(getBaseContext().getContentResolver(), Settings.Secure.ADB_ENABLED, 0);
	PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	pm.reboot("BegForShutdown"); // Magic reason that


	
    }

}
