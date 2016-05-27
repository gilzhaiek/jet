package com.reconinstruments.geodataservice.datasourcemanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Xml;

import com.reconinstruments.geodataservice.GeodataService;
import com.reconinstruments.geodataservice.R;
import com.reconinstruments.geodataservice.SourcedDataContainer;
import com.reconinstruments.geodataservice.TileDataRequest;
import com.reconinstruments.geodataservice.TileLoadingFromNetworkException;
import com.reconinstruments.geodataservice.datasourcemanager.DynamicData.BuddyTranscoder;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.MDTranscoder;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Base.ReconBaseTranscoder;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Snow.ReconSnowTranscoder;
import com.reconinstruments.geodataservice.devinterface.DevTestingState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoTile;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortInfoResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortRequest;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;
import com.reconinstruments.webapi.ReconOSHttpClient;

public class DataSourceManager implements StaticMapDataTranscoder.IStaticMapDataTranscoderCallbacks 
{
// constants 
	private final static String TAG = "DataSourceManager";
	private final static String PROFILE_FOLDER = "ReconApps/MapData";
	private final static long MAX_WAIT_TIME_IN_MS_FOR_LOCATION_LOADING_DATA = 180000;
//	public enum AvailableTranscoders {
//		RECON_SKI_DATA,
//		OPEN_STREET_MAPS
//	}
	
