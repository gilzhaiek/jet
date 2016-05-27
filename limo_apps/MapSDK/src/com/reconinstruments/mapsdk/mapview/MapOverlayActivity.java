
package com.reconinstruments.mapsdk.mapview;


import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.FrameLayout;


import android.support.v4.app.FragmentActivity;


import java.security.MessageDigest;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.io.FileNotFoundException;

import com.reconinstruments.mapsdk.R;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer.CustomAnnotationCache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.reconinstruments.mapsdk.mapview.StaticMapGenerator;
import com.reconinstruments.mapsdk.mapview.StaticMapGenerator.IReconMapImageGeneratorCallbacks;

import android.os.AsyncTask;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.Map2DLayer;

import com.reconinstruments.commonwidgets.FeedbackDialog;

public abstract class MapOverlayActivity extends FragmentActivity implements IReconMapImageGeneratorCallbacks {
    // constants
    private static final String PATH_TRIP_DATA = "/ReconApps/TripData/";
    private static final String FILE_NAME_TRIP_DATA = "tripdata.csv";
    //private static final String FILE_NAME_TRIP_DATA = "simpleLatLng2.csv";
    private static final float WIDTH_FRAME_IN_DEGREE = 0.005f;
    
    private static final float MIN_WIDTH_IN_DEGREE = 0.0075f;//=0.75km
    private static final float MIN_HEIGHT_IN_DEGREE = 0.00375f;//=0.35km
    
    
    private static final float MAX_WIDTH_IN_DEGREE = 0.36f;//=40km
    private static final float MAX_HEIGHT_IN_DEGREE = 0.18f;//=20km
    
    
    
    protected static final String TAG = "MapOverlayActivity";
    private static final boolean WAIT_FOR_DEBUGGER = false;
    private static final int FULLL_SCREEN_WIDTH = 428;
    private static final int FULL_SCREEN_HEIGHT = 240;

    // members 
    private FrameLayout mMainLayout = null;
    private ArrayList<PointXY> mTripRoute = null;
    private Bound mBound = null;
    private boolean mHasData = false;
    protected String mFileName = "simpleLatLng.tmp.txt";
    protected String mFileCheckSum = "";

    protected StaticMapGenerator mStaticMapGenerator = null;
	protected Bitmap mBitmap = null;

    // let child class set up the content view and mFileName value
    protected abstract void setupLayout();
    protected abstract void preCreate();
    protected abstract void postCreate();
    protected void DrawBitmap(){
        FeedbackDialog.dismissDialog(this);
    }
    
    private String mapIdentifier;

    private boolean mContinueLoadingMap = true;
    
    // methods

    // activity/fragment life cycle routines
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate " + System.currentTimeMillis());
        super.onCreate(savedInstanceState);

