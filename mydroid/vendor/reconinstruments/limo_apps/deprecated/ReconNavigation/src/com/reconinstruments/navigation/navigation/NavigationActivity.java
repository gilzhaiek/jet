package com.reconinstruments.navigation.navigation;


import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.reconinstruments.navigation.R;
import com.reconinstruments.navigation.navigation.dal.MapManagerDAL;
import com.reconinstruments.navigation.navigation.datamanagement.CountryInfo;
import com.reconinstruments.navigation.navigation.datamanagement.CountryListActivity;
import com.reconinstruments.navigation.navigation.datamanagement.RegionInfo;
import com.reconinstruments.navigation.navigation.datamanagement.ResortDBLoadTask;
import com.reconinstruments.navigation.navigation.datamanagement.ResortInfo;
import com.reconinstruments.navigation.navigation.datamanagement.ResortInfoProvider;
import com.reconinstruments.navigation.navigation.menuHandler.RootMenuHandler;
import com.reconinstruments.navigation.navigation.views.IRemoteControlEventHandler;
import com.reconinstruments.navigation.navigation.views.MapMoveView;
import com.reconinstruments.navigation.navigation.views.MapZoomView;
import com.reconinstruments.navigation.navigation.views.OverlayView;
import com.reconinstruments.navigation.navigation.views.RootTabView;
import com.recon.shp.ShpContent;


public class NavigationActivity extends Activity
{
    /** Called when the activity is first created. */
	ShpContent mShpContent = null;
	
	static final int MAP_VIEW_MODE_0 = 0;
	static final int MAP_VIEW_MODE_LAST = 3;
		
	//temporary flag for disabling/enabling gps signal to reset the map location 
	private boolean mDiscardGPSSignal = false;

	private MapView mMapView = null;		
	private GPSListener mGPSListener = null;
	private ToolbarHandler mToolbarHandler = null;
	private MapManager mMapManager = null;
	private BuddyInfoReceiver mBuddyInfoReceiver = null;	//the receiver for receiving buddy information broadcasted by FrontEndService	
	private boolean mToolbarOn = true;
	
		
	private MapMoveView mMapMoveView = null;
	private MapZoomView mMapZoomView = null;
	
	private RootMenuHandler mRootMenuHandler = null;
	
	private int mCurrentMapMode = MAP_VIEW_MODE_0;
	
	private TranscendServiceConnection mTranscendConnection = null;
	
	private OverlayManager mOverlayManager = null;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        //setTheme( android.R.style.Theme_Light_NoTitleBar );
        //setTheme( R.style.testTextViewStyle );
                          
 	    setContentView(R.layout.phone_layout);
 	
 	    ViewGroup rootView = (ViewGroup) this.findViewById(R.id.root_view); 	   
 	    
 	    mOverlayManager = new OverlayManager( rootView );
 	    
        mMapView = (MapView)this.findViewById(R.id.mapView);       

        mMapManager = new MapManager( this, mMapView, mOverlayManager );
        MapManagerDAL.Load(this, mMapManager);
        
        mBuddyInfoReceiver = new BuddyInfoReceiver( mMapManager.mCPoiManager );
        
        //register the mBuddyInfoReceiver for listening to XMLMessage.BUDDY_INFO_MESSAGE message
        registerReceiver( mBuddyInfoReceiver, new IntentFilter(com.reconinstruments.modlivemobile.dto.message.XMLMessage.BUDDY_INFO_MESSAGE) );
                
        
		ResortDBLoadTask task = new ResortDBLoadTask( this, 
				getResources().getString(R.string.wait_for_loading), 
				getResources().getString(R.string.db_loading_prompt),
				getResources().getString(R.string.resort_db_version),
				mPostDBLoadedCallBack);
		task.execute( "resortInfo.DB" );

