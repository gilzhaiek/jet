//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.passcodelock;

import java.io.IOException;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.SharedPreferences;
import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.commonwidgets.CarouselItemPageAdapter;
import com.reconinstruments.commonwidgets.FeedbackDialog;
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.utils.SystemPropUtil;
import com.reconinstruments.utils.UIUtils;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;


/**
 * <code>PasscodeLockActivity</code> The lock screen. The user will be
 * stuck here, until they entered the right password.
 *
 */
public class PasscodeLockActivity extends CarouselItemHostActivity {
    public static final String TAG = "PasscodeLockActivity";
    public static final int LR_PAGER_PADDING = 115;
    public static final int UD_PAGER_PADDING = 0;
    public static final String PASSCODE_ADB_WAS = "passcode_adb_was";
    public static final int CAROUSEL_INDEX_ON_START = 10;
    private static int STATE_SETTINGS_PASSWORD = 0;
    private static int STATE_CONFIRMING_PASSWORD = 1;
    private static int STATE_ATTEMPTING_UNLOCK = 2;
    private static int STATE_OLD_PASSCODE_BEFORE_CHANGE = 3;
    private int mCurrentState = STATE_SETTINGS_PASSWORD;
    private String mInitialPasscode = "";
    private int currentPasscodeIndex = 0;
    private TextView titleText;
    private ImageView[] mDotsAndDashes = new ImageView[4];
    private SharedPreferences mPrefs;
    private String mPasscodeBeingEntered = "";
    private String storedPasscode = "";
    private Intent intent;
    private boolean allowUserKeyPress = true;
    private CustomOnPageListenerAdapter mAdapterListener;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_passcode);
        initPager();
        mPager.setCurrentItem(CAROUSEL_INDEX_ON_START);
        mPager.setPadding(LR_PAGER_PADDING, UD_PAGER_PADDING, LR_PAGER_PADDING, UD_PAGER_PADDING);
        mAdapterListener = new CustomOnPageListenerAdapter();
        mAdapterListener.setBreadcrumbView(false);
        mAdapterListener.hideBreadcrumbs();
        mPager.setOnPageChangeListener(mAdapterListener);
        mPager.setAdapter(mAdapterListener);
        initViews();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        intent = getIntent();
        //((CarouselItemPageAdapter) mPager.getAdapter()).setBreadcrumbView(false);
        //((CarouselItemPageAdapter) mPager.getAdapter()).hideBreadcrumbs();

    }

    private void initViews() {
        titleText = (TextView) findViewById(R.id.text_view);
        mDotsAndDashes[0] =(ImageView)findViewById(R.id.passcode1);
        mDotsAndDashes[1] =(ImageView)findViewById(R.id.passcode2);
        mDotsAndDashes[2] =(ImageView)findViewById(R.id.passcode3);
        mDotsAndDashes[3] =(ImageView)findViewById(R.id.passcode4);
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Check the state and launch the correct mode.

        if(hasThePasswordbeenSet()) {
            if (intent == null) finish();

            String intentString = intent.getStringExtra("CALLING_ACTION");

            if ("CHANGE_PASSCODE".equals(intentString)) {
                enterOldPasscode();
            }else if("ENABLE_PASSCODE".equals(intentString)){
                //the password is set and theyre coming from clicking 'enable passcode'
                titleText.setText("ENTER YOUR PASSCODE");
                mPasscodeBeingEntered = "";
                resetDashes();
                mCurrentState = STATE_CONFIRMING_PASSWORD;
            }else{
                // Default behaviour is lock
                lock();
            }
        }else{
            enterInitialPasscode();
        }

    }
    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    public void onBackPressed(){
        Log.v(TAG, "onBackPressed");
        // Disable back button when locked
        if (mCurrentState != STATE_ATTEMPTING_UNLOCK) {
            super.onBackPressed();
        } else {
            Log.v(TAG,"Won't exit");
        }
    }

    private void resetDashes(){
        for (ImageView iv: mDotsAndDashes) {
            iv.setImageResource(R.drawable.dash);
        }
        mPager.setCurrentItem(CAROUSEL_INDEX_ON_START);
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                return true;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if(maxedStringLength()){
                    //do nothing
                }else if(!allowUserKeyPress) {
                    //do nothing
                }else{
                    updatePasscodeBeingEntered();
                }
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    private void updatePasscodeBeingEntered() {
        String currentItemNumber =
                ((PasscodeLockFragment)((CustomOnPageListenerAdapter)mPager.getAdapter())
                        .getItem(mPager.getCurrentItem())).getCarouselItemNumber();
        mPasscodeBeingEntered+=currentItemNumber;
        enteredPasscodeUpdated();
    }

    /**
     * Read the passcode that the user has entered into the UI
     *
     */
    private void enteredPasscodeUpdated() {
        mDotsAndDashes[currentPasscodeIndex].setImageResource(R.drawable.dot);
        if(currentPasscodeIndex != 3){
            currentPasscodeIndex++;
            return;
        }else{
            passCodeFullyEntered();
            currentPasscodeIndex = 0;
        }
    }

    private void passCodeFullyEntered() {
        if (mCurrentState == STATE_SETTINGS_PASSWORD) {
            initialPasswordSet();
        }
        else if (mCurrentState == STATE_CONFIRMING_PASSWORD) {
            confirmedPasswordEntered();
        }
        else if (mCurrentState == STATE_ATTEMPTING_UNLOCK) {
            if (haveEnteredStoredPassword()) {
                unlock();
            }else{
                // Password did not match
                titleText.setText("INCORRECT - TRY AGAIN");
                new CountDownTimer(1 * 1000, 1000) {
                    public void onTick(long millisUntilFinished) {
                    }
                    public void onFinish() {
                        lock();
                    }
                }.start();
            }
        }
        else if (mCurrentState == STATE_OLD_PASSCODE_BEFORE_CHANGE) {
            if (haveEnteredStoredPassword()) {
                new CountDownTimer(1 * 1000, 1000) {
                    public void onTick(long millisUntilFinished) {
                    }
                    public void onFinish() {
                        enterInitialPasscode();
                    }
                }.start();
            }
            else {
                titleText.setText("INCORRECT - TRY AGAIN");
                new CountDownTimer(1 * 1000, 1000) {
                    public void onTick(long millisUntilFinished) {
                    }
                    public void onFinish() {
                        enterOldPasscode();
                    }
                }.start();
            }
        }
    }

    private boolean haveEnteredStoredPassword() {
        storedPasscode = mPrefs.getString("PASSCODE_VALUE",mPasscodeBeingEntered);
        return checkPasscodes(mPasscodeBeingEntered,storedPasscode);
    }

    private void resetCachedPasswordsAndDashes() {
        mInitialPasscode = "";
        mPasscodeBeingEntered = "";
        resetDashes();
    }
    private void enterOldPasscode() {
        titleText.setText("ENTER OLD PASSCODE");
        mCurrentState = STATE_OLD_PASSCODE_BEFORE_CHANGE;
        resetCachedPasswordsAndDashes();
    }
    private void enterInitialPasscode() {
        if(hasThePasswordbeenSet()){
            titleText.setText("ENTER NEW PASSCODE");
        }else{
            titleText.setText("ENTER PASSCODE");
        }
        mCurrentState = STATE_SETTINGS_PASSWORD;
        resetCachedPasswordsAndDashes();
    }

    private void initialPasswordSet() {
        mInitialPasscode = mPasscodeBeingEntered;
        new CountDownTimer(1 * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                startPasswordConfirmation();
            }
        }.start();
    }
    private void startPasswordConfirmation() {
        mCurrentState = STATE_CONFIRMING_PASSWORD;
        mPasscodeBeingEntered = "";
        titleText.setText("RE-ENTER PASSCODE");
        resetDashes();
    }

    private void confirmedPasswordEntered() {
        String intentString = intent.getStringExtra("CALLING_ACTION");
        if(hasThePasswordbeenSet()&& intentString.equals("ENABLE_PASSCODE")){
            //if password is set and its getting
            //called from the enable_passcode area
            if (haveEnteredStoredPassword()) {
                disablePasscode();

            } else {                  // Didn't match
                titleText.setText("INCORRECT - TRY AGAIN");
                new CountDownTimer(1 * 1000, 1000) {
                    public void onTick(long millisUntilFinished) {
                    }
                    public void onFinish() {
                        //will keep trying again until correct or user presses back
                        titleText.setText("ENTER YOUR PASSCODE");
                        mPasscodeBeingEntered = "";
                        resetDashes();
                        mCurrentState = STATE_CONFIRMING_PASSWORD;
                    }
                }.start();
            }
        }else{
            confirmInitialPasscode();
        }
    }

    private void confirmInitialPasscode() {
        if (checkPasscodes(mInitialPasscode,mPasscodeBeingEntered)) {
            startDialog("Passcode Set", "Lock JET from OPTIONS menu");
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString("PASSCODE_VALUE", mPasscodeBeingEntered); // value to store
            editor.commit();
            SettingsUtil.setSystemInt(this, SettingsUtil.PASSCODE_ENABLED,1);
            new CountDownTimer(2 * 1000, 1000) {
                public void onTick(long millisUntilFinished) {
                }
                public void onFinish() {
                    FeedbackDialog.dismissDialog(PasscodeLockActivity.this);
                    finish();
                }
            }.start();

        } else {                  // Didn't match
            titleText.setText("NOT A MATCH - TRY AGAIN");
            new CountDownTimer(1 * 1000, 1000) {
                public void onTick(long millisUntilFinished) {
                }
                public void onFinish() {
                    //will keep trying again until correct or user presses back
                    enterInitialPasscode();
                }
            }.start();
        }
    }

    private void disablePasscode() {
        startDialog("Passcode Disabled", " ");
        //remove passcode value
        mPrefs.edit().remove("PASSCODE_VALUE").commit();
        SettingsUtil.setSystemInt(this, SettingsUtil.PASSCODE_ENABLED,0);

        new CountDownTimer(2 * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                FeedbackDialog.dismissDialog(PasscodeLockActivity.this);
                finish();
            }
        }.start();
    }

    private boolean checkPasscodes(String original, String newCode){
        return (original.equals(newCode));
    }

    private void lock() {
        Log.v(TAG,"Lock it");
        int adbwas = 0;
        mCurrentState = STATE_ATTEMPTING_UNLOCK;
        titleText.setText("ENTER PASSCODE");
        mPasscodeBeingEntered = "";
        resetDashes();
        UIUtils.setButtonHoldShouldLaunchApp(this,false);
        if (SettingsUtil
            .getCachableSystemIntOrSet(this,SettingsUtil.PASSCODE_IN_LOCK,0) == 0) {
            Log.v(TAG,"setting in lock");
            SettingsUtil.setSystemInt(this,SettingsUtil.PASSCODE_IN_LOCK,1);
            SystemPropUtil.setSystemProp("sys.reconinstruments.lockedonce", "1");
            adbwas =
                SettingsUtil.getSecureIntOrSet(this,Settings.Secure.ADB_ENABLED,0);
            SettingsUtil.setSystemInt(this,PASSCODE_ADB_WAS,adbwas);
        }
        else {
            Log.v(TAG,"not setting in lock");
        }
        //String oldConfig =  System.property.get("sys/usb.config");
        // newconfig = oldConfig.replace adb and mtp with ""
        if (adbwas != 1) {
            // ADB is not currently enabled, disable MTP manually.
            SystemPropUtil.setSystemProp("sys.usb.config", "none");
        }
        Log.v(TAG, "Disabling ADB");
        Settings.Secure.putInt(getBaseContext().getContentResolver(), Settings.Secure.ADB_ENABLED, 0);
    }
    private void unlock() {
        Log.v(TAG,"Unlock it");
        sendBroadcast(new Intent("com.reconinstruments.MTP_ENABLE"));
        if (SettingsUtil.getCachableSystemIntOrSet(this,PASSCODE_ADB_WAS,0) == 1) {
            Log.d(TAG, "Enabling ADB");
            Settings.Secure.putInt(getBaseContext().getContentResolver(), Settings.Secure.ADB_ENABLED, 1);
        } else {
            SystemPropUtil.setSystemProp("sys.usb.config", "mtp");
        }

        SettingsUtil.setSystemInt(this,SettingsUtil.PASSCODE_IN_LOCK,0);
        UIUtils.setButtonHoldShouldLaunchApp(this,true);
        finish();
    }

    private boolean hasThePasswordbeenSet() {
        return mPrefs.contains("PASSCODE_VALUE");
    }


    protected List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<Fragment>();
        int[] numArray = new int[]{0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9};
        int x = 0;
        for(int i : numArray){
            fList.add(new PasscodeLockFragment(R.layout.custom_carousel_remote_view,
                    ""+i, R.drawable.back_button, x));
            x++;
        }
        return fList;
    }
    private void remoteSetProp(String prop, String value) {
        Intent i = new Intent("com.reconinstruments.systempropertysetter.SET_PROP");
        i.putExtra("prop",prop);
        i.putExtra("value",value);
        sendBroadcast(i);
    }

    private boolean maxedStringLength(){
        Log.v("string length", "" + mPasscodeBeingEntered.length());
        if(mPasscodeBeingEntered.length() > 3){
            return true;
        }else{
            return false;
        }
    }

    private void startDialog(String titleText, String subtitleText){
        FeedbackDialog.showDialog(PasscodeLockActivity.this,
                titleText,
                subtitleText,
                FeedbackDialog.ICONTYPE.CHECKMARK,
                FeedbackDialog.HIDE_SPINNER);

    }

    List<Fragment> fragments = getFragments();

    private class CustomOnPageListenerAdapter extends CarouselItemPageAdapter {

        public CustomOnPageListenerAdapter() {
            super(getSupportFragmentManager(), (List<CarouselItemFragment>) ((List<?>) fragments), mPager, false, false, null);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            switch (state) {
                case ViewPager.SCROLL_STATE_IDLE:
                    allowUserKeyPress = true;
                    break;
                case ViewPager.SCROLL_STATE_SETTLING:
                    allowUserKeyPress = false;

                    break;
                case ViewPager.SCROLL_STATE_DRAGGING:
                    allowUserKeyPress = false;
                    break;
                default:
                    allowUserKeyPress = true;
                    break;
            }
        }
    }

}
