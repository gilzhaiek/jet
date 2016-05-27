package com.reconinstruments.jetunpairdevice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.utils.BTHelper;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.commonwidgets.FeedbackDialog;

import java.util.ArrayList;
import java.util.List;

public class ForgetDeviceActivity  extends CarouselItemHostActivity {

    private static final String TAG = ForgetDeviceActivity.class.getSimpleName();

    private TextView mTitleTV;
    private TextView mBodyTV;
    private BluetoothDevice mDevice = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.title_body_carousel);

        mTitleTV = (TextView) findViewById(R.id.title);
        mBodyTV = (TextView) findViewById(R.id.body);
        mTitleTV.setText("FORGET DEVICE?");
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.fade_slide_in_bottom,0);
        registerReceiver(hudStateReceiver, new IntentFilter("HUD_STATE_CHANGED"));
        
        String address = getIntent().getStringExtra("DEVICE_ADDRESS");
        if(address == null){
            mBodyTV.setText("No devices paired");
            return;
        }
        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
        if(mDevice != null){
            mBodyTV.setText(Html.fromHtml(getString(R.string.desc_unpair_device, mDevice.getName())));
            updateUI();
        }else{
            mBodyTV.setText("No devices paired");
        }
    }

    @Override
    protected void onStop() {
        try {
            this.unregisterReceiver(hudStateReceiver);
        } catch (IllegalArgumentException e) {
            //ignore it
        }
        super.onStop();
    }

    /**
     * prepare the fragements, called by method initPager()
     */
    protected List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<Fragment>();
        fList.add(new ForgetDeviceFragment(R.layout.title_body_carousel_item, "Cancel", 0, 0));
        fList.add(new ForgetDeviceFragment(R.layout.title_body_carousel_item, "Forget", 0, 1));
        return fList;
    }

    /**
     * display the metric data on the UI
     */
    private void updateUI() {
        initPager();
        mPager.setCurrentItem(0);
    }

    private class ForgetDeviceFragment extends CarouselItemFragment {

        public ForgetDeviceFragment(int defaultLayout, String defaultText, int defaultImage,
                int item) {
            super(defaultLayout, defaultText, defaultImage, item);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(mDefaultLayout, container, false);
            TextView messageTextView = (TextView) v.findViewById(R.id.text_view);
            messageTextView.setText(mDefaultText);
            inflatedView = v;
            reAlign(((CarouselItemHostActivity) getActivity()).getPager());
            return v;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if(mPager.getCurrentItem() == 1 && mDevice != null){
                    String address = BTHelper.getInstance(getApplicationContext()).getLastPairedDeviceAddress();
                    int connectionState = BTHelper.getInstance(getApplicationContext()).getBTConnectionState();
                    int deviceType = BTHelper.getInstance(getApplicationContext()).getLastPairedDeviceType();
                    if(address.equals(mDevice.getAddress()) && connectionState == 2){//ensure it's disconnected
                        BTHelper.getInstance(getApplicationContext()).disconnect();
                    }else{
                        BTHelper.getInstance(getApplicationContext()).unpairDevice(mDevice.getAddress());
                        
                        if(address.equals(mDevice.getAddress())){//if we unpair the last paired device
                            BTHelper.getInstance(getApplicationContext()).setLastPairedDeviceAddress("");
                            BTHelper.getInstance(getApplicationContext()).setLastPairedDeviceName("");
                            BTHelper.getInstance(getApplicationContext()).setLastPairedDeviceType(0);
                        }

                        showUnpairedConfirmation();
                    }
                } else {
                    CommonUtils.launchParent(this,null,true);
                }
                return false;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        CommonUtils.launchParent(this, null, false);
    }

    private final BroadcastReceiver hudStateReceiver = new BroadcastReceiver() {
        int previousState = -1;
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if ("HUD_STATE_CHANGED".equals(action)) {
                int state = intent.getIntExtra("state", 0);
                if(previousState == state){
                    return;
                }
                    
                if (state == 0) {
                    Log.w(TAG, "HUD_STATE_CHANGED to 0, HUDService reports: disconnected");
                    int deviceType = BTHelper.getInstance(getApplicationContext()).getLastPairedDeviceType();
                    BTHelper.getInstance(getApplicationContext()).unpairDevice(mDevice.getAddress());

                    BTHelper.getInstance(getApplicationContext()).setLastPairedDeviceAddress("");
                    BTHelper.getInstance(getApplicationContext()).setLastPairedDeviceName("");
                    BTHelper.getInstance(getApplicationContext()).setLastPairedDeviceType(0);

                    showUnpairedConfirmation();
                    
                } else if (state == 1) {
                    Log.i(TAG, "HUD_STATE_CHANGED to 1, HUDService reports: connecting");
                } else if (state == 2) {
                    Log.i(TAG, "HUD_STATE_CHANGED to 2, HUDService reports: connected");
                }
            }
        }
    };

    private void showUnpairedConfirmation() {
        String subtitle = null;
        if(DeviceUtils.isSun()){
            subtitle = "Be sure to forget Jet from<br />your Phone's Bluetooth Settings";
        }else{
            subtitle = "Be sure to forget Snow2 from your Phone's Bluetooth Settings";
        }
        FeedbackDialog.showDialog(this, "Device Unpaired", subtitle, FeedbackDialog.ICONTYPE.CHECKMARK, FeedbackDialog.HIDE_SPINNER);
        //gives 5 seconds delay to dismiss the dialog
        new CountDownTimer(5 * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                CommonUtils.launchParent(ForgetDeviceActivity.this, null, true);
                FeedbackDialog.dismissDialog(ForgetDeviceActivity.this);
            }
        }.start();
    }
}
