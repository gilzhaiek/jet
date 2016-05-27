package com.reconinstruments.jetunpairdevice;

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
import com.reconinstruments.commonwidgets.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class PairedDevicesActivity  extends CarouselItemHostActivity {

    private static final String TAG = PairedDevicesActivity.class.getSimpleName();

    private TextView mTitleTV;
    private TextView mBodyTV;
    
    private List<BluetoothDevice> mDevices = new ArrayList<BluetoothDevice>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.title_body_carousel);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDevices = BTHelper.getInstance(getApplicationContext()).getPairedDevice();

        mTitleTV = (TextView) findViewById(R.id.title);
        mBodyTV = (TextView) findViewById(R.id.body);
        mTitleTV.setText("FORGET DEVICES");
        if(mDevices.size() > 0){
            if(mDevices.size() == 1){
                mBodyTV.setText("1 Device Paired");
            }else{
                mBodyTV.setText(mDevices.size() + " Devices Paired");
            }
            //custom the viewPager width to fit the length of text
            ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
            LayoutParams params=(LayoutParams) viewPager.getLayoutParams();
            params.width=340;
            viewPager.setLayoutParams(params);
            updateUI();
        }else{
	    updateUI();
            mBodyTV.setText("No devices paired");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * prepare the fragements, called by method initPager()
     */
    protected List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<Fragment>();
        int i = 0;
        for(BluetoothDevice device : mDevices){
            fList.add(new PairedDevicesFragment(R.layout.title_body_carousel_item, device.getName(), 0, i));
            i++;
        }
        return fList;
    }

    /**
     * display the metric data on the UI
     */
    private void updateUI() {
        initPager();
        mPager.setCurrentItem(0);
        mPager.setPadding(70,0,70,0); // lower padding to accomodate relatively longer name
    }

    private class PairedDevicesFragment extends CarouselItemFragment {

        public PairedDevicesFragment(int defaultLayout, String defaultText, int defaultImage,
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
                if(mPager != null){
                    BluetoothDevice device = mDevices.get(mPager.getCurrentItem());
                    if(device != null){
                        Intent i = new Intent(this, ForgetDeviceActivity.class);
                        i.putExtra("DEVICE_ADDRESS", device.getAddress());
                        CommonUtils.launchNext(this, i, false);
                    }
                }
                return false;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        CommonUtils.launchPrevious(this, null, false);
    }
}
