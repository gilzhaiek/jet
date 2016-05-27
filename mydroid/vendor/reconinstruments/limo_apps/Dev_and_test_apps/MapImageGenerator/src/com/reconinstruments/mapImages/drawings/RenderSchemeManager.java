package com.reconinstruments.mapImages.drawings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xmlpull.v1.XmlPullParser;

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

import com.reconinstruments.mapImages.drawings.MapDrawings;
import com.reconinstruments.mapImages.helpers.LocationTransformer;
import com.reconinstruments.mapImages.R;


public class RenderSchemeManager {
	private final static String TAG = "RenderSchemeManager";
	private final static boolean USE_EXTERNAL_RENDERING_SCHEME_FILES = true;
	private final static String PROFILE_FOLDER = "ReconApps/MapData";
	
	public enum Rates {
		FRAME_RATE,
		PAN_RATE,
		ROTATION_RATE,
		SCALE_RATE
	}
	
	public enum Messages {
		STARTUP,
		OUT_OF_MEMORY,
		SCHEME_ERROR,
		WAITING_FOR_LOCATION,
		LOADING_DATA,
		CACHE_DATA_LOADED,
		NO_DATA,
		CLOSEST_RESORT,
		ERROR_LOADING_DATA
	}
	
	public enum TrailPaint {
		LINE,
		TEXTSIZE,
		TEXTOUTLINE
	}
	public boolean mErrorSchemesNotLoadedCorrectly = false;
	public boolean mLoadingSettingsFile = false;
	
	String mCurrentlyParsing = "";
	int mParseCurPOIType 	= 0;
	int mParseCurTrailType 	= 0;
	int mParseCurTrailSubType = 0;
	int mParseCurAreaType 	= 0;
	int	mParseQScaleCnt 	= 0;
	int mParseCurTypeID 	= 0;
	
	int	mNumQScales 	=  4;				// TODO init these with data from file (2 pass)
	int mNumPOITypes 	= 13;
	int mNumPOIStates 	=  4;
	int mNumTrailTypes 	= 13;
	int mNumTrailStates =  2;
	int mNumAreaTypes 	=  4;
	int mNumZoomLevels	=  4;
	
	String[]		mMessages = null;
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
	double 			mMinZoomLevel = 0.25;
    int 			mMapBackgroudColor = Color.WHITE;
    int 			mMapBackgroudAlpha = 255;
    boolean 		mMapAreaOutlineOn = false;	
    boolean 		mMapTrailOutlineOn = false;
    int 			mGridBkgdColor = Color.BLACK;
    int 			mGridLineColor = Color.GRAY;
    int 			mGridAlpha = 128;
    int 			mGridLineWidth = 1;
    int 			mPanRolloverTextSize = 20;
    int 			mPanRolloverTextColor = Color.WHITE;
    int 			mPanRolloverTextOulineWidth = 2;
    int 			mPanRolloverTextOulineColor = Color.BLACK;
    int 			mPanRolloverBoxBGColor = Color.WHITE;
    int 			mPanRolloverBoxBGAlpha = 255;
    int 			mPanRolloverBoxOutlineColor = Color.BLACK;
    int 			mPanRolloverBoxOutlineAlpha = 255;
    int 			mPanRolloverBoxOutlineWidth = 2;
    

	// this.getPackageName() to get packageName
	
