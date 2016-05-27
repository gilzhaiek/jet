package com.reconinstruments.dashlauncher.radar.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;

import android.content.Context;
import android.graphics.PointF;
import android.util.Log;

import com.reconinstruments.dashlauncher.radar.ReconSettingsUtil;
import com.reconinstruments.dashlauncher.radar.Util;
import com.reconinstruments.dashlauncher.radar.maps.objects.POI;
import com.reconinstruments.dashlauncher.radar.prim.Vector3;

public class GLPoi {
	private static final String TAG = "GLPoi";
	
	public static final float MAX_DISPLAY_CHAR_WIDTH	= 2.0f;
	private static final float FRAME_SCALE_RATIO		= 0.35f;
	
	private static boolean mStaticInits = false;
	
	public int 		mType;
	public String 	mName		= " ";
	public String 	mShortName	= " ";
	public Vector3	mPos = null;
	public Vector3	mGLPosition = null;
	public boolean	mIsNormalized = false;
	public float	mDistance = 0.0f;
	
	private static Point3f pTemp	= new Point3f();
	private static Matrix3f mTemp	= new Matrix3f();
	
	private static FloatBuffer mUnfocusedOutlineBuffer;
	private static FloatBuffer mUnfocusedBorderBoxVertexBuffer = null;
	private static float mUnfocusedBorderBoxVertices[] = new float[]{-CommonRender.POI_ASPECT*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT, 0.0f, 
												   	  		+CommonRender.POI_ASPECT*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT, 0.0f, 
												   	  		- CommonRender.POI_ASPECT*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT+2.0f*CommonRender.POI_HEIGHT, 0.0f, 
												   	  		+ CommonRender.POI_ASPECT*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT+2.0f*CommonRender.POI_HEIGHT, 0.0f };
	
	private static FloatBuffer mUnfocusedBorderPointVertexBuffer = null;
	private static float mUnfocusedBorderPointVertices[] = new float[]{ + 0.0f*CommonRender.POI_WIDTH, -1.0f, 0.0f, 
												   	  	-CommonRender.POI_ASPECT*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT, 0.0f, 
												   	  	+CommonRender.POI_ASPECT*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT, 0.0f };
	
	
	// Full Height
	private FloatBuffer mFocusedFHOutlineBuffer;
	private static FloatBuffer mFocusedFHBorderBoxVertexBuffer = null;
	private static float mFocusedFHBorderBoxVertices[] = new float[]{- 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_FOCUSED_TIP_HEIGHT, 0.0f, 
												   	  		+ 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_FOCUSED_TIP_HEIGHT, 0.0f, 
												   	  		- 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_FOCUSED_TIP_HEIGHT+2.0f*CommonRender.POI_HEIGHT, 0.0f, 
												   	  		+ 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_FOCUSED_TIP_HEIGHT+2.0f*CommonRender.POI_HEIGHT, 0.0f };
	
	private static FloatBuffer mFocusedFHBorderPointVertexBuffer = null;
	private static float mFocusedFHBorderPointVertices[] = new float[]{ + 0.0f*CommonRender.POI_WIDTH, -1.0f, 0.0f, 
												   	  	- 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_FOCUSED_TIP_HEIGHT, 0.0f, 
												   	  	+ 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_FOCUSED_TIP_HEIGHT, 0.0f };
	
	// Short Height Full Name
	private FloatBuffer mFocusedSHFNOutlineBuffer;
	private static FloatBuffer mFocusedSHFNBorderBoxVertexBuffer = null;
	private static float mFocusedSHFNBorderBoxVertices[] = new float[]{- 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT, 0.0f, 
												   	  		+ 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT, 0.0f, 
												   	  		- 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT+2.0f*CommonRender.POI_HEIGHT, 0.0f, 
												   	  		+ 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT+2.0f*CommonRender.POI_HEIGHT, 0.0f };
	
	private static FloatBuffer mFocusedSHFNBorderPointVertexBuffer = null;
	private static float mFocusedSHFNBorderPointVertices[] = new float[]{ + 0.0f*CommonRender.POI_WIDTH, -1.0f, 0.0f, 
												   	  	- 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT, 0.0f, 
												   	  	+ 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT, 0.0f };
	
	// Short Height First Name
	private FloatBuffer mFocusedSHSNOutlineBuffer;
	private static FloatBuffer mFocusedSHSNBorderBoxVertexBuffer = null;
	private static float mFocusedSHSNBorderBoxVertices[] = new float[]{- 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT, 0.0f, 
												   	  		+ 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT, 0.0f, 
												   	  		- 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT+2.0f*CommonRender.POI_HEIGHT, 0.0f, 
												   	  		+ 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT+2.0f*CommonRender.POI_HEIGHT, 0.0f };
	
