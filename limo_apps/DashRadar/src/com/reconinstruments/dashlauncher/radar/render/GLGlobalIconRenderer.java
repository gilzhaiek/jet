package com.reconinstruments.dashlauncher.radar.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.opengl.GLUtils;

import com.reconinstruments.dashlauncher.radar.maps.objects.POI;
import com.reconinstruments.dashradar.R;

public class GLGlobalIconRenderer
{
	private static final String TAG = "GLGlobalIconRenderer";
	private boolean mIsInitialized = false;
	
	private static final float POI_ICON_SCALE = 0.025f;
	
	// buffer holding the vertices
	private FloatBuffer mFocusedVertexBuffer;
	private float mFocusedVertices[] = new float[]{ -CommonRender.POI_ASPECT, -1.1f+1.0f*CommonRender.POI_FOCUSED_TIP_HEIGHT-POI_ICON_SCALE, 0.0f, 
										    -CommonRender.POI_ASPECT, -1.1f+1.0f*CommonRender.POI_FOCUSED_TIP_HEIGHT+2.0f, 0.0f, 
										    +CommonRender.POI_ASPECT+2.0f*POI_ICON_SCALE, -1.1f+1.0f*CommonRender.POI_FOCUSED_TIP_HEIGHT-POI_ICON_SCALE, 0.0f, 
										    +CommonRender.POI_ASPECT+2.0f*POI_ICON_SCALE, -1.1f+1.0f*CommonRender.POI_FOCUSED_TIP_HEIGHT+2.0f, 0.0f };
	
	private FloatBuffer mUnfocusedVertexBuffer;
	private float mUnfocusedVertices[] = new float[]{ -CommonRender.POI_ASPECT, -1.1f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT-POI_ICON_SCALE, 0.0f, 
										    -CommonRender.POI_ASPECT, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT+2.0f-POI_ICON_SCALE, 0.0f, 
										    +CommonRender.POI_ASPECT, -1.1f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT-POI_ICON_SCALE, 0.0f, 
										    +CommonRender.POI_ASPECT, -1.0f+1.0f*CommonRender.POI_UNFOCUSED_TIP_HEIGHT+2.0f-POI_ICON_SCALE, 0.0f };
	
	
	// buffer holding the texture coordinates
	private FloatBuffer textureBuffer;
	private float texture[] = { 0.0f, 1.0f,
								0.0f, 0.0f,
								1.0f, 1.0f,
								1.0f, 0.0f };
	
	// Unfocused Single, Focused Single, Unfocused Multiple, Focused Multiple
	private final int UNFOCUSED_SINGLE_OFFSET			= 0;
	private final int FOCUSED_SINGLE_OFFSET				= POI.NUM_POI_TYPE+UNFOCUSED_SINGLE_OFFSET;
	private final int UNFOCUSED_MULTI_OFFSET			= POI.NUM_POI_TYPE+FOCUSED_SINGLE_OFFSET;
	private final int FOCUSED_MULTI_OFFSET				= POI.NUM_POI_TYPE+UNFOCUSED_MULTI_OFFSET;
	private final int UNFOCUSED_BUDDY_NOT_FRESH_OFFSET	= POI.NUM_POI_TYPE+FOCUSED_MULTI_OFFSET;
	private final int FOCUSED_BUDDY_NOT_FRESH_OFFSET	= 1+UNFOCUSED_BUDDY_NOT_FRESH_OFFSET;
	private final int TEXTURE_POINTERS_SIZE				= 1+FOCUSED_BUDDY_NOT_FRESH_OFFSET;
	
	private int[] mTexturePointers	= new int[TEXTURE_POINTERS_SIZE];
	
	public GLGlobalIconRenderer(){ }
	
