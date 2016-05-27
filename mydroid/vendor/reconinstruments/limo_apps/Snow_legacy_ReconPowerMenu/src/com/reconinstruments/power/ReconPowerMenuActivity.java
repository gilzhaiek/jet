package com.reconinstruments.power;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.limopm.LimoPMNative;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.reconinstruments.utils.DeviceUtils;


public class ReconPowerMenuActivity extends Activity {

	TextView title;
	ListView menuList;

	public static boolean screenOn = true;
	private static final int POWER_MODE_NORMAL = 1;
	private static final int POWER_MODE_DISPLAY_OFF = 2;
	private static final int POWER_MODE_SUSPEND = 3;

	private static final int MENU_ID_DISPLAY_OFF = 0;
	private static final int MENU_ID_STAND_BY = 1;
	private static final int MENU_ID_POWER_OFF = 2;

	// JIRA: MODLIVE-782 Add Connect to smart phone to power menu
	private static final int MENU_ID_SMARTPHONE_CONNECTION = 3;
	// End of JIRA: MODLIVE-782

	private static final String[] MENU_ITEM_TITLES = {"DISPLAY SLEEP", "STANDBY", "POWER OFF", "SMARTPHONE"};
	private static final String TAG = "ReconPowerMenuActivity";

	protected int mMenuItemViewResource = R.layout.menu_item;
	ArrayList<Object> mMenuItems;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);


		title = (TextView) findViewById(R.id.pop_up_title);
		screenOn = true;

		mMenuItems = new ArrayList<Object>();
		mMenuItems.add(new MenuItem(MENU_ITEM_TITLES[MENU_ID_DISPLAY_OFF], getResources().getDrawable(R.drawable.sleep_selectable), MENU_ID_DISPLAY_OFF));
		// mMenuItems.add(new MenuItem(MENU_ITEM_TITLES[MENU_ID_STAND_BY], null, MENU_ID_STAND_BY));
		mMenuItems.add(new MenuItem(MENU_ITEM_TITLES[MENU_ID_SMARTPHONE_CONNECTION], getResources().getDrawable(R.drawable.smartphone_selectable), MENU_ID_SMARTPHONE_CONNECTION));
		mMenuItems.add(new MenuItem(MENU_ITEM_TITLES[MENU_ID_POWER_OFF], getResources().getDrawable(R.drawable.power_selectable), MENU_ID_POWER_OFF));

		menuList = (ListView) findViewById(R.id.power_options_list);
		menuList.setOnItemClickListener(mItemClickListener);
		//       menuList.setOnItemSelectedListener(mItemSelectedListener);
		menuList.setAdapter(new MenuViewAdapter(this, mMenuItemViewResource, R.id.menuview_item_text, mMenuItems));
		menuList.setOnKeyListener(mOnKeyListener);
		menuList.setItemsCanFocus(true);
		menuList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);


		menuList.setSelected(true);
		menuList.setFocusableInTouchMode(true);
		menuList.requestFocus();
		menuList.smoothScrollToPosition (0);
		menuList.setSelection(0);
		menuList.setItemChecked(0, true);
	}

	private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener( )
	{

		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			//Hide the menu view, then handle the menu-item selection action
			//mOverlayManager.setOverlayView(null);    		
			MenuItem item = (MenuItem)mMenuItems.get(position);
			Log.v("POWER", "onItemClick");

			switch(item.mID) {
			case MENU_ID_DISPLAY_OFF: initDisplayOff();
			break;

			case MENU_ID_STAND_BY: initStandBy();
			break;

			case MENU_ID_POWER_OFF: initPowerOff();
			break;

			case MENU_ID_SMARTPHONE_CONNECTION: initSmartphoneConnection();
			break;

			}
		}

	};

	private AdapterView.OnKeyListener mOnKeyListener = new AdapterView.OnKeyListener( )	{
		// This function makes sure that keypress is only used to
		// wake the screen up, when the screen is off.
		public boolean onKey(View view, int keycode, KeyEvent event)  {
		    if (DeviceUtils.isLimo()) {
				if (!screenOn){
					Log.d(TAG,"Back to normal power mode");
					screenOn = true;
					LimoPMNative.SetPowerMode(POWER_MODE_NORMAL);
					finish();
					return true;
				}
				else{
					return false;
				}
			}
			return false;
		}

	};

	private void initStandBy() {
		Toast.makeText(this, MENU_ITEM_TITLES[MENU_ID_STAND_BY], Toast.LENGTH_LONG);
		// Log.v(TAG,"initStandby: suspend mode");
		// System.setProperty("suspend.mode", "1");
		// Log.v(TAG,"suspend mode is"+System.getProperty("suspend.mode", "1"));
		screenOn = false; 

		if (DeviceUtils.isLimo()) {
			LimoPMNative.SetPowerMode(POWER_MODE_SUSPEND);		
		}
		else {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			pm.goToSleep(System.currentTimeMillis()); // add 10 seconds to let other apps go sleeping
		}

		Log.d(TAG, "initStandBy");

	}

	// JIRA: MODLIVE-782 Add Connect to smart phone to power menu
	private void initSmartphoneConnection() {
		Log.d(TAG, "initSmartphoneConnection");
		Intent intent = new Intent("com.reconinstruments.connectdevice.CONNECT");
		startActivity(intent);
		finish();
	}
	// End of JIRA: MODLIVE-782

	private void initDisplayOff() {
		Toast.makeText(this, MENU_ITEM_TITLES[MENU_ID_DISPLAY_OFF], Toast.LENGTH_LONG);
		// Log.v(TAG,"initDisplayOff: display_off mode");
		// System.setProperty("display_off.mode", "1");
		// Log.v(TAG,"display_off mode is"+System.getProperty("display_off.mode", "1"));
		screenOn= false;
		if (DeviceUtils.isLimo()) {
			LimoPMNative.SetPowerMode(POWER_MODE_DISPLAY_OFF);
		}
		else {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			pm.goToSleep(System.currentTimeMillis()); // add 10 seconds to let other apps go sleeping
			finish();
		}
		Log.d(TAG, "initDisplayOff");
	}

	private void initPowerOff() {
		Log.d(TAG, "initPowerOff");
		finish();
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		pm.reboot("BegForShutdown"); // Magic reason that causes shutdown as opposed to reboot

	}

	private AdapterView.OnItemSelectedListener mItemSelectedListener = new AdapterView.OnItemSelectedListener()
	{

		@Override
		public void onItemSelected(AdapterView parent, View view, int position, long id)
		{
			MenuItem item = (MenuItem)mMenuItems.get(position);
			int[] state = new int[] {android.R.attr.state_window_focused, android.R.attr.state_focused, android.R.attr.state_selected};
			Log.e("mItemSelectedListener", "setState");
			item.mIcon.setState(state);
		}

		@Override
		public void onNothingSelected(AdapterView parent) 
		{
			Log.v("POWER", "onNothingSelected");
		}

	};


	/**
	 * 
	 * The interface for supplying the MenuView
	 * with icon for an MenuItem.
	 *
	 */
	public class MenuItem
	{
		public String mText;
		public  Drawable mIcon = null;
		public int mID = -1;
		public MenuItem( String menuText, Drawable menuIcon, int id)
		{
			mText = menuText;
			mIcon = menuIcon;
			mID = id;
		}


	};
	/*
	 * private class for defining an ArrayAdapter that has it own view of list item
	 * 
	 */
	protected class MenuViewAdapter extends ArrayAdapter<Object>
	{
		private ArrayList<Object> mMenuItems;

		public MenuViewAdapter( Context context, int resourceId, int textViewResourceId, ArrayList<Object> menuItems)
		{
			super( context, resourceId, textViewResourceId, menuItems );
			mMenuItems = menuItems;
		}

		@Override
		public View getView( int position, View convertView, ViewGroup parent )
		{
			View v = convertView;
			if( v == null )
			{
				//create a new view from the poiCategoryitem_layout
				LayoutInflater inflater = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(mMenuItemViewResource, null);						
			}

			setMenuItemViewValue( position, v );
			return v;

		}

	}

	protected void setMenuItemViewValue( int idx, View view  )
	{
		MenuItem menuItem = (MenuItem)mMenuItems.get(idx);

		TextView title = (TextView)view.findViewById(R.id.menuview_item_text);
		title.setText( menuItem.mText);

		if( menuItem.mIcon != null )
		{
			ImageView icon = (ImageView)view.findViewById(R.id.menuview_item_icon);
			icon.setImageDrawable( menuItem.mIcon );
			if(idx == 0){
				icon.setSelected(true);
			}else{
				icon.setSelected(false);
			}
		}
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		//     String sus = System.getProperty("suspend.mode","1");
		//        	String doff = System.getProperty("display_off.mode", "1");
		// if (!sus.equals("0")){
		//          		System.setProperty("suspend.mode", "0");		
		//           		Log.v(TAG,"Keydown suspend mode is "+System.getProperty("suspend.mode", "1"));		
		// }
		// if (!doff.equals("0")){
		//        		System.setProperty("display_off.mode", "0");			
		//        		Log.v(TAG,"Keydown display_off mode is "+System.getProperty("display_off.mode", "1"));		
		// }

	    if (DeviceUtils.isLimo()) {
			if (!screenOn){
				screenOn = true;
				LimoPMNative.SetPowerMode(POWER_MODE_NORMAL);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
}
