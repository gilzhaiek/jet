package com.reconinstruments.dashlauncher.radar.render;

import javax.microedition.khronos.opengles.GL10;

import com.reconinstruments.dashlauncher.radar.prim.Vector3;

public abstract class GLObject
{
	protected boolean mIsHidden = false;
	protected float   mAlpha    = 1.0f;
	
	protected Vector3 mCamOffset		= null;
	protected Vector3 mCamOrientation	= null;
	protected Vector3 mUserOffset		= null;
	
	public void draw(GL10 gl)
	{
		if(!mIsHidden && mAlpha != 0.0f)
		{
			drawGL(gl);
		}
	}
	
	protected abstract void drawGL(GL10 gl);
	
	public void SetHidden(boolean isHidden){ mIsHidden = isHidden; }
	public void SetAlpha(float alpha){ mAlpha = alpha; }

	public float   GetAlpha(){ return mAlpha; }
	public boolean IsHidden(){ return mIsHidden; }
}
