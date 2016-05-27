package com.reconinstruments.dashlauncher;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;

import com.reconinstruments.dashlauncher.livestats.LiveStatsActivity;
import com.reconinstruments.dashlauncher.music.MusicControllerActivity;

public abstract class BoardActivity extends FragmentActivity {
	protected static final String TAG = "BoardActivity";
	
	public class Board {
		public Board(Class<?> activityClass, String name, int breadcrumbIcon)
		{
			this.activityClass = activityClass;
			this.name = name;
			this.breadcrumbIcon = breadcrumbIcon;
		}
		public Board(String packageName,String className, String name, int breadcrumbIcon)
		{
			this.packageName = packageName;
			this.className = className;
			this.name = name;
			this.breadcrumbIcon = breadcrumbIcon;
		}
		public Intent getIntent(Context context){
			Intent intent = new Intent();
			if(activityClass!=null){
				intent.setClass(context, activityClass);
			}
			else{
				intent.setClassName(packageName, className);
			}
			return intent;
		}
		String packageName,className;
		Class<?> activityClass;
		String name;
		int breadcrumbIcon;
	}
	Board music = new Board(com.reconinstruments.dashlauncher.music.MusicActivity.class,"Music",BreadcrumbView.OTHER_ICON);
	Board radar = new Board(com.reconinstruments.dashlauncher.radar.ReconRadarActivity.class, "Radar",BreadcrumbView.OTHER_ICON);
	Board notifications = new Board(com.reconinstruments.dashlauncher.notifications.NotificationsActivity.class,"Notifications",BreadcrumbView.OTHER_ICON);
	Board phone = new Board("com.reconinstruments.phone","com.reconinstruments.phone.PhoneActivity","Phone",BreadcrumbView.OTHER_ICON);
	Board liveStats = new Board(com.reconinstruments.dashlauncher.livestats.LiveStatsActivity.class,"Live Stats",BreadcrumbView.DASH_ICON);
        Board apps = new Board(com.reconinstruments.dashlauncher.applauncher.AppsOrSettingsActivity.class,"Apps/Settings",BreadcrumbView.APP_ICON);
	static ArrayList<Board> boards;
	static boolean showingPhoneBoards;
	
	// Breadcrumb toast and view
	protected static Toast breadcrumbToast;
	protected static BreadcrumbView mBreadcrumbView;
	
