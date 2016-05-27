package com.contour.connect.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.ImageButton;

public class RecordButton extends ImageButton implements OnFocusChangeListener{
    static final String TAG = "RecordButton";
    
    public interface OnRecordButtonClicked {
        public void onRecordButtonClicked(boolean recording);
    }
    int mLayoutMargin;
    int mLayoutSize;
    OnRecordButtonClicked mOnRecordButtonClicked; 
    
    public RecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        
//        mRecordDrawableOn = context.getResources().getDrawable(R.drawable.btn_record_pressed);
//        mRecordDrawableActive = context.getResources().getDrawable(R.drawable.btn_record_normal);
//        Drawable bgDraw = context.getResources().getDrawable(R.drawable.round_button_background);
//        CLog.out(TAG,this.getWidth(),this.getHeight());
        
        int screenWidth = this.getResources().getDisplayMetrics().widthPixels;
        int buttonRadius = (int) (screenWidth * .125);
        int shapeRadius = (int) (screenWidth * .1333);
        mLayoutMargin = (int)(screenWidth * .033);
        mLayoutSize = shapeRadius;
    }
    
    public void setOnRecordButtonClicked(OnRecordButtonClicked onRecordButtonClicked) {
        this.mOnRecordButtonClicked = onRecordButtonClicked;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // CLog.out(TAG,"onTouchEvent",event,event.getActionMasked());
//        super.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if(!isEnabled()) return true;
            this.setRecordDisabled();
          
            mOnRecordButtonClicked.onRecordButtonClicked(this.isShowingAsRecording() == false);

//            invalidate();
        }
        return true;
    }
    
    //called from MainActivity to replicate onTouchEvent
    public boolean onClickEvent() {
        // CLog.out(TAG,"onTouchEvent",event,event.getActionMasked());
//        super.onTouchEvent(event);

            if(!isEnabled()) return true;
            this.setRecordDisabled();
          
            mOnRecordButtonClicked.onRecordButtonClicked(this.isShowingAsRecording() == false);
            Log.i("RecordButtonDEBUG","onClickEvent");
//            invalidate();
        
        return true;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }
    
    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text
            result = mLayoutSize + mLayoutMargin;
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }

        return result;
    }

    /**
     * Determines the height of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = mLayoutSize + mLayoutMargin;
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    public void setRecordingOn() {
//        CLog.out(TAG,"setRecordingOn");

        if (this.isWaitingForCamera() || this.isShowingAsRecording() == false) {
            this.setWaitingForCamera(false);
            this.setShowAsRecording(true);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                TransitionDrawable td = ((TransitionDrawable) ((StateListDrawable) getDrawable()).getCurrent());
//                td.setCrossFadeEnabled(true);
//                td.startTransition(200);
//            }
        }
    }

    public void setRecordingOff() {
//        CLog.out(TAG,"setRecordingOff");

        if (isWaitingForCamera() || this.isShowingAsRecording() == true) {
            this.setWaitingForCamera(false);
            this.setShowAsRecording(false);
            setSelected(false);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                TransitionDrawable td = ((TransitionDrawable) ((StateListDrawable) getDrawable()).getCurrent());
//                td.setCrossFadeEnabled(true);
//                td.resetTransition();
//                td.startTransition(200);
//                td.reverseTransition(200);
//            }
        }
    }
    
    public void setRecordDisabled()
    {
//        CLog.out(TAG,"setRecordDisabled");
        if(this.isWaitingForCamera() == false) {
            setWaitingForCamera(true);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                TransitionDrawable td = ((TransitionDrawable) ((StateListDrawable) getDrawable()).getCurrent());
//                td.setCrossFadeEnabled(true);
//                td.startTransition(200);
//            }
        }
    }
    
    public void setRecordEnabled()
    {
//        CLog.out(TAG,"setRecordEnabled");
        if(this.isWaitingForCamera() == true) {
            setWaitingForCamera(false);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//
//                TransitionDrawable td = ((TransitionDrawable) ((StateListDrawable) getDrawable()).getCurrent());
//                td.setCrossFadeEnabled(true);
//                td.startTransition(200);
//                td.reverseTransition(200);
//            }
        }
    }
    
    public boolean isWaitingForCamera() {
        return this.isEnabled() == false;
    }
    
    public boolean isShowingAsRecording() {
        return this.isSelected() == true;
    }
    
    public void setWaitingForCamera(boolean wait) {
//        CLog.out(TAG,"setWaitingForCamera",wait);
        this.setEnabled(!wait);
    }
    public void setShowAsRecording(boolean show) {
        this.setSelected(show);
    }

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		Log.i("RECORDBUTTON","ONFOCUS!");
		
	}
}
