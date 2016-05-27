package com.reconinstruments.dashlauncher.radar.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

import com.reconinstruments.dashlauncher.radar.maps.objects.Trail;
import com.reconinstruments.dashlauncher.radar.prim.PointD;

public class GLTrail {
	protected static final String TAG = "GLTrail";
	
	private FloatBuffer	mVertexBuffer;  // Buffer for vertex-array
	private int mTrailLength = 0;
	private int mType = 0;
	
	// Constructor - Set up the buffers
	public GLTrail(ArrayList<PointD> pathPoints, int type) {
		mType = type;
		mTrailLength = pathPoints.size();
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(pathPoints.size() * 8);
		byteBuffer.order(ByteOrder.nativeOrder());
		mVertexBuffer = byteBuffer.asFloatBuffer();
		for(int i =0; i < pathPoints.size(); i++) {
			//Log.v(TAG,"X="+pathPoints.get(i).x+" Y="+pathPoints.get(i).y);
			mVertexBuffer.put(i*2, (float)pathPoints.get(i).x);
			mVertexBuffer.put(i*2+1, -1.0f*(float)pathPoints.get(i).y);
		}
		mVertexBuffer.position(0);      // Setup vertex-array buffer. Vertices in float. An float has 4 bytes
	}
   
	public void draw(GL10 gl, float width) {
		gl.glPushMatrix();
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

		switch (mType) {
		case Trail.GREEN_TRUNK:
		case Trail.GREEN_TRAIL: gl.glColor4f(0.0f, 1.0f, 0.0f, 1.0f); break;
		case Trail.BLUE_TRUNK:
		case Trail.BLUE_TRAIL: gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f); break;		
		case Trail.RED_TRUNK:
		case Trail.RED_TRAIL: gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f); break;		
		case Trail.BLACK_TRUNK:
		case Trail.DBLBLACK_TRUNK:
		case Trail.BLACK_TRAIL:
		case Trail.DBLBLACK_TRAIL: gl.glColor4f(0.0f, 0.0f, 0.0f, 1.0f); break;		
		case Trail.SKI_LIFT: gl.glColor4f(0.8f, 0.0f, 0.0f, 1.0f); break;
		case Trail.CHWY_RESID_TRAIL: gl.glColor4f(0.7f, 0.7f, 0.7f, 1.0f); break;
		case Trail.WALKWAY_TRAIL: gl.glColor4f(0.5f, 0.5f, 0.5f, 1.0f); break;	
		}
		
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, mTrailLength);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glPopMatrix();
		//gl.glLineWidth(width);
	}
}
