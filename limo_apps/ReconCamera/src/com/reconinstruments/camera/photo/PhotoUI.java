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


package com.reconinstruments.camera.photo;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout.LayoutParams;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.reconinstruments.camera.OnPreviewChangedListener;
import com.reconinstruments.camera.R;
import com.reconinstruments.camera.app.CameraActivity;
import com.reconinstruments.camera.ui.AbstractSettingPopup;
import com.reconinstruments.camera.ui.AnimationManager;
import com.reconinstruments.camera.ui.CameraRootView;
import com.reconinstruments.camera.ui.CountDownView;
import com.reconinstruments.camera.ui.CountDownView.OnCountDownFinishedListener;
import com.reconinstruments.camera.util.CameraUtil;

public class PhotoUI implements CameraRootView.MyDisplayListener, SurfaceHolder.Callback {

    private static final String TAG = "CAM_UI";
    private static final int DOWN_SAMPLE_FACTOR = 4;
    private final AnimationManager mAnimationManager;
    private CameraActivity mActivity;
    private PhotoController mController;

    private View mRootView;

    private PopupWindow mPopup;
    private CountDownView mCountDownView;

    private RelativeLayout mPreviewTimeoutCover;
    private RelativeLayout mCameraControls;
    private AlertDialog mLocationDialog;

    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private float mSurfaceViewUncroppedWidth;
    private float mSurfaceViewUncroppedHeight;

    private ImageView mPreviewThumb;
    private View mFlashOverlay;

    private SurfaceView mSurfaceView;
    //TODO: Temporary fix, remove duplicated setting for ratio. Supoosed to be 4:3, but we don't want to use any scaling, further code is from mMatrix.setScale in line 263
    //TODO: The camera hardware is using 1280X960 anyway and it's going to reforce the 4:3 ratio anyway. The further step is to follow TI camera example to use 320X240 rectangle to draw preview surface.
    private float mAspectRatio = 16f / 9f;

    private LinearLayout mInstructionTextBlock;
    private TextView mClickText, mClickMessage;
    private ImageView mSelectIcon;
    private Animation textFadeOut;

    private OnPreviewChangedListener mPreviewChangedListener;

