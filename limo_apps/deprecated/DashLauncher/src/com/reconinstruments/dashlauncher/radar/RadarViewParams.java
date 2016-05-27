package com.reconinstruments.dashlauncher.radar;

import com.reconinstruments.dashlauncher.radar.prim.Vector3;

public class RadarViewParams
{
	protected float	mXOffset		= 0;
	protected float	mYOffset		= Util.getZoomYOffset(Util.mZoomMin);
	protected float	mZOffset		= Util.mZoomMin;
	protected float mYaw			= 0.0f;	
	protected float mPitch			= 0.0f;
	protected float	mUserYaw		= 0.0f;
	protected float	mYawOffset		= 0.0f; 
	protected float	mCutoffAngle	= -1.0f;
	
	protected float	mRoll			= 0.0f; 
	
	public Vector3	mCamOffset		= new Vector3();
	public Vector3	mUserOffset		= new Vector3();
}
