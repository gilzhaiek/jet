package com.reconinstruments.mapsdk.mapfragment.subclass;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.reconinstruments.mapsdk.R;
import com.reconinstruments.mapsdk.mapfragment.MapFragment;
//import com.reconinstruments.dashelement1.ColumnElementFragmentActivity;

public class MapFragment_Zoom extends MapFragment {
	private static final String 	TAG = "MapFragment_Zoom";
	ImageView 			mZoomImgView = null;

	public MapFragment_Zoom() {
		super();
	}
	
	@SuppressWarnings("deprecation")
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState,  int layoutID) {
		FrameLayout frameLayout = new FrameLayout(getActivity());
		mOverlayView = inflater.inflate(R.layout.overlay_text, null);
	    View view = super.onCreateViewBase(inflater, container, savedInstanceState, layoutID);
	    mZoomImgView = (ImageView) view.findViewById(R.id.zoom_pan_icons);
	    gotoMessageMode();
	    return view;
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		FrameLayout frameLayout = new FrameLayout(getActivity());
		mOverlayView = inflater.inflate(R.layout.overlay_text, null);
	    View view = super.onCreateViewBase(inflater, container, savedInstanceState, R.layout.fragment_map_zoom);
	    mZoomImgView = (ImageView) view.findViewById(R.id.zoom_pan_icons);
	    gotoMessageMode();
	    
//	    if(mEnableOverlayText && (null != mOverlayView)){
//	    	Log.d(TAG, "Overlay showing");
//		    TextView overlayText = (TextView) mOverlayView.findViewById(R.id.overlay_text_id);
//		    overlayText.setText("This is some overlay text");
//		    overlayText.setTextSize(20f);
//		    overlayText.setTextColor(Color.WHITE);
//		    frameLayout.addView(view);
//		    frameLayout.addView(mOverlayView);
//		    return frameLayout;
//	    }
//	    else if(null != mOverlayView){
//	    	Log.e(TAG, "Can't inflate overlay_text.xml");
//	    }
//	    else {
//	    	Log.d(TAG, "Overlay text disabled!");
//	    }
//	    
//	    mMapView.setOverlayView(mOverlayView);
	    return view;
	}
	
	@Override
	public void ConfigurePostMapViewInit() {
		super.ConfigurePostMapViewInit();  // sets mMapView
		mMapView.SetCameraToFollowUser(true);
		mMapView.SetCameraToRotateWithUser(true);
		mMapView.SetCameraToPitchWithUser(true);
		mMapView.ShowUserIcon(true);
	}

	final Runnable HideZoomImage = new Runnable() {
	    public void run() {
			mZoomImgView.setVisibility(View.GONE);
	    }
	};

	final Runnable ShowZoomImage = new Runnable() {
	    public void run() {
			mZoomImgView.setVisibility(View.VISIBLE);
	    }
	};

	@Override
	protected void gotoMessageMode() {
		super.gotoMessageMode();
		getActivity().runOnUiThread(HideZoomImage);
	}
	
	@Override
	protected void gotoLoadMapMode() {
		super.gotoLoadMapMode();
		getActivity().runOnUiThread(HideZoomImage);
	}
	
	@Override
	protected void gotoBaseMapMode() {
		super.gotoBaseMapMode();
//		getActivity().runOnUiThread(ShowZoomImage);
		getActivity().runOnUiThread(HideZoomImage);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
			mMapView.ZoomCameraIn();	
			return true;
		}
//		if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
//			mMapView.ZoomCameraIn();	
//			return true;
//		}
//		else if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
//			mMapView.ZoomCameraOut();
//			return true;
//		}
		return false;

	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
	    return false;
	}
	
	
}
