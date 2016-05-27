package com.reconinstruments.itemhost;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.os.Message;
import android.provider.Settings;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.commonwidgets.CarouselItemPageAdapter;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.commonwidgets.CommonUtils;

/**
 * <code>ChooseActivityActivity</code> is the main activity to host
 * the items. It's implemented by ViewPager with Fragment. RemoteView
 * generated from the <code>ItemHostService</code> would be
 * represented in fragment.
 */
public class ChooseActivityActivity extends CarouselItemHostActivity {
    private static final String TAG = ChooseActivityActivity.class.getSimpleName();
    private int mAnimType;

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_slide_in_bottom,0);
        setContentView(R.layout.carousel_with_text_choose_activity);
        initPager();
        mPager.setCurrentItem(0);
        Log.d(TAG, "Oncreate Chage");        
        mAnimType = R.anim.fade_slide_in_bottom;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    protected void onPause(){
    	super.onPause();
	//Hides breadcrumbs
    	((CarouselItemPageAdapter) mPager.getAdapter()).hideBreadcrumbs();  
        overridePendingTransition(mAnimType,0);

    }
    
    @Override
    protected void onResume(){
        super.onResume();
        overridePendingTransition(R.anim.fade_slide_in_bottom,0);
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        CommonUtils.launchParent(this,null,false);
        mAnimType = R.anim.fade_slide_in_top;
    }
    
    protected List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<Fragment>();
        fList.add(new ChooseActivityFragment(R.layout.choose_activity_item,
					     getString(R.string.cycling),
                                             R.drawable.cycling_icon,
                                             0, ActivityUtil.SPORTS_TO_PROFILE
					     .get(ActivityUtil.SPORTS_TYPE_CYCLING),
                                             ActivityUtil.SPORTS_TYPE_CYCLING));
        fList.add(new ChooseActivityFragment(R.layout.choose_activity_item,
					     getString(R.string.running),
					     R.drawable.running_icon,
                                             1,ActivityUtil.SPORTS_TO_PROFILE
					     .get(ActivityUtil.SPORTS_TYPE_RUNNING),
                                             ActivityUtil.SPORTS_TYPE_RUNNING));
        return fList;
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_UP:
            return true;
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DPAD_CENTER:
            String profileName =
                ((ChooseActivityFragment)((CarouselItemPageAdapter)mPager.getAdapter())
                 .getItem(mPager.getCurrentItem()))
                .getAssociatedProfileName();
            int sat =
                ((ChooseActivityFragment)((CarouselItemPageAdapter)mPager.getAdapter())
                 .getItem(mPager.getCurrentItem()))
                .getSportsActivityType();
            broadcastSportsType(sat);
            runTheColumnHanlderService(profileName);
            return true;
        default:
            return super.onKeyUp(keyCode, event);
        }
    }

    public static final Intent getColumnHandlerServiceIntent(String profileName) {
        Intent intent = new Intent("com.reconinstruments.COLUMN_HANDLER_SERVICE");
        intent.putExtra("ChosenProfile", profileName);
	return intent;
    }
    private void runTheColumnHanlderService(String profileName) {
	Intent intent = getColumnHandlerServiceIntent(profileName);
        startService(intent);
        // set show gps overlay if needed to 1 for every time, this property will be used in
        // JetDashboard to determine if it should show gps, 1 means show overlay if needed.
        Settings.System.putInt(getApplicationContext().getContentResolver(), "SHOWGPSOVERLAYIFNEEDED", 1);
    }
    private static final int MSG_SET_SPORTS_ACTIVITY = 26; // TODO put in library
    private void broadcastSportsType(int type) {
        Log.v(TAG, "broadcastSportsType");
        Log.v(TAG, "sports type is "+type);
        Intent i =
            new Intent("com.reconinstruments.applauncher.transcend.BROADCAST_COMMAND");
        Message command = Message.obtain(null,MSG_SET_SPORTS_ACTIVITY,type,0);
        i.putExtra("command",command);
        sendBroadcast(i);
    }
}
