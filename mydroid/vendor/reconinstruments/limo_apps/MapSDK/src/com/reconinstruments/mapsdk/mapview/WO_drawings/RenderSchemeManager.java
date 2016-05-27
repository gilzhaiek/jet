package com.reconinstruments.mapsdk.mapview.WO_drawings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import com.reconinstruments.mapsdk.mapview.LoadFromResourceUtil;
import com.reconinstruments.mapsdk.mapview.ParsedObj;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MeshGL;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MeshGL.MeshType;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.ShaderProgram;
import com.reconinstruments.mapsdk.R;
import com.reconinstruments.utils.DeviceUtils;


public class RenderSchemeManager {
	private final static String TAG = "RenderSchemeManager";
	private final static String PROFILE_FOLDER = "ReconApps/MapData";
	
	public enum ObjectTypes {
		BITMAP,
		NAMED_PATHS,
		AREAS
	}
	
	public enum BitmapTypes {
		BUDDY,
		CARPARK,
		CHAIRLIFT,
		INFORMATION,
		RESTAURANT,
		STORE,
		HOSPITAL,
		WASHROOM,
		DRINKINGWATER
	}
	
	public enum NamedPathTypes {
		SKIRUN_GREEN,
		SKIRUN_BLUE,
		SKIRUN_BLACK,
		SKIRUN_DOUBLEBLACK,
		SKIRUN_RED,
		SKITRUNK_GREEN,
		SKITRUNK_BLUE,
		SKITRUNK_BLACK,
		SKITRUNK_DOUBLEBLACK,
		SKITRUNK_RED,
		CHAIRLIFT,
		ROAD_FREEWAY,
		ROAD_ARTERY_PRIMARY,
		ROAD_ARTERY_SECONDARY,
		ROAD_ARTERY_TERTIARY,
		ROAD_RESIDENTIAL,
		WALKWAY,
		BORDER_NATIONAL,
		WATERWAY
	}
	
	public enum AreaTypes {
		AREA_LAND,
		AREA_OCEAN,
		AREA_CITYTOWN,
		AREA_WOODS,
		AREA_PARK,
		AREA_WATER,
		AREA_SCRUB,
		AREA_TUNDRA,
		AREA_SKIRESORT,
		NO_DATA_ZONE
	}
	
	public enum Rates {
		FRAME_RATE,
		PAN_RATE,
		ROTATION_RATE,
		SCALE_RATE,
		PITCH_RATE,
		VELOCITY_RATE
	}
	
	public enum Messages {
		ERROR_INITIALIZING,
		WAITING_FOR_DATA_SERVICE,
		WAITING_FOR_LOCATION,
		PROBLEM_WITH_DATA_SERVICE,
		REQUIRED_DATA_UNAVAILABLE,
		OUT_OF_MEMORY,
		ERROR_LOADING_THEMES,
		LOADING_DATA,
		ERROR_LOADING_DATA,
		WAITING_FOR_MAP
	}
	
	public enum TrailPaint {
		LINE,
		TEXTSIZE,
		TEXTOUTLINE
	}
	public boolean mErrorSchemesNotLoadedCorrectly = false;
	public boolean mLoadingSettingsFile = false;
	boolean mTrailLabelCapitalization = false;

	Context mContext;

	String mCurrentlyParsing = "";
	int mParseCurPOIType 	= 0;
	int mParseCurTrailType 	= 0;
	int mParseCurTrailSubType = 0;
	int mParseCurAreaType 	= 0;
	int	mParseQScaleCnt 	= 0;
	int mParseCurTypeID 	= 0;
	
	int	mNumQScales 	=  4;				// TODO init these with data from file (2 pass)
	int mNumPOITypes 	= BitmapTypes.values().length;
	int mNumPOIStates 	=  4;
	int mNumTrailTypes 	= NamedPathTypes.values().length;
	int mNumTrailStates =  2;
	int mNumAreaTypes 	= AreaTypes.values().length;
	int mNumZoomLevels	=  4;
   int mNumMeshTypes       = MeshType.values().length;
   int mMeshCacheSize	=  8;
   int mLastParsedMeshIndex = -1;
	        
   ParsedObj               mParsedMesh;
   Bitmap 				   mUserIcon = null;
   String                  mLastParsedMeshFile = "";
   MeshGL                  mLastParsedMesh = null;
	        String[]                mMessages = null;
   String[]                mVertShaderFiles = null;
   String[]                mFragShaderFiles = null;
   String[]                mMeshFiles = null;
   
   /**
    * The array of meshes. Each outermost element may have multiple meshes,
    * as a cache, if a cache value > 1 is specified. As a result, for each 
    * mesh that we want to render with a texture, we can check if a mesh with 
    * that texture has been loaded before. If so, load that. <br><br> 
    * 
    * e.g.<br><br>
    * 
    * If {@code #mMeshCacheSize = 2}, and we have 4 MeshTypes, 
    * the size will be {@code mMeshes = new MeshGL[4][2]}
    */
   MeshGL[][]              mMeshes = null;
   /**
    * Maps a POI id to a MeshType<br><br>
    * e.g. <br>mPOIMeshes[0] (id=0) will have a 
    * MeshType of MeshType.BUDDY <br><br>
    */
   MeshType[]              mPOIMeshes = null;
   ShaderProgram[] mShaderPrograms = null;

