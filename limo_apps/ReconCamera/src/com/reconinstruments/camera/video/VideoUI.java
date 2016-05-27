/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reconinstruments.camera.video;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout.LayoutParams;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.reconinstruments.camera.OnPreviewChangedListener;
import com.reconinstruments.camera.R;
import com.reconinstruments.camera.app.CameraActivity;
import com.reconinstruments.camera.preference.PreferenceGroup;
import com.reconinstruments.camera.ui.AbstractSettingPopup;
import com.reconinstruments.camera.ui.AnimationManager;
import com.reconinstruments.camera.ui.CameraRootView;
import com.reconinstruments.camera.ui.RotateLayout;

public class VideoUI implements CameraRootView.MyDisplayListener, SurfaceHolder.Callback {
    private static final String TAG = "CAM_VideoUI";
    private static final int UPDATE_TRANSFORM_MATRIX = 1;
    // module fields
    private CameraActivity mActivity;
    private View mRootView;
    // An review image having same size as preview. It is displayed when
    // recording is stopped in capture intent.
    private ImageView mSelectIcon;
    private TextView mClickText;
    private TextView mClickMessage;
    private TextView mRecordingTimeView;
    private LinearLayout mLabelsLinearLayout, mInstructionTextBlock;
    private View mTimeLapseLabel;
    private RelativeLayout mPreviewTimeoutCover;
    private RelativeLayout mCameraControls;
    private SettingsPopup mPopup;
    private RotateLayout mRecordingTimeRect;
    private boolean mRecordingStarted = false;
    private VideoController mController;
    private View mPreviewThumb;
    private View mFlashOverlay;