	public RenderSchemeManager(String hardwareID, Resources res, String packageName) {

		
		LoadSettingsFile(hardwareID, res, packageName);

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
//			Log.i(TAG,"QuantScale contains: "+mLower+ "-"+viewScale+"-"+mUpper);
			return (mLower < viewScale && viewScale <= mUpper);
		}
	}
	
	private int getScaleRange(double viewScale) {
		for(ScaleRange qscale : mScaleRanges) {
			if(qscale.contains(viewScale)) {
				return qscale.mQScale;
			}
		}
		return 1;
	}
	
	public void LoadSettingsFile(String hardwareID, Resources res, String packageName) {
		mErrorSchemesNotLoadedCorrectly = false;

		// TODO get array dimensions from data file...  for now dimensions are hard coded above
		
		
		mLoadingSettingsFile = true;		// semaphore to block activity while class data is unstable
		
		mMessages = new String[Messages.values().length]; 
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
		
		ReadSettingsXMLFile(hardwareID, res, packageName);
		
		mLoadingSettingsFile = false;

	}
	
	public void ReadSettingsXMLFile (String hardwareID, Resources res, String packageName) {
		
		XmlPullParser parser = Xml.newPullParser();
		try {
			InputStream is;
			BufferedReader br;
			if(USE_EXTERNAL_RENDERING_SCHEME_FILES) {	// use files stored on devices external storage (/mnt/storage/)
				File path = Environment.getExternalStorageDirectory();
//				Log.e(TAG, "file path: "+path);
				File file;
				if(hardwareID.equalsIgnoreCase("limo")) {
					file = new File(path, PROFILE_FOLDER + "/" + "rendering_schemes_limo.xml"); 
				}
				else {
					file = new File(path, PROFILE_FOLDER + "/" + "rendering_schemes_jet.xml"); 
				}
				br = new BufferedReader(new FileReader(file));
			}
			else {	// use files that are stored in app resources
				if(hardwareID.equalsIgnoreCase("limo")) {
					is = res.openRawResource(R.raw.rendering_schemes_limo); 
				}
				else {
					is = res.openRawResource(R.raw.rendering_schemes_jet); 
				}
				br = new BufferedReader(new InputStreamReader(is));
			}

		    
		    // auto-detect the encoding from the stream
		    parser.setInput(br);

		    boolean done = false;
		    int eventType = parser.getEventType();   // get and process event
		    
		    while (eventType != XmlPullParser.END_DOCUMENT && !done){
		        String name = null;
                
//		        name = parser.getName();
//                if(name == null) name = "null";
//		        Log.e(TAG, "eventType:"+eventType + "-"+ name);

		        switch (eventType){
		            case XmlPullParser.START_DOCUMENT:
		                name = parser.getName();
		                break;
		            case XmlPullParser.START_TAG:
		                name = parser.getName();
		                if (name.equalsIgnoreCase("file_id")){
		                	Log.i(TAG, "processing scheme file: " +parser.getAttributeValue(0));
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
		                
		                if (name.equalsIgnoreCase("message")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 2) {
		                		Log.e(TAG, "Bad message while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		String item = parser.getAttributeValue(0);
		                		int msgIndex = -1;
		                		if(item.equalsIgnoreCase("startup")) 				{ msgIndex = Messages.STARTUP.ordinal(); };
		                		if(item.equalsIgnoreCase("scheme_error")) 			{ msgIndex = Messages.SCHEME_ERROR.ordinal(); };
		                		if(item.equalsIgnoreCase("out_of_memory")) 			{ msgIndex = Messages.OUT_OF_MEMORY.ordinal(); };
		                		if(item.equalsIgnoreCase("waiting_for_location")) 	{ msgIndex = Messages.WAITING_FOR_LOCATION.ordinal(); };
		                		if(item.equalsIgnoreCase("loading_data")) 			{ msgIndex = Messages.LOADING_DATA.ordinal(); };
		                		if(item.equalsIgnoreCase("cache_data_loaded")) 		{ msgIndex = Messages.CACHE_DATA_LOADED.ordinal(); };
		                		if(item.equalsIgnoreCase("no_data")) 				{ msgIndex = Messages.NO_DATA.ordinal(); };
		                		if(item.equalsIgnoreCase("closest_resort")) 		{ msgIndex = Messages.CLOSEST_RESORT.ordinal(); };
		                		if(item.equalsIgnoreCase("error_loading_data")) 	{ msgIndex = Messages.ERROR_LOADING_DATA.ordinal(); };
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
		                		if(rIndex == -1) {
			                		Log.e(TAG, "Bad rate while parsing in render schemes: invalid item '" + item + "'");
		               				mErrorSchemesNotLoadedCorrectly = true;
		                		}
		                		else {
		                			mRates[rIndex] = Double.parseDouble(parser.getAttributeValue(1));
		                		} 
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
//		                		Log.i(TAG, "Adding scale_range "+ mParseQScaleCnt);
		                	}
		                } 
		                if (name.equalsIgnoreCase("zoom_level")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes != 2 && numAttributes != 3) {
		                		Log.e(TAG, "Bad zoom_level while parsing in render schemes: incorrect number of attributes");
	               				mErrorSchemesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		mZoomLevels[Integer.parseInt(parser.getAttributeValue(0))-1] = Double.parseDouble(parser.getAttributeValue(1));
			                	if(numAttributes == 3) {
			                		mMinZoomLevel = Double.parseDouble(parser.getAttributeValue(2));
			                	}
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
				           		
//				           		Log.e(TAG, "sizes4scale: " + mPaintSizesForScale[Integer.parseInt(parser.getAttributeValue(0))-1][0] + ", " +
//				           									 mPaintSizesForScale[Integer.parseInt(parser.getAttributeValue(0))-1][1] + ", " +
//				           									 mPaintSizesForScale[Integer.parseInt(parser.getAttributeValue(0))-1][2] + ", " +
//				           									 mPaintSizesForScale[Integer.parseInt(parser.getAttributeValue(0))-1][3] + ", " +
//				           									 mPaintSizesForScale[Integer.parseInt(parser.getAttributeValue(0))-1][4]);
				          	} 
		                } 
		                if (name.equalsIgnoreCase("type")){
		                	int numAttributes = parser.getAttributeCount();
		                	// depends on mCurrentlyParsing 
		                	if (mCurrentlyParsing.equalsIgnoreCase("POIs")){
		                		mParseCurTypeID = Integer.parseInt(parser.getAttributeValue(0));
				                
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
//			                		Log.i(TAG, "Adding area paint "+ parser.getAttributeValue(0));
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
		                		MapDrawings.State iState = null;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("normal")) iState = MapDrawings.State.NORMAL;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("hasFocus")) iState = MapDrawings.State.HAS_FOCUS;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("disabled")) iState = MapDrawings.State.DISABLED;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("disabledFocus")) iState = MapDrawings.State.DISABLED_FOCUS;
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
		                		MapDrawings.State iState = null;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("normal")) iState = MapDrawings.State.NORMAL;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("hasFocus")) iState = MapDrawings.State.HAS_FOCUS;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("disabled")) iState = MapDrawings.State.DISABLED;
		                		if(parser.getAttributeValue(1).equalsIgnoreCase("disabledFocus")) iState = MapDrawings.State.DISABLED_FOCUS;
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
//		                		Log.e(TAG,"color:"+color);
		                		for(int i=0;i<mNumQScales;i++){
	                				Paint newPaint = new Paint( );
		                			if(mPaintSizesForScale[i][0] > 0) {		// trail width at scale
		                				newPaint.setStyle(Paint.Style.STROKE);
		                				newPaint.setStrokeCap(Paint.Cap.ROUND);
		                				newPaint.setStrokeJoin(Paint.Join.ROUND);
		                				newPaint.setAntiAlias(true);
		                				newPaint.setColor(color);
		                				newPaint.setAlpha(Integer.parseInt(parser.getAttributeValue(1)));
		                				newPaint.setStrokeWidth(mPaintSizesForScale[i][0]/LocationTransformer.DISTANCE_PER_PIXEL);

		                				mTrailPaints[mParseCurTypeID][iState][i] = newPaint;
		                			}
		                			
			                		if(mPaintSizesForScale[i][1] > 0) {		// text size at scale
			                			newPaint = new Paint( );
			                			newPaint.setTextSize(mPaintSizesForScale[i][1]);
			                			newPaint.setTextAlign(Align.CENTER);
			                			newPaint.setAntiAlias(true);
			                			newPaint.setTypeface(Typeface.SANS_SERIF);
			                			newPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));		
			                			//			                		newPaint.setColor(Color.WHITE);		
			                			newPaint.setColor(mPaintSizesForScale[i][2]);		
			                			newPaint.setAlpha(255);

			                			mTrailTextPaints[mParseCurTypeID][iState][i] = newPaint;


			                			newPaint = new Paint( );
			                			newPaint.setTextSize(mPaintSizesForScale[i][1]);
			                			newPaint.setTextAlign(Align.CENTER);
			                			newPaint.setAntiAlias(true);
			                			newPaint.setTypeface(Typeface.SANS_SERIF);
			                			newPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));		
			                			//			                		newPaint.setColor(Color.BLACK);		
			                			newPaint.setColor(mPaintSizesForScale[i][4]);		
			                			newPaint.setStyle(Paint.Style.STROKE);
			                			newPaint.setStrokeWidth(mPaintSizesForScale[i][3]);
			                			newPaint.setAlpha(255);

			                			mTrailOutlinePaints[mParseCurTypeID][iState][i] = newPaint;
			                		}
			                		
