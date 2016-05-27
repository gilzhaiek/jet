package com.reconinstruments.dashlauncher.radar.render;


import android.graphics.Rect;
import android.util.Log;

public class GLSurfaceTexture extends GLBaseTexture {
	private final static int TEXTURE_DIVISION = 2;
	private final static String TAG = "GLSurfaceTexture";
	
	public Rect mBoundingBox = null;
		
	public GLSurfaceTexture(){
		super(TEXTURE_DIVISION, 1);
		
		mBoundingBox = new Rect();
	}
	
	public GLSurfaceTexture(String textureFileName, Rect boundingBox) {
		super(TEXTURE_DIVISION, 1);
		SetTextureFile(textureFileName, boundingBox);
	}
	
	public void SetBoundingBox(Rect boundingBox) {
		mBoundingBox = boundingBox;
		
		float xInc = (mBoundingBox.right - mBoundingBox.left)/(float)TEXTURE_DIVISION;
		float yInc = (mBoundingBox.top - mBoundingBox.bottom)/(float)TEXTURE_DIVISION;
		int offsetY = 0;
		for(int y = 0; y < TEXTURE_DIVISION; y++)
		{
			offsetY = (y*(6*(TEXTURE_DIVISION+1)));
			for(int x = 0; x < (TEXTURE_DIVISION + 1); x++)
			{
				int offset = offsetY + (x*6);
				mVertexBuffer.put(offset + 0, mBoundingBox.left   + xInc*x);
				mVertexBuffer.put(offset + 1, mBoundingBox.bottom + yInc*y);
				
				mVertexBuffer.put(offset + 3, mBoundingBox.left   + xInc*x);
				mVertexBuffer.put(offset + 4, mBoundingBox.bottom + yInc*(y+1));
			}
			
			y++;
			offsetY = (y*(6*(TEXTURE_DIVISION+1)));
			
			for(int x = 0; x < (TEXTURE_DIVISION + 1); x++)
			{
				int offset = offsetY + (x*6);
				mVertexBuffer.put(offset + 0, mBoundingBox.right  - xInc*x);
				mVertexBuffer.put(offset + 1, mBoundingBox.bottom + yInc*y);
				
				mVertexBuffer.put(offset + 3, mBoundingBox.right  - xInc*x);
				mVertexBuffer.put(offset + 4, mBoundingBox.bottom + yInc*(y+1));
			}
		}
		
		float incX = 1.0f/(float)TEXTURE_DIVISION;	
		float incY = 1.0f/(float)TEXTURE_DIVISION;	
		for(int y = 0; y < TEXTURE_DIVISION; y++)
		{
			offsetY = (y*(4*(TEXTURE_DIVISION+1)));
			for(int x = 0; x < (TEXTURE_DIVISION + 1); x++)
			{
				int offset = offsetY + (x*4);
				mTextureBuffer.put(offset + 0, incX*x);
				mTextureBuffer.put(offset + 1, 1.0f - incY*y);
				
				mTextureBuffer.put(offset + 2, incX*x);
				mTextureBuffer.put(offset + 3, 1.0f - incY*(y+1));
			}
			
			y++;
			offsetY = (y*(4*(TEXTURE_DIVISION+1)));
			
			for(int x = 0; x < (TEXTURE_DIVISION + 1); x++)
			{
				int offset = offsetY + (x*4);
				mTextureBuffer.put(offset + 0, 1.0f - incX*x);
				mTextureBuffer.put(offset + 1, 1.0f - incY*y);
				
				mTextureBuffer.put(offset + 2, 1.0f - incX*x);
				mTextureBuffer.put(offset + 3, 1.0f - incY*(y+1));
			}
		}
	}
	
	public void SetTextureFile(String textureFileName, Rect boundingBox) {
		//Log.v(TAG,"SetTextureBytes");
		mLock.lock();
		try {
			int tmpVal = 0;
			if(boundingBox.left > boundingBox.right) {
				tmpVal = boundingBox.left;
				boundingBox.right = boundingBox.left;
				boundingBox.left = tmpVal;
			}
	
			if(boundingBox.top > boundingBox.bottom) {
				tmpVal = boundingBox.top;
				boundingBox.top = boundingBox.bottom;
				boundingBox.bottom = tmpVal;
			}
	
			SetBoundingBox(boundingBox);
	
			//Log.v(TAG,"left="+boundingBox.left+" bottom="+boundingBox.bottom + " right="+boundingBox.right+" top="+boundingBox.top);
	
			mTextureFileName = textureFileName;
	
			mLoadGlTexture = true;
		} catch (Exception e) {}
		
		mLock.unlock();
	}
}