	private static FloatBuffer mFocusedSHSNBorderPointVertexBuffer = null;
	private static float mFocusedSHSNBorderPointVertices[] = new float[]{ + 0.0f*CommonRender.POI_WIDTH, -1.0f, 0.0f, 
												   	  	- 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT, 0.0f, 
												   	  	+ 1.0f*CommonRender.POI_WIDTH, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT, 0.0f };		
	
	private GLGlobalTextRenderer mGLDynamicText = null;
	private GLGlobalIconRenderer mGLDynamicIcon = null;
	
	protected static int mUnits							= ReconSettingsUtil.RECON_UINTS_METRIC;
	
	
	
	private float UNFOCUSED_POI_SCALE_RATIO = 0.075f;
	private float mUnfocusedPOIScale = 1.0f;
	
	private static final float POINT_WIDTH_SCALE    = 0.1f;
	private static final float Y_OFFSET				= 1.35f;
		
	private float			mFNameDrawWidth 		= 0.0f;
	private float			mSNameDrawWidth 		= 0.0f;
	private float			mNameDrawHeight 		= 0.0f;
		
	public static final int	DRAW_FIRST_CHAR_TIME	= 2000;
	public static final int	DRAW_MIDDLE_CHAR_TIME	= 300;
	public static final int	DRAW_END_CHAR_TIME		= 1000;
	
	public ArrayList<String>	mNameSubStrings	 	= null; 
	public boolean 				mNeedsScrolling		= false;
	public long 				mScrollStartTime	= 0;
	public int					mTotalDrawTime		= 0;
	
	private static float mPOIBorderR[] = new float[POI.NUM_POI_TYPE];
	private static float mPOIBorderG[] = new float[POI.NUM_POI_TYPE];
	private static float mPOIBorderB[] = new float[POI.NUM_POI_TYPE];
	
	private float mPOIWidth			= 0; 
	private float mFNameBoxWidth	= 0;
	private float mSNameBoxWidth	= 0;
	private float mNameBoxHeight	= 0;
	private float mDistBoxWidth		= 0;
	private float mTotalBoxFWidth	= 0;
	private float mTotalBoxSWidth	= 0;
	
	public boolean  mIconIsFresh	= true;
		
	public GLPoi(int type, String name, Vector3 pos, Context context)
	{
		//Log.i(TAG, "(type: " + type + ", name: " + name + ", pos: " + pos + ")");
		
		mType   	= type;
		mName		= (name == null) ? " " : name;
		mPos 		= new Vector3(pos);				
		mGLPosition	= new Vector3();
		
		mUnits = ReconSettingsUtil.getUnits(context);
		
		if(!mStaticInits) {
			for(int i = 0; i < POI.NUM_POI_TYPE; i++) {
				int tempColor = CommonRender.POI_TYPE_UNDEFINED_COLOR;
				switch (i) {
				case POI.POI_TYPE_RESTAURANT	: tempColor = CommonRender.POI_TYPE_RESTAURANT_COLOR; break;
				case POI.POI_TYPE_CHAIRLIFTING	: tempColor = CommonRender.POI_TYPE_CHAIRLIFTING_COLOR; break;
				case POI.POI_TYPE_BUDDY			: tempColor = CommonRender.POI_TYPE_BUDDY_COLOR; break;
				}
		
				mPOIBorderR[i] = ((float)((tempColor >> 16) & 0xff))/255.0f;
				mPOIBorderG[i] = ((float)((tempColor >> 8) & 0xff))/255.0f;
				mPOIBorderB[i] = ((float)(tempColor & 0xff))/255.0f;
			}
			
			mUnfocusedBorderBoxVertexBuffer   = getInitializedFloatBuffer(mUnfocusedBorderBoxVertices);
			mUnfocusedBorderPointVertexBuffer = getInitializedFloatBuffer(mUnfocusedBorderPointVertices);
			mUnfocusedOutlineBuffer  = getInitializedFloatBuffer(new float[(5 * 2) * 3]);
			SetUnfocusedOutlineVertexBuffer();
			
			mFocusedFHBorderBoxVertexBuffer   = getInitializedFloatBuffer(mFocusedFHBorderBoxVertices);
			mFocusedFHBorderPointVertexBuffer = getInitializedFloatBuffer(mFocusedFHBorderPointVertices);

			mFocusedSHFNBorderBoxVertexBuffer   = getInitializedFloatBuffer(mFocusedSHFNBorderBoxVertices);
			mFocusedSHFNBorderPointVertexBuffer = getInitializedFloatBuffer(mFocusedSHFNBorderPointVertices);

			mFocusedSHSNBorderBoxVertexBuffer   = getInitializedFloatBuffer(mFocusedSHSNBorderBoxVertices);
			mFocusedSHSNBorderPointVertexBuffer = getInitializedFloatBuffer(mFocusedSHSNBorderPointVertices);

			mStaticInits = true;
		}
		
		mFocusedFHOutlineBuffer    = getInitializedFloatBuffer(new float[(7 * 2) * 3]);
		if(type == POI.POI_TYPE_BUDDY) {
			mFocusedSHFNOutlineBuffer    = getInitializedFloatBuffer(new float[(7 * 2) * 3]);
			mFocusedSHSNOutlineBuffer    = getInitializedFloatBuffer(new float[(7 * 2) * 3]);
			
			String[] fullName = mName.split("\\s+");
			if(fullName.length > 0)
			{
				mShortName = fullName[0];
			} else {
				mShortName = mName; 
			}
		}
	}


