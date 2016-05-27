/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 *This class defined a point-of-interest item for rendering in a
 *resort map
 */
package com.reconinstruments.navigation.navigation;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.reconinstruments.navigation.R;

public class ToolbarHandler
{	
	private NavigationActivity mHostActivity = null;
	private LinearLayout mToolbarView = null;
	
	public ToolbarHandler( LinearLayout toolBarView, NavigationActivity hostActivity )
	{
		mHostActivity = hostActivity;
		mToolbarView = toolBarView;
		
  
    	for( int idx = 0; idx < mToolbarView.getChildCount(); ++idx )
    	{
    		View child = mToolbarView.getChildAt(idx);
    		child.setOnClickListener(mMapViewTransformListener);
    	}

	}
	
	
	private View.OnClickListener mMapViewTransformListener = new View.OnClickListener() 
	{
		
		@Override
		public void onClick(View v) 
		{

			switch( v.getId() )
			{			
			case R.id.zoomRestore_button:
				mHostActivity.getMapView().zoomRestore( );
			break;
			
			case R.id.rotateLeft_button:
				mHostActivity.getMapView().rotate( -MapView.ROTATION_DELTA );
			break;
			
			case R.id.rotateRight_button:
				mHostActivity.getMapView().rotate( MapView.ROTATION_DELTA );
			break;
			
			case R.id.zoomIn_button:
				mHostActivity.getMapView().zoomIn();
				//findViewById(R.id.nav_bar).setVisibility(View.GONE);
/*				
				ReNetwork network = mReManager.mMap.getMajorNetwork();
				PointF testPoint = mMapView.getCenter();
				ReHitTestInfo hitTest = new ReHitTestInfo( testPoint, 100.f );
				hitTest.filterTrail(Trail.SKI_LIFT);
				hitTest.hitTest(network);
				if( hitTest.mSegmentId != -1 )
				{
					RePath path = hitTest.constructPath();
					mMapView.setPlannedRoute(path);
					mMapView.invalidate();
				}
				else
				{
					mMapView.setPlannedRoute(null);
					mMapView.invalidate();					
				}
*/				
			break;
			
			case R.id.zoomOut_button:
				mHostActivity.getMapView().zoomOut();
/*				
				ReNetwork network = mReManager.mMap.getMajorNetwork();
				Random random = new Random();
				int s = random.nextInt(network.mNodes.size()-1);
				int e = random.nextInt(network.mNodes.size()-1);
				Log.d(ReUtil.LOG_TAG_NETWORK_ROUTING, "Searching path between Node#" + s +" and Note#" + e);
				RePath path = network.findOptimalPath(network.mNodes.get(s), network.mNodes.get(e));
				if( path != null )
				{
					ReNode startNode = network.mNodes.get(s);
					mMapView.setCenter( startNode.mPosition.x, startNode.mPosition.y, false);
					mMapView.setPlannedRoute(path);
				}
				mMapView.mDrawDestination = true;
				ReNode endNode = network.mNodes.get(e);
				mMapView.setDestination( endNode.mPosition.x, endNode.mPosition.y );
*/				
			break;
			
			case R.id.zoomFit_button:
				mHostActivity.getMapView().zoomFit();				
			break;
			
			case R.id.selectPoi_button:
				//fill the PoiInfoProvider with the new point-of-interest of
				//of the map before we launch the activity
				
				PoiInfoProvider.fill(mHostActivity.getMap());
				
				Intent intent = new Intent( mHostActivity, PoiCategoryList.class );
				mHostActivity.startActivityForResult( intent, NavigationActivity.GET_POI );

				//mHostActivity.mCPoiManager.addDebugBuddies();
			break;
			
			case R.id.gps_button:
				
				mHostActivity.enableGPS( mHostActivity.isGPSEnabled() ? false : true );
				if( mHostActivity.isGPSEnabled() )
				{
					if(mHostActivity.getMapView() != null )
					{
						//let's reset the map center to origin after GPSSignal is discard
						//so that we can see the singal
						mHostActivity.getMapView().setCenter(0, 0,false);
					}
					
					Button btn = (Button)v;
					btn.setText(R.string.enable_gps);
				}
				else
				{
					Button btn = (Button)v;
					btn.setText(R.string.disable_gps);					
				}
			break;
			
			case R.id.selectResort_button:
				mHostActivity.loadAvailableResort();				
			break;
			}				
		}
	};
	
	protected void refreshToolbar()
	{   	
    	ShpMap map = mHostActivity.getMap();
		if(  map == null || map.isEmpty() )
		{
			//disable all the other buttons except the one for loading resort
	    	for( int idx = 0; idx < mToolbarView.getChildCount(); ++idx )
	    	{
	    		View child = mToolbarView.getChildAt(idx);
	    		if( child.getId() != R.id.selectResort_button )
	    		{
	    			child.setEnabled(false);
	    		}
	    	}
		}
		else
		{
	    	for( int idx = 0; idx < mToolbarView.getChildCount(); ++idx )
	    	{
	    		View child = mToolbarView.getChildAt(idx);
	    		if( child.getId() != R.id.selectResort_button )
	    		{
	    			child.setEnabled(true);
	    		}
	    	}
		}
	}

}