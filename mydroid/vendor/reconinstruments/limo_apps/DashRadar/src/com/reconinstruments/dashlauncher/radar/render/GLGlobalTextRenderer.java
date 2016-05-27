package com.reconinstruments.dashlauncher.radar.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.opengl.GLUtils;
import android.util.Log;

// This class renders text on the screen using a map of ascii on the screen
public class GLGlobalTextRenderer
{
	private static final String TAG			= "GLGlobalTextRenderer";
	private boolean mIsInitialized	= false;
	
	private int[] mAsciiCharRange	= {32, 127}; // Inclusive range of ascii characters supported
	private int   mAsciiCharCount	= (mAsciiCharRange[1] - mAsciiCharRange[0]) + 1;
	private float[] mWidth			= new float[mAsciiCharCount];
	private float[] mHeight			= new float[mAsciiCharCount];

	// buffer holding the vertices
	private FloatBuffer mVertexBuffer;
	private float[] mVertices;
	// buffer holding the texture coordinates
	private FloatBuffer mTextureBuffer;	
	private float[] mTexture = { 0.0f, 1.0f, 
								 0.0f, 0.0f, 
								 1.0f, 1.0f, 
								 1.0f, 0.0f };
	
	private int[] mTexturePointers = new int[mAsciiCharCount];
	private DecimalFormat df = new DecimalFormat();
	
	private static PointF mStringDimensions;

	public GLGlobalTextRenderer(){	}

	public PointF getStringDimensions(String text)
	{
		if(!mIsInitialized){ return null; }
		
		float currentWidth  = 0;
		float currentHeight = 0;
		
		for (int i = 0; i < text.length(); i++)
		{
			char glyph 		= text.charAt(i);
			int  glyphInt 	= (int)glyph - mAsciiCharRange[0];
			
			if(glyphInt > -1 && glyphInt < mAsciiCharCount)
			{
				currentWidth += mWidth[glyphInt];
				currentHeight = Math.max(currentHeight, mHeight[glyphInt]);
				
				mStringDimensions.set(currentWidth, currentHeight);
			}
		}
		
		return mStringDimensions;
	}
	
	public int getCharsWithinWidth(String text, float width, int offset)
	{
		if(!mIsInitialized){ return -1; }
		
		int charCount = 0;
		
		for (int i = offset; i < text.length(); i++)
		{
			char glyph 		= text.charAt(i);
			int  glyphInt 	= (int)glyph - mAsciiCharRange[0];
			
			if(glyphInt > -1 && glyphInt < mAsciiCharCount)
			{
				width -= mWidth[glyphInt];
				
				if(width < 0)
				{
					return charCount;
				}
				
				charCount++;
			}
		}
		
		return charCount;
	}
	
	/** The draw method for the square with the GL context */
	public void draw(GL10 gl, String text)
	{
		if(!mIsInitialized){ return; }
		
		// Get string dimensions rect
		PointF strDimensions = getStringDimensions(text);
		
		// Initialize vertices 
		for(int v = 0; v < mVertices.length / 3; v++)
		{	
			mVertices[(v * 3) + 0] = -strDimensions.x/2; 
			mVertices[(v * 3) + 1] = 0; 
			mVertices[(v * 3) + 2] = 0; 
		}

		for (int i = 0; i < text.length(); i++)
		{			
			char glyph 		= text.charAt(i);
			int  glyphInt 	= (int)glyph - mAsciiCharRange[0]; 
			
			if(glyphInt > -1 && glyphInt < mAsciiCharCount)
			{
				//Log.i(TAG, "glyph: '" + glyph +"', ID: " + Character.getType(glyph) + ", glyphInt: " + glyphInt + ", mAsciiCharRange[0]: " + mAsciiCharRange[0]);
				
				mVertices[6] += mWidth[glyphInt]; // right
				mVertices[9] += mWidth[glyphInt]; // right
				mVertices[4]  = mVertices[1] + mHeight[glyphInt];// top = bottom + heightofglyph
				mVertices[10] = mVertices[7] + mHeight[glyphInt];// top = bottom + heightofglyph
	
				// bind the previously generated texture
				gl.glBindTexture(GL10.GL_TEXTURE_2D, mTexturePointers[glyphInt]);
	
				// Point to our buffers 
				gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
				gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	
				mVertexBuffer.put(mVertices);
				mVertexBuffer.position(0); 
	
				// Point to our vertex buffer
				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
				gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);
				
					// --- DEBUG SHIIIIIIT --- //
//					gl.glDisable(GL10.GL_TEXTURE_2D);
//					gl.glLineWidth(1.5f);
//					gl.glColor4f(0.0f, 1.0f, 1.0f, 0.5f);
//					gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVertices.length / 3);
					// ----------------------- //
									
					gl.glEnable(GL10.GL_TEXTURE_2D);
					gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mVertices.length / 3);
	
				// Disable the client state before leaving
				gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
				gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
				
				mVertices[0] = mVertices[6]; // left = right
				mVertices[3] = mVertices[9]; // left = right
			}
		}
	}

	public void loadGLTextures(GL10 gl, int fontSize)
	{
		mStringDimensions = new PointF();
		
		df.setMaximumFractionDigits(4);
		df.setMinimumFractionDigits(4);
		
		mVertices = new float[12]; // 4 * 3D vertices (has 3 values)
		mVertexBuffer = ByteBuffer.allocateDirect(mVertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

		mTextureBuffer = ByteBuffer.allocateDirect(mTexture.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		mTextureBuffer.put(mTexture);
		mTextureBuffer.position(0);
		
		// Generate Texture Pointers
		gl.glGenTextures(mAsciiCharCount, mTexturePointers, 0);
		
		int currentAsciiCharID = mAsciiCharRange[0];

		for (int i = 0; i < mAsciiCharCount; i++)
		{
			// loading texture
			BitmapStruct bitmapStruct;

			// ...and bind it to our array
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTexturePointers[i]);

			// create nearest filtered texture
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

			Log.i(TAG, "currentAsciiCharID: " + currentAsciiCharID);
			bitmapStruct = BitmapUtil.getAsciiChar(Character.toString((char)currentAsciiCharID++), fontSize);
			
			// calculate the bitmap's size respectively to the size of the screen
//			mWidth[i]  = bitmap.getWidth()  / 100f;
//			mHeight[i] = bitmap.getHeight() / 100f;
			mWidth[i]  = bitmapStruct.mBitmap.getWidth()  / 100f;
			mHeight[i] = bitmapStruct.mBitmap.getHeight() / 100f;

//			Log.i(TAG, mWidth[i] + " x " + mHeight[i]);

//			if(mWidth[i] > mHeight[i]) mHeight[i] = mWidth[i];
//			else mWidth[i] = mHeight[i];

			// Use Android GLUtils to specify a two-dimensional texture image from our bitmap
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmapStruct.mBitmap, 0);

			// Clean up
			bitmapStruct.mBitmap.recycle();
		}
		
		mIsInitialized = true;
	}
}