        if (WAIT_FOR_DEBUGGER) {
            android.os.Debug.waitForDebugger();
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setupLayout();

        // if we're being restored from a previous state, then don't do anything
        // otherwise we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }


        mapIdentifier = getPackageName();

        
        try {

        
        	
        	preCreate();
        	
            
		    
		    
            postCreate();
    
	        
        } catch (Exception e) {
            Log.e(TAG,
                    "Error instantiating MapFragment or MapView:"
                            + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "-----onResume");
        if(!mContinueLoadingMap){
            return;
        }

        String fileCheckSum = GetTripFileCheckSum(mFileName);
        Log.d(TAG, "tripFileName=" + mFileName +", checksum=" +  fileCheckSum);
        if (!fileCheckSum.equals(mFileCheckSum)){//file checksum is being changed.
        	Log.d(TAG, "reload bitmap and redraw.");
        	kickoffLoadMapBitmap();
        }
        else{
        	Log.d(TAG, "not reload bitmap but redraw.");
        	DrawBitmap();
        }
        
        
    }


    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
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
        return super.onKeyDown(keyCode, event);
    }

	   
    private boolean readTripData(){
        String pathName = Environment.getExternalStorageDirectory().toString() + PATH_TRIP_DATA + mFileName;
        Log.d   (TAG, "readTripData.pathFullName=" + pathName);
        File file = new File(pathName);
        if (file == null || !file.exists()) {
            return false;
        }


        mTripRoute = new ArrayList<PointXY>();
        mBound = new Bound();
        
        
        FileInputStream fis = null;
        String line = null;
        BufferedReader lines = null;
        int counter = 0;
      

        try {
            fis = new FileInputStream(pathName);
             lines = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            
            ArrayList<String> list = new ArrayList<String>();
            String latStr, lngStr;
            float latFlt=0.f, lngFlt=0.f;
            int idx = 0;
            PointXY point = null;

            
            while((line = lines.readLine()) !=null){
                System.out.println(line);
                list.add(line);
                idx = line.indexOf(',');
                if (idx == -1){
                    Log.e(TAG, "Wrong format data=" + line +" in tripdata file=" + pathName);
                    continue;
                }
                latStr = line.substring(0, idx);
                lngStr = line.substring(idx+1);
                try{
                    latFlt = Float.parseFloat(latStr);
                    lngFlt = Float.parseFloat(lngStr);
                    point = new PointXY(lngFlt, latFlt);
                    mTripRoute.add(point);
                    ++counter;
                    
                }
                catch(Exception e){
                    e.printStackTrace();
                    Log.e(TAG, "Wrong format data (float)=" + line +" in tripdata file=" + pathName);
                }
            }//eof while
                
            
            //Adjust TripRouter to remove RedundantPathPoints
            ArrayList<PointXY> revisedList  = mStaticMapGenerator.RemoveRedundantPathPointsForCurrentViewport(mTripRoute);
            if(revisedList != null) {
                Log.v(TAG,"revised path size: " + revisedList.size());
                mTripRoute = revisedList;
             } //revisedList
            
            for(PointXY node : mTripRoute) {
                Log.v(TAG,"activity_node: " + node.x + ", " + node.y);
                mBound.adjustBound(node.x, node.y);
            }                
            Log.v(TAG,"bound_left=" + mBound.left + ", bound_right=" + mBound.right+", top=" + mBound.top +", bottom=" + mBound.bottom);

        }
        catch  (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error in tripdata file=" + pathName);
        }
        finally{
            try{
            lines.close();
            fis.close();
            }
            catch(Exception e){}
            Log.d(TAG, "TripList.counter=" + counter);
        }
        
        return true;
        
    }//eof readTripData
    
    protected void kickoffLoadMapBitmap(){
    	
    	Log.v(TAG, "kickoffLoadMapBitmap().");
        FeedbackDialog.showDialog(this, "Loading Map", null, null, true);
    	try{
    	
    			mStaticMapGenerator = new StaticMapGenerator(this, mapIdentifier, StaticMapGenerator.MapType.STREET);
        
        } catch (Exception e) {
            Log.e(TAG,
                    "Error createing StaticMapGenerator instance. " + e.getMessage());
        }
    	
		CreateMapImageTask newCreateTask = new CreateMapImageTask();

	    newCreateTask.execute();
	    
	    
    }
    