    private OnLayoutChangeListener mLayoutListener = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            int width = right - left;
            int height = bottom - top;
            if (mPreviewWidth != width || mPreviewHeight != height) {
                mPreviewWidth = width;
                mPreviewHeight = height;
                setTransformMatrix(width, height);
            }
        }
    };

    private class DecodeTask extends AsyncTask<Void, Void, Bitmap> {
        private final byte [] mData;
        private int mOrientation;
        private boolean mMirror;

        public DecodeTask(byte[] data, int orientation, boolean mirror) {
            mData = data;
            mOrientation = orientation;
            mMirror = mirror;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            // Decode image in background.
            Bitmap bitmap = CameraUtil.downSample(mData, DOWN_SAMPLE_FACTOR);
            if (mOrientation != 0 || mMirror) {
                Matrix m = new Matrix();
                if (mMirror) {
                    // Flip horizontally
                    m.setScale(-1f, 1f);
                }
                m.preRotate(mOrientation);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m,
                        false);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mPreviewThumb.setImageBitmap(bitmap);
            mAnimationManager.startCaptureAnimation(mPreviewThumb);
        }
    }

    public PhotoUI(CameraActivity activity, PhotoController controller, View parent, boolean showInstructions) {
        mActivity = activity;
        mController = controller;
        mRootView = parent;

        mActivity.getLayoutInflater().inflate(R.layout.photo_module, (ViewGroup) mRootView, true);
        mFlashOverlay = mRootView.findViewById(R.id.flash_overlay);
        mPreviewTimeoutCover = (RelativeLayout) mRootView.findViewById(R.id.preview_timeout_cover);
        mPreviewTimeoutCover.setVisibility(View.GONE);

        mSelectIcon = (ImageView) mRootView.findViewById(R.id.select_btn_photo);
        mSelectIcon.setImageResource(R.drawable.select_btn);
        mInstructionTextBlock = (LinearLayout) mRootView.findViewById(R.id.instruction_text_block_photo);
        mClickText = (TextView) mRootView.findViewById(R.id.click_text_photo);
        mClickText.setText(R.string.click_string);
        mClickMessage = (TextView) mRootView.findViewById(R.id.click_text_message_photo);
        mClickMessage.setText(R.string.photo_instruction_capture);
        if(!showInstructions) {
            mInstructionTextBlock.clearAnimation();
            mInstructionTextBlock.setVisibility(View.INVISIBLE);
        }

        // display the view
        mSurfaceView = (SurfaceView) mRootView.findViewById(R.id.preview_content);
        mSurfaceView.getHolder().addCallback(this);
        mSurfaceView.addOnLayoutChangeListener(mLayoutListener);

        mCameraControls = (RelativeLayout) mRootView.findViewById(R.id.camera_controls);
        mAnimationManager = new AnimationManager();
        setupAnimations();
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
        if (aspectRatio <= 0) {
            Log.e(TAG, "Invalid aspect ratio: " + aspectRatio);
            return;
        }
        if (aspectRatio < 1f) {
            aspectRatio = 1f / aspectRatio;
        }

        if (mAspectRatio != aspectRatio) {
            mAspectRatio = aspectRatio;
            // Update transform matrix with the new aspect ratio.
            if (mPreviewWidth != 0 && mPreviewHeight != 0) {
                setTransformMatrix(mPreviewWidth, mPreviewHeight);
            }
        }
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
        else {
            Log.e(TAG, "SurfaceView is null or invisible!");
        }
    }

    //Surface Holder callbacks
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){
        Log.v(TAG, "SurfaceVIEW ready");
        mController.onPreviewUIReady();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        Log.v(TAG, "SurfaceVIEW destroyed");
        mController.onPreviewUIDestroyed();
    }

    public View getRootView() {
        return mRootView;
    }

    public void animateCapture(final byte[] jpegData, int orientation, boolean mirror) {
        // Decode jpeg byte array and then animate the jpeg
        DecodeTask task = new DecodeTask(jpegData, orientation, mirror);
        task.execute();
    }

    public void initializeControlByIntent() {
        mPreviewThumb = (ImageView) mRootView.findViewById(R.id.preview_thumb);
        mPreviewThumb.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.gotoGallery();
            }
        });
    }

    protected void setupAnimations(){
        textFadeOut = new AlphaAnimation(1.0f, 0.0f);
        textFadeOut.setDuration(500);
        textFadeOut.setFillAfter(true);
    }

    public void hideUI() {
        mCameraControls.setVisibility(View.INVISIBLE);
    }

    public void showUI() {
        mCameraControls.setVisibility(View.VISIBLE);
    }

    public boolean arePreviewControlsVisible() {
        return (mCameraControls.getVisibility() == View.VISIBLE);
    }

    // called from onResume every other time
    public void initializeSecondTime(Camera.Parameters params) {
    }

    public void setCameraState(int state) {
    }

    public void animateFlash() {
        mAnimationManager.startFlashAnimation(mFlashOverlay);
    }

    public boolean onBackPressed() {
        // In image capture mode, back button should:
        // 1) if there is any popup, dismiss them, 2) otherwise, get out of
        // image capture
        if (mController.isImageCaptureIntent()) {
            mController.onCaptureCancelled();
            return true;
        } else if (!mController.isCameraIdle()) {
            // ignore backs while we're taking a picture
            return true;
        } else {
            return false;
        }
    }

    public void onPreviewFocusChanged(boolean previewFocused) {
        if (previewFocused) {
            showUI();
        } else {
            hideUI();
        }
        if (!previewFocused && mCountDownView != null) mCountDownView.cancelCountDown();
    }

    public void showPopup(AbstractSettingPopup popup) {
        hideUI();

        if (mPopup == null) {
            mPopup = new PopupWindow(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            mPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            mPopup.setOutsideTouchable(true);
            mPopup.setFocusable(true);
            mPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    mPopup = null;
                    showUI();
                }
            });
        }
        popup.setVisibility(View.VISIBLE);
        mPopup.setContentView(popup);
        mPopup.showAtLocation(mRootView, Gravity.CENTER, 0, 0);
    }

    public void dismissPopup() {
        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
        }
    }

    public boolean collapseCameraControls() {
        // Remove all the popups/dialog boxes
        boolean ret = false;
        if (mPopup != null) {
            dismissPopup();
            ret = true;
        }
        return ret;
    }

    public void setSwipingEnabled(boolean enable) {
        mActivity.setSwipingEnabled(enable);
    }

    public SurfaceHolder getSurfaceHolder(){
        return mSurfaceView.getHolder();
    }

    // Countdown timer

    private void initializeCountDown() {
        mActivity.getLayoutInflater().inflate(R.layout.count_down_to_capture,
                (ViewGroup) mRootView, true);
        mCountDownView = (CountDownView) (mRootView.findViewById(R.id.count_down_to_capture));
        mCountDownView.setCountDownFinishedListener((OnCountDownFinishedListener) mController);
    }

    public boolean isCountingDown() {
        return mCountDownView != null && mCountDownView.isCountingDown();
    }

    public void cancelCountDown() {
        if (mCountDownView == null) return;
        mCountDownView.cancelCountDown();
    }

    public void startCountDown(int sec, boolean playSound) {
        if (mCountDownView == null) initializeCountDown();
        mCountDownView.startCountDown(sec, playSound);
    }

    public void onResume(){
        mPreviewChangedListener.onPreviewResumed();
    }

    public void onPause() {
        cancelCountDown();

        // Clear UI.
        collapseCameraControls();

        mPreviewChangedListener.onPreviewPaused();

        if (mLocationDialog != null && mLocationDialog.isShowing()) {
            mLocationDialog.dismiss();
        }
        mLocationDialog = null;
    }

    public void initDisplayChangeListener() {
        ((CameraRootView) mRootView).setDisplayChangeListener(this);
    }

    public void removeDisplayChangeListener() {
        ((CameraRootView) mRootView).removeDisplayChangeListener();
    }

    @Override
    public void onDisplayChanged() {
        Log.d(TAG, "Device flip detected.");
    }

    public void fadeInstructionText(long fadeDuration, long startOffset){
        if(mInstructionTextBlock != null){
            textFadeOut.setDuration(fadeDuration);
            textFadeOut.setStartOffset(startOffset);
            mInstructionTextBlock.startAnimation(textFadeOut);
        }
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
