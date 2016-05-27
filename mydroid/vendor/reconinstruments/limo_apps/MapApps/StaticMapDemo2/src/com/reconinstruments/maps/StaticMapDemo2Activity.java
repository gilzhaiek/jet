package com.reconinstruments.maps;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.reconinstruments.dashelement1.ColumnElementFragmentActivity;
import com.reconinstruments.hud_phone_status_exchange.HudPhoneStatusExchanger;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoTile;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;
import com.reconinstruments.mapsdk.mapview.StaticMapGenerator;
import com.reconinstruments.mapsdk.mapview.StaticMapGenerator.IReconMapImageGeneratorCallbacks;
import com.reconinstruments.mapsdk.mapview.StaticMapGenerator.MapType;
import com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer.CustomAnnotationCache;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.Map2DLayer;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */

public class StaticMapDemo2Activity extends ColumnElementFragmentActivity  implements IReconMapImageGeneratorCallbacks
{
// constants    
    private static final String TAG = "StaticMapDemos2Activity";
    private static int FAST_LOCATION_POST_UPDATE_INTERVAL = 10;
    private static final boolean WAIT_FOR_DEBUGGER = false;
    private static final boolean SINGLE_TILE_GEOREGION = false;
    
    // members
    private StaticMapGenerator mMapGenerator = null;
    // For compass calibration
//    private FrameLayout         mMainLayout = null;

    
// methods    

// activity/fragment life cycle routines
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG,"onCreate "+System.currentTimeMillis());
        super.onCreate(savedInstanceState);

        if(WAIT_FOR_DEBUGGER) {
                android.os.Debug.waitForDebugger();
        } 

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = getWindow();
		window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


		setContentView(R.layout.staticmapdemo);
        
        // if we're being restored from a previous state, then don't do anything otherwise we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        String mapIdentifier = getPackageName(); 
        
        try {
        	mMapGenerator = new StaticMapGenerator(this, mapIdentifier, MapType.STREET);
        	if(mMapGenerator != null) {
        		CreateMapImageTask newCreateTask = new CreateMapImageTask();
//				Log.e(TAG,"Starting map load task");
			    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				    newCreateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);// required to allow parallel execution of Asynch tasks in map layer
			    }
			    else {
			    	newCreateTask.execute();
			    }

        	}
        	else {
    			((TextView) findViewById(R.id.text_label)).setText("System error !!");
        		Log.e(TAG, "Error instantiating StaticMapGenerator");
        		finish();
        	}

        }
        catch (Exception e) {
			((TextView) findViewById(R.id.text_label)).setText("System error !!");
       		Log.e(TAG, "Error instantiating StaticMapGenerator");
       		finish();
       	}
    }

    protected class CreateMapImageTask extends AsyncTask<Void, Void, String> {
		Map2DLayer mParent = null;
		Bitmap		mBitmap = null;
		
		protected String doInBackground(Void...voids)  {
			GeoRegion newGeoRegion = null;
			if(SINGLE_TILE_GEOREGION) {
				int tileIndex = GeoTile.GetTileIndex(-123.113948, 49.269188 );
				newGeoRegion = GeoTile.GetGeoRegionFromTileIndex(tileIndex);
				Log.d(TAG,"single tile selected: #" + tileIndex);
				mBitmap = mMapGenerator.GenerateMapImage(512, 512, newGeoRegion, 0.f, 0.f);
			}
			else {
				newGeoRegion = (new GeoRegion()).MakeUsingBoundingBox(-123.1588f, 49.3000f, -123.1021f, 49.2685f );
				RectXY gtBB = newGeoRegion.mBoundingBox;
				Log.d(TAG,"Request bounding box: " + gtBB.left + ", "+ gtBB.top + ", "+ gtBB.right + ", "+ gtBB.bottom );
				mBitmap = mMapGenerator.GenerateMapImage(428, 240, newGeoRegion, 0.f, 25.f);
			}
			RectXY gtBB = newGeoRegion.mBoundingBox;
			Log.d(TAG,"Request bounding box: " + gtBB.left + ", "+ gtBB.top + ", "+ gtBB.right + ", "+ gtBB.bottom );
            
    		if(mBitmap != null) {
    			return null;
     		}
    		else {
    			return "ERROR";
    		}
			
		}
		
		protected void onPostExecute(String endString) {
			if(endString != null) { // error case
    			((TextView) findViewById(R.id.text_label)).setText("Image creation failed !!");
       			Log.e(TAG, "Error: " + mMapGenerator.GetErrorMessage());
			}
			else {  // success - show image on screen
				ImageView bitmapView = (ImageView) findViewById(R.id.created_bitmap);
				bitmapView.setImageBitmap(mBitmap); 
				bitmapView.setVisibility(View.VISIBLE);
				
//				String path = Environment.getExternalStorageDirectory().toString();
//				File file = new File(path, "newMap.png");
//				try {
//					FileOutputStream out = new FileOutputStream(file);
//					out = new FileOutputStream(file);
//					mBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
//			        out.flush();
//			        out.close();
//				} 
//				catch (Exception e) {
//				    e.printStackTrace();
//				}
			}
		}
	}

    @Override
    public void onResume()  {
        super.onResume();

//        mMainLayout = (FrameLayout) getWindow().getDecorView().findViewById(android.R.id.content);

        HudPhoneStatusExchanger.sendLocationPostIntervalToPhone(this,FAST_LOCATION_POST_UPDATE_INTERVAL);  //TODO undo comment for ColumnElementFragmentActivity
    } 

// interface StaticMapGenerator.IReconMapImageGeneratorCallbacks 
	public void AddAnnotations() {
		// example annotations
	
//		Log.d(TAG, "adding Annotations...");
        CustomAnnotationCache.AnnotationErrorCode errorCode;
        int id = R.drawable.info_icon_3; 
        Bitmap image = BitmapFactory.decodeResource(getResources(), id);
        PointXY poiLocation = new PointXY(-123.122886f, 49.279793f);
        errorCode = mMapGenerator.AddPointAnnotation("TestPoint", poiLocation, image, 255) ;
        id = R.drawable.lift_icon_3; 
        image = BitmapFactory.decodeResource(getResources(), id);
        poiLocation = new PointXY(-123.121686f, 49.272793f);
        errorCode = mMapGenerator.AddPointAnnotation("BankPoint", poiLocation, image, 255) ;
        
        ArrayList<PointXY> nodes = new ArrayList<PointXY>();
        nodes.add(new PointXY(-123.131186f, 49.281103f));
        nodes.add(new PointXY(-123.111186f, 49.277793f));
        nodes.add(new PointXY(-123.121186f, 49.274793f));
        errorCode = mMapGenerator.AddOverlayAnnotation("TestOverlay", nodes, 0xdddddd,  128);
        errorCode = mMapGenerator.AddLineAnnotation("TestLine", nodes, 20.f, 0x40c4c3,  128);

        nodes.clear();
        nodes.add(new PointXY(-123.121186f, 49.274793f));
        nodes.add(new PointXY(-123.131186f, 49.281103f));
        errorCode = mMapGenerator.AddLineAnnotation("RedLine", nodes, 20.f, 0xc43030,  128);

        mMapGenerator.fo
    }

    @Override
    public void onPause()  
    {
        Log.d(TAG,"onPause");
//              HudPhoneStatusExchanger.sendLocationPostIntervalToPhone(this,0);   //TODO undo comment for ColumnElementFragmentActivity
        super.onPause();
    }

    @Override
    public void onDestroy(){
        Log.d(TAG,"onDestroy");
        super.onDestroy();
    }


    

}
