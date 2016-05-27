package com.reconinstruments.ss1.test;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity{
    
    private static final String TAG = MainActivity.class.getSimpleName();
    
    private String mBTAddress = ""; // F0:CB:A1:74:CB:57 Recon Dev's iPhone

    private Button mDiscoverableButton;
    private Button mChooseButton;
    private Button mConnectButton;
    private Button mDisconnetButton;

    private Button mDEVMDiscoverableButton;
//    private Button mDEVMChooseButton;
    private Button mDEVMConnectButton;
    private Button mDEVMDisconnetButton;
    private Button mDEVMUnpairButton;
    private Button mDEVMDeleteButton;

    private SPPMManager mSPPMManager;
    private DEVMManager mDEVMManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mDiscoverableButton = (Button)findViewById(R.id.button_discoverable);
        mChooseButton = (Button)findViewById(R.id.button_choose);
        mConnectButton = (Button)findViewById(R.id.button_connect);
        mDisconnetButton = (Button)findViewById(R.id.button_disconnect);

        mDEVMDiscoverableButton = (Button)findViewById(R.id.button_devm_discoverable);
//        mDEVMChooseButton = (Button)findViewById(R.id.button_devm_choose);
        mDEVMConnectButton = (Button)findViewById(R.id.button_devm_connect);
        mDEVMDisconnetButton = (Button)findViewById(R.id.button_devm_disconnect);
        mDEVMUnpairButton = (Button)findViewById(R.id.button_devm_unpair);
        mDEVMDeleteButton = (Button)findViewById(R.id.button_devm_delete);
        
        mDEVMManager = DEVMManager.getInstance(this);
        mSPPMManager = SPPMManager.getInstance(this);
        
        mDiscoverableButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (BluetoothAdapter.getDefaultAdapter().getScanMode() !=
                        BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivity(discoverableIntent);
                }
            }
        });

        mChooseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(intent, 1);
            }
        });
        
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!"".equals(mBTAddress)){
                    mSPPMManager.connectRemoteDevice();
                }else{
                    Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivityForResult(intent, 1);
                }
            }
        });
        
        mDisconnetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!"".equals(mBTAddress)){
                    mSPPMManager.disconnectRemoteDevice();
                }else{
                    Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivityForResult(intent, 1);
                }
            }
        });
        
        mDEVMDiscoverableButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mDEVMManager.updateDiscoverableMode();
            }
        });

//        mDEVMChooseButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                mDEVMManager.startDeviceDiscovery();
//            }
//        });
        
        mDEVMConnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!"".equals(mBTAddress)){
                    mDEVMManager.connectWithRemoteDevice();
                }else{
                    Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivity(intent);
                }
            }
        });
        
        mDEVMDisconnetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!"".equals(mBTAddress)){
                    mDEVMManager.disconnectRemoteDevice();
                }else{
                    Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivity(intent);
                }
            }
        });
        
        mDEVMUnpairButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!"".equals(mBTAddress)){
                    mDEVMManager.unpairRemoteDevice();
                }else{
                    Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivity(intent);
                }
            }
        });
        
        mDEVMDeleteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!"".equals(mBTAddress)){
                    mDEVMManager.deleteRemoteDevice();
                }else{
                    Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivity(intent);
                }
            }
        });
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == RESULT_OK){
                setBTAddress(data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS));
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        mDEVMManager.stop();
        mSPPMManager.stop();
        super.onDestroy();
    }
    
    public void setBTAddress(String address){
        mBTAddress = address;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Set Bluetooth address to\n" + mBTAddress, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    public String getBTAddress(){
        return mBTAddress;
    }
}
