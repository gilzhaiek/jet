package com.reconinstruments.tests.hudconnectivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.reconinstruments.hudconnectivitylib.HUDConnectivityManager;
import com.reconinstruments.hudconnectivitylib.IHUDConnectivity;
import com.reconinstruments.hudresources.activites.HUDBluetoothDeviceListActivity;

import java.io.IOException;

public class TestActivity extends Activity implements OnClickListener, IHUDConnectivity {
    // Debugging
    private static final String TAG = "TestActivity";
    private static final boolean DEBUG = true;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;

    String mConnectedDevice = null;
    TextView mIsHUDTV;
    Button mConnectDevice;
    Button mDownloadFile;
    Button mUploadFile;
    Button mLoadImage;
    Button mLoadLargeImage;
    Button mLoadURL;
    Button mHUDConnectedTV;
    Button mLocalWebTV, mRemoteWebTV;

    public static HUDConnectivityManager mHUDConnectivityManager = null;
    private ConnectionState mState;

    private boolean mIsHUD = false;

    private final int MRED = 0xFFFF0000;
    private final int MORANGE = 0xFFFF6600;
    private final int MGREEN = 0xFFFFFF00;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test);

        mDownloadFile = (Button) findViewById(R.id.download_file_button);
        mUploadFile = (Button) findViewById(R.id.upload_file_button);
        mLoadImage = (Button) findViewById(R.id.load_image_button);
        mLoadLargeImage = (Button) findViewById(R.id.load_large_image_button);
        mLoadURL = (Button) findViewById(R.id.load_url_button);
        mConnectDevice = (Button) findViewById(R.id.connected_device);
        mIsHUDTV = (TextView) findViewById(R.id.is_hud);
        mHUDConnectedTV = (Button) findViewById(R.id.hud_connected);
        mLocalWebTV = (Button) findViewById(R.id.local_web_connected);
        mRemoteWebTV = (Button) findViewById(R.id.remote_web_connected);

        mDownloadFile.setOnClickListener(this);
        mUploadFile.setOnClickListener(this);
        mLoadImage.setOnClickListener(this);
        mLoadLargeImage.setOnClickListener(this);
        mLoadURL.setOnClickListener(this);
        mConnectDevice.setOnClickListener(this);

        mDownloadFile.setEnabled(false);
        mUploadFile.setEnabled(false);
        mLoadImage.setEnabled(false);
        mLoadLargeImage.setEnabled(false);
        mLoadURL.setEnabled(false);

        mIsHUD = (android.os.Build.DEVICE.equalsIgnoreCase("jet")) ? true : false;

        if (mIsHUD) {
            System.load("/system/lib/libreconinstruments_jni.so");
            mIsHUDTV.setText("\"The HUD\"");
            mHUDConnectedTV.setText("HUD Disconnected");
            mHUDConnectedTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MRED));
        } else {
            mIsHUDTV.setText("\"The Phone\"");
            mHUDConnectedTV.setText("Phone Disconnected");
            mHUDConnectedTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MRED));
        }

        mLocalWebTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MRED));
        mRemoteWebTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MRED));

        try {
            mHUDConnectivityManager = new HUDConnectivityManager(this, this, mIsHUD);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start the HUDConnectivityManager", e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(HUDBluetoothDeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    try {
                        mHUDConnectivityManager.connect(address);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        if (v == mLoadImage || v == mLoadLargeImage) {
            intent = new Intent(this, WebImageActivity.class);
            if (v == mLoadImage) {
                intent.putExtra(WebImageActivity.EXTRA_IMAGE_URL, "http://www.reconinstruments.com/wp-content/themes/recon/img/jet/slide-3.jpg");
            } else {
                intent.putExtra(WebImageActivity.EXTRA_IMAGE_URL, "http://upload.wikimedia.org/wikipedia/commons/e/ee/Expl0717_-_Flickr_-_NOAA_Photo_Library.jpg");
            }
            startActivity(intent);
        } else if (v == mConnectDevice) {
            if (mState == ConnectionState.CONNECTED) {
                mHUDConnectivityManager.DEBUG_ONLY_stop();
            } else {
                intent = new Intent(this, HUDBluetoothDeviceListActivity.class);
                startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
            }
        } else if (v == mDownloadFile) {
            intent = new Intent(this, DownloadFileActivity.class);
            startActivity(intent);
        } else if (v == mUploadFile) {
            intent = new Intent(this, UploadFileActivity.class);
            startActivity(intent);
        }
    }



    @Override
    public void onConnectionStateChanged(ConnectionState state) {
        mState = state;

        if (mState != ConnectionState.CONNECTED) {
            onDeviceName("NULL");
        }

        switch (mState) {
            case LISTENING:
                if (mIsHUD) {
                    mHUDConnectedTV.setText("HUD Listening");
                    mHUDConnectedTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MRED));

                } else {
                    mHUDConnectedTV.setText("Phone Listening");
                    mHUDConnectedTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MRED));
                }
                break;
            case CONNECTED:
                if (mIsHUD) {
                    mHUDConnectedTV.setText("Phone Connected");
                    mHUDConnectedTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MGREEN));
                } else {
                    mHUDConnectedTV.setText("HUD Connected");
                    mHUDConnectedTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MGREEN));
                }
                break;
            case CONNECTING:
                if (mIsHUD) {
                    mHUDConnectedTV.setText("Phone Connecting");
                    mHUDConnectedTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MORANGE));
                } else {
                    mHUDConnectedTV.setText("HUD Connecting");
                    mHUDConnectedTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MORANGE));
                }
                break;
            case DISCONNECTED:
                if (mIsHUD) {
                    mHUDConnectedTV.setText("Phone Disconnected");
                    mHUDConnectedTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MRED));

                } else {
                    mHUDConnectedTV.setText("HUD Disconnected");
                    mHUDConnectedTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MRED));
                }
                break;
            default:
                mHUDConnectedTV.setText("WTF");
                mHUDConnectedTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MRED));
                break;
        }
    }

    @Override
    public void onNetworkEvent(NetworkEvent networkEvent, boolean hasNetworkAccess) {
        switch (networkEvent) {
            case LOCAL_WEB_GAINED:
                mLocalWebTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MGREEN));
                break;
            case LOCAL_WEB_LOST:
                mLocalWebTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MRED));
                break;
            case REMOTE_WEB_GAINED:
                mRemoteWebTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MGREEN));
                break;
            case REMOTE_WEB_LOST:
                mRemoteWebTV.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, MRED));
                break;
            default:
                break;
        }

        mDownloadFile.setEnabled(hasNetworkAccess);
        mUploadFile.setEnabled(hasNetworkAccess);
        mLoadImage.setEnabled(hasNetworkAccess);
        mLoadLargeImage.setEnabled(hasNetworkAccess);
        mLoadURL.setEnabled(hasNetworkAccess);
    }

    @Override
    public void onDeviceName(String deviceName) {
        mConnectDevice.setText(deviceName);
    }
}
