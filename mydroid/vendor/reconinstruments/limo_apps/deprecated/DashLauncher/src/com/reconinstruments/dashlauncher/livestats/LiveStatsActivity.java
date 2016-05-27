package com.reconinstruments.dashlauncher.livestats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.reconinstruments.dashlauncher.BoardActivity;
import com.reconinstruments.dashlauncher.BreadcrumbView;
import com.reconinstruments.dashlauncher.DashLauncherApp;
import com.reconinstruments.dashlauncher.HUDConnectivityMessage;
import com.reconinstruments.dashlauncher.HUDServiceHelper;
import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.notifications.NotificationsService;
import com.reconinstruments.dashlauncher.settings.service.SettingService;

public class LiveStatsActivity extends BoardActivity {

	public static final String TAG = "ReconLiveStatsActivity";

	private SharedPreferences mPrefs;
	private ArrayList<Fragment> dashFragments;

	private int currentPosition = 0;
	//private int prevPosition = -1;
	
	// Breadcrumb toast and view
	private Toast breadcrumbToast;
	private BreadcrumbView mBreadcrumbView;
	private FrameLayout fragmentFrameLayout;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.v(TAG, "onCreate()");
		this.setContentView(R.layout.livestats_main_layout);

		dashFragments = new ArrayList<Fragment>(5);
		dashFragments.add(FactoryDashFragment.newInstance(FactoryDashFragment.PRECONFIG_STYLE_1));
		dashFragments.add(FactoryDashFragment.newInstance(FactoryDashFragment.PRECONFIG_STYLE_2));
		dashFragments.add(FactoryDashFragment.newInstance(FactoryDashFragment.PRECONFIG_STYLE_3));

		ArrayList<String> customLayouts = getCustomLayouts();
		
		if (customLayouts != null) {
			for (String layout : customLayouts) {
				// make sure layout is valid & there are now more than 8 dashes
				if (layout != null && !layout.isEmpty() && dashFragments.size() < 8) {
					dashFragments.add(CustomDashFragment.newInstance(layout));
				}
			}
		}
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		currentPosition = mPrefs.getInt("current_dashboard", 0);
		
		// If currentPosition is invalid, set to 0
		if(currentPosition >= dashFragments.size())
			currentPosition = 0;

		Log.d(TAG, "current dashboard position: "+currentPosition);

		
		// only create fragments once or else they will be recreated..
		// fragments 
		if(savedInstanceState==null){
			setDashboard(currentPosition, 0);
		}
		
		fragmentFrameLayout = (FrameLayout) findViewById(R.id.dashboard_frame);
		
