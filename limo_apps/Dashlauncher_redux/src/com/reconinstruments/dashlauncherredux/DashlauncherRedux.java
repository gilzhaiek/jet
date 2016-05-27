package com.reconinstruments.dashlauncherredux;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.reconinstruments.profile.ProfileManager;
import com.reconinstruments.profile.ProfileParser;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DashlauncherRedux extends Activity {
    public static final String TAG = "DashlauncherRedux";
    private IColumnHandlerServiceController columnHandlerService;
    private boolean mIsBound;
    private ArrayAdapter<CharSequence> adapter; // The spinner adapter
    private Map<String, File> profileNameToFile = new HashMap<String, File>();
    private String chosenProfileName = null;
    private Spinner spinner;
    private PopulateSpinnerTask pst;
    // Service connection stuff:
    private ServiceConnection mConnection = new ServiceConnection() {
	    final public void onServiceConnected(ComponentName className,
						 IBinder boundService) {
		Log.d(TAG, "onServiceConnected");
		columnHandlerService = IColumnHandlerServiceController.Stub
		    .asInterface((IBinder) boundService);
	    }

	    final public void onServiceDisconnected(ComponentName className) {
		columnHandlerService = null;
		Log.d(TAG, "onServiceDisconnected");
	    }
	};

    private void initService() {
	bindService(new Intent(this, ColumnHandlerService.class), mConnection,
		    Context.BIND_AUTO_CREATE);
    }

    private void releaseService() {
	unbindService(mConnection);
	columnHandlerService = null;
    }

    // End of service connection

    /** called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
	Log.d(TAG, "onCreate");
	super.onCreate(savedInstanceState);
	setContentView(R.layout.main);
	// Connect to the service
	initService();
    }

    @Override
    protected void onDestroy() {
	releaseService();
	super.onDestroy();
    }

    @Override
    public void onResume() {
	super.onResume();
	configureSpinner();
    }

    @Override
    public void onPause() {
	updateTheChosenProfile();
	try {
	    if (columnHandlerService != null) {
		columnHandlerService.updateProfileInfo();
		columnHandlerService.launchHomeElement();
	    }
	} catch (RemoteException e) {
	    e.printStackTrace();
	}
	super.onPause();
    }

    private void configureSpinner() {
	spinner = (Spinner) findViewById(R.id.spinner);
	// Create an ArrayAdapter using the string array and a default spinner
	// layout
	adapter = new ArrayAdapter<CharSequence>(this,
						 android.R.layout.simple_spinner_item);
	// Specify the layout to use when the list of choices appears
	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	pst = new PopulateSpinnerTask();
	pst.execute("test");
    }

    private class PopulateSpinnerTask extends AsyncTask<String, Void, Boolean> {
	// private ProgressDialog dialog = new
	// ProgressDialog(DashlauncherRedux.this);
	/** progress dialog to show user that the backup is processing. */
	/** application context. */
	protected void onPreExecute() {
	    // this.dialog.setMessage("Fetching Profiles");
	    // this.dialog.show();
	}

	protected Boolean doInBackground(final String... args) {
	    try {
		File fileList = new File(
					 Environment.getExternalStorageDirectory(),
					 ProfileManager.PROFILE_FOLDER);
		Log.v(TAG, fileList.getAbsolutePath());
		if (fileList != null) {
		    Log.v(TAG, "folder exists");
		    // First discover all the profiles:
		    File[] filenames = fileList.listFiles();
		    for (File tmpf : filenames) {
			Log.v(TAG, tmpf.getAbsolutePath());
			if (tmpf.getName().endsWith(".xml")
			    || tmpf.getName().endsWith(".XML")) {
			    // 1) Parse the profile and extract the name:
			    String tmpString = ProfileManager
				.readFileAsString(tmpf);
			    Log.v(TAG, "tmpString = " + tmpString);
			    ProfileParser tmpPP = new ProfileParser(tmpString);
			    String tmpName = tmpPP.getProfileName();
			    // 2) Add the name to the adapter if good.
			    if (tmpName != null) {
				Log.v(TAG, "adding to the adapter");
				adapter.add(tmpName);
				profileNameToFile.put(tmpName, tmpf);
			    }
			}
		    }
		    // Now set the chosen profile name:
		    String chosenProfile = ProfileManager
			.fetchTheChosenProfile();
		    ProfileParser tmpPP = new ProfileParser(chosenProfile);
		    chosenProfileName = tmpPP.getProfileName();
		}
		return true;
	    } catch (Exception e) {
		Log.e(TAG, "no files found", e);
		return false;
	    }
	}

	@Override
	protected void onPostExecute(final Boolean success) {
	    // Apply the adapter to the spinner
	    Log.v(TAG, "setting the spinner adapter");
	    spinner.setAdapter(adapter);
	    spinner.setSelection(adapter.getPosition(chosenProfileName));
	    // dismiss the dialog
	    // if (dialog.isShowing()) {
	    // dialog.dismiss();
	    // }
	}
    }

    private void updateTheChosenProfile() {
	Object obj = spinner.getSelectedItem();
	if (obj != null) {
	    String pname = obj.toString();
	    chosenProfileName = pname;
	    File chosenProfileFile = profileNameToFile.get(pname);
	    String chosenProfileFileName = chosenProfileFile.getName();
	    ProfileManager.updateChosenProfile(chosenProfileFileName);
	}
    }
}