    private SurfaceView mSurfaceView = null;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private float mSurfaceViewUncroppedWidth;
    private float mSurfaceViewUncroppedHeight;
    private float mAspectRatio = 16f / 9f;
    private final AnimationManager mAnimationManager;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_TRANSFORM_MATRIX:
                    setTransformMatrix(mPreviewWidth, mPreviewHeight);
                    break;
                default:
                    break;
            }
        }
    };

    private OnPreviewChangedListener mPreviewChangedListener;

    private OnLayoutChangeListener mLayoutListener = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            int width = right - left;
            int height = bottom - top;
            // Full-screen screennail
            int w = width;
            int h = height;
            
            if (mPreviewWidth != width || mPreviewHeight != height) {
                mPreviewWidth = width;
                mPreviewHeight = height;
                onScreenSizeChanged(width, height, w, h);
            }
        }
    };
    
    private AnimationSet mMoveDownAndFade, mMoveDownAndStay;
    private AlphaAnimation delayedTextFadeOut, textFadeOut;
    private ForwardInterpolator mForwardInterpolator;
    private ReverseInterpolator mReverseInterpolator;

    private class SettingsPopup extends PopupWindow {
        public SettingsPopup(View popup) {
            super(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            setOutsideTouchable(true);
            setFocusable(true);
            popup.setVisibility(View.VISIBLE);
            setContentView(popup);
            showAtLocation(mRootView, Gravity.CENTER, 0, 0);
        }

        public void dismiss(boolean topLevelOnly) {
            super.dismiss();
            popupDismissed();
            showUI();
        }

        @Override
        public void dismiss() {
            // Called by Framework when touch outside the popup or hit back key
            dismiss(true);
        }
    }

    public VideoUI(CameraActivity activity, VideoController controller, View parent, boolean showInstructions) {
        mActivity = activity;
        mController = controller;
        mRootView = parent;

        LayoutInflater inflater = mActivity.getLayoutInflater(); 
        inflater.inflate(R.layout.video_module, (ViewGroup) mRootView, true);
        mPreviewTimeoutCover = (RelativeLayout) mRootView.findViewById(R.id.preview_timeout_cover);
        mPreviewTimeoutCover.setVisibility(View.GONE);

        setupAnimations();
        
        mSurfaceView = (SurfaceView) mRootView.findViewById(R.id.preview_content);
        mSurfaceView.addOnLayoutChangeListener(mLayoutListener);
        mSurfaceView.getHolder().addCallback(this);

        mFlashOverlay = mRootView.findViewById(R.id.flash_overlay);
        initializeMiscControls(showInstructions);
        initializeControlByIntent();
        initializeOverlay();
        mAnimationManager = new AnimationManager();
    }

    public void onPause(){
        mPreviewChangedListener.onPreviewPaused();
    }

    public void onResume(){
        mPreviewChangedListener.onPreviewResumed();
    }

    private void initializeControlByIntent() {

        mCameraControls = (RelativeLayout) mRootView.findViewById(R.id.camera_controls);
        if (mController.isVideoCaptureIntent()) {
        }
    }

    public void setPreviewSize(int width, int height) {
        if (width == 0 || height == 0) {
            Log.w(TAG, "Preview size should not be 0.");
            return;
        }
        if (width > height) {
            mAspectRatio = (float) width / height;
        } else {
            mAspectRatio = (float) height / width;
        }
    }

    public int getPreviewWidth() {
        return mPreviewWidth;
    }

    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    public void onScreenSizeChanged(int width, int height, int previewWidth, int previewHeight) {
        setTransformMatrix(width, height);
    }

    private void setTransformMatrix(int width, int height) {
        float scaledTextureWidth, scaledTextureHeight;
        if (width > height) {
            scaledTextureWidth = Math.max(width,
                    (int) (height * mAspectRatio));
            scaledTextureHeight = Math.max(height,
                    (int)(width / mAspectRatio));
        } else {
            scaledTextureWidth = Math.max(width,
                    (int) (height / mAspectRatio));
            scaledTextureHeight = Math.max(height,
                    (int) (width * mAspectRatio));
        }

        if (mSurfaceViewUncroppedWidth != scaledTextureWidth ||
                mSurfaceViewUncroppedHeight != scaledTextureHeight) {
            mSurfaceViewUncroppedWidth = scaledTextureWidth;
            mSurfaceViewUncroppedHeight = scaledTextureHeight;
        }

        if (mSurfaceView != null && mSurfaceView.getVisibility() == View.VISIBLE) {
            LayoutParams lp = (LayoutParams) mSurfaceView.getLayoutParams();
            lp.width = (int) mSurfaceViewUncroppedWidth;
            lp.height = (int) mSurfaceViewUncroppedHeight;
            lp.gravity = Gravity.CENTER;
            mSurfaceView.requestLayout();
        }
    }

    /**
     * Starts a flash animation
     */
    public void animateFlash() {
        mAnimationManager.startFlashAnimation(mFlashOverlay);
    }

    /**
     * Starts a capture animation
     */
    public void animateCapture() {
        Bitmap bitmap = null;
        if(mSurfaceView != null && mSurfaceView.getHolder() != null) {
            // SurfaceView doesn't have an easy way to get a bitmap
            // TODO capture the bitmap somehow
        }
        animateCapture(bitmap);
    }

    /**
     * Starts a capture animation
     * @param bitmap the captured image that we shrink and slide in the animation
     */
    public void animateCapture(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "No valid bitmap for capture animation.");
            return;
        }
        ((ImageView) mPreviewThumb).setImageBitmap(bitmap);
        mAnimationManager.startCaptureAnimation(mPreviewThumb);
    }

    /**
     * Cancels on-going animations
     */
    public void cancelAnimations() {
        mAnimationManager.cancelAnimations();
    }

    public void hideUI() {
        mCameraControls.setVisibility(View.INVISIBLE); // FIX this since there is no additional camera control other than Instruction message
    }

    public void showUI() {
        mCameraControls.setVisibility(View.VISIBLE);
    }

    public boolean arePreviewControlsVisible() {
        return (mCameraControls.getVisibility() == View.VISIBLE);
    }

    public boolean collapseCameraControls() {
        boolean ret = false;
        if (mPopup != null) {
            dismissPopup(false);
            ret = true;
        }
        return ret;
    }

    public boolean removeTopLevelPopup() {
        if (mPopup != null) {
            dismissPopup(true);
            return true;
        }
        return false;
    }

    public void initDisplayChangeListener() {
        ((CameraRootView) mRootView).setDisplayChangeListener(this);
    }

    public void removeDisplayChangeListener() {
        ((CameraRootView) mRootView).removeDisplayChangeListener();
    }

    public void setOrientationIndicator(int orientation, boolean animation) {
        // We change the orientation of the linearlayout only for phone UI
        // because when in portrait the width is not enough.
        if (mLabelsLinearLayout != null) {
            if (((orientation / 90) & 1) == 0) {
                mLabelsLinearLayout.setOrientation(LinearLayout.VERTICAL);
            } else {
                mLabelsLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
            }
        }
        mRecordingTimeRect.setOrientation(0, animation);
    }

    public SurfaceHolder getSurfaceHolder() {
        return mSurfaceView.getHolder();
    }

    public void hideSurfaceView() {
        mSurfaceView.setVisibility(View.GONE);
        setTransformMatrix(mPreviewWidth, mPreviewHeight);
    }

    public void showSurfaceView() {
        mSurfaceView.setVisibility(View.VISIBLE);
        setTransformMatrix(mPreviewWidth, mPreviewHeight);
    }

    private void initializeOverlay() {

        mPreviewThumb = mRootView.findViewById(R.id.preview_thumb);
        mPreviewThumb.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do not allow navigation to filmstrip during video recording
                if (!mRecordingStarted) {
                    mActivity.gotoGallery();
                }
            }
        });
    }

    private void initializeMiscControls(boolean showInstructions) {
        mSelectIcon = (ImageView) mRootView.findViewById(R.id.select_btn_video);
        mSelectIcon.setImageResource(R.drawable.select_btn);
        mInstructionTextBlock = (LinearLayout) mRootView.findViewById(R.id.instruction_text_block_video);
        mClickText = (TextView) mRootView.findViewById(R.id.click_text_video);
        mClickText.setText(R.string.click_string);
        mClickMessage = (TextView) mRootView.findViewById(R.id.click_text_message_video);
        mClickMessage.setText(R.string.video_instruction_record);
        mRecordingTimeView = (TextView) mRootView.findViewById(R.id.recording_time);
        mRecordingTimeRect = (RotateLayout) mRootView.findViewById(R.id.recording_time_rect);
        mTimeLapseLabel = mRootView.findViewById(R.id.time_lapse_label);
        // The R.id.labels can only be found in phone layout.
        // That is, mLabelsLinearLayout should be null in tablet layout.
        mLabelsLinearLayout = (LinearLayout) mRootView.findViewById(R.id.labels);
        if(!showInstructions) {
            mInstructionTextBlock.clearAnimation();
            mInstructionTextBlock.setVisibility(View.INVISIBLE);
        }
    }

    private void setupAnimations(){

        mForwardInterpolator = new ForwardInterpolator();
        mReverseInterpolator = new ReverseInterpolator();

        //fade-out animation for the "Click A to record video" text
        delayedTextFadeOut = new AlphaAnimation(1.0f, 0.0f);
        delayedTextFadeOut.setStartOffset(2000);
        delayedTextFadeOut.setDuration(500);
        delayedTextFadeOut.setFillAfter(true);

        textFadeOut = new AlphaAnimation(1.0f, 0.0f);
        textFadeOut.setDuration(500);
        textFadeOut.setFillAfter(true);
        textFadeOut.setInterpolator(mForwardInterpolator);

        mMoveDownAndFade = new AnimationSet(true);
        Animation moveDown = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f);
        moveDown.setDuration(500);
        moveDown.setFillAfter(true);

        mMoveDownAndFade.addAnimation(delayedTextFadeOut);
        mMoveDownAndFade.addAnimation(moveDown);
        mMoveDownAndFade.setFillAfter(true);
            mMoveDownAndFade.setAnimationListener(
                new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        Log.i(TAG, "onAnimationStart");
                        mSelectIcon.setAlpha(1.0f);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        Log.i(TAG, "onAnimationEnd");
                        if(mMoveDownAndFade.getInterpolator() instanceof ReverseInterpolator) {
                            mSelectIcon.setAlpha(1f);
                        }
                        else {
                            mSelectIcon.setAlpha(0f);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
        mMoveDownAndFade.setInterpolator(mForwardInterpolator);

        mMoveDownAndStay = new AnimationSet(true);
        mMoveDownAndStay.addAnimation(moveDown);
        mMoveDownAndStay.setFillAfter(true);
        mMoveDownAndStay.setStartOffset(2000);
        mMoveDownAndStay.setInterpolator(mForwardInterpolator);
    }

    public void setAspectRatio(double ratio) {
      //  mPreviewFrameLayout.setAspectRatio(ratio);
    }

    public void showTimeLapseUI(boolean enable) {
        if (mTimeLapseLabel != null) {
            mTimeLapseLabel.setVisibility(enable ? View.VISIBLE : View.GONE);
        }
    }

    public void dismissPopup(boolean topLevelOnly) {
        // In review mode, we do not want to bring up the camera UI
        if (mController.isInReviewMode()) return;
        if (mPopup != null) {
            mPopup.dismiss(topLevelOnly);
        }
    }

    private void popupDismissed() {
        mPopup = null;
    }

    public void showPopup(AbstractSettingPopup popup) {
        hideUI();

        if (mPopup != null) {
            mPopup.dismiss(false);
        }
        mPopup = new SettingsPopup(popup);
    }

    public void showPreviewBorder(boolean enable) {
       // TODO: mPreviewFrameLayout.showBorder(enable);
    }

    public void showRecordingUI(boolean recording) {
        mRecordingStarted = recording;
        RelativeLayout.LayoutParams relParams = (RelativeLayout.LayoutParams) mInstructionTextBlock.getLayoutParams();
        RelativeLayout.LayoutParams timeViewParams = (RelativeLayout.LayoutParams) mRecordingTimeRect.getLayoutParams();
        if (recording) {

            relParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            relParams.bottomMargin = 16;
            timeViewParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
            mMoveDownAndFade.setInterpolator(mForwardInterpolator);
            mInstructionTextBlock.setLayoutParams(relParams);
            mInstructionTextBlock.startAnimation(mMoveDownAndFade);
            mRecordingTimeRect.setLayoutParams(timeViewParams);
            mRecordingTimeRect.startAnimation(mMoveDownAndStay);

            setInstructionTextVisibility(View.VISIBLE);
            setInstructionTextAlpha(1f);
            mClickMessage.setText(R.string.video_instruction_stop);

            mRecordingTimeView.setText("");
            mRecordingTimeView.setVisibility(View.VISIBLE);
            mActivity.mMainHandler.sendEmptyMessage(CameraActivity.HIDE_INDICATOR);
        } else {
            relParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
            relParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
            relParams.bottomMargin = 0;
            mMoveDownAndFade.setInterpolator(mReverseInterpolator);
            mInstructionTextBlock.setLayoutParams(relParams);
            mInstructionTextBlock.startAnimation(mMoveDownAndFade);
            mRecordingTimeRect.setLayoutParams(timeViewParams);

        	mMoveDownAndFade.reset();
            mMoveDownAndStay.reset();
            setInstructionTextVisibility(View.INVISIBLE);
            setInstructionTextAlpha(0f);

            mRecordingTimeView.setVisibility(View.INVISIBLE);
            mActivity.mMainHandler.sendEmptyMessage(CameraActivity.SHOW_INDICATOR);
        }
    }

    public void showReviewImage(Bitmap bitmap) {
    }

    public void onPreviewFocusChanged(boolean previewFocused) {
        if (previewFocused) {
            showUI();
        } else {
            hideUI();
        }
    }

    public void initializePopup(PreferenceGroup pref) {
//        mVideoMenu.initialize(pref);
    }
    
    public void setRecordingTime(String text) {
        mRecordingTimeView.setText(text);
        mSurfaceView.invalidate();
    }

    public void setRecordingTimeTextColor(int color) {
        mRecordingTimeView.setTextColor(color);
    }

    public boolean isVisible() {
        return mCameraControls.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onDisplayChanged() {
    }

    /**
     * Fades the "Press 0 to start recording" text over <code>fadeDuration</code>
     * after <code>startOffset</code>
     * @param fadeDuration the fade animation duration in milliseconds
     * @param startOffset an offset to the start of the animation in milliseconds
     */
    public void fadeInstructionText(long fadeDuration, long startOffset){
        if(null != mInstructionTextBlock){
            textFadeOut.setStartOffset(startOffset);
            textFadeOut.setDuration(fadeDuration);
    		mInstructionTextBlock.startAnimation(textFadeOut);
    	}
    }

    private void setInstructionTextVisibility(int visibility){
        if(null != mInstructionTextBlock) mInstructionTextBlock.setVisibility(visibility);
    }

    private void setInstructionTextAlpha(float alpha){
        if(null != mInstructionTextBlock) mInstructionTextBlock.setAlpha(alpha);
    }

    private void setInstructionText(String textToSet){
    	if(null != mClickText){
    		mClickText.setText(textToSet);
    		mClickText.setAlpha(1.0f);
    	}
    }
    
    private void setInstructionText(int textFromResourceId){
    	if(null != mClickText){
    		mClickText.setText(mActivity.getResources().getString(textFromResourceId));
    		mClickText.setAlpha(1.0f);
    	}
    }

    // SurfaceHolder callbacks
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v(TAG, "Surface changed. width=" + width + ". height=" + height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "SurfaceVIEW created");
        mController.onPreviewUIReady();
        mSurfaceView.invalidate();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "Surface destroyed");
        mController.onPreviewUIDestroyed();
    }

    public void resetAnimations(){
    	Log.d(TAG,"Animations reset!");
    	mMoveDownAndFade.reset();
        mMoveDownAndStay.reset();
    }

    class ForwardInterpolator implements Interpolator {
        @Override
        public float getInterpolation(float input) {
            return fastInSlowOut(input);
        }
    }

    class ReverseInterpolator implements Interpolator {
        @Override
        public float getInterpolation(float input) {
            return fastInSlowOut(Math.abs(input - 1f));
        }
    }

    /**
     * Describes a formula for a fast-then-slow interpolation
     * @param x input value (should be within the range of [0f, 1f])
     * @return
     */
    private float fastInSlowOut(float x){
        return ( 6f / ( 5f * (x+0.2f) ) ) * x;
    }

    public void showPreviewTimeoutScreen(){
        mPreviewChangedListener.onPreviewPaused();
        mSurfaceView.setVisibility(View.GONE);
        mPreviewTimeoutCover.setVisibility(View.VISIBLE);
    }

    public void hidePreviewTimeoutScreen(){
        mPreviewTimeoutCover.setVisibility(View.GONE);
        mSurfaceView.setVisibility(View.VISIBLE);
        mPreviewChangedListener.onPreviewResumed();
    }

    public void setOnPreviewChangedListener(OnPreviewChangedListener listener){
        mPreviewChangedListener = listener;
    }
}