		startService(new Intent("RECON_HUD_SERVICE"));

	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		Log.d(TAG, "onDestroy()");
	}

	public void onResume() {
		super.onResume();
		startService(new Intent("RECON_PHONE_RELAY_SERVICE"));
		startService(new Intent("RECON_MOD_SERVICE"));
		startService(new Intent("RECON_MAP_SERVICE"));
		startService(new Intent("RECON_BLE_TEST_SERVICE"));        
		startService(new Intent("RECON_SETTING_SERVICE"));
		startService(new Intent("RECON_NOTIFICATION_SERVICE"));
		
		// JIRA: MODLIVE-772 Implement bluetooth connection wizard on MODLIVE
		bleReconnectInIOSMode();
		// End of JIRA: MODLIVE-772

		showVertBreadcrumb();
		Log.d(TAG, "onResume()");
	}
	
	private void bleReconnectInIOSMode(){
		if (DashLauncherApp.getInstance().bleService != null) {
			try{
				if(System.getProperty("bootup", "0").equals("0")){
					if(!DashLauncherApp.getInstance().bleService.getIsMasterBeforeonCreate()){
						Intent intent = new Intent("com.reconinstruments.connectdevice.RECONNECT");
						getBaseContext().sendBroadcast(intent);
					} else {
						Log.d( TAG, "It didn't work on ios mode, skip to reconnect ble." );
					}
				}
				System.setProperty("bootup", "1");
			} catch (RemoteException re){
				re.printStackTrace();
			}
		}
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			event.startTracking();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			startActivity(new Intent("RECON_TUTORIAL_VIDEO_SELECTOR"));
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	public void onPause() {
		super.onPause();

		Log.v(TAG, "onPause");

		SharedPreferences.Editor ed = mPrefs.edit();
		ed.putInt("current_dashboard", currentPosition);
		ed.commit();

		breadcrumbToast.cancel();
		breadcrumbToast.setView(null); // hack, cancel doesn't seem to work correctly
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		int oldPosition = currentPosition;

		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_UP:
			if (currentPosition > 0) {
				currentPosition--;
				setDashboard(currentPosition, oldPosition);
			} else {
			   	Animation upShake = AnimationUtils.loadAnimation(this, R.anim.shake_up);
			   	fragmentFrameLayout.startAnimation(upShake);
			}
			showVertBreadcrumb();
			return true;

		case KeyEvent.KEYCODE_DPAD_DOWN:
			if (currentPosition < dashFragments.size() - 1) {
				currentPosition++;
				setDashboard(currentPosition, oldPosition);
			} else {
			   	Animation upShake = AnimationUtils.loadAnimation(this, R.anim.shake_down);
			   	fragmentFrameLayout.startAnimation(upShake);
			}
			showVertBreadcrumb();
			return true;

		case KeyEvent.KEYCODE_DPAD_CENTER:
			if(dashFragments.get(currentPosition) instanceof CustomDashFragment
					&& ((CustomDashFragment) dashFragments.get(currentPosition)).hasChronoWidget()) {
				// Send chrono message
				((CustomDashFragment) dashFragments.get(currentPosition)).toggleChrono();
				return true;
			}
			return false;
		case KeyEvent.KEYCODE_BACK:
			// Launch the Power management window
			Intent i = new Intent("RECON_POWER_MENU");
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
			return true;
		}

		return super.onKeyUp(keyCode, event);
	}

	/*
	 * If prevPosition == -1, then the fragment view is empty
	 */
	public void setDashboard(int position, int prevPosition) {
		
		Log.d(TAG, "setDashboard(position="+position+",prevPosition="+prevPosition);
		
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.setTransition(FragmentTransaction.TRANSIT_NONE);

		if (prevPosition > -1) {
			if (position > prevPosition)
				ft.setCustomAnimations(R.anim.slide_in_bottom,
						R.anim.fade_out_top);
			
			else
				ft.setCustomAnimations(R.anim.slide_in_top,
						R.anim.fade_out_bottom);

			ft.remove(dashFragments.get(prevPosition));
		}

		if (ft.isEmpty())
			ft.add(R.id.dashboard_frame, dashFragments.get(position));
		else
			ft.replace(R.id.dashboard_frame, dashFragments.get(position));
		ft.commit();
	}

	private ArrayList<String> getCustomLayouts() {
		String userCustomDashXML;

		try {
			StringBuffer fileData = new StringBuffer(100);
			BufferedReader reader = new BufferedReader(new FileReader(new File(
					"/mnt/storage/ReconApps/Dashboard/user_dashboard.xml")));
			char[] buf = new char[1024];
			int numRead = 0;
			while ((numRead = reader.read(buf)) != -1) {
				fileData.append(buf, 0, numRead);
			}
			reader.close();

			// Remove line breaks to simplify regex processing later
			userCustomDashXML = fileData.toString().replace("\n", "");
		} catch (Exception e) {
			Log.e(TAG, e.toString());

			// Couldn't load custom xml
			return null;
		}

		Pattern pattern = Pattern.compile("<layout.*?</layout>");
		Matcher matcher = pattern.matcher(userCustomDashXML);

		ArrayList<String> matches = new ArrayList<String>();
		while(matcher.find()) {
			matches.add(matcher.group());
		}

		return matches;
	}

	private void showVertBreadcrumb() {
		int[] dashFrags = new int[dashFragments.size()];
		dashFrags[0]=BreadcrumbView.DASH_ICON;
		for(int i=1; i<dashFrags.length; i++)
			dashFrags[i] = BreadcrumbView.OTHER_ICON;

		mBreadcrumbView = new BreadcrumbView(getApplicationContext(), false, currentPosition, dashFrags);

		mBreadcrumbView.invalidate();

		if(breadcrumbToast == null) 
			breadcrumbToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);

		breadcrumbToast.setGravity(Gravity.RIGHT, 0, 0);
		breadcrumbToast.setView(mBreadcrumbView);
		breadcrumbToast.show();

	}
}
