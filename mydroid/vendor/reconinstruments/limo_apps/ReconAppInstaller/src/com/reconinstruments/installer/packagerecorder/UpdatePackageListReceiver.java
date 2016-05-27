package com.reconinstruments.installer.packagerecorder;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.reconinstruments.utils.installer.ReconPackageRecorder;

public class UpdatePackageListReceiver extends BroadcastReceiver {
	static final String TAG = "UpdatePackageListReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		ReconPackageRecorder.writePackageData(context, new File(Environment.getExternalStorageDirectory(),ReconPackageRecorder.PACKAGES_XML_PATH));
		Log.d(TAG, "Package is updated");
	}
}
