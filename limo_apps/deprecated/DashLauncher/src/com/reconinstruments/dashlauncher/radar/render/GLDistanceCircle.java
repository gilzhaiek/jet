package com.reconinstruments.dashlauncher.radar.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import com.reconinstruments.dashlauncher.radar.prim.Vector3;

import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;

public class GLDistanceCircle extends GLMapObject
{
	private final static String TAG = "GLDistanceCircle";
	
	public 	Vector3 mPosition			= null;
	public	float 	mRadius				= 0.0f;
	private int 	mNumberOfVertices	= 0;
	
	private FloatBuffer mVertexBuffer 	= null;
	
	// FIXME : Remove when done with
	// -- TEMP TEST VALUES -- //
	private float mRatioInc		= 0.1f;
	private float mRadiusRatio 	= 0.0f;
	
	public GLDistanceCircle(Vector3 pos, float radius, int numVertices)
	{
		mPosition			= pos;
		mRadius				= radius;
		mNumberOfVertices	= numVertices;
				
		generateCircleVertices();
	}
	
	protected void generateCircleVertices()
	{
		float[] vertices	= new float[mNumberOfVertices * 3];
		float inc			= (2*(float)Math.PI)/mNumberOfVertices;
		int index			= 0;
		for(float a = 0.0f; index < vertices.length / 3; a += inc)
		{
			vertices[(index * 3) + 0] = (float)Math.cos(a);
			vertices[(index * 3) + 1] = (float)Math.sin(a);
			vertices[(index * 3) + 2] = 0.0f;
			
			//Log.i(TAG, "index[" + index + "]: vertex(" + vertices[(index * 3) + 0] + ", " + vertices[(index * 3) + 1] + ", " + vertices[(index * 3) + 2] + ")");
			
			index++;
		}
		
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		mVertexBuffer = byteBuffer.asFloatBuffer();
		mVertexBuffer.put(vertices);
		mVertexBuffer.position(0);
	}

	public void drawGL(GL10 gl)
	{
		mRadiusRatio += mRatioInc;
		if(mRadiusRatio >= 1.0f)
		{ 
			mRadiusRatio = 1.0f;
			mRatioInc	 = -mRatioInc;
		}
		else if(mRadiusRatio <= 0.0f)
		{ 
			mRadiusRatio = 0.0f;
			mRatioInc	 = -mRatioInc;
		}	
		
		gl.glLineWidth(3.5f);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);	
		
		gl.glPushMatrix();
		gl.glTranslatef(mCamOffset.x, mCamOffset.y, mCamOffset.z);
		gl.glRotatef(mCamOrientation.y, 1.0f, 0.0f, 0.0f);
		gl.glTranslatef(mPosition.x, mPosition.y, mPosition.z);
		gl.glScalef(mRadius, mRadius, 0);
		
		//gl.glColor4f(0.0f, 1.0f, 1.0f, 1.0f);
		//gl.glColor4f(mRadiusRatio, 1.0f - mRadiusRatio, 1.0f, 1.0f);
		gl.glColor4f(1.0f, 0.0f, 0.0f, mRadiusRatio);
		gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, mNumberOfVertices);
		
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
	
	public void SetRadius(float radius){ Log.v(TAG,"mRadius="+mRadius);mRadius = radius; }
	public void SetPosition(float x, float y, float z){ mPosition.set(x, y, z); }
	
	public Vector3 getPosition(){ return mPosition; }
	public float   getRadius()  { return mRadius; }
}