	public enum DSMState {
		INITIALIZING,
		ERROR_INITIALIZING,
		READY_NO_USER_LOCATION,
		READY_WITH_USER_LOCATION_LOADING_DATA,
		READY_WITH_USER_LOCATION,
	}
   
// members 
	private static Context 				mContext = null;
	private GeodataService 				mOwner = null;
	private ArrayList<DSMTranscoder> 	mNonInitializedTranscoders = new ArrayList<DSMTranscoder>();
	public 	DSMState   					mState = null;
	private DevTestingState				mDevTestingState = null;
	private TreeMap<Capability.DataSources, DSMTranscoder> mTranscoders = new TreeMap<Capability.DataSources, DSMTranscoder>();
	private TreeMap<String, Integer> 	mDataTypeMapping = new TreeMap<String, Integer>();
//	private TreeMap<String, DataRetrievalCapability> 	mDataTypes = new TreeMap<String, DataRetrievalCapability>();
    private ReconOSHttpClient			mClient;
    private float						mDataLoadedPercentage = 1.0f;
    private long 						mStartLoadingDataTime = 0;
    private Handler 					mHandler;

// methods
	public DataSourceManager(GeodataService geodataService, DevTestingState _devTestingState) throws Exception{
		mOwner = geodataService;
		mState = DSMState.INITIALIZING;

		mContext = geodataService;
		
		mDevTestingState = _devTestingState;
		if( (mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
				 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.DSM_CREATE_ERROR.ordinal()) == 1) ) {
					throw new Exception ("Forced DSM creation error.");
		}

	} 

	public void init(ArrayList<Capability> serviceCapabilities) throws Exception{	 
		// create transcoders and add them to list
		
	    mHandler = new Handler();
//		Log.d(TAG, "Loading transcoders...");
		loadDSMConfiguration(mContext, mTranscoders);		// loads & inits transcoders from product_id.xml
		
		Log.d(TAG, "number of transcoders  = "+ mTranscoders.size());
		assert(mTranscoders.size() > 0) ;
		
		for(DSMTranscoder transcoder : mTranscoders.values()) {		// place each loaded transcoder in array to track init progress
			Log.d(TAG, "Transcoder: " + transcoder.mSourceID);
			transcoder.init();							// possibly asynctask - when done, this call mOwner.InitializationComplete()  (see StaticMapDataTranscoderInitializeComplete)
			mNonInitializedTranscoders.add(transcoder);
		}
		// add data retrieval capabilities to serviceCapabilities list
		for (DSMTranscoder transcoder: mTranscoders.values()){
			transcoder.AddCapabilities(serviceCapabilities);	// transcoder adds it's capabilites - fixed for each Transcoder
		}


	}
 
	public void setDevTestingState(DevTestingState _devTestingState) {  
		mDevTestingState = _devTestingState;
		if(mTranscoders != null) {
			for (DSMTranscoder transcoder: mTranscoders.values()){
				if(transcoder != null) transcoder.setDevTestingState(_devTestingState);  	// to propagate run-time DevTestingState changes to DSM's transcoders
			}
		}
	}

	public GeoTile SourceTileData(TileDataRequest tileDataRequest) throws Exception {
		// this method breaks the data request into source-dependent data calls which it routes to each source-dependent transcoder,
		// afterwards it amalgamates the results into a GeoTile	
		try {
			for(SourcedDataContainer sourcedDataContainer : tileDataRequest.mSourcedDataContainers) {
				DSMTranscoder nextTranscoder = (DSMTranscoder) mTranscoders.get(sourcedDataContainer.mSource);
				if(nextTranscoder == null) {
					throw new Exception ("Non-existant DSM Transcoder referenced in SourceData request"); 
				}
				sourcedDataContainer.mRequestedGeoTileIndex = tileDataRequest.mRequestedGeoTileIndex;	// distribute tile index to load
				nextTranscoder.SourceData(sourcedDataContainer);	// fill in sourceData data or fail trying
																	// TODO this used to spawn an asyncTask but now doesn't so Transcoder loading is sequential if data request requres more than one source
			}
		}
		catch (TileLoadingFromNetworkException e) {
			throw e;
		}		 

		catch (Exception e) {
			Log.e(TAG,"Error during DSM SourceData: " + e.getMessage());
//			mOwner.DSMBackgroundSourcingDataError(tileDataRequest, e.getMessage());
			throw new Exception("Error during DSM SourceData: " + e.getMessage());
		}
		
		GeoTile resultTile = new GeoTile(tileDataRequest.mRequestedGeoTileIndex);
		for(SourcedDataContainer loadedSDC : tileDataRequest.mSourcedDataContainers) {
			for(WorldObject loadedWorldObject : loadedSDC.mRetrievedWorldObjects) {		// transfer loaded world objects from each data source into GeoTile object
				resultTile.mWorldObjectArray.add(loadedWorldObject);
			}
		}

		return resultTile;
	}

	Runnable mLoadCacheCheckingTimer = new Runnable() {
		@Override 
		public void run() {
			long msSinceStartLoad = System.currentTimeMillis() - mStartLoadingDataTime;
			if(msSinceStartLoad > MAX_WAIT_TIME_IN_MS_FOR_LOCATION_LOADING_DATA) {
				mState = DSMState.READY_WITH_USER_LOCATION;		// force ready state incase something wrong with datasource cache load
				mOwner.DSMLocationStateChanged(mState);
			}
			else {
				int numDataElements = 0;
				int numDataElementsLoaded = 0;

				for (DSMTranscoder transcoder: mTranscoders.values()){
					if(transcoder != null && (transcoder instanceof StaticMapDataTranscoder)) {
						try {
							ArrayList<Integer> dataElements = ((StaticMapDataTranscoder)transcoder).GetCachedDataStats();  	// to propagate to each transducer
							int cnt=0;
							if(dataElements != null) {
								numDataElements += dataElements.get(0) ;
								numDataElementsLoaded += dataElements.get(1);
//								Log.e(TAG, "     elements for " + cnt++ + "=" + dataElements.get(0) + ", loaded=" +dataElements.get(1));
							}
						}
						catch (Exception e) {
//							Log.e(TAG,"Error loading caches for GeodataService data sources.");
						}
					}
				}
//				Log.e(TAG, "------ Num elements=" + numDataElements + ", loaded=" +numDataElementsLoaded);
				if(numDataElements > 0) {
					if(numDataElementsLoaded == numDataElements) {
						mState = DSMState.READY_WITH_USER_LOCATION;
						mOwner.DSMLocationStateChanged(mState);
						return;		// stop timer 
						//	mHandler.removeCallbacks(mLoadCacheCheckingTimer);
					}
					else{
						// figure out progress and broadcast to user feedback (if changed) 
						float currentLoadedPercentage = (float)numDataElementsLoaded/(float)numDataElements;
						if(currentLoadedPercentage != mDataLoadedPercentage) {
							mDataLoadedPercentage = currentLoadedPercentage;
							// broadcast change - through callback to service...
							mOwner.BroadcastDataLoadPercentage(currentLoadedPercentage);
						}
					}
				}
				//Log.e(TAG, "service init data: " + numDataElementsLoaded + "of " + numDataElements + " state = " + mState);
			}

			mHandler.postDelayed(mLoadCacheCheckingTimer, 1000);  // repeat if not done
		}
	};

	public void SetUserLocation(float longitude, float latitude) throws Exception {
//		Log.e(TAG,"PrefetchingLocation data -----------------------");
		for (DSMTranscoder transcoder: mTranscoders.values()){
			if(transcoder != null && (transcoder instanceof StaticMapDataTranscoder)) {
				((StaticMapDataTranscoder)transcoder).SetUserLocation(longitude, latitude);  	// to propagate to each transducer
			}
		}
		if(mState == DSMState.READY_NO_USER_LOCATION) { 
			mState = DSMState.READY_WITH_USER_LOCATION_LOADING_DATA;
			mOwner.DSMLocationStateChanged(mState);

			mStartLoadingDataTime = System.currentTimeMillis();
			mHandler.postDelayed(mLoadCacheCheckingTimer, 1000);
		}
		
//		if(mState == DSMState.READY_WITH_USER_LOCATION_LOADING_DATA) {
//			// state change now handled in mLoadCacheCheckingTimer() runnable
//		}
	}
	
