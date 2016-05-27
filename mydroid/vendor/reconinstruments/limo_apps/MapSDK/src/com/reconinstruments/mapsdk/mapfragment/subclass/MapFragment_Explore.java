package com.reconinstruments.mapsdk.mapfragment.subclass;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.reconinstruments.mapsdk.R;
import com.reconinstruments.mapsdk.mapfragment.MapFragment;
import com.reconinstruments.mapsdk.mapfragment.MapFragment.IReconMapFragmentCallbacks;
import com.reconinstruments.mapsdk.mapview.MapView;

public class MapFragment_Explore extends MapFragment {
	private static final String 	TAG = "MapFragment_Explore";

	public MapFragment_Explore() {
		super();
	}
	
	public enum PanDirection {
		NOT_SET,
		UP,
		DOWN,
		LEFT,
		RIGHT
	}
	
	public static enum MapMode {
		INITIALIZING,
		NO_MAP,
		USER_CENTERED,
		EXPLORE,
		FIND
	}

	public MapMode 		mMapMode = MapMode.INITIALIZING;
	LinearLayout		mRolloverContainer = null;
	ImageView 			mPanImgView = null;
	ImageView 			mZoomPanImgView = null;
	ImageView 			mExploreImgView = null;
	TextView			mRolloverTextView = null;
	TextView			mRolloverDistance = null;
	
	PanDirection 		mPanDirection = PanDirection.NOT_SET;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");
	    View view = super.onCreateViewBase(inflater, container, savedInstanceState, R.layout.fragment_map_explore);
	    mRolloverContainer = (LinearLayout) view.findViewById(R.id.rollover_container);
	    mZoomPanImgView = (ImageView) view.findViewById(R.id.zoom_pan_icons);
	    mExploreImgView = (ImageView) view.findViewById(R.id.find_explore_icon);
	    mRolloverTextView = (TextView) view.findViewById(R.id.rollover_textview);
	    mRolloverDistance = (TextView) view.findViewById(R.id.rollover_distance_text);

	    gotoMessageMode();

	    return view;
	}
	
	@Override
	public void ConfigurePreMapViewInit() {  // called from end of MapView onCreateView()
		mMapView.SetPreloadMultiplier(1.5f);	// 
	}

	@Override
	public void ConfigurePostMapViewInit() {
		super.ConfigurePostMapViewInit();  // sets mMapView
		mMapView.SetCameraToFollowUser(true);
		mMapView.SetCameraToRotateWithUser(true);
		mMapView.SetCameraToPitchWithUser(false);
		mMapView.ShowUserIcon(true);
	}

	@Override
	protected void gotoMessageMode() {
		Log.d(TAG, "entering Message mode");
		mMapMode = MapMode.NO_MAP;

		super.gotoMessageMode();
	}
	
	@Override
	protected void gotoLoadMapMode() {
		Log.d(TAG, "entering Load Map mode");
		mMapMode = MapMode.NO_MAP;

		super.gotoLoadMapMode();
	}
	
	final Runnable SetExploreBaseMapModeUIElements = new Runnable() {
	    public void run() {
//			mPanImgView.setVisibility(View.GONE);
			mZoomPanImgView.setVisibility(View.GONE);
			mExploreImgView.setVisibility(View.VISIBLE);
			
			mMessageTextView.setVisibility(View.GONE);
			mProgressBar.setVisibility(View.GONE);
			mActiveProgressBar.setVisibility(View.GONE);

			mMapView.SetCameraToFollowUser(true);
			mMapView.SetCameraToRotateWithUser(true);
			mMapView.SetCameraToPitchWithUser(false);
			mMapView.invalidate();
	    }
	};

	@Override
	protected void gotoBaseMapMode() {
		mMapMode = MapMode.USER_CENTERED;
		super.gotoBaseMapMode();
		
		getActivity().runOnUiThread(SetExploreBaseMapModeUIElements);
	}
	
	final Runnable SetExploreModeUIElements = new Runnable() {
	    public void run() {
			mMapMode = MapMode.EXPLORE;
			
			mMapView.SetScreenStatusBarHeight(0);
			mMapVisible = true;
//			mPanImgView.setVisibility(View.VISIBLE);
			mZoomPanImgView.setVisibility(View.VISIBLE);
			mExploreImgView.setVisibility(View.GONE);
			mMessageTextView.setVisibility(View.GONE);
			mProgressBar.setVisibility(View.GONE);
			mActiveProgressBar.setVisibility(View.GONE);
			
			mMapView.SetCameraToFollowUser(false);
		    mMapView.SetCameraToRotateWithUser(false);
			mMapView.SetCameraToPitchWithUser(false);
		    mMapView.ShowClosestItemDescription(true);
		    mMapView.ShowGrid(true);
		    mMapView.invalidate();
	    }
	};

	protected void gotoExploreMode() {
		Log.d(TAG, "entering Explore mode");
		mMapView.setExploreModeEnabled(true);
		getActivity().runOnUiThread(SetExploreModeUIElements);
		mFragmentMode = "Explore";
		getActivity().runOnUiThread(super.NotifyActivityOfModeChange);
	}
	
