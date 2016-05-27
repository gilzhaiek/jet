package com.reconinstruments.dashlauncher.music.library;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector.DeviceType;
import com.reconinstruments.dashlauncher.music.MusicHelper;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.modlivemobile.music.MusicDBFrontEnd.MusicListType;

public class LibraryActivity extends FragmentActivity {

	private static final String TAG = "LibraryActivity";
	private Gallery tabGallery;
	private Fragment[] listFragments;
	//private FrameLayout fragmentFrame;
	//private TextView errorTV;
	
	private int selection = Integer.MAX_VALUE/2;
	
	private String[] tabNames = {"Playlists", "Artists", "Songs"};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.music_library_main);
		
		//errorTV = (TextView) findViewById(R.id.error);
		//fragmentFrame = (FrameLayout) findViewById(R.id.details);
		tabGallery = (Gallery) findViewById(R.id.tab_gallery);
		tabGallery.setFocusable(false);
		

		if(SmartphoneConnector.lastDevice()==DeviceType.IOS){
			
			tabGallery.setAdapter(new InfiniteStringAdapter(this, new String[]{"Playlists", "Songs"}));
			listFragments = new Fragment[] {new LibraryListFragment(MusicListType.PLAYLISTS), 
					new LibraryListFragment(MusicListType.SONGS)};
		}
		else{
			
			tabGallery.setAdapter(new InfiniteStringAdapter(this, tabNames));
			listFragments = new Fragment[] {new LibraryListFragment(MusicListType.PLAYLISTS), 
					new LibraryListFragment(MusicListType.ARTISTS), 
					new LibraryListFragment(MusicListType.SONGS)};
		}

		tabGallery.setSelection(selection+1);
		setSelection(selection+1);
		
		registerReceiver(phoneConnectionReceiver, new IntentFilter(BTCommon.MSG_STATE_UPDATED));
	}
	
	public void onDestroy() {
		super.onDestroy();
		
		unregisterReceiver(phoneConnectionReceiver);
	}
	
	public void onResume() {
		super.onResume();
		MusicHelper.setInMusicApp(true);
	}
	public void onPause() {
		super.onPause();
		MusicHelper.setInMusicApp(false);
	}
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		int newSelection;
		switch(keyCode) {
		case KeyEvent.KEYCODE_DPAD_LEFT:
			newSelection = selection-1;
			tabGallery.setSelection(newSelection, true);
			setSelection(newSelection);
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			newSelection = selection+1;
			tabGallery.setSelection(newSelection, true);
			setSelection(newSelection);
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	public void onBackPressed() {
		super.onBackPressed();
		
		this.overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
	}
	
	public void setSelection(int newSelection) {
		if(newSelection==selection) return;
		
		int last = selection%listFragments.length;
		int next = newSelection%listFragments.length;
		
		Log.v(TAG, "position: " + next + ", prev: " + last);
		
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.setTransition(FragmentTransaction.TRANSIT_NONE);
		
		if(last >= 0) {
			ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
			ft.remove(listFragments[last]);
		}

		ft.add(R.id.details, listFragments[next]);		
		ft.commit();
		
		selection = newSelection;
	}
	
	class InfiniteStringAdapter extends BaseAdapter {
	     
	    /** The context your gallery is running in (usually the activity) */
	    private Context mContext;
	    private final String[] strings;
	 
	    public InfiniteStringAdapter(Context c, String[] strings) {
	        this.mContext = c;
	        this.strings = strings;
	    }
	 
	    /**
	     * The count of how many items are in this Adapter
	     * This will return the max number as we want it to scroll as much as possible
	     */
	    public int getCount() {
	        return Integer.MAX_VALUE;
	    }
	 
	    public Object getItem(int position) {
	        return strings[position % strings.length];
	    }
	 
	    public long getItemId(int position) {
	        return position;
	    }
	 
		public View getView(int position, View convertView, ViewGroup parent) {
			int itemPos = (position % strings.length);
			
			LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			TextView tabTV = (TextView) li.inflate(R.layout.music_library_tab, null);
			tabTV.setText(strings[itemPos]);
			
			return tabTV;
		}
	}
	BroadcastReceiver phoneConnectionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v(TAG, "action: " + intent.getAction());
			
			if (intent.getAction().equals(BTCommon.MSG_STATE_UPDATED)) {
				boolean isConnected = intent.getBooleanExtra("connected", false);
				Log.v(TAG, isConnected ? "Phone Connected" : "Phone Not Connected");
				
				if(!isConnected)
					finish();
			}
		}
	};
}
