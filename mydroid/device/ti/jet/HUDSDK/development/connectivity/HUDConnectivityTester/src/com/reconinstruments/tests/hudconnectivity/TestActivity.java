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
import android.os.AsyncTask;
import android.widget.Toast;

import com.reconinstruments.os.HUDOS;
import com.reconinstruments.os.connectivity.HUDConnectivityManager;
import com.reconinstruments.os.connectivity.IHUDConnectivity;
import com.reconinstruments.os.connectivity.http.HUDHttpRequest;
import com.reconinstruments.os.connectivity.http.HUDHttpResponse;
import com.reconinstruments.os.connectivity.http.HUDHttpRequest.RequestMethod;

import java.io.IOException;
import java.security.MessageDigest;

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

        mHUDConnectivityManager = (HUDConnectivityManager) HUDOS.getHUDService(HUDOS.HUD_CONNECTIVITY_SERVICE);
        if(mHUDConnectivityManager == null){
            Log.e(TAG, "Failed to get HUDConnectivityManager");
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        Log.d(TAG, "onStart()");
        mHUDConnectivityManager.register(this);
        Log.d(TAG, "mHUDConnectivityManager.register(this)");
    }

    @Override
    public void onStop(){
        Log.d(TAG, "onStop()");
        mHUDConnectivityManager.unregister(this);
        super.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG) Log.d(TAG, "onActivityResult " + resultCode);
        // We only listen now
        /*switch (requestCode) {
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
        }*/
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
        Log.d(TAG,"onConnectionStateChanged : state:" + state);
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
        Log.d(TAG,"onNetworkEvent : networkEvent:" + networkEvent + " hasNetworkAccess:" + hasNetworkAccess);

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
        Log.d(TAG,"onDeviceName : deviceName:" + deviceName);
        mConnectDevice.setText(deviceName);
    }

    public void urlTimeoutOnClick(View v) {
        (new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = false;
                try {
                    String mUrl = "http://download.thinkbroadband.com/5MB.zip?T=" + System.currentTimeMillis();
                    String mMD5String = "b3215c06647bc550406a9c8ccc378756";

                    long startTime = System.currentTimeMillis();
                    HUDHttpRequest request = new HUDHttpRequest(RequestMethod.GET, mUrl);
                    request.setTimeout(500);
                    HUDHttpResponse response = TestActivity.mHUDConnectivityManager.sendWebRequest(request);
                    long stopTime = System.currentTimeMillis();

                    if (response.hasBody()) {
                        if (DEBUG)
                            Log.d(TAG, " sendWebRequest(response) bodySize:" + response.getBody().length);
                        byte[] data = response.getBody();
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        md.reset();
                        md.update(data);
                        byte[] resultByte = md.digest();
                        String value = "";
                        for (int i = 0; i < resultByte.length; i++) {
                            value += Integer.toString((resultByte[i] & 0xff) + 0x100, 16).substring(1);
                        }
                        if (value.trim().equalsIgnoreCase(mMD5String)) {
                            result = true;
                        }
                    }
                    Log.d(TAG, "Code:" + response.getResponseCode() + " Msg:" + response.getResponseMessage());
                    Log.d(TAG, "Time: " + (stopTime - startTime) + " Result: " + (result ? "Successful" : "Failed"));
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return result;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                Toast.makeText(TestActivity.this, "Result:" + result, Toast.LENGTH_LONG).show();
            }

        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        ;
    }
}
