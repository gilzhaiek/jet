package com.reconinstruments.camera.ui;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.reconinstruments.camera.R;

/**
 * This is the side menu for the Recon Camera which turns off and turns on the selected state
 * whenever the module changes
 * 
 * @author patrickcho
 *
 */
public class CameraSideMenu extends LinearLayout {
	private static final String TAG = CameraSideMenu.class.getSimpleName();
	private Context mContext;
	private int offset = 0;
	private int indexLast = 0, indexCurrent = 0;
	private CameraSideMenu mThis;
	private float originalPosition = 0;

	public CameraSideMenu(Context context) {
		super(context);
		mContext = context;
		init();
	}
	
	public CameraSideMenu(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
	}
	
	public CameraSideMenu(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		init();
	}
	
	private void init() {
		Log.d(TAG, "init() called");
		mThis = this;
		ImageView view;
		
		view = new ImageView(mContext);
		view.setImageResource(R.drawable.photo_icon);
		view.setPadding(10, 5, 10, 5);
		this.addView(view);
		
		view = new ImageView(mContext);
		view.setImageResource(R.drawable.video_icon);
		view.setPadding(10, 5, 10, 5);
		this.addView(view);

		int widthMeasureSpec = MeasureSpec.makeMeasureSpec(LinearLayout.LayoutParams.WRAP_CONTENT, MeasureSpec.EXACTLY);
		int heightMeasureSpec = MeasureSpec.makeMeasureSpec(LinearLayout.LayoutParams.MATCH_PARENT, MeasureSpec.EXACTLY);
		
		this.measure(widthMeasureSpec, heightMeasureSpec);
		
		offset = getMeasuredWidth();
		Log.d(TAG, "Measured width at init is: " + offset);
		
		originalPosition = mThis.getX();
		
		new Handler().post(new Runnable(){

			@Override
			public void run() {
				if(indexCurrent == ModuleSwitcher.PHOTO_MODULE_INDEX){
					offset = (int)(offset/2f);
				}
				else if(indexCurrent == ModuleSwitcher.VIDEO_MODULE_INDEX){
					offset = (int)(-offset/2f);
				}
				mThis.animate().translationX(originalPosition + offset);
			}
			
		});
		
//		offset+=24;
//		offsetLeftAndRight(offset);
	}
	
	public void setSelectedMenu(int index) {
		Log.d(TAG, "setSelected() called");
		
		for (int i = 0; i < this.getChildCount() ; i++) {
			if(getChildAt(i).isSelected()){
				indexLast = i;
			}
			this.getChildAt(i).setSelected(false);
		}

		offset = (int)(getMeasuredWidth() / 2f);
		Log.d(TAG, "getting measured width to be: " + offset);
		this.animate().translationX(originalPosition + ((indexLast - index) * offset)/2);
	
		this.getChildAt(index).setSelected(true);
		indexCurrent = index;
	}
	
	public int getLayoutWidthOfSelected(){
		return getChildAt(indexCurrent).getWidth();
	}
	
	public int getLayoutHeightOfSelected(){
		return getChildAt(indexCurrent).getHeight();
	}
}
