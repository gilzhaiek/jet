package com.reconinstruments.dashcompass;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

public class CompassBuddyInfo {
	private static String TAG = "CompassBuddyInfo";
	private static int BUDDY_TOP_MARGIN = 17;
	int					mID;
	String 				mName;
	double	 			mLatitude;	// in world GPS coords
	double				mLongitude;
	long 				mLocationTimestamp = 0;
	double 				mDistFromUser;
	double				mAngleFromNorth;  // in degrees
	double				mRelAngleBuddyToUserInDegrees;  // in degrees
	long 				mLastUpdateTime = 0;
	boolean				mOnline;	// have received response within 1 minute, offline is when last response was > 1 minute
	ImageView			mNormalBuddyImageView = null;
	ImageView			mHasFocusBuddyImageView = null;
	ImageView			mNormalFadingBuddyImageView = null;
	ImageView			mHasFocusFadingBuddyImageView = null;
	TextView			mBuddyName = null;
	int					mIconWidth = 0;
	int					mIconHeight = 0;
	int					mDrawingOffset = -100;
	
	public CompassBuddyInfo(Context context, int id, String name, double latitude, double longitude, long locationTime, long receivedTime) {
		mID = id;
		mName = name;
		mLatitude = latitude;
		mLongitude = longitude;
		mLocationTimestamp = locationTime;
		mLastUpdateTime = receivedTime;
		mDistFromUser = 0.0;
		mAngleFromNorth = 0.0;
		mRelAngleBuddyToUserInDegrees = 0.0;

		mNormalBuddyImageView = new ImageView(context); 
		mNormalBuddyImageView.setImageResource(R.drawable.buddy_active);
		Bitmap bitmap = ((BitmapDrawable)mNormalBuddyImageView.getDrawable()).getBitmap();
		mIconWidth = bitmap.getWidth();
		mIconHeight = bitmap.getHeight();
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, mIconHeight);
		lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		lp.setMargins(0, BUDDY_TOP_MARGIN, 0, 0);
		mNormalBuddyImageView.setLayoutParams(lp);
		Matrix matrix = new Matrix();
		matrix.reset();
		matrix.postTranslate(-100, 0);
		mNormalBuddyImageView.setScaleType(ScaleType.MATRIX);
		mNormalBuddyImageView.setImageMatrix(matrix);

		mHasFocusBuddyImageView = new ImageView(context); 
		mHasFocusBuddyImageView.setImageResource(R.drawable.buddy_active_yellow);
		Bitmap bitmap2 = ((BitmapDrawable)mHasFocusBuddyImageView.getDrawable()).getBitmap();
		int iconWidth = bitmap2.getWidth();
		int iconHeight = bitmap2.getHeight();
		lp = new LayoutParams(LayoutParams.MATCH_PARENT, iconHeight);
		lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		lp.setMargins(0, BUDDY_TOP_MARGIN, 0, 0);
		mHasFocusBuddyImageView.setLayoutParams(lp);
		matrix = new Matrix();
		matrix.reset();
		matrix.postTranslate(-100, 0);
		mHasFocusBuddyImageView.setScaleType(ScaleType.MATRIX);
		mHasFocusBuddyImageView.setImageMatrix(matrix);

		mNormalFadingBuddyImageView = new ImageView(context); 
		mNormalFadingBuddyImageView.setImageResource(R.drawable.buddy_active_offline);
		Bitmap bitmap3 = ((BitmapDrawable)mNormalFadingBuddyImageView.getDrawable()).getBitmap();
		iconWidth = bitmap3.getWidth();
		iconHeight = bitmap3.getHeight();
		lp = new LayoutParams(LayoutParams.MATCH_PARENT, iconHeight);
		lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		lp.setMargins(0, BUDDY_TOP_MARGIN, 0, 0);
		mNormalFadingBuddyImageView.setLayoutParams(lp);
		matrix = new Matrix();
		matrix.reset();
		matrix.postTranslate(-100, 0);
		mNormalFadingBuddyImageView.setScaleType(ScaleType.MATRIX);
		mNormalFadingBuddyImageView.setImageMatrix(matrix);

		mHasFocusFadingBuddyImageView = new ImageView(context); 
		mHasFocusFadingBuddyImageView.setImageResource(R.drawable.buddy_active_offline);
		Bitmap bitmap4 = ((BitmapDrawable)mHasFocusFadingBuddyImageView.getDrawable()).getBitmap();
		iconWidth = bitmap4.getWidth();
		iconHeight = bitmap4.getHeight();
		lp = new LayoutParams(LayoutParams.MATCH_PARENT, iconHeight);
		lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		lp.setMargins(0, BUDDY_TOP_MARGIN, 0, 0);
		mHasFocusFadingBuddyImageView.setLayoutParams(lp);
		matrix = new Matrix();
		matrix.reset();
		matrix.postTranslate(-100, 0);
		mHasFocusFadingBuddyImageView.setScaleType(ScaleType.MATRIX);
		mHasFocusFadingBuddyImageView.setImageMatrix(matrix);

		mBuddyName = new TextView(context);
		mBuddyName.setText(name);
		mBuddyName.setTextSize(14);
		mBuddyName.setTextColor(Color.BLUE);
//		mBuddyName.setAlpha(196);
		
	}

	public void AddBuddyToView(RelativeLayout layout)
	{
		layout.addView(mNormalBuddyImageView);
		layout.addView(mHasFocusBuddyImageView);
		layout.addView(mNormalFadingBuddyImageView);
		layout.addView(mHasFocusFadingBuddyImageView);
//		layout.addView(mBuddyName);
	}

	public void RemoveBuddyFromView(RelativeLayout layout)
	{
		layout.removeView(mNormalBuddyImageView);
		layout.removeView(mHasFocusBuddyImageView);
		layout.removeView(mNormalFadingBuddyImageView);
		layout.removeView(mHasFocusFadingBuddyImageView);
//		layout.addView(mBuddyName);
	}

}
