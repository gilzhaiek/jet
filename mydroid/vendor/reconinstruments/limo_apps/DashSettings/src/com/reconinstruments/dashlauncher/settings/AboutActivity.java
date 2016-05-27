package com.reconinstruments.dashlauncher.settings;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.reconinstruments.dashsettings.R;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AboutActivity extends Activity{
	private static final String TAG = "AboutActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.setting_about_layout);
		
		ImageView headerIcon = (ImageView) findViewById(R.id.setting_icon);
		headerIcon.setBackgroundResource(R.drawable.about_white);
		TextView title = (TextView) findViewById(R.id.setting_title);
		title.setText("ABOUT");
		
		LinearLayout view = (LinearLayout) findViewById(R.id.setting_about_version);
		TextView tv = (TextView) view.findViewById(R.id.setting_text);
		tv.setText("SOFTWARE VERSION");
		tv = (TextView) view.findViewById(R.id.setting_subtext);
		tv.setText(getVersionName());
		
		view = (LinearLayout) findViewById(R.id.setting_about_serial);
		tv = (TextView) view.findViewById(R.id.setting_text);
		tv.setText("SERIAL NUMBER");
		tv = (TextView) view.findViewById(R.id.setting_subtext);
		tv.setText(Build.SERIAL);
				
		view = (LinearLayout) findViewById(R.id.setting_about_model);
		tv = (TextView) view.findViewById(R.id.setting_text);
		tv.setText("MODEL");
		tv = (TextView) view.findViewById(R.id.setting_subtext);
		tv.setText(Build.MODEL);
				
		view = (LinearLayout) findViewById(R.id.setting_about_android);
		tv = (TextView) view.findViewById(R.id.setting_text);
		tv.setText("ANDROID OS");
		tv = (TextView) view.findViewById(R.id.setting_subtext);
		tv.setText(Build.VERSION.RELEASE);
		
		view = (LinearLayout) findViewById(R.id.setting_about_build);
		tv = (TextView) view.findViewById(R.id.setting_text);
		tv.setText("BUILD");
		tv = (TextView) view.findViewById(R.id.setting_subtext);
		tv.setText(Build.DISPLAY);
		
		view = (LinearLayout) findViewById(R.id.setting_about_kernel);
		tv = (TextView) view.findViewById(R.id.setting_text);
		tv.setText("KERNEL");
		tv = (TextView) view.findViewById(R.id.setting_subtext);
		tv.setText(this.getFormattedKernelVersion());	
		
		
	}
		
	private String getVersionName(){

		String versionName="unknown";
		try {
		    PackageInfo pInfo =
			this.getPackageManager().getPackageInfo(this.getPackageName(),  PackageManager.GET_META_DATA);
		    versionName = pInfo.versionName;
		} catch (NameNotFoundException e1) {
		    Log.e(this.getClass().getSimpleName(), "Name not found", e1);
		}
		return versionName;
	    }
		
	    private String getFormattedKernelVersion() {
	        String procVersionStr;

	        try {
	            BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
	            try {
	                procVersionStr = reader.readLine();
	            } finally {
	                reader.close();
	            }

	            final String PROC_VERSION_REGEX =
	                "\\w+\\s+" + /* ignore: Linux */
	                "\\w+\\s+" + /* ignore: version */
	                "([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
	                "\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /* group 2: (xxxxxx@xxxxx.constant) */
	                "\\((?:[^(]*\\([^)]*\\))?[^)]*\\)\\s+" + /* ignore: (gcc ..) */
	                "([^\\s]+)\\s+" + /* group 3: #26 */
	                "(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
	                "(.+)"; /* group 4: date */

	            Pattern p = Pattern.compile(PROC_VERSION_REGEX);
	            Matcher m = p.matcher(procVersionStr);

	            if (!m.matches()) {
	                Log.e(TAG, "Regex did not match on /proc/version: " + procVersionStr);
	                return "Unavailable";
	            } else if (m.groupCount() < 4) {
	                Log.e(TAG, "Regex match on /proc/version only returned " + m.groupCount()
			      + " groups");
	                return "Unavailable";
	            } else {
	                return (new StringBuilder(m.group(1)).append("\n").append(
										  m.group(2)).append(" ").append(m.group(3)).append("\n")
	                        .append(m.group(4))).toString();
	            }
	        } catch (IOException e) {  
	            Log.e(TAG,
			  "IO Exception when getting kernel version for Device Info screen",
			  e);

	            return "Unavailable";
	        }
	    }


}
