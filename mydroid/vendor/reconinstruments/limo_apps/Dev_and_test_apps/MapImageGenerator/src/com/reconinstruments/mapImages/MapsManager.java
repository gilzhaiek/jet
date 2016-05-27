package com.reconinstruments.mapImages;

import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;

import android.graphics.RectF;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.reconinstruments.mapImages.bo.MapDataBO;
import com.reconinstruments.mapImages.bundlers.MapBundle;
import com.reconinstruments.mapImages.common.MapsHandler;
import com.reconinstruments.mapImages.controllers.MapViewController;
import com.reconinstruments.mapImages.drawings.MapDrawings;
import com.reconinstruments.mapImages.mapdata.DataSourceManager;
import com.reconinstruments.mapImages.objects.ResortData;
import com.reconinstruments.mapImages.objects.ResortInfo;
import com.reconinstruments.mapImages.objects.ResortsInfo;

import java.lang.Math;

public class MapsManager extends MapsHandler  {
	protected static final String TAG = "MapsManager";
	protected static final double HORIZONTAL_RESORT_MARGIN_IN_METERS = 2000;
	protected static final double VERTICAL_RESORT_MARGIN_IN_METERS = 1500;
	
	public enum State {
		PRE_START,
		LOADING_RESORT_INFO,
		ERROR_LOADING_RESORT_INFO,
		WAITING_FOR_LOCATION_TO_LOAD_DATA,	// interim state between resort info loaded and data loading...
		NO_DATA_AVAILABLE_FOR_THIS_LOCATION,
		LOADING_DATA,
		ERROR_LOADING_DATA,
		BUNDLING_DATA,		// have finished loading all objects, need to still bundle data
		DATA_AVAILABLE		// ie, bundle is ready
	}
	
	boolean	mTrapResortInfo = false;
	private static MapDataBO			mMapDataBO = null;
	private static boolean				mRunning = false;
	private static Context				mContext = null;
	private static MapViewController 	mMapViewController = null;
	
	private static ResortsInfo 			mResortsInfo = null;
	private static ResortData 			mResortData = null;
	public static MapDrawings			mMapDrawings = null;
	private static MapBundle			mMapBundle = null;

	protected static final ReentrantLock mLock = new ReentrantLock();
	
	private static boolean				mMainThreadRunning = false;
	public  boolean 					mDataAvailable = false;
	public State 						mState = State.PRE_START;
	public DataSourceManager 			mStateListener = null;  // external object to report service state to
	
	public Location						mClosestResortLocation = null;
	public String						mClosestResortName = null;
	public int							mClosestResortIndex = -1;
	public GenerateResortThread		    mTestResortThread = null;
	public boolean 						mTestingResort = false;

	
	public MapsManager(Context context) {
		mContext = context;
	}
	
	public Bundle GetResortIDBundle() {
		if(mResortData == null)
			return null;
		
		Bundle bundle = new Bundle();
		bundle.putInt("ResortID", mResortData.mResortInfo.ResortID);
		
		return bundle;
	}
	
	public Bundle GetMapBundle() {
		if(mMainThreadRunning) return null;
		
		mMainThreadRunning = true;
		
		Bundle retBundle = null;
		try {
			if(mMapBundle != null){
				Log.v(TAG, "mMapBundle != null");
				if(mMapBundle.mBundle != null) {
					Log.v(TAG, "mMapBundle.mBundle != null");
					retBundle = mMapBundle.mBundle;
				} else {
					mMapBundle = null;
				}
			} 			
			
			if(mMapBundle != null){
			}
			else if(mMapDrawings != null) {
				//Log.v(TAG, "mMapDrawings != null");
				GenerateBundle(mMapDrawings);
			}
			else if(mResortData != null) {
			    //Log.v(TAG, "mResortData != null");
				SuccessResortData(mResortData); 
			}		
			else if(mResortsInfo != null) {
				//Log.v(TAG, "mResortsInfo != null");
				SuccessResortsInfo(mResortsInfo);
			}
		}
		catch (Exception e) { }
		finally {
			mMainThreadRunning = false;
		}
		return retBundle;
	}
	
	public void SetState(State state) {
		if(mState != state) {
			mState = state;
			Log.v(TAG, "State changed to "+state);
			if(mStateListener != null) {
				mStateListener.MapServiceStateChangedTo(mState);
			}
		}
	}	
	
