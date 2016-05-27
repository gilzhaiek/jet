package com.reconinstruments.dashlauncher.radar.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

public class GLDynamicPath extends GLMapObject
{
	protected static final String TAG = "GLDynamicPath";
	private final static int COORDINATES = 2;
	private final static int MAX_HISTORY = 512;

	private FloatBuffer mVertexBuffer; // Buffer for vertex-array
	private int mPathLength = 0;
	private int mPosition = 0;

	float mRed, mGreen, mBlue, mAlpha;
	float mWidth = 0.0f;

	// Constructor - Set up the buffers
	public GLDynamicPath(float red, float green, float blue, float alpha, float width)
	{
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(MAX_HISTORY * COORDINATES * 4 * 2); // 2(xy)*4bytes per buffer*2 is the double size
		byteBuffer.order(ByteOrder.nativeOrder());
		mVertexBuffer = byteBuffer.asFloatBuffer();
		mVertexBuffer.position(0); // Setup vertex-array buffer. Vertices in float. An float has 4 bytes

		mRed = red;
		mGreen = green;
		mBlue = blue;
		mAlpha = alpha;
		
		mWidth = width;
	}

	public void AddPoint(float x, float y)
	{
		AddPoint(x, y, 0.0f);
	}

	public void AddPoint(float x, float y, float z)
	{
		int endPosition = mPathLength + mPosition;

		if (endPosition >= (MAX_HISTORY * COORDINATES * 2))
		{
			endPosition = MAX_HISTORY * COORDINATES;
		}

		if (endPosition >= (MAX_HISTORY * COORDINATES))
		{
			mVertexBuffer.put(endPosition - (MAX_HISTORY * COORDINATES), x);
			mVertexBuffer.put(endPosition - (MAX_HISTORY * COORDINATES) + 1, y);
			if (COORDINATES > 2)
			{
				mVertexBuffer.put(endPosition - (MAX_HISTORY * COORDINATES) + 2, z);
			}
		}
		mVertexBuffer.put(endPosition, x);
		mVertexBuffer.put(endPosition+1, y);
		if(COORDINATES > 2) {
			mVertexBuffer.put(endPosition+2, z);
		}

		if (mPathLength >= (MAX_HISTORY * COORDINATES))
		{
			mPosition += COORDINATES;
			if (mPosition >= (MAX_HISTORY * COORDINATES))
				mPosition = 0;
		}
		if (mPathLength < (MAX_HISTORY * COORDINATES))
			mPathLength += COORDINATES;

		// Log.v(TAG,"X="+x+" Y="+y+", ePos="+endPosition+" sPos="+mPosition+" length="+mPathLength);
	}

	public void Clear()
	{
		mPathLength = 0;
		mPosition = 0;
	}

	@Override
	public void drawGL(GL10 gl)
	{
		gl.glColor4f(mRed, mGreen, mBlue, mAlpha);
		gl.glLineWidth(mWidth);
		
		gl.glPushMatrix();
		gl.glTranslatef(mCamOffset.x, mCamOffset.y, mCamOffset.z);
		gl.glRotatef(mCamOrientation.y, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(mCamOrientation.x, 0.0f, 0.0f, 1.0f);
		gl.glTranslatef(mUserOffset.x, mUserOffset.y, mUserOffset.z);
		
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);	

		mVertexBuffer.position(mPosition);

		gl.glVertexPointer(COORDINATES, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, mPathLength / COORDINATES);
		
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);		
		
		gl.glPopMatrix();
	}
}