	public Bitmap GetBitMapImg() {

		 GeoRegion newGeoRegion;
		 mBitmap = null;
	     mHasData = readTripData();
	     Log.v(TAG, "GetBitMapImg");
		 float percentWidthMargin = 0.15f, cameraHeading = 0.0f;

	     if (mHasData){
	

	    	 
	    	 int id1 = R.drawable.start_point; 
	         Bitmap image1 = BitmapFactory.decodeResource(getResources(), id1);
	         int pointWidth = image1.getWidth();
	         int pointHeight = image1.getHeight();
	         Log.v(TAG, "START/STOP point width=" + pointWidth +", height=" + pointHeight);
	         
	         float width = (mBound.right - mBound.left);
			 float height = (mBound.top - mBound.bottom);
			
			 Log.v(TAG, "MyTrip width=" + width +", height=" + height);
			 Log.v(TAG, "MIM width=" + MIN_WIDTH_IN_DEGREE +", height=" + MIN_HEIGHT_IN_DEGREE);
			 Log.v(TAG, "MAX width=" + MAX_WIDTH_IN_DEGREE +", height=" + MAX_HEIGHT_IN_DEGREE);
			 
	         
	         if (width < MIN_WIDTH_IN_DEGREE && height < MIN_HEIGHT_IN_DEGREE){
	        	 float CenterPointX = (mBound.right + mBound.left)/2.0f;
	        	 float CenterPointY = (mBound.top + mBound.bottom)/2.0f;
	        	 float halfWidth = MIN_WIDTH_IN_DEGREE/2.0f;
	        	 float halfHeight = MIN_HEIGHT_IN_DEGREE/2.0f;
	        	 
	        	 newGeoRegion = (new GeoRegion()).MakeUsingBoundingBox(CenterPointX - halfWidth , CenterPointY + halfHeight,
	        			   CenterPointX + halfWidth, CenterPointY  - halfHeight );
	        	 
	        	 Log.v(TAG, "Reach MIN1");
	         }
	         else if (width > MAX_WIDTH_IN_DEGREE || height > MAX_HEIGHT_IN_DEGREE){
	          	 float CenterPointX = (mBound.right + mBound.left)/2.0f;
	        	 float CenterPointY = (mBound.top + mBound.bottom)/2.0f;
	        	 float halfWidth = MAX_WIDTH_IN_DEGREE/2.0f;
	        	 float halfHeight = MAX_HEIGHT_IN_DEGREE/2.0f;
	        	 
	        	 newGeoRegion = (new GeoRegion()).MakeUsingBoundingBox(CenterPointX - halfWidth , CenterPointY + halfHeight,
	        			   CenterPointX + halfWidth, CenterPointY  - halfHeight );
	        	 
	        	 Log.v(TAG, "Reach MAX1");
	         }
	         else{
	        	 

	        	 newGeoRegion = (new GeoRegion()).MakeUsingBoundingBox(mBound.left , mBound.top,
	      	            mBound.right , mBound.bottom  );
	        	 Log.v(TAG, "GeoRegion, Width="+ width + ", height=" + height);
	        	 
	        	 
	         }     
		         
	            
	     }
	     else {
	    	 	Log.i(TAG, "GetBitMapImg.readTripData, no trip data available, use default screen.");
	            newGeoRegion = (new GeoRegion()).MakeUsingBoundingBox(-123.1388f, 49.2818f, -123.133f, 49.2767f );
	     }
		 
		 //newGeoRegion = (new GeoRegion()).MakeUsingBoundingBox(-123.1588f, 49.3000f, -123.1021f, 49.2685f );
				
	     if (mStaticMapGenerator == null) {
			 Log.d(TAG, "mStaticMapGenerator == NULL .");
			 return null;
		 }
	     
         mBitmap = mStaticMapGenerator.GenerateMapImage(FULLL_SCREEN_WIDTH, FULL_SCREEN_HEIGHT, newGeoRegion, percentWidthMargin, cameraHeading);
		 
		 mFileCheckSum = GetTripFileCheckSum(mFileName);
		 
		return mBitmap;
		
	}
	
