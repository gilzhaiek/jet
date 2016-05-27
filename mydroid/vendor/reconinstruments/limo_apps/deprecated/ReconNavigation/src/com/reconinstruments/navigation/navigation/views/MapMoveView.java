/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation.views;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.os.Handler;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.reconinstruments.navigation.R;
import com.reconinstruments.navigation.navigation.IOverlayManager;
import com.reconinstruments.navigation.navigation.MapManager;
import com.reconinstruments.navigation.navigation.MapView;
import com.reconinstruments.navigation.navigation.PoInterest;

/**
 *The view for move a map
 */

public class MapMoveView extends OverlayView
{
	static public final float MOVEMENT_DELTA_X = 20.f;
	static public final float MOVEMENT_DELTA_Y = 20.f;

	protected MapView mMapView = null;
	protected MapManager mMapManager = null;
	protected boolean mHiliteClosestPoi = true;						//turn on/off hi-lite closest poi around the center
	protected PoInterest mClosestPoi = null; 

    boolean mSelectKeyHandled = true;	// select 
    boolean mKeyDownLast = false;
    Handler mHandler;		// handler for handling select key hold
    Context crap = null;

	public MapMoveView( Context context, MapView mapView, IOverlayManager overlayManager, IRemoteControlEventHandler throwBack, MapManager mapManager )
	{
		super( context, overlayManager, throwBack );
		mThrowBack = throwBack;
		mMapView = mapView;
		mMapManager = mapManager;

		//inflate a view from predefined xml file
        LayoutInflater factory = LayoutInflater.from(context);
        View navView = factory.inflate(R.layout.map_move_view, null);
        this.addView(navView);       

        // for select key hold
        mHandler = new Handler();
	}
	
	@Override
	public boolean onDownArrowDown( View srcView )
	{
		moveMap(0, -MOVEMENT_DELTA_Y);	
		return true;
	}
	
	@Override
	public boolean onUpArrowDown( View srcView )
	{			
		moveMap(0, MOVEMENT_DELTA_Y);
		return true;
	}
	

	@Override
	public boolean onLeftArrowDown( View srcView )
	{
		moveMap(MOVEMENT_DELTA_X, 0);
		return true;
	}

	@Override
	public boolean onRightArrowDown( View srcView )
	{
		moveMap(-MOVEMENT_DELTA_X, 0);
		return true;
	}
		
	@Override
	public boolean onSelectDown( View srcView )
	{
		if(mKeyDownLast)
		{
			return true;
		}
		mKeyDownLast = true;
		
		mSelectKeyHandled = false;
	    mHandler.postDelayed(mSelectLongPress, 1000);
	    return true;
	}

    @Override
	public boolean onSelectUp ( View srcView )
    {
    	mKeyDownLast = false;
    	
	    if (!mSelectKeyHandled) {
			mHandler.removeCallbacks(mSelectLongPress);
			this.mThrowBack.onSelectDown(srcView);
	    }
	    return true;
	}
	
	@Override
	public void onPreShow( )
	{		
		mSelectKeyHandled = true;
		mMapManager.recordMapFeature();
		mMapView.clearFeatures();
		mMapView.setFeature(MapView.MAP_FEATURE_COMPASS, true);
		mMapView.setForceDrawPOIs(true);
		hilitePoi();
	}
	
	@Override
	public void onPreHide()
	{
		mMapView.setForceDrawPOIs(false);
		mMapManager.restoreMapFeature(false);
		if( mHiliteClosestPoi )
		{
			mMapManager.clearHilitedPoi();
		}
	}
	
	protected AlertDialog GetSelectDialog(View textEntryView)
	{
		return new AlertDialog.Builder( new ContextThemeWrapper( this.getContext(), android.R.style.Theme_Translucent_NoTitleBar ) )
            .setView(textEntryView)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) 
                {
                	if(mClosestPoi.getType() == PoInterest.POI_TYPE_CDP)
                	{
                		mMapManager.removePin(mClosestPoi);
                	}
                	else
                	{
                		mClosestPoi.toggleStatus();
                		mMapManager.Save();
                		mMapManager.mMapView.invalidate();
                	}
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            })
            .create();	
	}	 	
	
	public void ShowTrackingDialog()
	{
		LayoutInflater factory = LayoutInflater.from(this.getContext().getApplicationContext());
        final View textEntryView = factory.inflate(R.layout.pin_delete_view, null);
        TextView msgView  =  (TextView)textEntryView.findViewById(R.id.msg_view);       
                
        if(mClosestPoi.getType() == PoInterest.POI_TYPE_CDP)
        {
        	msgView.setText("Delete  " + mClosestPoi.mName);
        }
        else if(mClosestPoi.getStatus() != PoInterest.POI_STATUS_TRACKED )
		{
			msgView.setText("Start tracking  " + mClosestPoi.mName);
		}
		else
		{
			msgView.setText("Stop tracking  " + mClosestPoi.mName);
		}
        

        AlertDialog dlg = GetSelectDialog(textEntryView);

        dlg.show();		
	}

    private final Runnable mSelectLongPress = new Runnable() {
        public void run() {
		    mSelectKeyHandled = true;
		    
		    if(mClosestPoi != null)
		    {
		    	ShowTrackingDialog();
		    }
			
			mHandler.removeCallbacks(mSelectLongPress);
        }
    };
	
	private void moveMap( float deltaX, float deltaY )
	{
		mMapView.move(deltaX, deltaY);
		hilitePoi();
	}
	
	private void hilitePoi()
	{
		if( mHiliteClosestPoi )
		{
			PointF center = mMapView.getCenter();
			mClosestPoi = mMapManager.hiliteClosestPoi(center.x, center.y);
		}
	}
}