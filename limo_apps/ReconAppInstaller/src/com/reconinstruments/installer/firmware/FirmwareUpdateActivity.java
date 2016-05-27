package com.reconinstruments.installer.firmware;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.installer.R;
import com.reconinstruments.utils.BatteryUtil;
import com.reconinstruments.utils.FileUtils;
import com.reconinstruments.utils.installer.FirmwareUtils;

public class FirmwareUpdateActivity extends Activity {
	public static final String TAG = "FirmwareUpdateActivity";
	
	private AlertDialog discardDlg = null;
	private AlertDialog newUpdateDlg = null;
	private AlertDialog lowBatteryDlg = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/*
		 * At This stage, we assume that there is only
		 * /cache/update.bin file which is copied from 
		 * /sdcard/ReconApps/cache/update.bin which is deleted after copied correctly.
		 * 
		 *  All the checking has already been done in previous stages and we are unable to check the file integrity
		 *  since /sdcard/ReconApps/cache/update.bin file is deleted.
		 *  If not deleted, it is most likely for /cache/update.bin to be corrupted. 
		 */
		
		LayoutInflater factory = LayoutInflater.from(this);
		final View updateView = factory.inflate(R.layout.firmware_dlg_main, null);
		final View nextTimeView = factory.inflate(R.layout.firmware_dlg_main, null);
		
		((TextView)updateView.findViewById(R.id.msg_view)).setText("A new system update has been found. Would you like to start updating your system?");
		((TextView)nextTimeView.findViewById(R.id.msg_view)).setText("Discard the update file or try it for in the next reboot?");

		/*
		 * Low Battery Alert Dialog Initialisation  
		 */
		lowBatteryDlg = new AlertDialog.Builder(this)
		    .setTitle("WARNING")
		    .setMessage("Battery Too Low For Update")
		    .setCancelable(false)
		    .setPositiveButton("OK",new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog,int id) {
		            finish();
		        }
		    }).create();
		
		discardDlg = new AlertDialog.Builder( new ContextThemeWrapper( this, android.R.style.Theme_Translucent_NoTitleBar ) )
    		.setView(nextTimeView)
    		.setPositiveButton("Discard", new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int whichButton) 
    			{
    			    FirmwareUtils.discardUpdateFiles();
    				finish();
    			}
    		})
    		.setNegativeButton("Next Time", new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int whichButton) 
    			{
    				System.setProperty(FirmwareUtils.SYSTEM_PROP_UPDATE_SHOWN, "1");
    				finish();
    			}
    		})
    		.create();
		
		newUpdateDlg = new AlertDialog.Builder( new ContextThemeWrapper( this, android.R.style.Theme_Translucent_NoTitleBar ) )
    		.setView(updateView)
    		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int whichButton) 
    			{
    				dialog.dismiss();
    				doUpdate();
    			}
    		})
    		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int whichButton) 
    			{
    				dialog.dismiss();
    				discardDlg.show();
    			}
    		})
    		.create();
		
		newUpdateDlg.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				Log.d(TAG, "onKeyUp : " + keyCode );
				if (keyCode == KeyEvent.KEYCODE_POWER) {
					//dlg.getButton(DialogInterface.BUTTON_POSITIVE).requestFocus();
					doUpdate();
					return true;
				}
				return false;
			}
		});
		newUpdateDlg.show();
	}
	
	private void doUpdate() {
		
		if (FileUtils.isFileAndExists(FirmwareUtils.JET_UPDATE_BIN_CACHE) &&
		        FileUtils.isFileAndExists(FirmwareUtils.JET_COMMAND_RECOVERY_BAK)) {
			if (BatteryUtil.getBatteryLevel(this) < 0.25) {
			    lowBatteryDlg.show();
				return;
			}
			FirmwareUtils.doJetFirmwareUpgrade(FirmwareUpdateActivity.this);
		}
	}
}
