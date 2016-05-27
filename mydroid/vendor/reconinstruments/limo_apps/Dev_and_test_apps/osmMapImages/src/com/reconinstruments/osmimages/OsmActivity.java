package com.reconinstruments.osmimages;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;

import com.reconinstruments.dashelement1.ColumnElementFragmentActivity;
import com.reconinstruments.hud_phone_status_exchange.HudPhoneStatusExchanger;
import com.reconinstruments.mapsdk.mapview.StaticMapGenerator;
import com.reconinstruments.mapsdk.mapview.StaticMapGenerator.IReconMapImageGeneratorCallbacks;



/**
 * @author simonwang
 *
 */


public class OsmActivity extends Activity implements IReconMapImageGeneratorCallbacks {
//ColumnElementFragmentActivity implements
		//IReconMapImageGeneratorCallbacks {
	
	// constants
	private static final String TAG = "TileImages";
	private static int FAST_LOCATION_POST_UPDATE_INTERVAL = 10;
	private static final boolean WAIT_FOR_DEBUGGER = true;

	// members
	private StaticMapGenerator mStaticMapGenerator = null;
	
	
	
	private MapImageManager mMapImageManager = null;

	// methods
	// activity/fragment life cycle routines
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCReate-OSM Activity.");

		Log.i(TAG, "onCreate " + System.currentTimeMillis());
		super.onCreate(savedInstanceState);

		if (WAIT_FOR_DEBUGGER) {
			android.os.Debug.waitForDebugger();
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);

	    Window window = getWindow();
		window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
			
		//setContentView(R.layout.activity_map); // empty FrameLayoutIReconMapImageGeneratorCallbacks
		setContentView(R.layout.staticmapdemo);

		// if we're being restored from a previous state, then don't do anything
		// otherwise we could end up with overlapping fragments.
		if (savedInstanceState != null) {
			return;
		}


		String mapIdentifier = getPackageName();

		try {

			mStaticMapGenerator = new StaticMapGenerator(this, mapIdentifier, StaticMapGenerator.MapType.STREET);
			
			mMapImageManager = new MapImageManager(this, mStaticMapGenerator);
			
			

		} catch (Exception e) {
			Log.e(TAG,
					"Error instantiating MapFragment or MapView:"
							+ e.getMessage());
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.i(TAG, "-----onReume");


		HudPhoneStatusExchanger.sendLocationPostIntervalToPhone(this, FAST_LOCATION_POST_UPDATE_INTERVAL); // TODO undo comment for
														// ColumnElementFragmentActivity

	}

	// interface IMapViewCallbacks
	public void MapViewReadyToConfigure() { // indicates mapview is ready to
		                                    // receive camera configuration
		Log.i(TAG, "MapViewReadyToConfigure " + System.currentTimeMillis());
		
		//mMapImageManager.StartGenerateOsmImgs();
	   
	}

	public void MapViewReadyForAnnotation() { 
		Log.i(TAG, "MapViewReadyForAnnotation " + System.currentTimeMillis());
		//mMapImageManager.NotifyImageLoaded();
		
	}
	public void AddAnnotations(){
		Log.i(TAG, "AddAnnotations " + System.currentTimeMillis());
	}

	@Override
	public void onPause() {
		Log.d(TAG, "onPause");
		// HudPhoneStatusExchanger.sendLocationPostIntervalToPhone(this,0);
		// //TODO undo comment for ColumnElementFragmentActivity
		super.onPause();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "onKeyDown");
		/*if (mMapFragment.onKeyDown(keyCode, event)) {
			return true;
		}*/
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		/*if (!mMapFragment.onBackPressed())
			goBack();*/
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.d(TAG, "onKeyUp");

		if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			Log.d(TAG, "---KEY RIGHT--onKeyUp");
			//saveImages();
			mMapImageManager.getOsmTileManager().GetTileIdListFromStorage();
		}

		else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			Log.d(TAG, "---KEY UP--onKeyUp");
			TextView textView = ((TextView) findViewById(R.id.text_label));
			textView.setVisibility(View.INVISIBLE);
			mMapImageManager.StartGenerateOsmImgs();

		}

		else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			Log.d(TAG, "---KEY DOWN--onKeyUp");
			
			mMapImageManager.StopGenerateOsmImgs();
			


		}
				
		
		
		/*if (mMapFragment.onKeyUp(keyCode, event)) {
			return true;
		}*/
		return super.onKeyUp(keyCode, event);
	}

	
	public void showImg(final Bitmap bitmap) {
		try {
			runOnUiThread(new Runnable() {
			    public void run() {
					TextView textView = ((TextView) findViewById(R.id.text_label));
					textView.setText("Tile created.");
					textView.setVisibility(View.VISIBLE);
					
					Bitmap resizeBitmap = getResizedBitmap(bitmap, 240, 428);
					ImageView bitmapView = (ImageView) findViewById(R.id.created_bitmap);
					bitmapView.setImageBitmap(resizeBitmap); 
					bitmapView.setVisibility(View.VISIBLE);
					
					
			    }
			});
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void showErrorMsg() {
		try {
			runOnUiThread(new Runnable() {
			    public void run() {
					TextView textView = ((TextView) findViewById(R.id.text_label));
					textView.setText("Image creation failed !!");
					Log.e(TAG, "Error Tile: " + mStaticMapGenerator.GetErrorMessage());
					textView.setVisibility(View.VISIBLE);
					
					ImageView bitmapView = (ImageView) findViewById(R.id.created_bitmap);
					bitmapView.setVisibility(View.INVISIBLE);
					
			    }
			});
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public void showIntialScreen() {
		try {
			runOnUiThread(new Runnable() {
			    public void run() {
			    	ImageView bitmapView = (ImageView) findViewById(R.id.created_bitmap);
			    	//bitmapView.setImageBitmap(null);
			    	bitmapView.setVisibility(View.INVISIBLE);
			    	
			    	TextView textView = ((TextView) findViewById(R.id.text_label));
			    	textView.setText("'up' key to create Tiles; 'down' to stop");
			    	textView.setVisibility(View.VISIBLE);
			    	
			    }
			});
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
	    int width = bm.getWidth();
	    int height = bm.getHeight();
	    float scaleWidth = ((float) newWidth) / width;
	    float scaleHeight = ((float) newHeight) / height;
	    // CREATE A MATRIX FOR THE MANIPULATION
	    Matrix matrix = new Matrix();
	    // RESIZE THE BIT MAP
	    matrix.postScale(scaleWidth, scaleHeight);

	    // "RECREATE" THE NEW BITMAP
	    Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
	    return resizedBitmap;
	}
	

}