	@Override
	protected void onCreate(Bundle arg0)
	{
		super.onCreate(arg0);

		if(boards==null){
			loadBoards();
		}
	}
	public void loadBoards(){
		showingPhoneBoards = shouldShowPhoneBoards();
		
		boards = new ArrayList<Board>();

		if(showingPhoneBoards) boards.add(music);
		boards.add(radar);
		//if(showSmartPhoneBoards) boards.add(phone);
		boards.add(liveStats);
		boards.add(notifications);
		boards.add(apps);
	}
	public boolean shouldShowPhoneBoards(){
		String disableSmartPhone = Settings.System.getString(getContentResolver(), "DisableSmartphone" );
		return !(disableSmartPhone!=null&&disableSmartPhone.equals("true"));
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();

		//Debug.startMethodTracing("/mnt/storage/board.trace");
	}
	public void onResume() {
		super.onResume();
		
		boolean showPhoneBoards = shouldShowPhoneBoards();
		if(showPhoneBoards!=showingPhoneBoards){
			loadBoards();
		}
		//showBreadcrumb(getCurrentPosition());
		//Debug.stopMethodTracing();
	}
	public Board getBoard(int position){
		return boards.get(position);
	}
	
	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event)
	{
		if(keyCode==KeyEvent.KEYCODE_DPAD_CENTER){
			Log.d(TAG, "Long pressed center key!");
			showMusicController();
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}
	public OnLongClickListener musicShortcut = new OnLongClickListener(){
		public boolean onLongClick(View arg0)
		{
			showMusicController();
			return true;
		}
    };
    public OnItemLongClickListener itemMusicShortcut = new OnItemLongClickListener(){
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
		{
			showMusicController();
			return true;
		}
    };
	public void showMusicController(){
		changeBoard(new Intent(this,MusicControllerActivity.class),ANIM_MOVE_DOWN,false);
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		int position = getCurrentPosition();
		
		switch(keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
	        event.startTracking();
	        return true;
		case KeyEvent.KEYCODE_DPAD_LEFT:
				if(position > 0) {
					showBreadcrumb(position-1);

					changeBoard(boards.get(position - 1).getIntent(this),ANIM_MOVE_LEFT,true);

				} else {
					// Indicate all the way left already
					showBreadcrumb(getCurrentPosition());
					
					View rootView = findViewById(android.R.id.content);
					Animation upShake = AnimationUtils.loadAnimation(this, R.anim.shake_left);
					rootView.startAnimation(upShake);
				}
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
				if(position < boards.size() - 1) {
					showBreadcrumb(position+1);

					changeBoard(boards.get(position + 1).getIntent(this),ANIM_MOVE_RIGHT,true);

				} else {
					// Indicate all the way right already
					showBreadcrumb(getCurrentPosition());
					
					View rootView = findViewById(android.R.id.content);
					Animation upShake = AnimationUtils.loadAnimation(this, R.anim.shake_right);
					rootView.startAnimation(upShake);
				}
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	/*public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch(keyCode) {
			case KeyEvent.KEYCODE_DPAD_LEFT:
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				return true;
			default:
				return super.onKeyUp(keyCode, event);
		}
	}*/
	
	public void onBackPressed() {
		int anim = ANIM_MOVE_DOWN;
		
		if(getCurrentPosition() < 2) {
			anim = ANIM_MOVE_RIGHT;
		} else if(getCurrentPosition() > 2) {
			anim = ANIM_MOVE_LEFT;
		}

		changeBoard(new Intent(this,LiveStatsActivity.class),anim,false);
		showBreadcrumb(2);
	}
	
	static final int ANIM_MOVE_RIGHT = 0;
	static final int ANIM_MOVE_LEFT = 1;
	static final int ANIM_MOVE_DOWN = 2;
	
	public void changeBoard(Intent nextIntent,int anim,boolean clearStack){
		
		
		if(nextIntent != null) {
			startActivity(nextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			//if(clearStack)
			//	finish(); // Hack, to make sure old screen isn't added to activity stack
			// below is a link the to flag that would do this appropriately
			// http://developer.android.com/reference/android/content/Intent.html#FLAG_ACTIVITY_CLEAR_TASK
			switch(anim){
			case ANIM_MOVE_LEFT:
				overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
				break;
			case ANIM_MOVE_RIGHT:
				overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
				break;
			case ANIM_MOVE_DOWN:
				overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
				break;
			}
		}
	}
	public int[] getBreadcrumbIcons(){
		int[] icons = new int[boards.size()];
		for(int i=0;i<boards.size();i++){
			icons[i] = boards.get(i).breadcrumbIcon;
		}
		return icons;
	}
	
	private int getCurrentPosition() {
		for(int i=0; i<boards.size(); i++) {
			if(this.getClass().equals(boards.get(i).activityClass))
				return i;
		}
		return 0;
	}
	
	private void showBreadcrumb(int pos) {
		int position = pos;

		mBreadcrumbView = new BreadcrumbView(getApplicationContext(), true, position, getBreadcrumbIcons());
		
		//mBreadcrumbView.blockCount = classes.length;
		//mBreadcrumbView.pos = position;
		mBreadcrumbView.invalidate();
		
		if(breadcrumbToast == null) 
			breadcrumbToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
		
		breadcrumbToast.setGravity(Gravity.TOP, 0, 0);
		breadcrumbToast.setView(mBreadcrumbView);
		breadcrumbToast.show();
		
	}
	
}