	ScaleRange[] 	mScaleRanges = null;			
	Bitmap[][][] 	mPOIBitmaps = null;  
	int[][][] 		mPOIBitmapOffsetPercents = null;  
	double[] 		mBitmapScale = null;  
	int[][] 		mPaintSizesForScale = null;  
	Paint[][][] 	mTrailPaints = null;
	Paint[][][] 	mTrailTextPaints = null;
	Paint[][][] 	mTrailOutlinePaints = null;
	Paint[] 		mAreaPaints = null;
	double[] 		mZoomLevels = null;
	double[] 		mRates = null;
	double 			mMaxZoomScale = 0.25;
    int 			mMapBackgroudColor = Color.WHITE;
    int 			mMapBackgroudAlpha = 255;
    boolean 		mMapAreaOutlineOn = false;	
    boolean 		mMapTrailOutlineOn = false;
    int 			mGridBkgdColor = Color.BLACK;
    int 			mGridLineColor = Color.GRAY;
    int 			mGridAlpha = 128;
    int 			mGridLineWidth = 1;
    float			mIdleVelocityThrshold = 3.0f;
    int 			mPanRolloverTextSize = 20;
    int 			mPanRolloverTextColor = Color.WHITE;
    int 			mPanRolloverTextOulineWidth = 2;
    int 			mPanRolloverTextOulineColor = Color.BLACK;
    int 			mPanRolloverBoxBGColor = Color.WHITE;
    int 			mPanRolloverBoxBGAlpha = 255;
    int 			mPanRolloverBoxOutlineColor = Color.BLACK;
    int 			mPanRolloverBoxOutlineAlpha = 255;
    int 			mPanRolloverBoxOutlineWidth = 2;
    int 			mResortNameTextSize = 20;
    int 			mResortNameTextColor = Color.YELLOW;
	float 			mTrailLabelOffsetFactor = 0.5f;

	// this.getPackageName() to get packageName
	
	public RenderSchemeManager(Context context, String hardwareID, Resources res, String packageName, String xmlFile) {

		mContext = context;
		
		LoadSettingsFile(hardwareID, res, packageName, xmlFile);

	}
	
	class ScaleRange {
		double mLower = 0.0;
		double mUpper = 0.0;
		int	   mQScale = 0;

		ScaleRange(double lower, double upper, int qScale) {
			mLower = lower;
			mUpper = upper;
			mQScale = qScale;
		}
		
		boolean contains(double viewScale) {
			return (mLower < viewScale && viewScale <= mUpper);
		}
	}
	
	public int getScaleRange(double viewScale) {
		for(ScaleRange qscale : mScaleRanges) {
			if(qscale.contains(viewScale)) {
				return qscale.mQScale;
			}
		}
		return 1;
	}
	
	public void LoadSettingsFile(String hardwareID, Resources res, String packageName, String xmlFile) {
		mErrorSchemesNotLoadedCorrectly = false;

		// TODO get array dimensions from data file...  for now dimensions are hard coded above
		
		
		mLoadingSettingsFile = true;		// semaphore to block activity while class data is unstable
		
		mMessages = new String[Messages.values().length]; 
      mVertShaderFiles = new String[mNumMeshTypes];
      mFragShaderFiles = new String[mNumMeshTypes];
      mMeshFiles = new String[mNumMeshTypes];
      mPOIMeshes = new MeshType[mNumPOITypes];
      mShaderPrograms = new ShaderProgram[mNumMeshTypes];

		mScaleRanges = new ScaleRange[mNumQScales];			
		mPOIBitmaps = new Bitmap[mNumPOITypes][mNumPOIStates][mNumQScales];  
		mPOIBitmapOffsetPercents = new int[mNumPOITypes][mNumPOIStates][mNumQScales];  
		mBitmapScale = new double[mNumQScales];
		mPaintSizesForScale = new int[mNumQScales][5];
		mTrailPaints = new Paint[mNumTrailTypes][mNumTrailStates][mNumQScales];
		mTrailTextPaints = new Paint[mNumTrailTypes][mNumTrailStates][mNumQScales];
		mTrailOutlinePaints = new Paint[mNumTrailTypes][mNumTrailStates][mNumQScales];
		mAreaPaints = new Paint[mNumAreaTypes];
		mZoomLevels = new double[mNumZoomLevels];
		mRates = new double[Rates.values().length];
		
		int i, j, k;
		for(i = 0; i<Messages.values().length; i++) {
			mMessages[i] = "";
		}
		for(i = 0; i<mNumPOITypes; i++) {
			for(j = 0; j<mNumPOIStates; j++) {
				for(k = 0; k<mNumQScales; k++) {
					mPOIBitmaps[i][j][k] = null;
					mPOIBitmapOffsetPercents[i][j][k] = 0;
				}
			}
		}
		for(i = 0; i<mNumTrailTypes; i++) {
			for(j = 0; j<mNumTrailStates; j++) {
				for(k = 0; k<mNumQScales; k++) {
					mTrailPaints[i][j][k] = null;
					mTrailTextPaints[i][j][k] = null;
					mTrailOutlinePaints[i][j][k] = null;
				}
			}
		}
		for(i = 0; i<mNumQScales; i++) {
			mAreaPaints[i] = null;
		}
		for(i = 0; i<mNumZoomLevels; i++) {
			mZoomLevels[i] = -1.0;
		}
		mRates[Rates.FRAME_RATE.ordinal()] = 15.0;
		mRates[Rates.PAN_RATE.ordinal()] = 0.0002;		// in degrees per screen update (ie approx. at frame rate)
		mRates[Rates.ROTATION_RATE.ordinal()] = 3.0;	// in degrees
		mRates[Rates.SCALE_RATE.ordinal()] = 0.2;
		
		ReadSettingsXMLFile(hardwareID, res, packageName, xmlFile);
		
		mLoadingSettingsFile = false;

	}
	
