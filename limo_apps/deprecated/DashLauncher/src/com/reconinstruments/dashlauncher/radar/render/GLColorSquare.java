package com.reconinstruments.dashlauncher.radar.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;

public class GLColorSquare extends GLMapObject
{
	private final static int SQUARE_DIVISION = 2;
	private final static String TAG = "GLColorSquare";

	private float[] mColor 				= null;
	private Rect mBoundingBox 			= null;

	private int mNumValues            	= 0;
	private int mNumVertices			= 0;
	private FloatBuffer mVertexBuffer 	= null;
	private float mDepth				= 0; 
	
	public GLColorSquare(Rect boundingBox, float depth, int color)
	{
		Log.i(TAG, "GLColorSquare");
		
		mDepth = depth;
		mBoundingBox = boundingBox;
		mColor		 = new float[]{ Color.red(color)/255.0f,  
									Color.green(color)/255.0f, 
								    Color.blue(color)/255.0f, 
								    Color.alpha(color)/255.0f };
	}

	private void setVertexBuffer(Rect boundingBox, float depth)
	{
		mNumValues 		= (6 * (SQUARE_DIVISION + 1)) * SQUARE_DIVISION;
		mNumVertices 	= mNumValues / 3;
		
		// Initialize vertex buffer
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(mNumValues * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		mVertexBuffer = byteBuffer.asFloatBuffer();
		mVertexBuffer.put(new float[mNumValues]);
		mVertexBuffer.position(0);
		
		// Set the vertex buffers values 
		float xInc = (boundingBox.right - boundingBox.left) / (float) SQUARE_DIVISION;
		float yInc = (boundingBox.top - boundingBox.bottom) / (float) SQUARE_DIVISION;
		int offsetY = 0;
		for (int y = 0; y < SQUARE_DIVISION; y++)
		{  
			offsetY = (y * (6 * (SQUARE_DIVISION + 1)));
			for (int x = 0; x < (SQUARE_DIVISION + 1); x++)
			{
				int offset = offsetY + (x * 6);
				mVertexBuffer.put(offset + 0, boundingBox.left   + xInc * x);
				mVertexBuffer.put(offset + 1, boundingBox.bottom + yInc * y);
				mVertexBuffer.put(offset + 2, depth);

				mVertexBuffer.put(offset + 3, boundingBox.left   + xInc * x);
				mVertexBuffer.put(offset + 4, boundingBox.bottom + yInc * (y + 1));
				mVertexBuffer.put(offset + 5, depth);
			}

			y++;
			offsetY = (y * (6 * (SQUARE_DIVISION + 1)));

			for (int x = 0; x < (SQUARE_DIVISION + 1); x++)
			{
				int offset = offsetY + (x * 6);
				mVertexBuffer.put(offset + 0, boundingBox.right  - xInc * x);
				mVertexBuffer.put(offset + 1, boundingBox.bottom + yInc * y);
				mVertexBuffer.put(offset + 2, depth);

				mVertexBuffer.put(offset + 3, boundingBox.right  - xInc * x);
				mVertexBuffer.put(offset + 4, boundingBox.bottom + yInc * (y + 1));
				mVertexBuffer.put(offset + 5, depth);
			}
		}
	}

	public void drawGL(GL10 gl)
	{
		if(mVertexBuffer == null) {
			setVertexBuffer(mBoundingBox, mDepth);
		}
		
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glColor4f(mColor[0], mColor[1], mColor[2], 1.0f);
		
		gl.glPushMatrix();
		gl.glTranslatef(mCamOffset.x, mCamOffset.y, mCamOffset.z);
		gl.glRotatef(mCamOrientation.y, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(mCamOrientation.x, 0.0f, 0.0f, 1.0f);
		
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mNumVertices);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		
		gl.glPopMatrix();
	}

}