//	private void gotoFindMode() {
//		mMapMode = MapMode.FIND;
//		Log.d(TAG, "entering Find mode");
//		
//		if(mJetOrLimoHardware) {
//			mPanImgView.setVisibility(View.GONE);
//			mZoomImgView.setVisibility(View.GONE);
//
//			mExploreImgView.setVisibility(View.GONE);
//		}		
//	    mMapView.ShowClosestItemDescription(false);
//	    mMapView.ShowGrid(true);
//	    mMapView.MapRotates(false);
//		mMapView.invalidate();
//		
//	}

	@Override
	public boolean onBackPressed() { 
		if(mMapMode != MapMode.EXPLORE) { 
			if(mMapView != null) {
				mMapView.UserLeavingApp();
			}
			return false;
		}
		else {
			gotoBaseMapMode();
			return true;  // back key handled
		}
	}
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
	    if (mMapMode != MapMode.EXPLORE && !(keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
			return false;
	    }

//		Log.d(TAG, "keydown: "+mMapMode);
		switch(mMapMode) {
		case INITIALIZING:		// will only get here with KEYCODE_DPAD_CENTER
			break;
		case NO_MAP:			// will only get here with KEYCODE_DPAD_CENTER
			break;
		case USER_CENTERED:		// will only get here with KEYCODE_DPAD_CENTER
			Log.d(TAG, "dpad go to explore mode");
			gotoExploreMode();
			break;
		case EXPLORE:
			switch(keyCode) {
			case KeyEvent.KEYCODE_DPAD_UP:
//				Log.d(TAG, "dpad pan up pressed "+mPanDirection);
				if(!mMapView.mCameraFollowsUser) {
					if(mPanDirection == PanDirection.NOT_SET) {
						mPanDirection = PanDirection.UP;
					}
					mMapView.Pan(MapView.CameraPanDirection.UP);
				}
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
//				Log.d(TAG, "dpad pan down pressed "+mPanDirection);
				if(!mMapView.mCameraFollowsUser) {
					if(mPanDirection == PanDirection.NOT_SET) {
						mPanDirection = PanDirection.DOWN;
					}
					mMapView.Pan(MapView.CameraPanDirection.DOWN);
				}
				break;
			case KeyEvent.KEYCODE_DPAD_LEFT:
//				Log.d(TAG, "dpad pan left pressed "+mPanDirection);
				if(!mMapView.mCameraFollowsUser) {
					if(mPanDirection == PanDirection.NOT_SET) {
						mPanDirection = PanDirection.LEFT;
					}
					mMapView.Pan(MapView.CameraPanDirection.LEFT);
				}
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				//				Log.d(TAG, "dpad pan right pressed "+mPanDirection);
				if(!mMapView.mCameraFollowsUser) {
					if(mPanDirection == PanDirection.NOT_SET) {
						mPanDirection = PanDirection.RIGHT;
					}
					mMapView.Pan(MapView.CameraPanDirection.RIGHT);
				}
				break;
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_ENTER:
//				Log.d(TAG, "dpad cycle zoom pressed");
				mMapView.ZoomCameraIn();	//TODO possibly do something with scale??
				break;
			}
			return true;
		}
		return true;

	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
	    if (mMapMode != MapMode.EXPLORE) {
	    	return false;
	    }
		
		switch(keyCode) {
		case KeyEvent.KEYCODE_DPAD_UP:
		case KeyEvent.KEYCODE_DPAD_DOWN:
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			mMapView.FreezePan();
			break;
		case KeyEvent.KEYCODE_BACK:
			gotoBaseMapMode();
			break;
		}
		return true;

	}
	
	public LinearLayout getRolloverContainer(){
		return mRolloverContainer;
	}
	
	public TextView getRolloverTextView(){
		return mRolloverTextView;
	}
	
	public TextView getRolloverDistance(){
		return mRolloverDistance;
	}
	
//	@Override
//	protected void handleHeadingChange() {
//		if(mMapMode == MapMode.USER_CENTERED) {
//			mMapView.SetCameraHeading(mUserHeading);
//		}
//	}
	

}