	public void SetDrawParams(boolean iconIsFresh) {
		mIconIsFresh = iconIsFresh;
	}

	public FloatBuffer getInitializedFloatBuffer(float[] a)
	{
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(a.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		FloatBuffer b = byteBuffer.asFloatBuffer();
		b.put(a);
		b.position(0);
		return b;
	}
	
	public void setGLDrawObjects(GLGlobalTextRenderer glText, GLGlobalIconRenderer glIcon)
	{
		// Set the references to both draw classes
		mGLDynamicText = glText;
		mGLDynamicIcon = glIcon;
		
		// Set the width of the draw surface for the resort name string
		float fullNameDrawWidth = mGLDynamicText.getStringDimensions(mName).x;
		float shortNameDrawWidth = mGLDynamicText.getStringDimensions(mShortName+"1").x;
		mFNameDrawWidth	= Math.min(fullNameDrawWidth, MAX_DISPLAY_CHAR_WIDTH);
		mSNameDrawWidth = Math.min(shortNameDrawWidth, MAX_DISPLAY_CHAR_WIDTH);
		// Set the height of the draw surface for the resort name string
		mNameDrawHeight	= mGLDynamicText.getStringDimensions(mName).y;
		
		// Set the actual values used to draw by openGL
		mFNameBoxWidth		= mFNameDrawWidth*FRAME_SCALE_RATIO;
		mSNameBoxWidth		= mSNameDrawWidth*FRAME_SCALE_RATIO;
		mNameBoxHeight		= mNameDrawHeight*FRAME_SCALE_RATIO;
		mPOIWidth			= mNameBoxHeight*CommonRender.POI_ASPECT;
		
		if(mUnits == ReconSettingsUtil.RECON_UINTS_METRIC) {
			mDistBoxWidth		= mGLDynamicText.getStringDimensions("188.8km").x*FRAME_SCALE_RATIO;
		} else {
			mDistBoxWidth		= mGLDynamicText.getStringDimensions("18888ft").x*FRAME_SCALE_RATIO;
		}
		
		mTotalBoxFWidth		= mPOIWidth + mFNameBoxWidth + mDistBoxWidth;
		mTotalBoxSWidth		= mPOIWidth + mSNameBoxWidth;
		
		//Log.v(TAG,"poi="+mPOIWidth+" name="+mNameBoxWidth+" dist="+mDistBoxWidth+" box="+mTotalBoxWidth);
		
		// Sets the boolean flag for the text needing to be scrolled
		//if(mGLDynamicText.getCharsWithinWidth(mName, mStringDrawWidth, 0) < mName.length())
		if(mFNameDrawWidth	== MAX_DISPLAY_CHAR_WIDTH && fullNameDrawWidth > MAX_DISPLAY_CHAR_WIDTH)
		{
			mNeedsScrolling = true;
			
			mNameSubStrings = new ArrayList<String>();
			
			int currentCharIndex	= 0;
			int subStringWidth		= mGLDynamicText.getCharsWithinWidth(mName, mFNameDrawWidth, 0);
			
			do {
				mNameSubStrings.add(mName.substring(currentCharIndex, currentCharIndex + subStringWidth));
				currentCharIndex++;
			}
			while(currentCharIndex + subStringWidth <=  mName.length());
			
			if(mNameSubStrings.size() > 2) {
				mTotalDrawTime = DRAW_FIRST_CHAR_TIME + DRAW_MIDDLE_CHAR_TIME*(mNameSubStrings.size()-2) + DRAW_END_CHAR_TIME;
			} else {
				mTotalDrawTime = DRAW_FIRST_CHAR_TIME + DRAW_END_CHAR_TIME;
			}
		} else {
			mTotalDrawTime = DRAW_FIRST_CHAR_TIME;
		}
		
		if(mSNameDrawWidth	== MAX_DISPLAY_CHAR_WIDTH && shortNameDrawWidth > MAX_DISPLAY_CHAR_WIDTH)
		{
			mShortName = mShortName.substring(0, mGLDynamicText.getCharsWithinWidth(mName, mSNameDrawWidth, 0));
		}
		
		SetFocusedOutlineVertexBuffer();
	}
	
	public void SetUnfocusedOutlineVertexBuffer()
	{
		// Set the points for the unfocused outline buffer
		
		mUnfocusedOutlineBuffer.put(mUnfocusedBorderBoxVertices, 6, 6); // add top line
		mUnfocusedOutlineBuffer.put(mUnfocusedBorderBoxVertices, 0, 3); // left
		mUnfocusedOutlineBuffer.put(mUnfocusedBorderBoxVertices, 6, 3);
		mUnfocusedOutlineBuffer.put(mUnfocusedBorderBoxVertices, 3, 3); // right
		mUnfocusedOutlineBuffer.put(mUnfocusedBorderBoxVertices, 9, 3);
		
		mUnfocusedOutlineBuffer.put(mUnfocusedBorderPointVertices, 0, 6); 
		mUnfocusedOutlineBuffer.put(mUnfocusedBorderPointVertices, 0, 3);
		mUnfocusedOutlineBuffer.put(mUnfocusedBorderPointVertices, 6, 3);
		mUnfocusedOutlineBuffer.position(0);		
	}
	
	public void SetFocusedOutlineVertexBuffer()
	{		
		float[] nullTrans = {0, 0, 0};
		float[] preTrans  = {0, Y_OFFSET, 0};
		float[] postTrans = {0, 0, 0};
		
		float[] boxFScale   = {(mTotalBoxFWidth)/2.0f, CommonRender.POI_FOCUSED_BOX_HEIGHT*(mNameBoxHeight/2), 1.0f};
		float[] boxSScale   = {(mTotalBoxSWidth)/2.0f, CommonRender.POI_FOCUSED_BOX_HEIGHT*(mNameBoxHeight/2), 1.0f};
		float[] pntScale = {POINT_WIDTH_SCALE*CommonRender.POI_FOCUSED_TIP_WIDTH, CommonRender.POI_FOCUSED_BOX_HEIGHT*(mNameBoxHeight/2), 1.0f};
		
		mFocusedFHOutlineBuffer.position(0);
		mFocusedFHOutlineBuffer.put(mFocusedFHBorderBoxVertices, 0, 3);
		mFocusedFHOutlineBuffer.put(mFocusedFHBorderBoxVertices, 6, 3);
		mFocusedFHOutlineBuffer.put(mFocusedFHBorderBoxVertices, 3, 3);
		mFocusedFHOutlineBuffer.put(mFocusedFHBorderBoxVertices, 9, 3);
		mFocusedFHOutlineBuffer.put(mFocusedFHBorderBoxVertices, 6, 6);
		
		// Top Box
		for(int p = 0; p < 6; p++)
		{
			transformPoint(mFocusedFHOutlineBuffer, p*3, preTrans, boxFScale, postTrans);
		}
		
		// Point
		mFocusedFHOutlineBuffer.put(mFocusedFHBorderBoxVertices, 0, 3);
		transformPoint(mFocusedFHOutlineBuffer, 18, preTrans, boxFScale, postTrans);
		mFocusedFHOutlineBuffer.put(mFocusedFHBorderPointVertices, 3, 3);
		transformPoint(mFocusedFHOutlineBuffer, 21, preTrans, pntScale, nullTrans);
		
		mFocusedFHOutlineBuffer.put(mFocusedFHBorderPointVertices, 0, 6);
		transformPoint(mFocusedFHOutlineBuffer, 24, preTrans, pntScale, nullTrans);
		transformPoint(mFocusedFHOutlineBuffer, 27, preTrans, pntScale, nullTrans);
		
		mFocusedFHOutlineBuffer.put(mFocusedFHBorderPointVertices, 0, 3);
		mFocusedFHOutlineBuffer.put(mFocusedFHBorderPointVertices, 6, 3);
		transformPoint(mFocusedFHOutlineBuffer, 30, preTrans, pntScale, nullTrans);
		transformPoint(mFocusedFHOutlineBuffer, 33, preTrans, pntScale, nullTrans);
		
		mFocusedFHOutlineBuffer.put(mFocusedFHBorderBoxVertices, 3, 3);
		transformPoint(mFocusedFHOutlineBuffer, 36, preTrans, boxFScale, postTrans);
		mFocusedFHOutlineBuffer.put(mFocusedFHBorderPointVertices, 6, 3);
		transformPoint(mFocusedFHOutlineBuffer, 39, preTrans, pntScale, nullTrans);
		
		mFocusedFHOutlineBuffer.position(0);
		
		if(mType == POI.POI_TYPE_BUDDY) {
			///// Full Name  /////
			//////////////////////
			mFocusedSHFNOutlineBuffer.position(0);
			mFocusedSHFNOutlineBuffer.put(mFocusedSHFNBorderBoxVertices, 0, 3);
			mFocusedSHFNOutlineBuffer.put(mFocusedSHFNBorderBoxVertices, 6, 3);
			mFocusedSHFNOutlineBuffer.put(mFocusedSHFNBorderBoxVertices, 3, 3);
			mFocusedSHFNOutlineBuffer.put(mFocusedSHFNBorderBoxVertices, 9, 3);
			mFocusedSHFNOutlineBuffer.put(mFocusedSHFNBorderBoxVertices, 6, 6);
			
			// Top Box
			for(int p = 0; p < 6; p++)
			{
				transformPoint(mFocusedSHFNOutlineBuffer, p*3, preTrans, boxFScale, postTrans);
			}
			
			// Point
			mFocusedSHFNOutlineBuffer.put(mFocusedSHFNBorderBoxVertices, 0, 3);
			transformPoint(mFocusedSHFNOutlineBuffer, 18, preTrans, boxFScale, postTrans);
			mFocusedSHFNOutlineBuffer.put(mFocusedSHFNBorderPointVertices, 3, 3);
			transformPoint(mFocusedSHFNOutlineBuffer, 21, preTrans, pntScale, nullTrans);
			
			mFocusedSHFNOutlineBuffer.put(mFocusedSHFNBorderPointVertices, 0, 6);
			transformPoint(mFocusedSHFNOutlineBuffer, 24, preTrans, pntScale, nullTrans);
			transformPoint(mFocusedSHFNOutlineBuffer, 27, preTrans, pntScale, nullTrans);
			
			mFocusedSHFNOutlineBuffer.put(mFocusedSHFNBorderPointVertices, 0, 3);
			mFocusedSHFNOutlineBuffer.put(mFocusedSHFNBorderPointVertices, 6, 3);
			transformPoint(mFocusedSHFNOutlineBuffer, 30, preTrans, pntScale, nullTrans);
			transformPoint(mFocusedSHFNOutlineBuffer, 33, preTrans, pntScale, nullTrans);
			
			mFocusedSHFNOutlineBuffer.put(mFocusedSHFNBorderBoxVertices, 3, 3);
			transformPoint(mFocusedSHFNOutlineBuffer, 36, preTrans, boxFScale, postTrans);
			mFocusedSHFNOutlineBuffer.put(mFocusedSHFNBorderPointVertices, 6, 3);
			transformPoint(mFocusedSHFNOutlineBuffer, 39, preTrans, pntScale, nullTrans);
			
			mFocusedSHFNOutlineBuffer.position(0);		
			
			///// First Name  /////
			///////////////////////
			

			mFocusedSHSNOutlineBuffer.position(0);
			mFocusedSHSNOutlineBuffer.put(mFocusedSHSNBorderBoxVertices, 0, 3);
			mFocusedSHSNOutlineBuffer.put(mFocusedSHSNBorderBoxVertices, 6, 3);
			mFocusedSHSNOutlineBuffer.put(mFocusedSHSNBorderBoxVertices, 3, 3);
			mFocusedSHSNOutlineBuffer.put(mFocusedSHSNBorderBoxVertices, 9, 3);
			mFocusedSHSNOutlineBuffer.put(mFocusedSHSNBorderBoxVertices, 6, 6);
			
			// Top Box
			for(int p = 0; p < 6; p++)
			{
				transformPoint(mFocusedSHSNOutlineBuffer, p*3, preTrans, boxSScale, postTrans);
			}
			
			// Point
			mFocusedSHSNOutlineBuffer.put(mFocusedSHSNBorderBoxVertices, 0, 3);
			transformPoint(mFocusedSHSNOutlineBuffer, 18, preTrans, boxSScale, postTrans);
			mFocusedSHSNOutlineBuffer.put(mFocusedSHSNBorderPointVertices, 3, 3);
			transformPoint(mFocusedSHSNOutlineBuffer, 21, preTrans, pntScale, nullTrans);
			
			mFocusedSHSNOutlineBuffer.put(mFocusedSHSNBorderPointVertices, 0, 6);
			transformPoint(mFocusedSHSNOutlineBuffer, 24, preTrans, pntScale, nullTrans);
			transformPoint(mFocusedSHSNOutlineBuffer, 27, preTrans, pntScale, nullTrans);
			
			mFocusedSHSNOutlineBuffer.put(mFocusedSHSNBorderPointVertices, 0, 3);
			mFocusedSHSNOutlineBuffer.put(mFocusedSHSNBorderPointVertices, 6, 3);
			transformPoint(mFocusedSHSNOutlineBuffer, 30, preTrans, pntScale, nullTrans);
			transformPoint(mFocusedSHSNOutlineBuffer, 33, preTrans, pntScale, nullTrans);
			
			mFocusedSHSNOutlineBuffer.put(mFocusedSHSNBorderBoxVertices, 3, 3);
			transformPoint(mFocusedSHSNOutlineBuffer, 36, preTrans, boxSScale, postTrans);
			mFocusedSHSNOutlineBuffer.put(mFocusedSHSNBorderPointVertices, 6, 3);
			transformPoint(mFocusedSHSNOutlineBuffer, 39, preTrans, pntScale, nullTrans);
			
			mFocusedSHSNOutlineBuffer.position(0);
		}
	}
	
	private void transformPoint(FloatBuffer b, int point, float[] preTrans, float[] scale, float[] postTrans)
	{
		float x = b.get(point + 0);
		float y = b.get(point + 1);
		float z = b.get(point + 2);
		
		b.put(point + 0, ((x + preTrans[0])*scale[0]) + postTrans[0]);
		b.put(point + 1, ((y + preTrans[1])*scale[1]) + postTrans[1]);
		b.put(point + 2, ((z + preTrans[2])*scale[2]) + postTrans[2]);
	}
	
	public void SetPosition(float xPos, float yPos, float zPos) 
	{
		mPos.set(xPos, yPos, zPos);
	}
	
	public void SetPosition(Vector3 position) 
	{
		mPos.set(position);
	}
	
	public void ResetScroll(){
		mScrollStartTime = 0;
	}
	
	public float GetTextAlpha() {
		if(mNameSubStrings == null || mScrollStartTime == 0) return 1.0f;
		
		long timeAfterLastChar = (System.currentTimeMillis()-mScrollStartTime)-(mTotalDrawTime-DRAW_END_CHAR_TIME);
		if(timeAfterLastChar > DRAW_END_CHAR_TIME)
			return 0.0f;
		else
			return 1.0f-(float)timeAfterLastChar/(float)DRAW_END_CHAR_TIME;
	}
	
	public boolean FullScrollComplete() {
		//Log.v(TAG,mName+" mScrollStartTime="+mScrollStartTime+" mTotalDrawTime="+mTotalDrawTime);
		if(mScrollStartTime == 0) return false;
		
		return (System.currentTimeMillis()-mScrollStartTime > mTotalDrawTime);
	}
	
	// Returns an updated name string (scrolled, if it requires scrolling)
	public String GetUpdateNameString(boolean firstNameOnly)
	{		
		if(mScrollStartTime == 0) {
			mScrollStartTime = System.currentTimeMillis();
		}
		
		if(firstNameOnly)
			return mShortName;
		
		if(mNameSubStrings == null) {
			return mName;
		}

		long currentDrawTime = (System.currentTimeMillis()-mScrollStartTime);
		
		//Log.v(TAG,mName+" currentDrawTime="+currentDrawTime+" mScrollStartTime="+mScrollStartTime+" mTotalDrawTime="+mTotalDrawTime);
		
		if(currentDrawTime > mTotalDrawTime) {
			return mNameSubStrings.get(mNameSubStrings.size()-1); 	// Last Char
		} else if(currentDrawTime < DRAW_FIRST_CHAR_TIME) {  
			return mNameSubStrings.get(0);							// First Char
		} 
		
		currentDrawTime -= DRAW_FIRST_CHAR_TIME;  // Middle or End Char
		int numSubStrings = (int) (currentDrawTime/DRAW_MIDDLE_CHAR_TIME)+1;
		if(numSubStrings >= mNameSubStrings.size()) numSubStrings = mNameSubStrings.size()-1;
		
		return mNameSubStrings.get(numSubStrings);
	}
	
	public void setGLPos(Vector3 orientation, Vector3 camOffset, Vector3 userOffset)
	{
		pTemp.set(mPos.x + userOffset.x, mPos.y + userOffset.y, mPos.z + userOffset.z);
		mTemp.setZero();		
		mTemp.rotZ(Util.degreesToRadians(orientation.x));
		mTemp.transform(pTemp);
		mTemp.rotX(Util.degreesToRadians(orientation.y));
		mTemp.transform(pTemp);
		mGLPosition.set(pTemp.x, pTemp.y, pTemp.z).add(camOffset).normalize();
		mIsNormalized = false;
	}
	
	public void setNormalizedGLPos(Vector3 orientation, Vector3 camOffset, Vector3 userOffset, Vector3 drawOrigin, float radius)
	{
		//Log.v(TAG,mName+" POS="+mPos.x+","+mPos.y);
		mGLPosition.set(mPos).sub(drawOrigin).normalize().mult(radius).add(drawOrigin);
		
		pTemp.set(mGLPosition.x + userOffset.x, mGLPosition.y + userOffset.y, mGLPosition.z + userOffset.z);
		mTemp.setZero();		
		mTemp.rotZ(Util.degreesToRadians(orientation.x));
		mTemp.transform(pTemp);
		mTemp.rotX(Util.degreesToRadians(orientation.y));
		mTemp.transform(pTemp);
		mGLPosition.set(pTemp.x, pTemp.y, pTemp.z).add(camOffset).normalize();
		mIsNormalized = true;
		
		//Log.v(TAG,mName+" GLPOS="+mGLPosition.x+","+mGLPosition.y);
	}
	
	// The draw method for the square with the GL context */
	public void DrawFocused(GL10 gl, float metersDistFromUser, float alpha, boolean singlePOI, Vector3 glPos, boolean firstNameOnly, boolean radarMode)
	{	
		//Log.v(TAG,"DrawFocused " + mName);
		
		if(radarMode && (mType != POI.POI_TYPE_BUDDY)) return;
		
		if(glPos == null) glPos = mGLPosition;
		
		String currentDisplayName = "";
		String currentDisplayDist = "";
		PointF distDimensions = null;
		
		currentDisplayName = GetUpdateNameString(firstNameOnly);
		if(!firstNameOnly) {
			currentDisplayDist = Util.GetDistanceString(metersDistFromUser, mUnits);
			
			distDimensions = mGLDynamicText.getStringDimensions(currentDisplayDist);
			distDimensions.set(distDimensions.x*FRAME_SCALE_RATIO, distDimensions.y*FRAME_SCALE_RATIO);
		}
		
		PointF nameDimensions = mGLDynamicText.getStringDimensions(currentDisplayName);
		nameDimensions.set(nameDimensions.x*FRAME_SCALE_RATIO, nameDimensions.y*FRAME_SCALE_RATIO);
			
		// Location Values
		float focusedItemYLoc	= CommonRender.POI_FOCUSED_BOX_HEIGHT*(mNameBoxHeight/2.0f);
		float boxWidth			= (firstNameOnly) ? mTotalBoxSWidth : mTotalBoxFWidth;
		//Log.v(TAG,"poi="+mPOIWidth+" name="+mNameBoxWidth+" dist="+mDistBoxWidth+" box="+mTotalBoxWidth);
		
		// Set the face rotation 
		gl.glFrontFace(GL10.GL_CW); 
				
		// Apply matrix transformations
		gl.glPushMatrix();
	 
			gl.glTranslatef(glPos.x, glPos.y, glPos.z);
			
			//Log.v(TAG,mName + " glPos="+glPos.x+", "+glPos.y+", "+glPos.z);

			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glDisable(GL10.GL_TEXTURE_2D);

			// Outline Color
			gl.glColor4f(CommonRender.POI_BORDER, CommonRender.POI_BORDER, CommonRender.POI_BORDER, alpha);
			gl.glPushMatrix(); // Outline
				gl.glLineWidth(2.0f);
				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, (radarMode) ? ( firstNameOnly ? mFocusedSHSNOutlineBuffer : mFocusedSHFNOutlineBuffer ) : mFocusedFHOutlineBuffer);				
				gl.glDrawArrays(GL10.GL_LINES, 0, 14);
			gl.glPopMatrix();
			
			// Draw Background
			gl.glColor4f(CommonRender.POI_BG_COLOR, CommonRender.POI_BG_COLOR, CommonRender.POI_BG_COLOR, alpha);
			gl.glPushMatrix(); // Triangle
				gl.glScalef(POINT_WIDTH_SCALE*CommonRender.POI_FOCUSED_TIP_WIDTH, focusedItemYLoc, 1.0f);
				gl.glTranslatef(0.0f, Y_OFFSET, 0.0f);				

				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, (radarMode) ? ( firstNameOnly ? mFocusedSHSNBorderPointVertexBuffer : mFocusedSHFNBorderPointVertexBuffer ) : mFocusedFHBorderPointVertexBuffer);				
				gl.glDrawArrays(GL10.GL_TRIANGLES, 0, mFocusedFHBorderPointVertices.length/3);
			gl.glPopMatrix();		