	/** The draw method for the square with the GL context */
	public void draw(GL10 gl, int type, boolean focused, boolean singlePOI, boolean iconIsFresh)
	{			
		if(!mIsInitialized){ return; }
		
		gl.glPushMatrix();
		
			gl.glScalef(1.0f+2.0f*POI_ICON_SCALE, 1.0f+2.0f*POI_ICON_SCALE, 1.0f);
		
			// -- DRAW ICON TEXTURE -- //
			// Enable the client state before leaving
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			
			// Bind the previously generated texture
			if(focused){
				if(type == POI.POI_TYPE_BUDDY && !iconIsFresh) {
					gl.glBindTexture(GL10.GL_TEXTURE_2D, mTexturePointers[FOCUSED_BUDDY_NOT_FRESH_OFFSET]);
				}
				else if(singlePOI) {
					gl.glBindTexture(GL10.GL_TEXTURE_2D, mTexturePointers[FOCUSED_SINGLE_OFFSET+type]);
				}
				else {
					gl.glBindTexture(GL10.GL_TEXTURE_2D, mTexturePointers[FOCUSED_MULTI_OFFSET+type]);
				}
			} else {
				if(type == POI.POI_TYPE_BUDDY && !iconIsFresh) {
					gl.glBindTexture(GL10.GL_TEXTURE_2D, mTexturePointers[UNFOCUSED_BUDDY_NOT_FRESH_OFFSET]);
				}
				else if(singlePOI) {
					gl.glBindTexture(GL10.GL_TEXTURE_2D, mTexturePointers[UNFOCUSED_SINGLE_OFFSET+type]);
				}
				else {
					gl.glBindTexture(GL10.GL_TEXTURE_2D, mTexturePointers[UNFOCUSED_MULTI_OFFSET+type]);
				}
			}
			
			// Point to our vertex buffer
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, (focused) ? mFocusedVertexBuffer : mUnfocusedVertexBuffer);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
	
