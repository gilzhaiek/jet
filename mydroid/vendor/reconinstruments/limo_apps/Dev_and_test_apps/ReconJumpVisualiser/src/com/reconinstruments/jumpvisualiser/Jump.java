package com.reconinstruments.jumpvisualiser;

import android.os.Bundle;

public class Jump
{
	public final static int INVALID = -10000;

	public int 	 mNumber 	= INVALID;
	public long  mDate 		= INVALID;
	public int 	 mAir 		= INVALID;
	public float mDistance 	= INVALID;
	public float mDrop 		= INVALID;
	public float mHeight 	= INVALID;
	
	public float mSpeedX 	= INVALID; // In meters per second 

	public Jump(){	}
	
	public Jump(int air, float dist, float height, float drop)
	{
		mAir = air;
		mDistance = dist;
		mHeight = height;
		mDrop = drop;		
	}
	
	public Jump(Bundle b)
	{
		// Constructor from Bundle
		setJump(b);
	}

	public void setJump(Bundle b)
	{
		mNumber = b.getInt("Number");
		mDate = b.getLong("Date");
		mAir = b.getInt("Air");
		mDistance = b.getFloat("Distance");
		mHeight = b.getFloat("Height");
		mDrop = b.getFloat("Drop");		
		
		mSpeedX = mDistance/mAir;
	}
}
