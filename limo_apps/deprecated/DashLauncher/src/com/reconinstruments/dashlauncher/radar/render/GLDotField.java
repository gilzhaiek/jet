package com.reconinstruments.dashlauncher.radar.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import com.reconinstruments.dashlauncher.radar.prim.Vector3;

import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;

public class GLDotField extends GLMapObject
{
	private final static String TAG = "GLDotField";
	
	private FloatBuffer mVertexBuffer 	= null;
	private int 		mVertexCount	= 0;
	
	public GLDotField(int left, int top, int right, int bottom, int rows, int columns)
	{
		float[] vertices = new float[rows * columns * 3 * 2];
		
		float xInc = (right - left)/columns;
		float yInc = (top - bottom)/rows;
		
		for(int x = 0; x < rows; x++)
		{
			for(int y = 0; y < columns; y++)
			{
				int vertex = ((x * columns) + y);
				vertices[(vertex * 6) + 0] = left + (x*xInc);
				vertices[(vertex * 6) + 1] = top  - (y*yInc);
				vertices[(vertex * 6) + 2] = +5.0f;
				
				vertices[(vertex * 6) + 3] = left + (x*xInc);
				vertices[(vertex * 6) + 4] = top  - (y*yInc);
				vertices[(vertex * 6) + 5] = -5.0f;
				
				mVertexCount += 2;
			}
		}
		
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		mVertexBuffer = byteBuffer.asFloatBuffer();
		mVertexBuffer.put(vertices);
		mVertexBuffer.position(0);
	}

	public void drawGL(GL10 gl)
	{
		gl.glColor4f(0.0f, 0.0f, 0.0f, mAlpha);
		gl.glLineWidth(2.0f);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		
		gl.glPushMatrix();
		gl.glTranslatef(mCamOffset.x, mCamOffset.y, mCamOffset.z);
		gl.glRotatef(mCamOrientation.y, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(mCamOrientation.x, 0.0f, 0.0f, 1.0f);
		
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);	
		gl.glDrawArrays(GL10.GL_LINES, 0, mVertexCount);
		
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		
		gl.glPopMatrix();
	}

}
