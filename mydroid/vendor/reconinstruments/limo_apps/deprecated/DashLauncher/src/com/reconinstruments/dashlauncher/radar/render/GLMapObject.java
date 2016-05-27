package com.reconinstruments.dashlauncher.radar.render;

import com.reconinstruments.dashlauncher.radar.prim.Vector3;

public abstract class GLMapObject extends GLObject
{
	public GLMapObject() {
		mCamOffset		= new Vector3();
		mCamOrientation	= new Vector3();
		mUserOffset		= new Vector3();		
	}
		
	public void setDrawParams(Vector3 camOrientation, Vector3 camOffset, Vector3 userOffset)
	{
		mCamOrientation.set(camOrientation);
		mCamOffset.set(camOffset);
		mUserOffset.set(userOffset);
	}
}
