package com.reconinstruments.camera.video;

import java.util.ArrayList;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;

import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;
import com.reconinstruments.camera.R;
import com.reconinstruments.camera.app.GalleryActivity;
import com.reconinstruments.camera.ui.DeleteDialog;
import com.reconinstruments.camera.ui.DeleteDialogFragment;

public class VideoPlayerActivity extends FragmentActivity
	implements MediaPlayer.OnCompletionListener, DeleteDialog.DialogKeyListener {
	private static final String TAG = VideoPlayerActivity.class.getSimpleName();

	public static final String PAGER_MENU_TEXT_PLAY		= "Play";
    public static final String PAGER_MENU_TEXT_CANCEL	= "Cancel";

    public static final int MENU_PLAY_OR_PAUSE = 0;
    public static final int MENU_DELETE = 1;
	public static final int MENU_SHARE = 2;
	
	public static final int WHAT_UPDATE_DURATION		= 100;
	public static final int WHAT_UPDATE_PROGRESS		= 101;
	public static final int WHAT_RESET_ALL				= 102;
	public static final int WHAT_PLAY_OR_PAUSE_PRESSED = 103;
	public static final int WHAT_SHARE_PRESSED			= 104;
	public static final int WHAT_DELETE_PRESSED			= 105;
	
	/** PrgressBar update time in ms */
	private static final int PROGRESS_BAR_UPDATE_TIME = 100;

	private Uri mVideoURI;
	private VideoView mVideoView;
	private ProgressBar mBottomProgressBar;

    private DeleteDialog mPlayOrDeleteDialog;
    private boolean mPlayOrDeleteShowing = false, mDeleteConfirmShowing = false;

	/** Removing message from Handler is not enough for race condition since callback is called
	 * in diff thread */
	private boolean mCanUpdateProgressBar = false;

	private int mTotalDuration;


	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			/**
			 * msg for update total duration since there could be race condition for
			 * getting the total duration before finish loading the video.
			 **/
			case WHAT_UPDATE_DURATION:
				mTotalDuration = mVideoView.getDuration();
				if (mTotalDuration <= 0) {
					mHandler.sendEmptyMessageDelayed(WHAT_UPDATE_DURATION, PROGRESS_BAR_UPDATE_TIME);
				}
				break;
			case WHAT_UPDATE_PROGRESS:
				int progress = Math.round((100.0f*(float)mVideoView.getCurrentPosition()/(float)mTotalDuration));
				if (mCanUpdateProgressBar) {
					mBottomProgressBar.setProgress(progress);
				}
				mHandler.sendEmptyMessageDelayed(WHAT_UPDATE_PROGRESS, PROGRESS_BAR_UPDATE_TIME);
				break;
			case WHAT_RESET_ALL:
				Log.d(TAG, "reset all");
				mVideoView.seekTo(0);
				mBottomProgressBar.setProgress(0);
				// following lines are put here for racing condition
				mVideoView.start();
                setDefaultItemText(PAGER_MENU_TEXT_PLAY, 0);
				onBeginProgressBarUpdate();
				break;
			case WHAT_PLAY_OR_PAUSE_PRESSED:
                //toggle the state of the video between Playing and Paused
				if (mVideoView.isPlaying()) {
					Log.d(TAG, "stopped playing : " + mVideoURI.toString());
					mVideoView.pause();
					onStopProgressBarUpdate();
				} else {
					Log.d(TAG, "playing : " + mVideoURI.toString());
					if (mBottomProgressBar.getProgress() == 100) {
						mHandler.sendEmptyMessage(WHAT_RESET_ALL);
					} else {
						mVideoView.start();
						onBeginProgressBarUpdate();
					}
				}
				break;
			case WHAT_DELETE_PRESSED:
				Log.d(TAG, "Delete pressed");
				Intent returnIntent = new Intent();
				setResult(GalleryActivity.DELETE_VIDEO_ACTION, returnIntent);
				finish();
				break;
			case WHAT_SHARE_PRESSED:
				
				break;
			}
		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.video_player);

		/*************************
		 *** Video Preparation 
		 *************************/

		mVideoURI = getIntent().getData();

		if (mVideoURI == null) {
			Toast.makeText(this, "Video is not Provided", Toast.LENGTH_LONG).show();
			finish();
		}

		mVideoView = (VideoView) findViewById(R.id.video_player_videoview);
		mVideoView.setVideoURI(mVideoURI);
		mVideoView.setOnCompletionListener(this);

		mTotalDuration = mVideoView.getDuration();

		/* Update the right total duration for progress bar */
		if (mTotalDuration <= 0) {
			mHandler.sendEmptyMessageDelayed(WHAT_UPDATE_DURATION,PROGRESS_BAR_UPDATE_TIME);
		}

		mBottomProgressBar = (ProgressBar) findViewById(R.id.video_player_progress);
		onBeginProgressBarUpdate();
		
		mVideoView.start();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		onStopProgressBarUpdate();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_DOWN:
		case KeyEvent.KEYCODE_DPAD_UP:
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			return true;
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER:
            if(!mPlayOrDeleteShowing) {
                mHandler.sendEmptyMessage(WHAT_PLAY_OR_PAUSE_PRESSED);
            }
			showPlayOrDeleteDialog();
			return true;
		case KeyEvent.KEYCODE_BACK:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

    @Override
    public boolean handleKeyEvent(DialogInterface dialog, int index, int keyCode, KeyEvent event){
        if(event.getAction() == KeyEvent.ACTION_DOWN) {
            switch(keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    switch (index) {
                        case MENU_PLAY_OR_PAUSE:
                            // in the delete confirm dialog, this is "Cancel",
                            // so resume playback and dismiss dialog
                            if(mDeleteConfirmShowing){
                                mDeleteConfirmShowing = false;
                                mHandler.sendEmptyMessage(WHAT_PLAY_OR_PAUSE_PRESSED);
                                break;
                            }
                            setTitleText("VIDEO OPTIONS");
                            mHandler.sendEmptyMessage(WHAT_PLAY_OR_PAUSE_PRESSED);
                            break;
                        case MENU_DELETE:
                            if(!mDeleteConfirmShowing) {
                                setTitleText("DELETE VIDEO?");
                                setDefaultItemText(PAGER_MENU_TEXT_CANCEL, 0);
                                mPlayOrDeleteDialog.getPager().setCurrentItem(0, false);
                                mDeleteConfirmShowing = true;
                                return true;
                            }
                            else {
                                mHandler.sendEmptyMessage(WHAT_DELETE_PRESSED);
                            }
                            break;
                        case MENU_SHARE:
                            break;
                        default:
                            return false;
                    }
                    mPlayOrDeleteShowing = false;
                    dialog.dismiss();
                    break;
                case KeyEvent.KEYCODE_BACK:
                    mPlayOrDeleteShowing = false;
                    mDeleteConfirmShowing = false;
                    if(mBottomProgressBar.getProgress() < 100) {
                        mHandler.sendEmptyMessage(WHAT_PLAY_OR_PAUSE_PRESSED);
                        dialog.dismiss();
                    }
                    else {
                        finish();
                    }
                default:
                    return false;
            }
        }
        return true;
    }
	
	@Override
	public void onCompletion(MediaPlayer mp) {
        if(!mPlayOrDeleteShowing) showPlayOrDeleteDialog();

        // Change the button to PLAY
        setDefaultItemText(PAGER_MENU_TEXT_PLAY, 0);

        // Make sure progress is cleanly set
		mBottomProgressBar.setProgress(100);

		onStopProgressBarUpdate();
	}

	private void onBeginProgressBarUpdate() {
		mCanUpdateProgressBar = true;
		mHandler.sendEmptyMessageDelayed(WHAT_UPDATE_PROGRESS, PROGRESS_BAR_UPDATE_TIME);
	}

	private void onStopProgressBarUpdate() {
		mCanUpdateProgressBar = false;
		mHandler.removeMessages(WHAT_UPDATE_PROGRESS);
	}
	
    private void showPlayOrDeleteDialog(){
        ArrayList<Fragment> fragList = new ArrayList<Fragment>();
        fragList.add(new DeleteDialogFragment(R.layout.delete_dialog_fragment_layout, "Resume", 0, 0));
        fragList.add(new DeleteDialogFragment(R.layout.delete_dialog_fragment_layout, "Delete", 0, 1));

        mPlayOrDeleteDialog = new DeleteDialog(this, "VIDEO OPTIONS", fragList, R.layout.delete_dialog_layout);
        mPlayOrDeleteDialog.setDialogKeyListener(this);
        mPlayOrDeleteDialog.show(getSupportFragmentManager().beginTransaction(), "RESUME_VIDEO");
        mPlayOrDeleteShowing = true;
    }

    private void setDefaultItemText(String text, int index){
        if(mPlayOrDeleteDialog != null) {
            mPlayOrDeleteDialog.setDefaultText(text, index);
        }
    }

    private void setTitleText(String title){
        if(mPlayOrDeleteDialog != null){
            mPlayOrDeleteDialog.setTitleText(title);
        }
    }
}


