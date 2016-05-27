package com.reconinstruments.dashlauncher.radar.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import com.reconinstruments.dashlauncher.radar.prim.Vector3;

import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;

public class GLDistanceLines extends GLMapObject
{
	private final static String TAG = "GLDistanceLines";
	
	private Vector3 mPosition		= null;
	private float 	mGroundDistance = 0.0f;
	private int 	mNumberOfLines	= 0;
	
	private FloatBuffer mVertexBuffer 	= null;
	private float[] 	mVertices		= {-5.0f, 0.0f, 0.0f, 
										    5.0f, 0.0f, 0.0f};
	
	public GLDistanceLines(Vector3 pos, float distance, int numLines)
	{
		mPosition		= pos;
		mGroundDistance = distance;
		mNumberOfLines	= numLines;
		
		
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(mVertices.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		mVertexBuffer = byteBuffer.asFloatBuffer();
		mVertexBuffer.put(mVertices);
		mVertexBuffer.position(0);
	}

	public void drawGL(GL10 gl)
	{
		gl.glColor4f(0, 0, 1, 1);
		gl.glLineWidth(3.0f);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		
		gl.glPushMatrix();
		gl.glTranslatef(mCamOffset.x, mCamOffset.y, mCamOffset.z);
		gl.glRotatef(mCamOrientation.y, 1.0f, 0.0f, 0.0f);
		//gl.glRotatef(mCamOrientation.x, 0.0f, 0.0f, 1.0f);
		gl.glTranslatef(mPosition.x, mPosition.y, mPosition.z);
		
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
		
		for(int x = 0; x < mNumberOfLines; x++)
		{
			gl.glPushMatrix();
				gl.glTranslatef(0, mGroundDistance*(x+1), 0);
				gl.glDrawArrays(GL10.GL_LINES, 0, 2);
			gl.glPopMatrix();
			
			gl.glPushMatrix();
				gl.glTranslatef(0, -mGroundDistance*(x+1), 0);
				gl.glDrawArrays(GL10.GL_LINES, 0, 2);
			gl.glPopMatrix();
		}
		
		gl.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
		
		for(int x = 0; x < mNumberOfLines; x++)
		{
			gl.glPushMatrix();
				gl.glTranslatef(0, mGroundDistance*(x+1), 0);
				gl.glDrawArrays(GL10.GL_LINES, 0, 2);
			gl.glPopMatrix();
			
			gl.glPushMatrix();
				gl.glTranslatef(0, -mGroundDistance*(x+1), 0);
				gl.glDrawArrays(GL10.GL_LINES, 0, 2);
			gl.glPopMatrix();
		}
		
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		
		gl.glPopMatrix();
	}

}