	public void EchoState() {
		if(mStateListener != null) {
			Log.v(TAG, "echo State as "+mState);
			mStateListener.MapServiceStateChangedTo(mState);
		}
	}					
	
	private void CheckClosestResort(int closestResortIndex) {
		if(closestResortIndex != -1 && mClosestResortIndex != closestResortIndex) {
			mClosestResortIndex = closestResortIndex;
			double crLong = mResortsInfo.mResorts.get(closestResortIndex).BoundingBox.right/2. + mResortsInfo.mResorts.get(closestResortIndex).BoundingBox.left/2.;
			double crLat = mResortsInfo.mResorts.get(closestResortIndex).BoundingBox.top/2. + mResortsInfo.mResorts.get(closestResortIndex).BoundingBox.bottom/2.;
			mStateListener.SetClosestResort(mResortsInfo.mResorts.get(closestResortIndex).Name, crLat, crLong);
		}
	}

	public void onLocationChanged(Location location) {
		//SMN:don't need location data when generate resort image
		return;
	}
	
	protected double LongitudeAtLocationPlusHorizontalOffset(double refLatitude, double refLongitude, double distInMeters) {
		double equitorialCircumference = 40075017.0; // m  - taken from wikipedia/Earth

		return refLongitude + distInMeters/(equitorialCircumference*Math.cos(Math.toRadians(refLatitude))) * 360.0;	
	}

	protected double LatitudeAtLocationPlusVerticalOffset(double refLatitude, double refLongitude, double distInMeters) {
		double meridionalCircumference = 40007860.0; // m  - taken from wikipedia/Earth

		return refLatitude + distInMeters/meridionalCircumference * 360.0;
	}

	protected double DistanceBetweenGPSCoords(double latitude1, double longitude1, double latitude2, double longitude2) {
		double equitorialCircumference = 40075017.0; // m  - taken from wikipedia/Earth
		double meridionalCircumference = 40007860.0; // m  - taken from wikipedia/Earth

		double hDist = Math.abs(longitude1-longitude2) / 360.0 * (equitorialCircumference*Math.cos(Math.toRadians(latitude1)));
		double vDist = Math.abs(latitude1-latitude2) / 360.0 * meridionalCircumference;
//		if(mTrapResortInfo) {
//			Log.i(TAG, "   - " + hDist + ", " + vDist);
//		}mResortData
		return Math.sqrt(hDist*hDist + vDist*vDist);
	}

	
	public interface IMapServiceListener {
		public void MapServiceStateChangedTo(MapsManager.State mapServiceState);
	}

	void registerStateListener(DataSourceManager dsm) {
		mStateListener = dsm;
		if(dsm != null) {
			Log.i(TAG, "dsm registered as state listener");
		}
		else {
			Log.i(TAG, "dsm unregistered as state listener");
		}
		EchoState();		// echo current state to StateListener dsm
	}

    public void onStart() {
    	mRunning = true;
    	
    	if(mMapDataBO == null) {
    		mMapDataBO = new MapDataBO(mContext);
    		mMapDataBO.mOwner = this;
    	}
    	    
    	Log.d(TAG, "in start, calling loadResortInfoTask");
    	mMapDataBO.LoadResortInfoTask(this);
    } 
    
    public void onStop() {
    	mRunning = false;
    	mMapDataBO.onStop();
    }
    
    @Override
    public void SuccessDone(Object object) {
    	Log.v(TAG, "SuccessDone");
    	if(!mRunning) return;
    	if(object == null)
    		return;
    
    	if(ResortsInfo.IsMyInstance(object)) 
    		SuccessResortsInfo((ResortsInfo)object);
    	else if(ResortData.IsMyInstance(object))
    		SuccessResortData((ResortData)object);
    	else if(MapDrawings.IsMyInstance(object))
    		SuccessResortDrawing((MapDrawings)object);
    	else if(MapBundle.IsMyInstance(object)) {
    		SuccessMapBundle((MapBundle)object);
    	}
    }
    	
    protected void SuccessResortsInfo(ResortsInfo resortsInfo) {
    	mResortsInfo = resortsInfo;
    }
    
