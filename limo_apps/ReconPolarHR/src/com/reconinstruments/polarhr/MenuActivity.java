package com.reconinstruments.polarhr;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TableRow;
import android.widget.TextView;

public class MenuActivity extends Activity {
	public static final String TAG = "MenuActivity";
	public static final boolean D = true; //debug
	
	private ArrayList<TableRow> menu_items;
	private ArrayList<TextView> text_items;
	private int currentSelection = 0;
	// Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentSelection = mPrefs.getInt("current_selection", 0);
        
        Log.v(TAG, "currentSelection = " + Integer.toString(currentSelection));
        
        setContentView(R.layout.menu_layout);
        
        Typeface tf = Typeface.createFromAsset(this.getResources().getAssets(), "fonts/Eurostib_1.TTF");
        ((TextView) findViewById(R.id.text_live_tracking)).setTypeface(tf);
        //((TextView) findViewById(R.id.text_stats_report)).setTypeface(tf);
        ((TextView) findViewById(R.id.text_select_device)).setTypeface(tf);
        
        menu_items = new ArrayList<TableRow>();
        menu_items.add((TableRow) findViewById(R.id.table_row_live_tracking));
        //menu_items.add((TableRow) findViewById(R.id.table_row_stats_report));
        menu_items.add((TableRow) findViewById(R.id.table_row_select_device));
        
        text_items = new ArrayList<TextView>();
        text_items.add((TextView) findViewById(R.id.text_live_tracking));
        text_items.add((TextView) findViewById(R.id.text_select_device));
        
        if(currentSelection >= menu_items.size()) {
        	currentSelection = 0;
        }

    }
    public void onResume(){
    	super.onResume();
    	
        highlightMenuItem();

    }
	@Override
	public void onPause() {
		super.onPause();
		Log.v(TAG, "onPause");

		// Save dashboard view index
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putInt("current_selection", currentSelection); // value to
		editor.commit();
		
    	
	}
	
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	
    	if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
        	
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
        	
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
        	Log.v(TAG, "KEY EVENT UP");
        	currentSelection = (currentSelection + menu_items.size() - 1) % menu_items.size();        	
        	highlightMenuItem();
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
        	Log.v(TAG, "KEY EVENT DOWN");
        	currentSelection = (currentSelection + 1) % menu_items.size();
        	highlightMenuItem();
        } 
        else if (keyCode == KeyEvent.KEYCODE_BACK) {
        	finish();
        } 
        else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER|| keyCode == KeyEvent.KEYCODE_ENTER) {
        	if(currentSelection==0){
        		text_items.get(0).setTextAppearance(getApplicationContext(), R.style.white_text_shadow);
        		text_items.get(0).setShadowLayer(1.0f, 1.0f, 1.0f, Color.BLACK);
        	}else{
        		text_items.get(1).setTextAppearance(getApplicationContext(), R.style.white_text_shadow);
        		text_items.get(1).setShadowLayer(1.0f, 1.0f, 1.0f, Color.BLACK);
        	}
        	selectMenuItem();
        	
        }
        else {
        	return false;
        }
    
        return true;
    }
    
    private void highlightMenuItem() {
    	for(TableRow r : menu_items) {
    		r.setBackgroundResource(0); //remove background
    	}

    	menu_items.get(currentSelection).setBackgroundResource(R.drawable.selector_42);
    	
    	if(currentSelection==0){
    		/**
    		 * This is currently working as intended. The shadow is not considered part of the text appearance.
    		 */
    		text_items.get(0).setTextAppearance(getApplicationContext(), R.style.black_text_shadow);
    		text_items.get(0).setShadowLayer(1.0f, 1.0f, 1.0f, Color.WHITE);
    		text_items.get(1).setTextAppearance(getApplicationContext(), R.style.white_text_shadow);
    		text_items.get(1).setShadowLayer(1.0f, 1.0f, 1.0f, Color.BLACK);
    	}else{
    		text_items.get(0).setTextAppearance(getApplicationContext(), R.style.white_text_shadow);
    		text_items.get(0).setShadowLayer(1.0f, 1.0f, 1.0f, Color.BLACK);
    		text_items.get(1).setTextAppearance(getApplicationContext(), R.style.black_text_shadow);
    		text_items.get(1).setShadowLayer(1.0f, 1.0f, 1.0f, Color.WHITE);
    	}
    	
    		

    }
    
    private void selectMenuItem() {
    	if (currentSelection == 0) {
        	Intent myIntent = new Intent(this, LiveTrackingActivity.class);
        	startActivityForResult(myIntent, 0);
    	//} else if (currentSelection == 1) {
    	//	Intent myIntent = new Intent(this, StatsReportActivity.class);
        //	startActivityForResult(myIntent, 0);
    	} else if (currentSelection == 1) {
    		Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    	}
  
    }
    
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D) Log.d(TAG, "onActivityResult " + resultCode);
		
		switch (requestCode) {
		
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				((PolarApplication) getApplicationContext()).setPolarMAC(address);
			}
			break;
		}
	}
}