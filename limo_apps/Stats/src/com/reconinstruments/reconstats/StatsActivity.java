package com.reconinstruments.reconstats;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.TextView;
import com.reconinstruments.messagecenter.ReconMessageAPI;



public class StatsActivity extends FragmentActivity {
	private static final String TAG = "StatsActivity";

	public static enum StatsType{
		LASTRUN, TODAY, ALLTIME 
	}

	public static int currentRowPosition = 0;
	public static final int RUN_POSITION = 0;
	public static final int MAX_SPD_POSITION = 1;
	public static final int VERT_POSITION = 2;
	public static final int DISTANCE_POSITION = 3;
	public static final int ALT_POSITION = 4;
	public static final int JUMP_POSITION = 5;
	
	private Gallery tabGallery;
	private StatsFragment[] listFragments;
	private String[] tabNames = {"Last Run", "Today","All Time"};
	private int selection = 0;

	private TranscendServiceConnection mTranscendConnection = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.v(TAG, this.getIntent().getAction());
		
		if(getIntent().getAction().equals("RECON_STATS_LAST_RUN")) 	selection = 0;
		if(getIntent().getAction().equals("RECON_STATS_TODAY")) 	selection = 1;
		if(getIntent().getAction().equals("RECON_STATS_ALLTIME")) 	selection = 2;

		int categoryId = getIntent().getIntExtra("category_id",-1);
		if (categoryId != -1) {
		    ReconMessageAPI.markAllMessagesInCategoryAsRead(this, categoryId);
		}
		
		setContentView(R.layout.list_gallery);

		tabGallery = (Gallery) findViewById(R.id.tab_gallery);
		tabGallery.setFocusable(false);
		tabGallery.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Log.v(TAG, "arg2: " + arg2);
				if(selection != arg2)
					setSelection(arg2, selection);

				selection = arg2;

			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		tabGallery.setAdapter(new InfiniteStringAdapter(this, tabNames));

		listFragments = new StatsFragment[] {new StatsFragment(StatsType.LASTRUN),
				new StatsFragment(StatsType.TODAY),
				new StatsFragment(StatsType.ALLTIME)};

		mTranscendConnection = TranscendServiceConnection.getInstance(this); 

		if(getIntent().hasExtra("Notification")){
			int notificationType = getIntent().getIntExtra("Notification", 0);
			switch(notificationType){
			case Notification.TYPE_ALLTIME_AIR_BEST:
			case Notification.TYPE_ALLTIME_ALT_MAX:
			case Notification.TYPE_ALLTIME_COLDEST_TEMP:
			case Notification.TYPE_ALLTIME_DISTANCE:
			case Notification.TYPE_ALLTIME_SPEED_MAX:
			case Notification.TYPE_ALLTIME_VERTICAL:
				selection = 2;
				break;
			case Notification.TYPE_LAST_AIR:
			case Notification.TYPE_LAST_RUN:
				selection = 0;
				break;
			case Notification.TYPE_TODAY_AIR_BEST:
			case Notification.TYPE_TODAY_ALT_MAX:
			case Notification.TYPE_TODAY_COLDEST_TEMP:
			case Notification.TYPE_TODAY_DISTANCE:
			case Notification.TYPE_TODAY_SPEED_MAX:
			case Notification.TYPE_TODAY_VERTICAL:
				selection = 1;
				break;
			}
		}
		tabGallery.setSelection(selection);
		setSelection(selection, -1);
	}

	@Override
	public void onStart(){
		super.onStart();

		bindService( new Intent( "RECON_MOD_SERVICE" ), mTranscendConnection, Context.BIND_AUTO_CREATE );
	}
	
	@Override
	public void onResume(){
		super.onResume();
		
		Log.v(TAG, "position extra: " + getIntent().getIntExtra("position", -1));
		
		currentRowPosition = getIntent().getIntExtra("position", 0);
	}
	

	@Override
	public void onStop(){
		super.onStop();

		unbindService(mTranscendConnection);
	}


	public void setSelection(int position, int prevPosition) {
		Log.v(TAG, "position: " + position + ", prev: " + prevPosition);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.setTransition(FragmentTransaction.TRANSIT_NONE);

		if(prevPosition >= 0) {

			ft.setCustomAnimations(prevPosition>position?R.anim.slide_in_left:R.anim.slide_in_right, prevPosition>position?R.anim.slide_out_right:R.anim.slide_out_left);
			ft.remove(listFragments[prevPosition]);
		}
		Log.v(TAG, "Position" + position);
		mTranscendConnection.setViewToUpdate(listFragments[position]);

		ft.add(R.id.details, listFragments[position]);
		ft.commit();
	}
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode) {
		case KeyEvent.KEYCODE_DPAD_LEFT:
			tabGallery.onKeyDown(keyCode, event);
			if (selection>0){
				setSelection(selection-1,selection);
				selection--;
			}
			return true;

		case KeyEvent.KEYCODE_DPAD_RIGHT:
			tabGallery.onKeyDown(keyCode, event);
			if (selection<listFragments.length-1){
				setSelection(selection+1,selection);
				selection++;
			}
			return true;
		case KeyEvent.KEYCODE_BACK:
			finish();
			return true;
		}

		return false;
	}
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
		return strings.length;
	}

	public Object getItem(int position) {
		return strings[position];
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {

		LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		TextView tabTV = (TextView) li.inflate(R.layout.gallery_tab, null);
		tabTV.setText(strings[position]);

		return tabTV;
	}
}