//=============================================
// internal
	void loadDSMConfiguration(Context context, TreeMap<Capability.DataSources, DSMTranscoder> transcodersMap) throws Exception {


		Resources res = context.getResources();
		XmlPullParser parser = Xml.newPullParser();

		try {
			InputStream is;
			BufferedReader br;
			File path = Environment.getExternalStorageDirectory();
			File file = new File(path, PROFILE_FOLDER + "/" + "dsm_configuration.xml"); 
			br = new BufferedReader(new FileReader(file));

		    // auto-detect the encoding from the stream
			parser.setInput(br);

			boolean done = false;
			int eventType = parser.getEventType();   // get and process event

			if( (mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
					 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.DSM_CONFIG_XML_READ_ERROR.ordinal()) == 1) ) {
						throw new Exception ("Forced dsm_configuration.xml read error.");
			}

			while (eventType != XmlPullParser.END_DOCUMENT && !done){
				String name = null;

				name = parser.getName();
				if(name == null) name = "null";
//				Log.e(TAG, "eventType:"+eventType + "-"+ name);

				switch (eventType){
					case XmlPullParser.START_DOCUMENT: {
						name = parser.getName();
						break;
					}
	
					case XmlPullParser.END_TAG: {
						name = parser.getName();
						break;
					}

					case XmlPullParser.START_TAG: { 
						name = parser.getName();
						if(name != null) {
							if(name.equalsIgnoreCase("file_id")){ 
								String fileID = parser.getAttributeValue(0);
								Log.i(TAG, "Loading configuration.xml file: " + fileID);
							} 
							if(name.equalsIgnoreCase("profile")){ 
								String profileName = parser.getAttributeValue(0);
							} 
							if(name.equalsIgnoreCase("transcoder")){ 
								String transcoderName = parser.getAttributeValue(0);
								if(transcoderName.equalsIgnoreCase("MDSkiTranscoder")) {
									MDTranscoder newMDTranscoder = new MDTranscoder(this, mContext, mDevTestingState);
									mTranscoders.put(Capability.DataSources.MD_SKI, newMDTranscoder);
								}
								if(transcoderName.equalsIgnoreCase("ReconSnowTranscoder")) {
									ReconSnowTranscoder newMDTranscoder = new ReconSnowTranscoder(this, mContext, mDevTestingState);
									mTranscoders.put(Capability.DataSources.RECON_SNOW, newMDTranscoder);
								}
								if(transcoderName.equalsIgnoreCase("ReconBaseTranscoder")) {
									ReconBaseTranscoder newReconBaseTranscoder = new ReconBaseTranscoder(this, mContext, mDevTestingState);
									mTranscoders.put(Capability.DataSources.RECON_BASE, newReconBaseTranscoder);
								}
							
							} 
						}
					}
				} // end switch
			    eventType = parser.next();   // get and process event

			}	// while end
	
		}
		catch (Exception e) {
			Log.e(TAG, "Error loading dsm_configuration.xml file: " + e.getMessage());
			throw new Exception("Error loading dsm_configuration.xml file: " + e.getMessage());
		}

	}
	
	
