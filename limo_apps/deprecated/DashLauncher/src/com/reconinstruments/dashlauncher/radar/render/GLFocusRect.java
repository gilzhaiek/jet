package com.reconinstruments.dashlauncher.radar.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.PointF;
import android.util.Log;

import com.reconinstruments.dashlauncher.radar.RadarOOI;
import com.reconinstruments.dashlauncher.radar.Util;
import com.reconinstruments.dashlauncher.radar.prim.Vector3;

public class GLFocusRect extends GLMapObject {
private final static String TAG = "GLFocusRect";
	
	private Vector3 mPosition			= null;
	private PointF mFocusOffset		= new PointF();
	
	private FloatBuffer mVertexBuffer 	= null;
	
	
	public GLFocusRect(Vector3 pos)
	{
		mPosition = pos;
		generateRectVertices();
	}
	
	protected void generateRectVertices()
	{
		float[] vertices	= new float[4 * 3];

		vertices[0] = -1.0f;
		vertices[1] = -1.0f;
		vertices[2] = 0.0f;

		vertices[3] = -1.0f;
		vertices[4] = 1.0f;
		vertices[5] = 0.0f;

		vertices[6] = 1.0f;
		vertices[7] = 1.0f;
		vertices[8] = 0.0f;

		vertices[9] = 1.0f;
		vertices[10] = -1.0f;
		vertices[11] = 0.0f;

		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		mVertexBuffer = byteBuffer.asFloatBuffer();
		mVertexBuffer.put(vertices);
		mVertexBuffer.position(0);
	}
	
	public void drawGL(GL10 gl)
	{
		gl.glLineWidth(3.5f);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);	
		
		gl.glPushMatrix();
		gl.glTranslatef(mCamOffset.x, mCamOffset.y, mCamOffset.z);
		gl.glRotatef(mCamOrientation.y, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(mCamOrientation.x, 0.0f, 0.0f, 1.0f);
		//gl.glTranslatef((float)Math.sin(mCamOrientation.x*Math.PI/180.0f)*mFocusOffset, (float)Math.cos(mCamOrientation.x*Math.PI/180.0f)*mFocusOffset, 0.0f);
		//Log.v(TAG,"mFocusOffset.x="+mFocusOffset.x+" mFocusOffset.y="+mFocusOffset.y);
		gl.glTranslatef(mFocusOffset.x, mFocusOffset.y, 0.0f);
		gl.glScalef(10, 10, 0);
		
		//gl.glColor4f(0.0f, 1.0f, 1.0f, 1.0f);
		//gl.glColor4f(mRadiusRatio, 1.0f - mRadiusRatio, 1.0f, 1.0f);
		gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
		gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, 4);
		
		/*
		gl.glPushMatrix();
			gl.glScalef(mRadiusRatio, mRadiusRatio, 0);
			//gl.glColor4f(1.0f - mRadiusRatio, mRadiusRatio, mRadiusRatio, 1.0f);
			gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, mNumberOfVertices);
		gl.glPopMatrix();
		
		gl.glPushMatrix();
			gl.glRotatef(360.0f * mRadiusRatio, 1.0f, 0.0f, 0.0f);
			gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, mNumberOfVertices);
		gl.glPopMatrix();
		*/
		
		gl.glPopMatrix();
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);	
	}
	
	public void setDrawParams(Vector3 camOrientation, Vector3 camOffset, Vector3 userOffset, PointF focusOffset) {
		super.setDrawParams(camOrientation, camOffset, userOffset);
		mFocusOffset.set(focusOffset);
	}
}