    	LinearLayout layout = (LinearLayout)this.findViewById(R.id.nav_bar);
    	mToolbarHandler = new ToolbarHandler( layout, this  );
    	
        
        //turn on Rotator if the device has a touch screen
        PackageManager pm = getPackageManager();
        if( pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN )  == false )
        {
        	mMapView.setDrawRotator(false);
        }
         
        //create the connection to connect TranscendService
        mTranscendConnection = new TranscendServiceConnection( this, mMapView );            
        boolean connect = bindService( new Intent( "RECON_MOD_SERVICE" ), mTranscendConnection, Context.BIND_AUTO_CREATE );
    }
    
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		//call the Activity onKeyDown which handle the back key
		//to finish the activity
		if(keyCode != KeyEvent.KEYCODE_BACK)
		{
			super.onKeyDown(keyCode, event);
		}
		
	   	switch( keyCode )
    	{
			case KeyEvent.KEYCODE_DPAD_DOWN:
				return mKeyEventHandler.onDownArrowDown( null );
				
			case KeyEvent.KEYCODE_DPAD_UP:
				return mKeyEventHandler.onUpArrowDown( null );
				
			case KeyEvent.KEYCODE_DPAD_LEFT:
				return mKeyEventHandler.onLeftArrowDown( null );
			
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				return mKeyEventHandler.onRightArrowDown( null );
				
			case KeyEvent.KEYCODE_DPAD_CENTER:
				return mKeyEventHandler.onSelectDown( null );
				
			case KeyEvent.KEYCODE_BACK:
				return mKeyEventHandler.onBackDown( null );
				
			//all the other buttons, just ignore it
			default:
				return false;	    	
		}
    	
	}
	
	private ResortDBLoadTask.IPostLoadedCallback mPostDBLoadedCallBack = new ResortDBLoadTask.IPostLoadedCallback() {
		
		@Override
		public void onPostDBLoaded() 
		{
			showRootMenu();

			//make sure the GPS listener created after  
			//resort DB is loaded first
			mGPSListener = new GPSListener( NavigationActivity.this, mMapManager );
			
	        //hook up LocationManager for listening to GPS signals
	        LocationManager locMan = (LocationManager)getSystemService( Context.LOCATION_SERVICE );
	        locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER,0, 0, mGPSListener );

		}
	};
    public MapView getMapView()
    {
    	return mMapView;    	
    }
    
    public ShpMap getMap( )
    {
    	return mMapManager.mMap;
    }
    
    public boolean isGPSEnabled( )
    {
    	return mDiscardGPSSignal == false;
    }
    
    public void enableGPS( boolean flag )
    {
    	mDiscardGPSSignal = flag == false;
    }
    
    
    @Override 
    protected void onDestroy()
    {
    	super.onDestroy();
    	
    	//remove from LocationManager as an location listener
    	if( mGPSListener != null )
    	{
	        LocationManager locMan = (LocationManager)getSystemService( Context.LOCATION_SERVICE );
	        locMan.removeUpdates(mGPSListener);
    	}    	
    	
        //unregister broadcastReceiver for buddy info
        unregisterReceiver( mBuddyInfoReceiver );
        
        //unbind Transcend Service connection
        unbindService( mTranscendConnection );
        
    }

 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {   
    	if( DebugUtil.DEBUG_MENU )
    	{
	        // Inflate the currently selected menu XML resource.
	        MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.debug_menu, menu);
    	}
    	
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {    	
    	if( DebugUtil.DEBUG_MENU )
    	{
            switch (item.getItemId()) 
            {
            case R.id.debug_menu_toggleArrowEffect:
				Trail.ToggleArrowEffect();
				mMapView.invalidate();
				
		        //requestWindowFeature(Window.FEATURE_NO_TITLE);
		        return true;

            case R.id.debug_menu_showLocationXY:            	
				//mMapView.setFeature(MapView.MAP_FEATURE_LOCATION, !mMapView.isFeatureOn(MapView.MAP_FEATURE_LOCATION));
            	if( mMapManager.mMap != null && mMapManager.mMap.isEmpty() == false )
            	{
            		mMapManager.mCPoiManager.addDebugBuddies();
            		mMapManager.mMapView.invalidate();
            	}            	
                return true;
               
            case R.id.debug_menu_showReNetworkDiagnostic:
				Intent diaInent = new Intent( NavigationActivity.this, NetworkDiagnosticList.class );
				startActivityForResult( diaInent, GET_ISOLATED_NODE );								
				return true;
				
            case R.id.debug_menu_loadSite:
            	loadAvailableResort( );
				return true;
				
            case R.id.debug_menu_dropPin:
            	//create a random pin name
    			int persoId = (int)(Math.random()*1000);
    			String pinName = "Pin" + persoId;
    			
                LayoutInflater factory = LayoutInflater.from(this);
                final View textEntryView = factory.inflate(R.layout.pin_name_editor, null);
                EditText pinNameEditor =  (EditText)textEntryView.findViewById(R.id.pinname_edit);       
                pinNameEditor.setText(pinName);
                        
                AlertDialog dlg = new AlertDialog.Builder( new ContextThemeWrapper( this, android.R.style.Theme_Translucent_NoTitleBar ) )
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
                
                dlg.show();

            	return true;
            
            case R.id.debug_menu_toggle_toolbar:
            	View toolbar = findViewById(R.id.nav_bar);
            	if( mToolbarOn )
            		toolbar.setVisibility(View.GONE);
            	else
            		toolbar.setVisibility(View.VISIBLE);
            	mToolbarOn = !mToolbarOn;
            	return true;
            }   
            
            
            	

    	}
        return false;
    }

    	
	// Definition of the one requestCode we use for receiving poi-item selection data.
    static  final public int GET_POI = 0;
    static  final public int GET_ISOLATED_NODE = 1;
    static  final public int GET_RESORT_INFO =2;
    
	/**
     * This method is called when the PoiCategoryList activity has finished, with the
     * result it supplied.
     * 
     * @param requestCode The original request code as given to
     *                    startActivity().
     * @param resultCode From sending activity as per setResult().
     * @param data From sending activity as per setResult().
     */
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        // You can use the requestCode to select between multiple child
        // activities you may have started.  Here there is only one thing
        // we launch.
    	//make sure we test if data is null or not, coz when BACK is press
    	//within the PoiItemList activity, no item will be selected
    	//thus, a null intent will be return. For this special case
    	//we should not exit current activity.
        if (requestCode == GET_POI && data != null ) 
        {
        	int actionCode = data.getIntExtra("PoiItemAction", 0);
        	int poiItemType = data.getIntExtra("PoiType", 0);
        	
        	//view the poi on the map
        	if( actionCode == 0 )
        	{
            	int poiItemId = data.getIntExtra("PoiItemId", 0);
            	            	
            	ArrayList<PoInterest> pois = mMapManager.mMap.mPoInterests.get(poiItemType);
            	PoInterest poi = pois.get(poiItemId);
            	mMapView.setCenter(poi.mPosition.x, poi.mPosition.y,false);
        		
        	}
        	else if( actionCode == 1)	//delete cdp
        	{
        		int[] poiItemId = data.getIntArrayExtra("PoiItemIds");
        		ArrayList<PoInterest> pois = new ArrayList<PoInterest>( poiItemId.length );
        		for( int i = 0; i < poiItemId.length; ++i )
        		{
        			pois.add( mMapManager.mMap.mPoInterests.get(poiItemType).get(poiItemId[i]));        			
        		}
        		
        		for( PoInterest poi : pois )
        		{
        			mMapManager.mCPoiManager.removePoi(poi);        			
        			mMapManager.mMap.removePOI(poi);
        		}
        		
        		mMapManager.mCPoiManager.saveCDPs(this, mMapManager.mActiveResort.mID);        		
        		mMapView.invalidate();
        	}
        }
        else if ( requestCode == GET_ISOLATED_NODE && data != null ) 
        {
           	float x = data.getFloatExtra("PositionX", 0);
        	float y = data.getFloatExtra("PositionY", 0);
        	
        	mMapView.setCenter(x, y,false);
        }
        else if( requestCode == GET_RESORT_INFO && data != null )
        {
        	String resortName = data.getStringExtra("resortName");
        	String countryName = data.getStringExtra("countryName");
        	String regionName = data.getStringExtra("regionName");
        	
        	CountryInfo country = ResortInfoProvider.getCountryInfo(countryName);
        	ResortInfo resort = null;
        	if( regionName != null )
        	{
        		RegionInfo region = country.getRegion(regionName);
        		resort = region.getResort(resortName);
        	}
        	else
        	{
        		resort = country.getResort(resortName);
        	}
        	
        	if( resort != null )
        	{
        		mMapManager.loadResort(resort);
        	}
        }
    }

    /**
     * Implemented the interface of GPSListener.IGPSUpdater
     * 
     */

	protected void addTestPath( ShpMap map )
	{
		ArrayList<PointF> list = new ArrayList<PointF>();
		
		float scale = 1;
		//list.add( new PointF( -50*scale, -150*scale ) );
		//list.add( new PointF( 50*scale, -150*scale) );
		//list.add( new PointF( 50*scale, 150*scale ) );
		//list.add( new PointF( -50*scale, 150*scale ) );
		//list.add( new PointF( -50*scale, -150*scale ) );

		//list.add( new PointF( -100*scale, 150*scale ) );
		//list.add( new PointF( 100*scale, -150*scale) );
		
		list.add( new PointF( 0, 150*scale ) );
		list.add( new PointF( 0, 0) );

		Trail trail = new Trail( list, "Test Path", Trail.RED_TRAIL, false );
		map.addTrail( trail );	
	
/*
		list.add( new PointF( 0, 0) );
		list.add( new PointF( 100, 150 ) );
		list.add( new PointF( 100, 900 ) );
		AddPath( list );
*/
		
/*	
		list.add( new PointF( 0, 0) );
		list.add( new PointF( 100, 150 ) );
		list.add( new PointF( 100, 300 ) );
		list.add( new PointF( 0, 300 ) );
		AddPath( list );
*/

/*
		float scale = 0.1f;

		list.add( new PointF( -100*scale, -400*scale ) );
		list.add( new PointF( -85*scale, -301*scale) );
		list.add( new PointF( -85*scale, -240*scale ) );
		list.add( new PointF( -23*scale, -100*scale ) );
		list.add( new PointF( -34*scale, -200*scale ) );
		list.add( new PointF( 50*scale, -120*scale ) );
		list.add( new PointF( 44*scale, -20*scale ) );
		list.add( new PointF( 99*scale, 430*scale ) );
		list.add( new PointF( 560*scale, 99*scale ) );
		list.add( new PointF( 230*scale, 201*scale ) );
		list.add( new PointF( 477*scale, 500*scale ) );
		map.AddTrail( list );		
		
		ArrayList<PointD> list2 = new ArrayList<PointD>();
		list2.add( new PointF( 477*scale, 500*scale ) );
		list2.add( new PointF( 477*scale, 1000*scale) );
		map.AddTrail(list2);
*/		
	}
	

	/**
	 * start an activity to list out all
	 * available resorts; if none, show
	 * an alert dialog
	 */
	public void loadAvailableResort( )
	{
    	//check if any resorts map available on the device
    	if( ResortInfoProvider.hasResortMapsAvailable() )
    	{
			Intent resortIntent = new Intent( NavigationActivity.this, CountryListActivity.class );
			startActivityForResult( resortIntent, GET_RESORT_INFO );
    	}
    	else
    	{
    		//pop up alert dialog for user to purchase map data online
			AlertDialog.Builder dlg = new AlertDialog.Builder( this );
			dlg.setCancelable(true);
			dlg.setIcon(R.drawable.alert_dialog_icon);
			dlg.setTitle( getResources().getString(R.string.no_map_dialog_title));
			dlg.setMessage( getResources().getString(R.string.no_map_warning));
			dlg.show();		
    	}	
	}
	
	public void showRootMenu()
	{
		RootTabView tabView = new RootTabView( NavigationActivity.this, mOverlayManager, mMapManager );
		mOverlayManager.addOverlayView(tabView);
	}
	
	public void showExitDialog()
	{
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.pin_delete_view, null);
        TextView msgView  =  (TextView)textEntryView.findViewById(R.id.msg_view);       
        msgView.setText("Are you sure you want to exit?");
                
        AlertDialog dlg = new AlertDialog.Builder( new ContextThemeWrapper( this, android.R.style.Theme_Translucent_NoTitleBar ) )
            .setView(textEntryView)
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) 
                {
                	 finish();
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            })
            .create();

        dlg.show();						
	}
	
	private IRemoteControlEventHandler mKeyEventHandler = new IRemoteControlEventHandler()
	{
		public boolean onDownArrowDown(  View srcView )
		{
			if(mOverlayManager.getActiveOverlay() == null)
			{
				showRootMenu();
			}
			return true;
		}
		
		public boolean onDownArrowUp(  View srcView )
		{
			return true;
		}

		public boolean onUpArrowDown(  View srcView )
		{
			if(mOverlayManager.getActiveOverlay() == null)
			{
				showRootMenu();
			}
			return true;
		}
		
		public boolean onUpArrowUp(  View srcView )
		{
			return true;
		}

		public boolean onLeftArrowDown(  View srcView )
		{
			if( mMapManager.mActiveResort != null && mOverlayManager.getActiveOverlay() == null )
			{
				--mCurrentMapMode;
				mCurrentMapMode = mCurrentMapMode < 0 ? MAP_VIEW_MODE_LAST : mCurrentMapMode;
				mCurrentMapMode %= (MAP_VIEW_MODE_LAST +1);
				updateMapViewMode();
			}
			return true;
		}
		
		public boolean onLeftArrowUp(  View srcView )
		{
			
			return true;
		}

		public boolean onRightArrowDown(  View srcView )
		{
			if( mMapManager.mActiveResort != null && mOverlayManager.getActiveOverlay() == null )
			{
				++mCurrentMapMode;			
				mCurrentMapMode %= (MAP_VIEW_MODE_LAST +1);
				updateMapViewMode();
			}
			return true;
		}
		
		public boolean onRightArrowUp(  View srcView )
		{
			return true;
		}

		public boolean onSelectDown(  View srcView )
		{			
			if( mMapManager.mActiveResort == null )
			{
				return true;
			}
			
			OverlayView activeOverlay = mOverlayManager.getActiveOverlay();
			if( activeOverlay == null )
			{
				if( mMapMoveView == null )
				{
					mMapMoveView = new MapMoveView( NavigationActivity.this, mMapView, mOverlayManager, this, mMapManager );					
				}
				
				mOverlayManager.setOverlayView( mMapMoveView );
			}
			else if( activeOverlay == mMapMoveView )			
			{
				if( mMapZoomView == null )
				{
					mMapZoomView = new MapZoomView( NavigationActivity.this, mMapView, mOverlayManager, this, mMapManager );					
				}
				
				mOverlayManager.setOverlayView( mMapZoomView );
			}
			else if( activeOverlay == mMapZoomView )
			{				
				mOverlayManager.setOverlayView( null );
			}
			return true;
		}
		
		public boolean onSelectUp(  View srcView )
		{
			return true;
		}

		
		public boolean onBackDown(  View srcView )
		{
			showExitDialog();
			return true;
		}
		
		public boolean onBackUp(  View srcView )
		{
			return true;
		}

	};
	
	private void setFullscreenMode( boolean on )
	{
        if( on == true )
        {
        	getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        	getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
	                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        else
        {
        	getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        	getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,  
                    WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        	
        }

	}
	
	private void updateMapViewMode( )
	{
		switch( mCurrentMapMode )
		{
		case MAP_VIEW_MODE_0:
			//turn off all overlay features
			//shows the statusbar
			setFullscreenMode( true );
			mMapView.clearFeatures();			
		break;
		
		case MAP_VIEW_MODE_0 + 1:
			//show compass
			setFullscreenMode( true );
			mMapView.clearFeatures();
			mMapView.setFeature(MapView.MAP_FEATURE_COMPASS, true);
			mMapView.setFeature(MapView.MAP_FEATURE_SPEED, true);
		break;
		
		case MAP_VIEW_MODE_0 + 2:
			//show compass
			setFullscreenMode( false );
			mMapView.clearFeatures();
			mMapView.setFeature(MapView.MAP_FEATURE_COMPASS, true);
			mMapView.setFeature(MapView.MAP_FEATURE_SPEED, true);			
		break;
		

		case MAP_VIEW_MODE_0 + 3:
			//show compass, scale metric and location
			setFullscreenMode( false );
			mMapView.clearFeatures();			
			mMapView.setFeature(MapView.MAP_FEATURE_COMPASS, true);
			mMapView.setFeature(MapView.MAP_FEATURE_LOCATION, true);
		break;
		
		}
	}	
}
