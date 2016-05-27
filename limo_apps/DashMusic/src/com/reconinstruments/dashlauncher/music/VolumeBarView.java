package com.reconinstruments.dashlauncher.music;

import com.reconinstruments.dashmusic.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class VolumeBarView extends View{

	Paint paint = new Paint();
	
	private int widthSize = 170;
	private int heightSize = 27;
	String TAG = "VolumeBarView";
	Context mContext ;
	
	public VolumeBarView(Context context) {
		super(context);
		mContext = context;
	}
	public VolumeBarView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}
	public VolumeBarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}
	
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		int volume = MusicHelper.getVolumeInt();
		Log.d(TAG,"current volume to be updated with: "+volume);
		
		paint.setStyle(Style.FILL);
		paint.setColor(getResources().getColor(R.color.reconGrayVolContainer));
		canvas.drawRect(0, 0, widthSize, heightSize, paint);
		paint.setColor(Color.WHITE);
		float tempWidth = volume*widthSize/10;
		Log.d(TAG,"width white vol: "+tempWidth);
		canvas.drawRect(0, 0, tempWidth, heightSize, paint);
		
		
		
		if(tempWidth!=0){
			 Drawable mDrawable = mContext.getResources().getDrawable(R.drawable.vol_level);
			 Bitmap mBitmap=((BitmapDrawable)mDrawable).getBitmap();
			 canvas.drawBitmap(mBitmap, 3, 5 , paint);
		}else{
			Drawable mDrawable = mContext.getResources().getDrawable(R.drawable.vol_level_zero);
			 Bitmap mBitmap=((BitmapDrawable)mDrawable).getBitmap();
			 canvas.drawBitmap(mBitmap, 3, 5 , paint);
		}
	}
	
	
}