    protected void SuccessResortData(ResortData resortData) {
    	Log.v(TAG, "SuccessResortData");
    	mResortData = resortData;

    	if(mMapViewController == null) {
    		mMapViewController = new MapViewController(mContext);
    	}
    	
    	mMapViewController.TransformResortDataToMapDrawings(resortData, this);	
    }
    
    protected void SuccessResortDrawing(MapDrawings mapDrawings){
    	Log.v(TAG, "SuccessResortDrawing mapDrawings");
    	mMapDrawings = mapDrawings;
    	GenerateBundle(mapDrawings);
    	
    }
    
    protected void SuccessMapBundle(MapBundle mapBundle) {
    	Log.v(TAG, "%%%%%SuccessMapBundle mapBundle");
    	mMapBundle = mapBundle;
    	mDataAvailable = true;
		Log.e(TAG,"------------- has data -----------");
		
		//notify the testResorts
		if (mTestResortThread != null)
			mTestResortThread.notifyThread();
    	
		SetState(State.DATA_AVAILABLE);
		
    	//GenerateResortImages(mContext, mMapDrawings);

   }

    protected void GenerateBundle(MapDrawings mapDrawings){
    	Log.v(TAG, "%%%%%GenerateBundle()");
    	if(mMapViewController == null) return;
    	if(mapDrawings == null) return; 
    	if(mResortData == null) return;
    	
    	//MapsManager.setResortIdName(mapDrawings);
    	
    	mMapViewController.GenerateBundle(mapDrawings, this);


    }
    
    
    
    public void StartGenerateResortImgs() {
    	Log.v(TAG, "StartGenerateResortImgs START");
    	if (mTestResortThread == null){
        	mTestResortThread = new GenerateResortThread(this);
    	}
    	
		if (mTestResortThread.isRunning()) {
			Log.v(TAG, "StartGenerateResortImgs has already started");
			return;
		}
		
       mTestResortThread.start();
		
	}//eof TestResorts
    
    public void NotifyResort(){
    	if (mTestResortThread != null)
    		mTestResortThread.notifyThread();
    }
    
    public void StopGenerateResortImgs() {
    	Log.v(TAG, "StopGenerateResortImgs STOP");
    	if (mTestResortThread == null){
    		return;
    	}

		mTestResortThread.stopThread();
		try {
			mTestResortThread.interrupt();
			mTestResortThread = null;
		}
		catch (Exception e) {
			e.printStackTrace();
			mTestResortThread = null;
		}

    }

    
    class GenerateResortThread extends Thread {
    	
    	private MapsManager mMapsManager = null;
    	private boolean isRunning = false;
    	private boolean isRunNext = false;
		private boolean isTimeout = true;
    	
    	public GenerateResortThread(MapsManager mapManager){
    		mMapsManager = mapManager;
    	}
    	public boolean isRunning() {
    		return isRunning;
    	}
    	
