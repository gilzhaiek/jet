package com.reconinstruments.dashlauncher.radar.render;

//import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.ETC1Util;
import android.opengl.GLES10;
import android.opengl.GLUtils;
import android.util.Log;

public class GLBaseTexture extends GLMapObject
{
	private final static String TAG = "BaseTexture";
	
	protected int mTextureDivision = 0;
	
	protected final ReentrantLock mLock = new ReentrantLock();
	
	// buffer holding the vertices
	protected FloatBuffer	mVertexBuffer	= null;
	protected float[] 		mVertices		= null;

	// buffer holding the mapped texture coordinates
	protected FloatBuffer	mTextureBuffer	= null;
	protected float[] 		mTexture		= null;
	
	// The texture pointer 
	protected int[]			mTextures = null;
	protected int 			mTextureIndex = 0;
	protected int			mMaxTextures = 1;
	protected String 		mTextureFileName = null;
	
	protected boolean		mLoadGlTexture = false;
	
	protected float			mXOffset = 0.0f;
	protected float			mYOffset = 0.0f;
	protected float			mZOffset = 0.0f;	
	
	public GLBaseTexture(int textureDivision, int maxTextures){
		mTextureDivision = textureDivision;
		
		mTextures = new int[maxTextures];
		mMaxTextures = maxTextures;
		
		mVertices = new float[6*(mTextureDivision + 1)*mTextureDivision];
		for(int x = 0; x < mVertices.length; x++){ mVertices[x] = 0; } // Set array values to 0
		
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(mVertices.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		mVertexBuffer = byteBuffer.asFloatBuffer();
		mVertexBuffer.put(mVertices);
		mVertexBuffer.position(0);

		mTexture = new float[4*(mTextureDivision + 1)*mTextureDivision];
		for(int x = 0; x < mTexture.length; x++){ mTexture[x] = 0; } // Set array values to 0
		
		byteBuffer = ByteBuffer.allocateDirect(mTexture.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		mTextureBuffer = byteBuffer.asFloatBuffer();
		mTextureBuffer.put(mTexture);
		mTextureBuffer.position(0);
	}
	
	public GLBaseTexture(int maxTextures)
	{
		mTextureDivision = 1;
		
		mTextures = new int[maxTextures];
		mMaxTextures = maxTextures;
		
		mVertices = new float[]{ - 1.0f, - 1.0f, 0.0f, 
							     - 1.0f, + 1.0f, 0.0f, 
							     + 1.0f, - 1.0f, 0.0f, 
							     + 1.0f, + 1.0f, 0.0f };
		
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(mVertices.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		mVertexBuffer = byteBuffer.asFloatBuffer();
		mVertexBuffer.put(mVertices);
		mVertexBuffer.position(0);
		
		mTexture = new float[]{ 0.0f, 1.0f,
							    0.0f, 0.0f,
							    1.0f, 1.0f,
							    1.0f, 0.0f };

		byteBuffer = ByteBuffer.allocateDirect(mTexture.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		mTextureBuffer = byteBuffer.asFloatBuffer();
		mTextureBuffer.put(mTexture);
		mTextureBuffer.position(0);
	}
	
	public void SetOffsets(float x, float y, float z) {
		mXOffset = x;
		mYOffset = y;
		mZOffset = z;
	}
	
	public float GetXOffset() {return mXOffset;	}
	public float GetYOffset() {return mYOffset;	}
	public float GetZOffset() {return mZOffset;	}
	
	public void setTextureIndex(int index)
	{
		mTextureIndex = index;
	}
	
	public void draw(GL10 gl, float alpha) 
	{
		SetAlpha(alpha);
		draw(gl);
	}
	
	@Override
	public void drawGL(GL10 gl) 
	{
		gl.glPushMatrix();
			if(mLoadGlTexture) { LoadGLTexture(gl); }
			
			if(mTextures[mTextureIndex] == 0 || mTextureIndex >= mTextures.length)
				return;			
			
			gl.glTranslatef(mXOffset, mYOffset, mZOffset);

			gl.glEnable(GL10.GL_TEXTURE_2D);
			gl.glColor4f(1.0f, 1.0f, 1.0f, mAlpha);
			
			// bind the previously generated texture
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextures[mTextureIndex]);
	
			// Set the face rotation
			gl.glFrontFace(GL10.GL_CW);
			
			// Point to our buffersC
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	
			// Point to our vertex buffer
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);
	
			// Draw the vertices as triangle strip
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mVertices.length / 3);
	
			// Disable the client state before leaving
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		
		gl.glPopMatrix();
	}

	public void LoadGLTexture(GL10 gl, Context context, int id, int textureIndex)
	{
		mLock.lock();
		
		gl.glPushMatrix();
		
			// generate one texture pointer 
			gl.glGenTextures(1, mTextures, textureIndex);
			// ...and bind it to our array
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextures[textureIndex]);
	
			// create nearest filtered texture
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
	
			// PKM
			/*InputStream rawInputStream = context.getResources().openRawResource(id);
			try {
				ETC1Util.loadTexture(GL10.GL_TEXTURE_2D, 0, 0, GL10.GL_RGB, GL10.GL_UNSIGNED_SHORT_5_6_5, rawInputStream);
			} catch (IOException e) {
				e.printStackTrace();
			}*/
			
			Bitmap bitmap = BitmapFactory.decodeStream(context.getResources().openRawResource(id));//BitmapFactory.decodeResource(context.getResources(), id);
	
			// Use Android GLUtils to specify a two-dimensional texture image from our bitmap
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
					
			// Clean up
			bitmap.recycle();
			
			mLoadGlTexture = false;
		
		gl.glPopMatrix();
		
		mLock.unlock();
	}
	
	public void SetLoadOnDraw() {
		mLoadGlTexture = true;
	}
	
	public boolean NeedsLoading() {
		return mLoadGlTexture; 
	}
	
	public void UnloadGLTexture(GL10 gl) {
		if(mTextures[0] != 0) {
			gl.glDeleteTextures(1, mTextures, 0);
			mTextures[0] = 0;
			Log.v(TAG,"UnloadGLTexture " + mTextureFileName);
		}
		
		mLoadGlTexture = true;
	}
	
	public void LoadGLTexture(GL10 gl) {
		if(mTextureFileName == null) {
			return;
		}
				
		Log.v(TAG,"LoadGLTexture " + mTextureFileName);
		mLock.lock();
		
		gl.glPushMatrix();
		
	        gl.glGenTextures(1, mTextures, 0); // generate one texture pointer
	        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextures[0]); // ...and bind it to our array
	
	        // create nearest filtered texture
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE );
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE );
	        
	        Bitmap bitmap;
			try {
				InputStream istream = new BufferedInputStream(new FileInputStream(mTextureFileName));
				bitmap = BitmapFactory.decodeStream(istream);
		        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
				istream.close();
		        bitmap.recycle(); 
			} catch (FileNotFoundException e) {
				Log.v(TAG,"File not found - Failed to load " + mTextureFileName);
				e.printStackTrace();
			} catch (IOException e) {
				(new File(mTextureFileName)).delete();
				Log.v(TAG,"IOException  - Failed to load " + mTextureFileName);
				e.printStackTrace();
			} catch (Exception e) {
				(new File(mTextureFileName)).delete();
				Log.v(TAG,"Exception  - Failed to load " + mTextureFileName);
				e.printStackTrace();
			}
		    
	        mLoadGlTexture = false;
        
        gl.glPopMatrix();
        
        mLock.unlock();
        Log.v(TAG,"Done LoadGLTexture " + mTextureFileName);
	}		
}
