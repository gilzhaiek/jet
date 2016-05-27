package com.reconinstruments.dashlauncher.radar.render;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Rect;
import android.util.Log;

public class GLUserArrow extends GLSurfaceTexture {
	private static final String TAG = "GLUserArrow";
	private float 		mScaleF = 1.0f;
    
	// Constructor - Set up the buffers
	public GLUserArrow()
	{
		super();
		SetBoundingBox(new Rect(-1,1,1,-1));
	}

	@Override
	public void drawGL(GL10 gl)
	{
		gl.glDisable(GL10.GL_TEXTURE_2D);		

		gl.glPushMatrix();
			gl.glTranslatef(mCamOffset.x, mCamOffset.y, mCamOffset.z);
			gl.glRotatef(mCamOrientation.y, 1.0f, 0.0f, 0.0f);
			gl.glRotatef(mCamOrientation.x, 0.0f, 0.0f, 1.0f);
			gl.glScalef(mScaleF, mScaleF, mScaleF);
			super.drawGL(gl);
		
		gl.glPopMatrix();
	}

	public void SetScaleF(float scaleF)
	{
		// Log.v(TAG,"scaleF="+scaleF);
		mScaleF = scaleF;
	}
}