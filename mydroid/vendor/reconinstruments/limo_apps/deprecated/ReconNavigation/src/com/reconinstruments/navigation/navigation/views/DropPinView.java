/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.reconinstruments.navigation.R;
import com.reconinstruments.navigation.navigation.IOverlayManager;
import com.reconinstruments.navigation.navigation.MapManager;
import com.reconinstruments.navigation.navigation.MapView;

/**
 *The view for drop a bin
 */
public class DropPinView extends MapMoveView
{
	MapManager mMapManager = null;
	
	public DropPinView( Context context, MapView mapView, IOverlayManager overlayManager, IRemoteControlEventHandler throwBack, MapManager mapManager )
	{
		super( context, mapView, overlayManager, throwBack, mapManager );
		mMapManager = mapManager;	
		this.mHiliteClosestPoi = false;
	}
	
	
	@Override
	public boolean onSelectDown( View srcView )
	{
		//drop a pin in the current center location of the map
		//int persoId = (int)(Math.random()*1000);
		String pinName = mMapManager.GetAvailablePinName("Pin-");//"Pin" + persoId;
		
        LayoutInflater factory = LayoutInflater.from(this.getContext().getApplicationContext());
        final View textEntryView = factory.inflate(R.layout.pin_name_editor, null);
        EditText pinNameEditor =  (EditText)textEntryView.findViewById(R.id.pinname_edit);       
        pinNameEditor.setText(pinName);
                
        AlertDialog dlg = new AlertDialog.Builder( new ContextThemeWrapper( this.getContext(), android.R.style.Theme_Translucent_NoTitleBar ) )
        //AlertDialog dlg = new AlertDialog.Builder( this.getContext() )
            .setView(textEntryView)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) 
                {
                	EditText nameEd =  (EditText)textEntryView.findViewById(R.id.pinname_edit);                       	
                	mMapManager.dropPin( nameEd.getText().toString() );
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .create();

/*        
        WindowManager.LayoutParams params = dlg.getWindow().getAttributes();        	
        params.x = 0;
        params.height = 100;
        params.width = 250;
        params.y = -200;
        dlg.getWindow().setAttributes(params); 
*/        
        dlg.getWindow().getAttributes().y = -150;
        dlg.show();

		return true;
	}
	
	@Override
	public void onPreShow()
	{
		//record the MapView feature so it can be restored late on
		mMapManager.recordMapFeature();
		
		//now set the feature specifically for dropping pin
		mMapManager.mMapView.clearFeatures();
		mMapManager.mMapView.setFeature(MapView.MAP_FEATURE_CENTERPIN, true);		
		mMapManager.mMapView.setFeature(MapView.MAP_FEATURE_COMPASS, true);
	}
	
	@Override
	public void onPreHide()
	{
		mMapManager.restoreMapFeature(true);
	}

}