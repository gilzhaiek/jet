
package com.reconinstruments.jetconnectdevice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.utils.BTHelper;


/**
 * Entry point Activity that decides on which activity to call, start etc without having to show a UI to the user.
 */
public class ConnectSmartphoneActivity extends Activity {

	public static final int REQUEST_CODE = 1;
	public static final int RESULT_PROCEED_CONNECTION = 2;
	public static final int RESULT_DO_LATER = 3;
	public static final String HAS_CONNECTED_BEFORE = "com.reconinstruments.jetconnectdevice.HAS_CONNECTED_BEFORE";
	public static final String CALLED_FROM_INITIAL_SETUP = "com.reconinstruments.QuickstartGuide.CALLED_FROM_INITIAL_SETUP";
	
	public static String RECON_YELLOW_TEXT;
	
    private static final String TAG = ConnectSmartphoneActivity.class.getSimpleName();
    private static boolean sCalledFromInitialSetup = false;
    
    private int mConnectionState = -1;
    private String mBTAddress = null;
    private boolean mShowingConnectReason = false;
    private int mBTType;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sCalledFromInitialSetup = getIntent().getBooleanExtra(CALLED_FROM_INITIAL_SETUP, false);
        
        mBTAddress = BTHelper.getInstance(getApplicationContext()).getLastPairedDeviceAddress();
        
        // 0 disconnected, 1 connecting, 2 connected
        mConnectionState = BTHelper.getInstance(getApplicationContext()).getBTConnectionState();
        
        mBTType = BTHelper.getInstance(getApplicationContext()).getLastPairedDeviceType();
        
        //yellow text color set to #ffb300
        RECON_YELLOW_TEXT = String.format("#%06X", (0xFFFFFF &
                getResources().getColor(R.color.recon_jet_highlight_text_button)));
        
        Log.i(TAG, "connection state: " + mConnectionState);
        Log.i(TAG, "last paired device address: " + mBTAddress);
        
        chooseConnectOption();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	if(requestCode == REQUEST_CODE){
    		if(resultCode == RESULT_PROCEED_CONNECTION){
    			//user proceeds with smartphone connectivity
    			Intent intent = new Intent(this, ChooseDeviceActivity.class);
    			CommonUtils.launchNew(this,intent,true);
				overridePendingTransition(R.anim.fade_slide_in_bottom,0);
    		}
    		else if(resultCode == RESULT_DO_LATER){
    			//user knows consequences of quitting smartphone pairing
    			// and wants to do it later
    			Log.i(TAG, "received result DO_LATER");
    			showTutorialOnItemHost();
    			finish();
    		}
    		else {
    			finish();
    		}
    	}
    	else {
    		super.onActivityResult(requestCode, resultCode, data);
    	}
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent keyEvent){
    	switch(keyCode){
    	case KeyEvent.KEYCODE_DPAD_CENTER:
    		if(mShowingConnectReason){
//    			chooseConnectOption();
    			return true;
    		}
    		break;
    	default:
    		break;
    	}
    	return super.onKeyUp(keyCode, keyEvent);
    }
    
    @Override
    public void onBackPressed(){
    	super.onBackPressed();
    }
    
    private void chooseConnectOption(){
    	Intent intent = null;
    	if((mConnectionState == 2) || (mBTType == 1 && mConnectionState == 1)){ //if connected or connecting(iPhone)
            if(!"".equals(mBTAddress)){
                // has connected device, ask for disconnecting or not
                intent = new Intent(this, DisconnectSmartphoneActivity.class);
            }
        }else{ // disconnected
            if("".equals(mBTAddress)){
            	// no any paired device, ask for pairing and connecting
            	boolean firstTimeConnecting = Settings.System.getInt(getContentResolver(), HAS_CONNECTED_BEFORE, 0) == 0; 
            	if(firstTimeConnecting){
            		Settings.System.putInt(getContentResolver(), HAS_CONNECTED_BEFORE, 1);
	            	Intent i = new Intent(this, ConnectConfirmationActivity.class);
	        		startActivityForResult(i, REQUEST_CODE);
	        		return;
            	}
            	else {
            		intent = new Intent(this, ChooseDeviceActivity.class);
            	}
            }else{
                // ask for reconnecting or connect other device
                intent = new Intent(this, ReconnectSmartphoneActivity.class);
            }
        }
	    CommonUtils.launchNew(this,intent,true);
		overridePendingTransition(R.anim.fade_slide_in_bottom,0);
    }
    
    private void showTutorialOnItemHost(){
    	if(isCalledFromInitialSetup()){
    		Settings.System.putInt(getContentResolver(), "com.reconinstruments.itemhost.SHOULD_SHOW_COACHMARKS", 1);
    	}
    }
    
    public static boolean isCalledFromInitialSetup(){
    	return sCalledFromInitialSetup;
    }
}
