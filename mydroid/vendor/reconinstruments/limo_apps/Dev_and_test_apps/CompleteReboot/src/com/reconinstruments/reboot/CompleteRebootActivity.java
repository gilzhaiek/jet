package com.reconinstruments.reboot;

import android.app.Activity;
import android.os.Bundle;
import android.os.PowerManager;
import android.platform.UBootEnvNative;
import android.util.Log;
import android.widget.Button;
import android.view.View;
import android.content.Context;
public class CompleteRebootActivity extends Activity
{
    public final static String TAG = "CompleteRebootActivity";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	final Button regularBootBtn = (Button) findViewById(R.id.regular_reboot_btn);
        regularBootBtn.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    regularReboot();
		}
	    });

	final Button completeBootBtn = (Button) findViewById(R.id.complete_reboot_btn);
        completeBootBtn.setOnClickListener(new View.OnClickListener()  {
		public void onClick(View v) {
		    completeReboot();
		}
	    });

    }
    public void regularReboot() {
	Log.v(TAG,"regularReboot");
	PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
	pm.reboot(null);
    }
    public void completeReboot() {
	Log.v(TAG,"completeReboot");
	UBootEnvNative.Set_UBootVar("boot_to_android","1");
	regularReboot();
    }

}