    public void AddAnnotations(){
        Log.i(TAG, "AddAnnotations.");
        if (!mHasData)
            return;
        
        CustomAnnotationCache.AnnotationErrorCode errorCode;
        
        errorCode = mStaticMapGenerator.AddLineAnnotation("TripRoute", mTripRoute, 27.f, 0X00a8ff,  255); //Blue - 0xff0000ff, CYAN -0xff00ffff
        Log.i(TAG, "drawAnnotation, return_code=" + errorCode);
        
        int size = mTripRoute.size();
        if (size >1){
        	PointXY startPoint = mTripRoute.get(0);
        	PointXY stopPoint = mTripRoute.get(size-1);
        	int id1 = R.drawable.start_point, id2= R.drawable.end_point;
            Bitmap image1 = BitmapFactory.decodeResource(getResources(), id1);
            Bitmap image2 = BitmapFactory.decodeResource(getResources(), id2);
        	mStaticMapGenerator.AddPointAnnotation("start", startPoint, image1, 255);
        	mStaticMapGenerator.AddPointAnnotation("stop", stopPoint, image2, 255);
        }
        else if (size ==1){
        	int id1 = R.drawable.start_point;
        	Bitmap image1 = BitmapFactory.decodeResource(getResources(), id1);
        	mStaticMapGenerator.AddPointAnnotation("point", mTripRoute.get(0), image1, 255);
        }
        
	}//eof AddAnnotations
    
    protected class CreateMapImageTask extends AsyncTask<Void, Void, String> {
		Map2DLayer mParent = null;
		Bitmap		mBitmap = null;
		
		protected String doInBackground(Void...voids)  {
			
			GetBitMapImg();
			
			return null;
		}
		
		protected void onPostExecute(String endString) {
			
			DrawBitmap();
			
		}
	}//eof CreateMapImageTask
    
    class Bound{
        float left = 0.f;
        float right = 0.f;
        float top = 0.f;
        float bottom = 0.f;
        
        public Bound(float left, float right, float top, float bottom){
            
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
        public Bound(){
            
            this.left = 0.f;
            this.right = 0.f;
            this.top =  0.f;
            this.bottom =  0.f;
        }
        
        public void adjustBound(float lng, float lat){
            if (left == 0)
                left = lng;
            if (right == 0)
                right = lng;
            if (top == 0)
                top = lat;
            if (bottom == 0)
                bottom = lat;
            
            if (lng < left)
                left = lng;
            if (lng > right)
                right = lng;
            if (lat < bottom)
                bottom = lat;
            if (lat > top)
                top = lat;
        }
            
    }//class bound
    
    
    protected String GetTripFileCheckSum (String fileName ){
    
        String pathName = Environment.getExternalStorageDirectory().toString() + PATH_TRIP_DATA + fileName;
        File file = new File(pathName);
    	if (file == null || !file.exists()) {
    		Log.d(TAG, "GetTripFileCheckSum, fileName=" + pathName+ ", does not exist.");
            return "";
        }
        
        return GetFileChecksum(pathName);
    }
    
    private String GetFileChecksum (String fileFullName ){
        StringBuffer sb = new StringBuffer("");
    	 try
         {
    	 
	    	MessageDigest md = MessageDigest.getInstance( "SHA-1" );
	        FileInputStream fis = new FileInputStream(fileFullName);
	        byte[] dataBytes = new byte[1024];
	     
	        int nread = 0; 
	     
	        while ((nread = fis.read(dataBytes)) != -1) {
	          md.update(dataBytes, 0, nread);
	        };
	     
	        byte[] mdbytes = md.digest();
	     
	        //convert the byte to hex format
	        for (int i = 0; i < mdbytes.length; i++) {
	        	sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
	        }
	     
	     
         }
         catch( NoSuchAlgorithmException e )
         {
             e.printStackTrace();
         }
         catch( UnsupportedEncodingException e )
         {
             e.printStackTrace();
         }
    	 catch( FileNotFoundException fe)
    	 {
    		 fe.printStackTrace();
    	 }
    	 catch( Exception e)
    	 {
    		 e.printStackTrace();
    	 }
        
    return sb.toString();
    
}//GetFileChecksum

    protected void continueLoadingMap(boolean continueMapLoad){
        mContinueLoadingMap = continueMapLoad;
    }
    
}