	public void ReadSettingsXMLFile (String hardwareID, Resources res, String packageName, String xmlFile) {
		
		XmlPullParser parser = Xml.newPullParser();
		try {
			BufferedReader br;
			File path = Environment.getExternalStorageDirectory();
			File file = new File(path, PROFILE_FOLDER + "/" + xmlFile); 
			br = new BufferedReader(new FileReader(file));
			
		    // auto-detect the encoding from the stream
		    parser.setInput(br);

		    boolean done = false;
		    int eventType = parser.getEventType();   // get and process event
		    
		    Typeface streetNameFont = LoadFromResourceUtil.getFontFromRes(mContext, R.raw.opensans_semibold);
		    
		    while (eventType != XmlPullParser.END_DOCUMENT && !done){
		        String name = null;
                
		        switch (eventType){
		            case XmlPullParser.START_DOCUMENT:
		                name = parser.getName();
		                break;
		            case XmlPullParser.START_TAG:
		                name = parser.getName();
		                if (name.equalsIgnoreCase("file_id")){
		                	String fileID = parser.getAttributeValue(0);
		                	Log.i(TAG, "processing scheme file: " +fileID);
		                	if(DeviceUtils.isSnow2()){
		                		mMeshCacheSize = 18;
		                	} else {
		                		mMeshCacheSize = 4;
		                	}
		                	mMeshes = new MeshGL[mNumMeshTypes][(mMeshCacheSize < 1) ? 1 : mMeshCacheSize];
		                	Log.i(TAG, "Mesh Cache size is " + ((mMeshCacheSize < 1) ? "less than 1... setting to 1" : String.valueOf(mMeshCacheSize)));
		                } 
		                if (name.equalsIgnoreCase("scale_ranges")){
		                	mCurrentlyParsing = "scale_ranges";
		                } 
		                if (name.equalsIgnoreCase("zoom_levels")){
		                	mCurrentlyParsing = "zoom_levels";
		                } 
		                if (name.equalsIgnoreCase("general")){
		                	mCurrentlyParsing = "general";
		                } 
		                if (name.equalsIgnoreCase("pois")){
		                	mCurrentlyParsing = "POIs";
		                } 
		                if (name.equalsIgnoreCase("trails")){
		                	mCurrentlyParsing = "Trails";
		                } 
		                if (name.equalsIgnoreCase("areas")){
		                	mCurrentlyParsing = "Areas";
		                } 
		                if (name.equalsIgnoreCase("messages")){
		                	mCurrentlyParsing = "Messages";
		                } 
		                if (name.equalsIgnoreCase("rates")){
		                	mCurrentlyParsing = "Rates";
		                } 
                        if (name.equalsIgnoreCase("meshes")){
                                       mCurrentlyParsing = "Meshes";
                               }
		                
		                if (name.equalsIgnoreCase("message")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 2) {
		                		Log.e(TAG, "Bad message while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		String item = parser.getAttributeValue(0);
		                		int msgIndex = -1;
		                		if(item.equalsIgnoreCase("error_initializing"))		{ msgIndex = Messages.ERROR_INITIALIZING.ordinal(); };
		                		if(item.equalsIgnoreCase("waiting_for_service"))	{ msgIndex = Messages.WAITING_FOR_DATA_SERVICE.ordinal(); };
		                		if(item.equalsIgnoreCase("waiting_for_location")) 	{ msgIndex = Messages.WAITING_FOR_LOCATION.ordinal(); };
		                		if(item.equalsIgnoreCase("error_with_service")) 	{ msgIndex = Messages.PROBLEM_WITH_DATA_SERVICE.ordinal(); };
		                		if(item.equalsIgnoreCase("map_data_unavailable")) 	{ msgIndex = Messages.REQUIRED_DATA_UNAVAILABLE.ordinal(); };
		                		if(item.equalsIgnoreCase("out_of_memory")) 			{ msgIndex = Messages.OUT_OF_MEMORY.ordinal(); };
		                		if(item.equalsIgnoreCase("error_loading_themes"))	{ msgIndex = Messages.ERROR_LOADING_THEMES.ordinal(); };
		                		if(item.equalsIgnoreCase("loading_data")) 			{ msgIndex = Messages.LOADING_DATA.ordinal(); };
		                		if(item.equalsIgnoreCase("error_loading_data")) 	{ msgIndex = Messages.ERROR_LOADING_DATA.ordinal(); };
		                		if(item.equalsIgnoreCase("waiting_for_map")) 		{ msgIndex = Messages.WAITING_FOR_MAP.ordinal(); };
		                		
		                		if(msgIndex == -1) {
			                		Log.e(TAG, "Bad message while parsing in render schemes: invalid item '" + item + "'");
		               				mErrorSchemesNotLoadedCorrectly = true;
		                		}
		                		else {
		                			mMessages[msgIndex] = parser.getAttributeValue(1);
		                		} 
		                	}	
		                }

		                if (name.equalsIgnoreCase("rate")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 2) {
		                		Log.e(TAG, "Bad rate while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		String item = parser.getAttributeValue(0);
		                		int rIndex = -1;
		                		if(item.equalsIgnoreCase("frame_rate")) 	{ rIndex = Rates.FRAME_RATE.ordinal(); };
		                		if(item.equalsIgnoreCase("pan_rate")) 		{ rIndex = Rates.PAN_RATE.ordinal(); };
		                		if(item.equalsIgnoreCase("rotation_rate")) 	{ rIndex = Rates.ROTATION_RATE.ordinal(); };
		                		if(item.equalsIgnoreCase("scale_rate")) 	{ rIndex = Rates.SCALE_RATE.ordinal(); };
		                		if(item.equalsIgnoreCase("velocity_rate")) 	{ rIndex = Rates.VELOCITY_RATE.ordinal(); };
		                		if(rIndex == -1) {
			                		Log.e(TAG, "Bad rate while parsing in render schemes: invalid item '" + item + "'");
		               				mErrorSchemesNotLoadedCorrectly = true;
		                		}
		                		else {
		                			mRates[rIndex] = Double.parseDouble(parser.getAttributeValue(1));
		                		} 
		                	}	
		                }

		                if (name.equalsIgnoreCase("user_icon")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 1) {
		                		Log.e(TAG, "Bad user_icon while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		mUserIcon = BitmapFactory.decodeResource(res, res.getIdentifier(parser.getAttributeValue(0), "drawable", packageName)); 
		                	}
		                } 
		                

		                if (name.equalsIgnoreCase("scale_range")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 3) {
		                		Log.e(TAG, "Bad scale_range while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;

		                	}
		                	else {
		                		mScaleRanges[mParseQScaleCnt++] = new ScaleRange(Double.parseDouble(parser.getAttributeValue(1)), Double.parseDouble(parser.getAttributeValue(2)), Integer.parseInt(parser.getAttributeValue(0)));
		                	}
		                } 
		                
		                if (name.equalsIgnoreCase("trail_label_offset")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 1) {
		                		Log.e(TAG, "Bad trail_label_offset while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;

		                	}
		                	else {
		                		mTrailLabelOffsetFactor = (float)Double.parseDouble(parser.getAttributeValue(0));
		                	}
		                } 
		                
		                if (name.equalsIgnoreCase("trail_label_capitaliztion")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 1) {
		                		Log.e(TAG, "Bad trail_label_capitaliztion while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;

		                	}
		                	else {
		                		mTrailLabelCapitalization = (parser.getAttributeValue(0).equalsIgnoreCase("true") ? true : false);
		                	}
		                } 
		                
		                if (name.equalsIgnoreCase("idle_velocity")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 1) {
		                		Log.e(TAG, "Bad scale_range while parsing in render schemes: incorrect number of attributes");
		                		mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		mIdleVelocityThrshold = Float.parseFloat(parser.getAttributeValue(0));
		                	}
		                } 
		                
		                if (name.equalsIgnoreCase("zoom_level")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 2 && numAttributes != 3) {
		                		Log.e(TAG, "Bad zoom_level while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		int curIndex = Integer.parseInt(parser.getAttributeValue(0))-1;
		                		mZoomLevels[curIndex] = Double.parseDouble(parser.getAttributeValue(1));
		                		if(mZoomLevels[curIndex] > mMaxZoomScale ) mMaxZoomScale = mZoomLevels[curIndex];
		                	}
		                }
		                if (name.equalsIgnoreCase("asset_scale")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 2) {
		                		Log.e(TAG, "Bad asset_scale while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		mBitmapScale[Integer.parseInt(parser.getAttributeValue(0))-1] = Double.parseDouble(parser.getAttributeValue(1));
		                	} 
		                }
		                
		                if (name.equalsIgnoreCase("sizes4scale_range")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 6) {
		                		Log.e(TAG, "Bad sizes4scale_range while parsing in render schemes: incorrect number of attributes: " + numAttributes);
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		mPaintSizesForScale[Integer.parseInt(parser.getAttributeValue(0))-1][0] = Integer.parseInt(parser.getAttributeValue(1));	// trail width
		                		mPaintSizesForScale[Integer.parseInt(parser.getAttributeValue(0))-1][1] = Integer.parseInt(parser.getAttributeValue(2));	// text size
           						mPaintSizesForScale[Integer.parseInt(parser.getAttributeValue(0))-1][2] = Integer.parseInt(parser.getAttributeValue(3),16);	// text color
		                		mPaintSizesForScale[Integer.parseInt(parser.getAttributeValue(0))-1][3] = Integer.parseInt(parser.getAttributeValue(4));	// text outline size
				           		mPaintSizesForScale[Integer.parseInt(parser.getAttributeValue(0))-1][4] = Integer.parseInt(parser.getAttributeValue(5),16);	// text outline color
				          	}
		                } 
                      
                      if      (name.equalsIgnoreCase("mesh")){
                              int numAttributes = parser.getAttributeCount();
                              if(numAttributes != 4){
                                      Log.e(TAG,"Bad mesh type while parsing in render schemes: incorrect number of attributes: " + numAttributes);
                                      mErrorSchemesNotLoadedCorrectly = true;
                              }
                              else {
                                      int meshIndex = -1;
                                      String item = parser.getAttributeValue(0);
                                      String vertShaderFileName = parser.getAttributeValue(1), 
                                                      fragShaderFileName = parser.getAttributeValue(2),
                                                      meshFileName = parser.getAttributeValue(3); 
                                      
                                      if(item.equalsIgnoreCase("map"))                { meshIndex = MeshType.MAP.ordinal(); };
                                      if(item.equalsIgnoreCase("user_icon"))  		  { meshIndex = MeshType.USER_ICON.ordinal(); };
                                      if(item.equalsIgnoreCase("reticle"))            { meshIndex = MeshType.RETICLE.ordinal(); };
                                      if(item.equalsIgnoreCase("reticle_item"))	  	  { meshIndex = MeshType.RETICLE_ITEM.ordinal();};
                                      if(item.equalsIgnoreCase("poi"))                { meshIndex = MeshType.POI.ordinal(); };
                                      if(meshIndex == -1) {
                                              Log.e(TAG, "Bad mesh while parsing in render schemes: invalid item '" + item + "'");
                                              mErrorSchemesNotLoadedCorrectly = true;
                                      }
                                      else {
                                              mVertShaderFiles[meshIndex] = vertShaderFileName;
                                              mFragShaderFiles[meshIndex] = fragShaderFileName;
                                              mMeshFiles[meshIndex] = Environment.getExternalStorageDirectory().getPath() + "/" + PROFILE_FOLDER + "/" + meshFileName;
                                      }
                              }
                      }
                      

		                if (name.equalsIgnoreCase("type")){
		                	int numAttributes = parser.getAttributeCount();
		                	// depends on mCurrentlyParsing 
		                	if (mCurrentlyParsing.equalsIgnoreCase("POIs")){
		                		mParseCurTypeID = Integer.parseInt(parser.getAttributeValue(0));
		                		if(numAttributes != 3){
		                			Log.e(TAG, "Bad mesh type while parsing in render schemes: incorrect number of attributes");
	               					mErrorSchemesNotLoadedCorrectly = true;
		                		}
		                		else {
			                		//map POI id to the mesh it is using
			                		MeshType meshType = null;
			                		String meshTypeString = parser.getAttributeValue(2);
			                		if(meshTypeString.equalsIgnoreCase("map")) 			{ meshType = MeshType.MAP; };
			                		if(meshTypeString.equalsIgnoreCase("user_icon")) 	{ meshType = MeshType.USER_ICON; };
			                		if(meshTypeString.equalsIgnoreCase("reticle")) 		{ meshType = MeshType.RETICLE; };
			                		if(meshTypeString.equalsIgnoreCase("reticle_item"))	{ meshType = MeshType.RETICLE_ITEM;};
			                		if(meshTypeString.equalsIgnoreCase("poi")) 			{ meshType = MeshType.POI; };
			                		if(meshType == null) {
				                		Log.e(TAG, "Bad meshtype while parsing in render schemes: invalid item '" + meshTypeString + "'");
			               				mErrorSchemesNotLoadedCorrectly = true;
			                		}
			                		else {
			                			mPOIMeshes[mParseCurTypeID] = meshType;
			                		}
		                		}

			                }
		                	if (mCurrentlyParsing.equalsIgnoreCase("Areas")){
			                	if(numAttributes != 4) {
			                		Log.e(TAG, "Bad area type while parsing in render schemes: incorrect number of attributes");
	               					mErrorSchemesNotLoadedCorrectly = true;
			                	}
			                	else {
		                		// create paint for area
			                		Paint newPaint = new Paint( );
			                		newPaint.setStyle(Paint.Style.FILL);
			                		newPaint.setColor(Integer.parseInt(parser.getAttributeValue(2),16));
			                		newPaint.setAlpha(Integer.parseInt(parser.getAttributeValue(3)));
			                		mAreaPaints[Integer.parseInt(parser.getAttributeValue(0))] = newPaint;
			                	}
			                }
		                	
		                } 
		                if (name.equalsIgnoreCase("image4state")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 2) {
		                		Log.e(TAG, "Bad image4state while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		WorldObjectDrawing.WorldObjectDrawingStates iState = null;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("normal")) iState = WorldObjectDrawing.WorldObjectDrawingStates.NORMAL;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("hasFocus")) iState = WorldObjectDrawing.WorldObjectDrawingStates.HAS_FOCUS;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("disabled")) iState = WorldObjectDrawing.WorldObjectDrawingStates.DISABLED;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("disabledFocus")) iState = WorldObjectDrawing.WorldObjectDrawingStates.DISABLED_FOCUS;
		                		if(iState != null) {
		                			Bitmap mIcon = BitmapFactory.decodeResource(res, res.getIdentifier(parser.getAttributeValue(0), "drawable", packageName)); 
			                		for(int i=0;i<mNumQScales;i++){
			                			mPOIBitmaps[mParseCurTypeID][iState.ordinal()][i] = Bitmap.createScaledBitmap(mIcon, (int)(mIcon.getWidth() * mBitmapScale[i]), (int)(mIcon.getHeight() * mBitmapScale[i]), false);
			                		}
		                		}
		                	}
		                } 
		                
		                if (name.equalsIgnoreCase("image_mod")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 3 && numAttributes != 4) {
		                		Log.e(TAG, "Bad image_mod while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		WorldObjectDrawing.WorldObjectDrawingStates iState = null;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("normal")) iState = WorldObjectDrawing.WorldObjectDrawingStates.NORMAL;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("hasFocus")) iState = WorldObjectDrawing.WorldObjectDrawingStates.HAS_FOCUS;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("disabled")) iState = WorldObjectDrawing.WorldObjectDrawingStates.DISABLED;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("disabledFocus")) iState = WorldObjectDrawing.WorldObjectDrawingStates.DISABLED_FOCUS;
		                		if(iState != null) {
			                		Bitmap mIcon = BitmapFactory.decodeResource(res, res.getIdentifier(parser.getAttributeValue(0), "drawable", packageName)); 
			                		int i=Integer.parseInt(parser.getAttributeValue(2))-1;
			                		mPOIBitmaps[mParseCurTypeID][iState.ordinal()][i] = Bitmap.createScaledBitmap(mIcon, (int)(mIcon.getWidth()), (int)(mIcon.getHeight()), false);
			                		if(numAttributes == 4) {
				                		mPOIBitmapOffsetPercents[mParseCurTypeID][iState.ordinal()][i] = Integer.parseInt(parser.getAttributeValue(3));
			                		}
		                		}
		                	}
		                } 
		                
		                if (name.equalsIgnoreCase("map_bg_color")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 2) {
		                		Log.e(TAG, "Bad map_bg_color while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		mMapBackgroudColor = Integer.parseInt(parser.getAttributeValue(0),16);
		                		mMapBackgroudAlpha = Integer.parseInt(parser.getAttributeValue(1));
		                	}
		                } 
		                
		                if (name.equalsIgnoreCase("pan_over_text")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 4) {
		                		Log.e(TAG, "Bad pan_over_text while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		mPanRolloverTextSize = Integer.parseInt(parser.getAttributeValue(0));
		                		mPanRolloverTextColor = Integer.parseInt(parser.getAttributeValue(1),16);
		                		mPanRolloverTextOulineWidth = Integer.parseInt(parser.getAttributeValue(2));
		                		mPanRolloverTextOulineColor = Integer.parseInt(parser.getAttributeValue(3),16);
		                	}
		                } 
		                if (name.equalsIgnoreCase("resort_labels")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 2) {
		                		Log.e(TAG, "Bad resort_labels while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		mResortNameTextSize = Integer.parseInt(parser.getAttributeValue(0));
		                		mResortNameTextColor = Integer.parseInt(parser.getAttributeValue(1),16);
 		                	}
		                }
		                
		                if (name.equalsIgnoreCase("pan_over_box")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 5) {
		                		Log.e(TAG, "Bad pan_over_box while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		mPanRolloverBoxBGColor = Integer.parseInt(parser.getAttributeValue(0),16);
		                		mPanRolloverBoxBGAlpha = Integer.parseInt(parser.getAttributeValue(1));
		                		mPanRolloverBoxOutlineWidth = Integer.parseInt(parser.getAttributeValue(2));
		                		mPanRolloverBoxOutlineColor = Integer.parseInt(parser.getAttributeValue(3),16);
		                		mPanRolloverBoxOutlineAlpha = Integer.parseInt(parser.getAttributeValue(4));
		                	}
		                } 
		                
		                if (name.equalsIgnoreCase("grid")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 4) {
		                		Log.e(TAG, "Bad grid definition while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		mGridBkgdColor = Integer.parseInt(parser.getAttributeValue(0),16);
		                		mGridLineColor = Integer.parseInt(parser.getAttributeValue(1),16);
		                		mGridAlpha = Integer.parseInt(parser.getAttributeValue(2));
		                		mGridLineWidth = Integer.parseInt(parser.getAttributeValue(3));
		                	}
		                } 

		                if (name.equalsIgnoreCase("trail_outlines")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 1) {
		                		Log.e(TAG, "Bad trail_outlines while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		int val = Integer.parseInt(parser.getAttributeValue(0));
		                		mMapTrailOutlineOn = val == 1 ? true : false;
		                	}
		                } 
		                
		                if (name.equalsIgnoreCase("area_outlines")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 1) {
		                		Log.e(TAG, "Bad area_outlines while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		int val = Integer.parseInt(parser.getAttributeValue(0));
		                		mMapAreaOutlineOn = val == 1 ? true : false;
		                	}
		                } 
		                
		                if (name.equalsIgnoreCase("subtype")){
	                		mParseCurTypeID = Integer.parseInt(parser.getAttributeValue(0));
		                } 
		                
		                if (name.equalsIgnoreCase("params4state")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 3) {
		                		Log.e(TAG, "Bad params4state while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		int iState;
		                		if(parser.getAttributeValue(2).equalsIgnoreCase("normal")) iState = 0;
		                		else iState = 1;

		                		int color = Integer.parseInt(parser.getAttributeValue(0),16);
		                		for(int i=0;i<mNumQScales;i++){
	                				Paint newPaint = new Paint( );
		                			if(mPaintSizesForScale[i][0] > 0) {		// trail width at scale. -if = 0, don't define paint, ie, don't render trail

		                				// Trails
		                				newPaint.setStyle(Paint.Style.STROKE);
		                				newPaint.setStrokeCap(Paint.Cap.ROUND);
		                				newPaint.setStrokeJoin(Paint.Join.ROUND);
		                				newPaint.setAntiAlias(true);
		                				newPaint.setColor(color);
		                				newPaint.setAlpha(Integer.parseInt(parser.getAttributeValue(1)));
		                				newPaint.setStrokeWidth(mPaintSizesForScale[i][0]/World2DrawingTransformer.DISTANCE_PER_PIXEL);

		                				mTrailPaints[mParseCurTypeID][iState][i] = newPaint;
		                			}
		                			
			                		if(mPaintSizesForScale[i][1] > 0) {		// text size at scale  -if = 0, don't define paint, ie, don't render trail names

			                			// Trail names
			                			newPaint = new Paint( );
			                			newPaint.setTextSize(mPaintSizesForScale[i][1]);
			                			newPaint.setTextAlign(Align.CENTER);
			                			newPaint.setAntiAlias(true);
			                			newPaint.setTypeface(streetNameFont);
			                			newPaint.setColor(mPaintSizesForScale[i][2]);		
			                			newPaint.setAlpha(255);

			                			mTrailTextPaints[mParseCurTypeID][iState][i] = newPaint;


			                			// Trail outlines
//			                			newPaint = new Paint( );
//			                			newPaint.setTextSize(mPaintSizesForScale[i][1]);
//			                			newPaint.setTextAlign(Align.CENTER);
//			                			newPaint.setAntiAlias(true);
//			                			newPaint.setTypeface(regularFont);
//			                			newPaint.setColor(mPaintSizesForScale[i][4]);		
//			                			newPaint.setStyle(Paint.Style.STROKE);
//			                			newPaint.setStrokeWidth(mPaintSizesForScale[i][3]);
//			                			newPaint.setAlpha(255);
//
//			                			mTrailOutlinePaints[mParseCurTypeID][iState][i] = newPaint;
			                		}
		                		}
		                	}
		                } 
		                
		                           
		                
		                break;
		            case XmlPullParser.END_TAG:
		                name = parser.getName();
		                break;
		            case XmlPullParser.TEXT:
		                break;
		            }
		        eventType = parser.next();
		        }
		} 
		catch (FileNotFoundException e) {
		    // TODO
			mErrorSchemesNotLoadedCorrectly = true;
			e.printStackTrace();
		} 
		catch (IOException e) {
		    // TODO
			mErrorSchemesNotLoadedCorrectly = true;
			e.printStackTrace();
		} 
		catch (Exception e){
		    // TODO
			mErrorSchemesNotLoadedCorrectly = true;
			e.printStackTrace();

		}

	}
	
	private MeshGL loadMeshWithCachedTexture(MeshGL[] mesh, Bitmap bmp, int textureUnit){
		int indexToCache = -1;
		int size = mesh.length;
		if(bmp == null){
			Log.e(TAG,"POI Bitmap is null!");
		}
		//check cache if the mesh we want is already created
		for(int t = 0; t < size; t++){
			if(mesh[t].getTextureBitmap() == null){
				Log.e(TAG, "POI texture not loaded yet!");
				indexToCache = t;
				break;
			}
			if(bmp.sameAs(mesh[t].getTextureBitmap())){
				return mesh[t];
			}
			else {
				indexToCache = t;
			}
		}
		
		//couldn't find mesh in cache, so we load new texture.
		//This effectively adds it to cache, since it will be
		//found the next time this bitmap is requested
		mesh[indexToCache].loadMeshTexture(bmp, true, textureUnit);
		return mesh[indexToCache];
	}

    /**
     * Parses and initializes meshes defined in <code>rendering_schemes.xml</code>
     * @param meshTypeIndex one of <code>MeshType</code>:  MAP, USER_ICON, RETICLE, RETICLE_ITEM, POI
     * @param mesh Non-null <code>MeshGL</code> object
     */
    public void setMeshes(int meshTypeIndex, MeshGL mesh){
            //checking that we aren't parsing the same mesh file again
            //currently only compares the current mesh file with the
            // last parsed mesh
            if(!mLastParsedMeshFile.equals(mMeshFiles[meshTypeIndex])){
                mParsedMesh = new ParsedObj(mMeshFiles[meshTypeIndex], mesh);
                mParsedMesh.parse();
                mLastParsedMesh = mesh;
                if(meshTypeIndex == MeshGL.MeshType.POI.ordinal()){
	                for(int i = 0; i < mMeshCacheSize; i ++){
	                	mMeshes[meshTypeIndex][i] = new MeshGL(mesh);
	                	Log.d(TAG,"Copying " + MeshGL.MeshType.values()[meshTypeIndex].toString() + " mesh " + i);
	                }
                }
                else {
                	mMeshes[meshTypeIndex][0] = mesh;
                }
            }
            else {
                mesh.init(mLastParsedMesh.getVertices(), mLastParsedMesh.getTextures(), null, mLastParsedMesh.getMeshBoundingRadius());
                if(meshTypeIndex == MeshGL.MeshType.POI.ordinal()){
		            for(int i = 0; i < mMeshCacheSize; i ++){
		            	mMeshes[meshTypeIndex][i] = new MeshGL(mesh);
		            }
                }
                else {
                	mMeshes[meshTypeIndex][0] = mesh;
                }
            }
            mLastParsedMeshFile = mMeshFiles[meshTypeIndex];
    }

	
	//==================================================================
	// public API to get paints and bitmaps...
	public Bitmap GetUserIconBitmap() {
		return mUserIcon;
	}
	/**
	 * Gets POI bitmap for a given POI ID, state, and camera altitude value.
	 * @param typeIndex - POI id number
	 * @param poiState - POI state: focused or not focused
	 * @param viewScale - camera altitude scale
	 * @return Bitmap to use (as texture)
	 */
	public Bitmap GetPOIBitmap(int typeIndex, int poiState, double viewScale) {
		return mPOIBitmaps[typeIndex][poiState][getScaleRange(viewScale)-1];
	}
	
	/**
	 * Gets a {@code MeshGL} object with a texture for the corresponding 
	 * Point Of Interest. Checks if there already is a mesh with the correct
	 * texture before returning.  
	 * @param typeIndex - POI id number
	 * @param poiState - POI state: NORMAL,	HAS_FOCUS, DISABLED, DISABLED_FOCUS
	 * @param viewScale - camera altitude scale
	 * @return MeshGL object with the correct texture
	 */
	public MeshGL getPOIMesh(int typeIndex, int poiState, double viewScale){
		MeshGL[] temp = mMeshes[mPOIMeshes[typeIndex].ordinal()];
		Bitmap bmp = GetPOIBitmap(typeIndex, poiState, viewScale);
		
        return loadMeshWithCachedTexture(temp, bmp, (typeIndex == BitmapTypes.WASHROOM.ordinal()) ? 2 : 3);
	}
	
   public MeshGL getMeshOfType(int meshTypeIndex){
       return mMeshes[meshTypeIndex][0];
   }
   
   public MeshGL getMeshOfTypeWithTexture(int meshTypeIndex, Bitmap bitmap){
	   MeshGL[] meshCache = mMeshes[meshTypeIndex];
	   return loadMeshWithCachedTexture(meshCache, bitmap, 4);
   }
   
   /**
    * 
    * @param meshTypeIndex - one of {@link MeshType} like MeshType.MAP, MeshType.BUDDY
    * @return String filename of the vertex shader to use
    */
   public String getVertShaderFile(int meshTypeIndex){
           return mVertShaderFiles[meshTypeIndex];
   }
   
   public String[] getVertShaderFiles(){
           return mVertShaderFiles;
   }
   
   /**
    * 
    * @param meshTypeIndex - one of {@link MeshType} like MeshType.MAP, MeshType.BUDDY
    * @return String filename of the fragment shader to use
    */
   public String getFragShaderFile(int meshTypeIndex){
           return mFragShaderFiles[meshTypeIndex];
   }
   
   public String[] getFragShaderFiles(){
           return mFragShaderFiles;
   }
   
   /**
    * 
    * @param meshTypeIndex - one of {@link MeshType} like MeshType.MAP, MeshType.BUDDY
    * @return String filename of the mesh file to use (OBJ format)
    */
   public String getMeshFile(int meshTypeIndex){
           return mMeshFiles[meshTypeIndex];
   }

	
	public int GetPOIBitmapOffsetPercent(int typeIndex, int poiState, double viewScale) {
		return mPOIBitmapOffsetPercents[typeIndex][poiState][getScaleRange(viewScale)-1];
	}
	
	public Paint[] GetTrailPaint(int typeIndex, int trailState, double viewScale) {
		Paint[] paintArray = new Paint[3];
		paintArray[0] = mTrailPaints[typeIndex][trailState][getScaleRange(viewScale)-1];
		paintArray[1] = mTrailTextPaints[typeIndex][trailState][getScaleRange(viewScale)-1];
		paintArray[2] = mTrailOutlinePaints[typeIndex][trailState][getScaleRange(viewScale)-1];
		
		return paintArray;
	}

	public Paint GetAreaPaint(int typeIndex) {
		return mAreaPaints[typeIndex];
	}
	public double[] GetZoomLevels() {
		return mZoomLevels;
	}
	public double GetMaxZoomLevel() {
		return mMaxZoomScale;
	}
	public int GetMapBGColor() {
	    return mMapBackgroudColor;
	}
	public int GetMapAlpha() {
	    return mMapBackgroudAlpha;
	}
	public boolean IsTrailOutlineOn() {
	    return mMapTrailOutlineOn;
	}
	public boolean IsAreaOutlineOn() {
	    return mMapAreaOutlineOn;	
	}

	public int GetResortNameTextColor() {
	    return mResortNameTextColor;
	}
	public int GetResortNameTextSize() {
	    return mResortNameTextSize;
	}
	public int GetPanRolloverTextColor() {
	    return mPanRolloverTextColor;
	}
	public int GetPanRolloverTextSize() {
	    return mPanRolloverTextSize;
	}
	public int GetPanRolloverTextOutlineColor() {
	    return mPanRolloverTextOulineColor;
	}
	public int GetPanRolloverTextOutlineWidth() {
	    return mPanRolloverTextOulineWidth;
	}

    public int GetGridBkgdColor() {
	    return mGridBkgdColor;
	}
    public int GetGridLineColor() {
	    return mGridLineColor;
	}
	public int GetGridAlpha() {
	    return mGridAlpha;
	}
	public int GetGridLineWidth() {
	    return mGridLineWidth;
	}
	public float GetIdleVelocityThreshold() {
	    return mIdleVelocityThrshold;
	}
	public boolean GetTrailLabelCapitalization() {
	    return mTrailLabelCapitalization;
	}
	public float GetTrailLabelOffsetFactor() {
	    return mTrailLabelOffsetFactor;
	}
	public int GetPanRolloverBoxBGColor() {
	    return mPanRolloverBoxBGColor;
	}
	public int GetPanRolloverBoxBGAlpha() {
	    return mPanRolloverBoxBGAlpha;
	}
	public int GetPanRolloverBoxOutlineColor() {
	    return mPanRolloverBoxOutlineColor;
	}
	public int GetPanRolloverBoxOutlineAlpha() {
	    return mPanRolloverBoxOutlineAlpha;
	}
	public int GetPanRolloverBoxOutlineWidth() {
	    return mPanRolloverBoxOutlineWidth;
	}
	public String GetMessage(Messages msg) {
		return mMessages[msg.ordinal()];
	}
	public double GetRate(Rates rate) {
		return mRates[rate.ordinal()];
	}

	public ShaderProgram[] getShaderPrograms() {
		return mShaderPrograms;
	}
	public int getNumMeshTypes() {
		return mNumMeshTypes;
	}
	public int getNumPOITypes(){
		return mNumPOITypes;
	}
}