//			                		Log.i(TAG, "Adding trail paints["+ mParseCurTypeID + "][" + iState + "][" + i + "]");
			                		
			                		
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
	
	
	
	//==================================================================
	// public API to get paints and bitmaps...
	public Bitmap GetPOIBitmap(int poiType, int poiState, double viewScale) {
		return mPOIBitmaps[poiType][poiState][getScaleRange(viewScale)-1];
	}
	
	public int GetPOIBitmapOffsetPercent(int poiType, int poiState, double viewScale) {
		return mPOIBitmapOffsetPercents[poiType][poiState][getScaleRange(viewScale)-1];
	}
	
	public Paint[] GetTrailPaint(int trailType, int trailState, double viewScale) {
		Paint[] paintArray = new Paint[3];
		paintArray[0] = mTrailPaints[trailType][trailState][getScaleRange(viewScale)-1];
		paintArray[1] = mTrailTextPaints[trailType][trailState][getScaleRange(viewScale)-1];
		paintArray[2] = mTrailOutlinePaints[trailType][trailState][getScaleRange(viewScale)-1];
		
		return paintArray;
	}
	
	public Paint GetAreaPaint(int areaType) {
		return mAreaPaints[areaType];
	}
	public double[] GetZoomLevels() {
		return mZoomLevels;
	}
	public double GetMinZoomLevel() {
		return mMinZoomLevel;
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
}