    	public void run(){
			if(mMapDataBO == null) {
				Log.i(TAG,"+location change received before MapsManager started.  Location ignored");
				return;
			}
			
			// Checking Resorts Info
			if(mMapDataBO.IsLoadingResortsInfo()) {// Loading Resorts Info
				Log.i(TAG,"+location change received while loading resort info.  Location ignored");
				return;
			}
			
			if(mResortsInfo == null) {
				Log.e(TAG,"+location change ignored due to problem with loading resort info");
				return;
			}
	
			if(mMapDataBO.IsLoadingResortData()) {
				Log.i(TAG,"+location change received while loading resort data.  Location ignored");
				return;
			}
			
			
			isRunning = true;
			isRunNext = true;
			RectF rBB = null;
			int goodLoaded = 0;
			int badLoaded = 0;
			Log.v(TAG, "*****total_resort_size=" + mResortsInfo.mResorts.size());
			
			for(int i = 0; isRunNext && (i < mResortsInfo.mResorts.size()); i++) {
				
				
				rBB = mResortsInfo.mResorts.get(i).BoundingBox;
				
				Log.v(TAG, "*****id=" + mResortsInfo.mResorts.get(i).ResortID + ", NAME=" + mResortsInfo.mResorts.get(i).Name + ", left=" +rBB.left + ", right="+rBB.right +", top="+rBB.top+", bottom="+rBB.bottom+"\n");
	
				synchronized (this)
				{
					try {
						isTimeout = true;
						Log.v(TAG, "LoadResortDataTask");
						mMapDataBO.LoadResortDataTask(mMapsManager, mResortsInfo.mResorts.get(i));
						Log.v(TAG, "wait 10 mins max to wait loading completion.");
						this.wait(10*60*1000);
					}
					catch(InterruptedException ie){
						Log.v(TAG, "Loading resort, interrrupted Exception....");
						ie.printStackTrace();
						++badLoaded;
						continue;
					}
					catch (Exception e) {
						Log.v(TAG, "Loading Resort, Exception....");
						e.printStackTrace();
						++badLoaded;
						continue;
					}
				}//eof synchronized
					
				if (isTimeout){
					
					Log.v(TAG, "Loading resort, Timeout.");
					Log.v(TAG, "id=" + mResortsInfo.mResorts.get(i).ResortID + ", NAME=" + mResortsInfo.mResorts.get(i).Name + ", left=" +rBB.left + ", right="+rBB.right +", top="+rBB.top+", bottom="+rBB.bottom+"\n");
					isTimeout = true;
					++badLoaded;
					
				}
				else{
					++goodLoaded;
				}
				
				Log.v(TAG, "resort loaded, total=" + (goodLoaded+badLoaded) + ", good=" +goodLoaded+", bad=" +badLoaded+", good%=" + goodLoaded*100/(goodLoaded+badLoaded) +", notifiled=" + ((isTimeout==false)? "1" : "0"));
				
				
			
			if (isRunNext) {
				Log.v(TAG, "sleep 1 second to load another resort....");
				try {
					//wait for loading image
					sleep(1*1000);
				}
				catch (Exception e) {
					Log.v(TAG, "Loading Resort, sleep Exception....");
				}
			}
			
			}//eof for
			isRunning = false;
			Log.v(TAG, "Completed resort loading, total=" + (goodLoaded+badLoaded) + ", good=" +goodLoaded+", bad=" +badLoaded+", good%=" + goodLoaded*100/(goodLoaded+badLoaded));
			
		}//EOF RUN
    	
       public void notifyThread(){
    	   Log.v(TAG, "notifyThread");
    	   synchronized (this){
    		   //Log.v(TAG, "%%%%%%% notify1.");
    		   isTimeout = false;
    		   this.notify();
    	   }
    	   
       }
       public void stopThread(){
    	   isRunNext = false;
    	   notifyThread();
       }

    }//eof class TestRestort
    
    
    public static String getResortIdNameByDrawings(MapDrawings mapDrawings) {
    	
    	String result = "";
    	int resortId = 0;
		String resortName = ""; 
    	ResortInfo resortInfo = null;
    	
		if (mapDrawings != null){
			resortInfo = mapDrawings.mResortInfo;
			if (resortInfo != null)
				resortId = resortInfo.ResortID;
				resortName = resortInfo.Name;
		}
		
		
    	if (resortName == null) {
    		result = "None";
    	}
    	else if (resortName.length() > 200) {
    		result = resortName.substring(0, 200);
    	}
    	else 
    		result = resortName;
    	
       result += "_" + resortId;
       return result;
    }//eof getResortIdNameByDrawings
    
    

/*    
    private boolean  jpeg_file_is_complete(String path) {
    	try {
    	File file = new File(path);
    	if (file == null)
    		return false;
    	
    	RandomAccessFile raf = new RandomAccessFile(file, "r");
    	
    	if (raf == null)
    		return false;
    	
    	// Seek to the end of file
    	int n =2;
    	raf.seek(file.length() - n);
    	// Read it out.
    	byte[]  yourbyteArray = new byte[n+1];
    	
    	int size = raf.read(yourbyteArray, 0, n);
    	
    	if (size != 2)
    		return false;
    	
    	if (yourbyteArray[0]!=0xFF || yourbyteArray[1] != 0xD9)
    		return false;
    				
    	raf.close();
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    		Log.v(TAG, "jpeg_file_is_complete, exception");
    		
    	}
        
        return true;
    }

    public boolean isjpeg_file_is_corrupted(String name) {
        return !jpeg_file_is_complete(name);
    }
    
     */
    
}