			gl.glPushMatrix(); // Box
				gl.glTranslatef(mPOIWidth/2.0f, 0.0f, 0.0f);
				gl.glScalef((boxWidth-mPOIWidth)/2.0f, focusedItemYLoc, 1.0f);
				gl.glTranslatef(0.0f, Y_OFFSET, 0.0f);					

				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, (radarMode) ? ( firstNameOnly ? mFocusedSHSNBorderBoxVertexBuffer : mFocusedSHFNBorderBoxVertexBuffer ) : mFocusedFHBorderBoxVertexBuffer);				
				gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mFocusedFHBorderBoxVertices.length/ 3);
			gl.glPopMatrix();
			
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

			// Draw Text
			//float textAlpha = GetTextAlpha();
			//gl.glColor4f(CommonRender.POI_TEXT, CommonRender.POI_TEXT, CommonRender.POI_TEXT, textAlpha > alpha ? alpha : textAlpha);  // Take Smaller
			gl.glColor4f(CommonRender.POI_TEXT, CommonRender.POI_TEXT, CommonRender.POI_TEXT, alpha);
			gl.glPushMatrix(); // Name
				gl.glTranslatef(-1.0f*(boxWidth/2.0f)+mPOIWidth+((firstNameOnly) ? mSNameBoxWidth : mFNameBoxWidth)/2.0f, 1.05f*focusedItemYLoc*(radarMode ? CommonRender.POI_UNFOCUSED_TIP_HEIGHT : CommonRender.POI_FOCUSED_TIP_HEIGHT), 0.0f);		
				gl.glScalef(FRAME_SCALE_RATIO, FRAME_SCALE_RATIO, 1.0f);
				gl.glTranslatef(0.0f, focusedItemYLoc, 0.0f);
				mGLDynamicText.draw(gl, currentDisplayName);
			gl.glPopMatrix();
			
			if(!mIconIsFresh) {
				gl.glColor4f(CommonRender.POI_NOT_FRESH, CommonRender.POI_NOT_FRESH, CommonRender.POI_NOT_FRESH, alpha);
			}			
			if(!firstNameOnly) { // Distance
				gl.glPushMatrix();  
					gl.glTranslatef((boxWidth/2.0f)-mDistBoxWidth/2.0f, 1.05f*focusedItemYLoc*(radarMode ? CommonRender.POI_UNFOCUSED_TIP_HEIGHT : CommonRender.POI_FOCUSED_TIP_HEIGHT), 0.0f);
					gl.glScalef(FRAME_SCALE_RATIO, FRAME_SCALE_RATIO, 1.0f);
					gl.glTranslatef(0.0f, focusedItemYLoc, 0.0f);				
	
					mGLDynamicText.draw(gl, currentDisplayDist);
				gl.glPopMatrix();
			}
			
			// Draw Icon
			gl.glPushMatrix();
				gl.glTranslatef(-1.0f*((boxWidth/2.0f)-mPOIWidth/2.0f), 0.0f, 0.0f);
				gl.glScalef(mNameBoxHeight/2.0f, focusedItemYLoc, 1.0f);
				gl.glTranslatef(0.0f, Y_OFFSET, 0.0f);
							
				gl.glColor4f(1.0f, 1.0f, 1.0f, alpha);
				mGLDynamicIcon.draw(gl, mType, !radarMode, singlePOI, mIconIsFresh);			
			gl.glPopMatrix();
			
		gl.glPopMatrix();
	}
	
	public void SetScale(float scale) {
		mUnfocusedPOIScale = UNFOCUSED_POI_SCALE_RATIO*scale;
	}
	
	// The draw method for the square with the GL context */
	public void DrawUnfocused(GL10 gl, float alpha, boolean singlePOI)
	{		
		// Set the face rotation 
		gl.glFrontFace(GL10.GL_CW); 
				
		// Apply matrix transformations
		gl.glPushMatrix();	
			
			//Log.v(TAG,mName+": "+mGLPosition.x+","+mGLPosition.y+","+mGLPosition.z);
			gl.glTranslatef(mGLPosition.x, mGLPosition.y, mGLPosition.z);
			gl.glScalef(mUnfocusedPOIScale, mUnfocusedPOIScale, 1.0f);
			gl.glTranslatef(0.0f, Y_OFFSET, 0.0f);			
			
			
			// -- DRAW ICON TEXTURE -- //		
			gl.glColor4f(1.0f, 1.0f, 1.0f, alpha);
			mGLDynamicIcon.draw(gl, mType, false, singlePOI, mIconIsFresh);

			// -- DRAW BACKGROUND -- //		
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glDisable(GL10.GL_TEXTURE_2D);
			//gl.glColor4f(0.1f, 0.1f, 0.1f, alpha);
			if(mIconIsFresh) {
				gl.glColor4f(mPOIBorderR[mType], mPOIBorderG[mType], mPOIBorderB[mType], alpha);
			} else {
				gl.glColor4f(CommonRender.POI_NOT_FRESH, CommonRender.POI_NOT_FRESH, CommonRender.POI_NOT_FRESH, alpha);
			}
			
			// draw box
			//gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mUnfocusedBorderBoxVertexBuffer);				
			//gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mUnfocusedBorderBoxVertices.length / 3);
			
			// draw triangular point
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mUnfocusedBorderPointVertexBuffer);				
			gl.glDrawArrays(GL10.GL_TRIANGLES, 0, mUnfocusedBorderPointVertices.length / 3);
			
			// draw outline
			//gl.glColor4f(mPOIBorderR[mType], mPOIBorderG[mType], mPOIBorderB[mType], alpha);
			gl.glColor4f(0.0f, 0.0f, 0.0f, alpha);
			gl.glLineWidth(CommonRender.POI_OUTLINE_WIDTH_PX);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mUnfocusedOutlineBuffer);				
			gl.glDrawArrays(GL10.GL_LINES, 0, 10);
			
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
			
				
		gl.glPopMatrix();
	}
}