			// Draw the vertices as triangle strip
			gl.glEnable(GL10.GL_TEXTURE_2D);
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mFocusedVertices.length / 3);
			
			//-- DEBUG SHIZ -- //
	//		gl.glDisable(GL10.GL_TEXTURE_2D);
	//		gl.glLineWidth(1.5f);
	//		gl.glColor4f(1.0f, 0.0f, 0.0f, 0.5f);
	//		gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, vertices.length / 3);
			// --------------- //
			
			// Disable the client state before leaving
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);		
		
		gl.glPopMatrix();
	}

	public void loadGLTextures(GL10 gl, Context context)
	{
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(mFocusedVertices.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		mFocusedVertexBuffer = byteBuffer.asFloatBuffer();
		mFocusedVertexBuffer.put(mFocusedVertices);
		mFocusedVertexBuffer.position(0);

		byteBuffer = ByteBuffer.allocateDirect(mUnfocusedVertices.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		mUnfocusedVertexBuffer = byteBuffer.asFloatBuffer();
		mUnfocusedVertexBuffer.put(mUnfocusedVertices);
		mUnfocusedVertexBuffer.position(0);

		byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		textureBuffer = byteBuffer.asFloatBuffer();
		textureBuffer.put(texture);
		textureBuffer.position(0);
		
		// Generate Texture Pointers
		gl.glGenTextures(mTexturePointers.length, mTexturePointers, 0);

		for (int mType = 0; mType < mTexturePointers.length; mType++)
		{
			int resourceID = -1;
			switch (mType) 
			{	
				/*case POI.POI_TYPE_SKICENTER 			+ UNFOCUSED_SINGLE_OFFSET	: resourceID = R.drawable.nav_chairlift; break;
				case POI.POI_TYPE_SKICENTER 			+ FOCUSED_SINGLE_OFFSET		: resourceID = R.drawable.nav_chairlift; break;
				case POI.POI_TYPE_SKICENTER 			+ UNFOCUSED_MULTI_OFFSET	: resourceID = R.drawable.nav_chairlift; break;
				case POI.POI_TYPE_SKICENTER 			+ FOCUSED_MULTI_OFFSET		: resourceID = R.drawable.nav_chairlift; break;*/  
				case POI.POI_TYPE_RESTAURANT 			+ UNFOCUSED_SINGLE_OFFSET	: resourceID = R.drawable.nav_food_icon; break;
				case POI.POI_TYPE_RESTAURANT 			+ FOCUSED_SINGLE_OFFSET		: resourceID = R.drawable.nav_food_lable; break;
				case POI.POI_TYPE_RESTAURANT 			+ UNFOCUSED_MULTI_OFFSET	: resourceID = R.drawable.nav_food_plus_icon; break;
				case POI.POI_TYPE_RESTAURANT 			+ FOCUSED_MULTI_OFFSET		: resourceID = R.drawable.nav_food_plus_lable; break;
				/*case POI.POI_TYPE_BAR					: resourceID = R.drawable.nav_bar; break;
				case POI.POI_TYPE_PARK					: resourceID = R.drawable.nav_park; break;
				case POI.POI_TYPE_CARPARKING			: resourceID = R.drawable.nav_parking; break;
				case POI.POI_TYPE_RESTROOM				: resourceID = R.drawable.nav_restroom; break;*/
				case POI.POI_TYPE_CHAIRLIFTING 			+ UNFOCUSED_SINGLE_OFFSET	: resourceID = R.drawable.nav_chairlift_icon; break;
				case POI.POI_TYPE_CHAIRLIFTING 			+ FOCUSED_SINGLE_OFFSET		: resourceID = R.drawable.nav_chairlift_lable; break;
				case POI.POI_TYPE_CHAIRLIFTING 			+ UNFOCUSED_MULTI_OFFSET	: resourceID = R.drawable.nav_chairlift_plus_icon; break;
				case POI.POI_TYPE_CHAIRLIFTING 			+ FOCUSED_MULTI_OFFSET		: resourceID = R.drawable.nav_chairlift_plus_lable; break;  
				/*case POI.POI_TYPE_SKIERDROPOFF_PARKING	: resourceID = R.drawable.nav_parking; break;
				case POI.POI_TYPE_INFORMATION			: resourceID = R.drawable.nav_info; break;
				case POI.POI_TYPE_HOTEL					: resourceID = R.drawable.nav_hotel; break;
				case POI.POI_TYPE_BANK					: resourceID = R.drawable.nav_bank; break;
				case POI.POI_TYPE_SKISCHOOL				: resourceID = R.drawable.nav_skischool; break;*/
				case POI.POI_TYPE_BUDDY 				+ UNFOCUSED_SINGLE_OFFSET	: resourceID = R.drawable.nav_buddy_icon; break;
				case POI.POI_TYPE_BUDDY 				+ FOCUSED_SINGLE_OFFSET		: resourceID = R.drawable.nav_buddy_lable; break;
				case POI.POI_TYPE_BUDDY 				+ UNFOCUSED_MULTI_OFFSET	: resourceID = R.drawable.nav_buddy_plus_icon; break;
				case POI.POI_TYPE_BUDDY 				+ FOCUSED_MULTI_OFFSET		: resourceID = R.drawable.nav_buddy_plus_lable; break; 
				case UNFOCUSED_BUDDY_NOT_FRESH_OFFSET 								: resourceID = R.drawable.nav_buddy_lost_icon; break;
				case FOCUSED_BUDDY_NOT_FRESH_OFFSET 								: resourceID = R.drawable.nav_buddy_lost_lable; break;
			}
			
			if(resourceID >= 0) {
				// ...and bind it to our array
				gl.glBindTexture(GL10.GL_TEXTURE_2D, mTexturePointers[mType]);
				
				// create nearest filtered texture
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
				gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
			
				Bitmap bitmap = null;
				
				bitmap = BitmapUtil.getIconBitmap(BitmapFactory.decodeResource(context.getResources(), resourceID));
	
				// Use Android GLUtils to specify a two-dimensional texture image from our bitmap
				GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
	
				bitmap.recycle();
			}
		}
		
		mIsInitialized = true;
	}
}