//=============================================
// interfaces

	// DSMTranscoder.IStaticMapDataTranscoderCallbacks for superclass
	public interface IDataSourceManagerCallbacks {
		public void DSMInitializeComplete(Object dsm);
		public void DSMInitializationError(Object component, String errorMsg);
		public void DSMLocationStateChanged(DSMState state);
//		public void DSMBackgroundSourcingDataComplete(TileDataRequest tileDataRequest);   // data requests no longer need these callbacks as they are now blocking
//		public void DSMBackgroundSourcingDataError(TileDataRequest tileDataRequest, String errorMsg);
	}

	
	// IDSMTransducer callbacks
	@Override
	public void StaticMapDataTranscoderInitializeComplete(StaticMapDataTranscoder dsmTranscoder) {
		Log.i(TAG,"StaticMapDataTranscoderInitializeComplete");

		Iterator itr = mNonInitializedTranscoders.iterator();
		while(itr.hasNext()){
			if(itr.next().equals(dsmTranscoder)) {
				itr.remove();
			}
		}
		
		if(mNonInitializedTranscoders.size() == 0) {	// once all transcoders have been initialized
			mState = DSMState.READY_NO_USER_LOCATION;
			mOwner.DSMInitializeComplete(this);
		}
	}
	
	@Override
	public void StaticMapDataTranscoderInitializeError(Object component, String errorMsg)  {
		Log.e(TAG,"StaticMapDataTranscoderInitializeError");
		mOwner.DSMInitializationError(component, errorMsg);
		mState = DSMState.ERROR_INITIALIZING;
	}

	@Override
	public void StaticMapDataTranscoderBackgroundSourcingDataComplete(SourcedDataContainer sourceDataContainer) {
		Log.e(TAG,"DSMTranscoderSourcingDataComplete for source: " + sourceDataContainer.mSource);
		
		Iterator itr = sourceDataContainer.mParentDataRequest.mStillToLoad.iterator();
		while(itr.hasNext()){
			if(itr.next().equals(sourceDataContainer)) {
				itr.remove();
			}
		}

//		if(sourceDataContainer.mParentDataRequest.IsLoaded()) {								// no longer reoporting back asynch... data requests are now blocking
//			mOwner.DSMBackgroundSourcingDataComplete(sourceDataContainer.mParentDataRequest);  
//		}
	}
	
	@Override
	public void StaticMapDataTranscoderBackgroundSourcingDataError(SourcedDataContainer sourceDataContainer, String errorMsg)  {
		Log.e(TAG,"DSMTranscoderSourcingDataError");
//		mOwner.DSMBackgroundSourcingDataError(sourceDataContainer.mParentDataRequest, errorMsg);
		
	}

	public ArrayList<ResortInfoResponse> GetClosestResorts(ResortRequest resortRequest) {
		DSMTranscoder trans = mTranscoders.get(resortRequest.mDataSource);
		if(trans == null) return new ArrayList<ResortInfoResponse>();
		else return mTranscoders.get(resortRequest.mDataSource).GetClosestResorts(resortRequest);
		
	}

	public void phoneNetworkIsAvailable(boolean phoneNetworkIsAvailable) {
		for(DSMTranscoder transcoder : mTranscoders.values()) {		
			transcoder.phoneNetworkIsAvailable(phoneNetworkIsAvailable);							
		}
		
	}

}
