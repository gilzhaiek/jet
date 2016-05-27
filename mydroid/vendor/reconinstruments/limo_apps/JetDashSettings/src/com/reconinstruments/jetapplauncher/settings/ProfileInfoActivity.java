package com.reconinstruments.jetapplauncher.settings;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.widget.TextView;
import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.utils.ConversionUtil;
import com.reconinstruments.utils.UserInfo;
import android.os.Build;
import android.widget.LinearLayout;
import com.reconinstruments.commonwidgets.CommonUtils;

import java.text.DecimalFormat;


/**
 * <code>ProfileInfoActivity</code> shows information associated with
 * the user profile xml. See
 * <code>com.reconinstruments.utils.UserInfo</code>
 *
 */
public class ProfileInfoActivity extends Activity {

    UserInfo uinfo;
    boolean noUnExists;
    private static final DecimalFormat sTwoOrLessFormat = new DecimalFormat("#.##");
    private static final String sNoDecimalFormat = "%.0f";
    private static final String FONT_COLOR_FFB300 = "<font color=\"#ffb300\">";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        uinfo = UserInfo.getACopyOfSystemUserInfo();
        noUnExists = uinfo.get(UserInfo.NAME).equals("");
        if (noUnExists) {
            setContentView(R.layout.no_profile_alert);
            TextView tv = (TextView) findViewById(R.id.subTitleView);
	    String deviceName = Build.MODEL;
            tv.setText(Html.fromHtml("Connect " +deviceName+
				     " to " + FONT_COLOR_FFB300 +"Uplink</font> or the "+ FONT_COLOR_FFB300 +"Recon Engage</font> app to sync your profile."));
        } else {
            setContentView(R.layout.setting_profile_info_layout);
            String tmpString;    // Will be used for reading from userinfo xml
            double tmpValue;    // Will be used for parsing tmpString
            int unit = SettingsUtil.getCachableSystemIntOrSet(this,
                    SettingsUtil.RECON_UNIT_SETTING,
                    SettingsUtil.RECON_UNITS_METRIC);

            // Name
            LinearLayout view = (LinearLayout) findViewById(R.id.setting_profile_name);
            view.setPadding(view.getPaddingLeft(), 20, 0, 0);
            TextView tv = (TextView) view.findViewById(R.id.setting_text);
            tv.setText("NAME");
            tv = (TextView) view.findViewById(R.id.setting_subtext);
            tv.setText(uinfo.get(UserInfo.NAME));

            // Height:
            tmpString = uinfo.get(UserInfo.HEIGHT);
            if (!tmpString.equals("")) {
                tmpValue = Double.parseDouble(tmpString); // cm
                tmpString = (unit == SettingsUtil.RECON_UNITS_METRIC) ?
                        String.format(sNoDecimalFormat, tmpValue) + " cm" :
                        metersToFeetInch(tmpValue / 100.0);
            }
            view = (LinearLayout) findViewById(R.id.setting_profile_height);
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), 8, 0);
            tv = (TextView) view.findViewById(R.id.setting_text);
            tv.setText("HEIGHT");
            tv = (TextView) view.findViewById(R.id.setting_subtext);
            tv.setText(tmpString);

            // Weight:
            tmpString = uinfo.get(UserInfo.WEIGHT);
            if (!tmpString.equals("")) {
                tmpValue = Double.parseDouble(tmpString);
                tmpString = (unit == SettingsUtil.RECON_UNITS_METRIC) ?
                        sTwoOrLessFormat.format(tmpValue) + " kg" :
                        "" + sTwoOrLessFormat.format(ConversionUtil.round(ConversionUtil.kgToLBs(tmpValue))) + " lbs";
            }
            view = (LinearLayout) findViewById(R.id.setting_profile_weight);
            view.setPadding(8, view.getPaddingTop(), 0, 0);
            tv = (TextView) view.findViewById(R.id.setting_text);
            tv.setText("WEIGHT");
            tv = (TextView) view.findViewById(R.id.setting_subtext);
            tv.setText(tmpString);

            // Gender:
            view = (LinearLayout) findViewById(R.id.setting_profile_gender);
            tv = (TextView) view.findViewById(R.id.setting_text);
            tv.setText("GENDER");
            tv = (TextView) view.findViewById(R.id.setting_subtext);
            tv.setText(uinfo.get(UserInfo.GENDER));

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        CommonUtils.launchPrevious(this,null,false);
    }
    
    private String metersToFeetInch(double meters) {
	double feet = ConversionUtil.metersToFeet(meters) + 0.5/12;
	return ""+(int)feet + "' "+(int)(ConversionUtil.feetToInches(feet - ((int)feet)))+"\"";
    }
}
