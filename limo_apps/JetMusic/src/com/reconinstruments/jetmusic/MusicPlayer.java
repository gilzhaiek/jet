package com.reconinstruments.jetmusic;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.reconinstruments.jetmusic.service.AVRCPManager;
import com.reconinstruments.jetmusic.service.MusicService;
import com.stonestreetone.bluetopiapm.AVRCP.PlayStatus;

public class MusicPlayer extends Activity{

    private TextView line1TV;
    private TextView line2TV;
    private ImageView mPlayPauseBtn;
    private ImageView mPrevBtn;
    private ImageView mNextBtn;
    private ImageView mVolUpBtn;
    private ImageView mVolDownBtn;
    
    private String mTitle;
    private String mArtist;
    private String mAlbum;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);
        startService(new Intent(MusicService.ACTION_MUSIC_SERVICE));
        line1TV = (TextView) findViewById(R.id.line1);
        line2TV = (TextView) findViewById(R.id.line2);
        mPlayPauseBtn = (ImageView) findViewById(R.id.play_pause_icon);
        mPrevBtn = (ImageView) findViewById(R.id.prev_icon);
        mNextBtn = (ImageView) findViewById(R.id.next_icon);
        mVolUpBtn = (ImageView) findViewById(R.id.volup_icon);
        mVolDownBtn = (ImageView) findViewById(R.id.voldown_icon);
        AVRCPManager avrcpManager = AVRCPManager.getInstance(this.getApplicationContext());
        if(avrcpManager.getMostRecentPlayStatus() == PlayStatus.PLAYING){
            mPlayPauseBtn.setImageResource(R.drawable.controller_pause_selector);
        }else{
            mPlayPauseBtn.setImageResource(R.drawable.controller_play_selector);
        }
        if(!"".equals(avrcpManager.getMostRecentTitle())){
            line1TV.setText(avrcpManager.getMostRecentTitle());
            if(!"".equals(avrcpManager.getMostRecentArtist()) || !"".equals(avrcpManager.getMostRecentAlbum())){
                line2TV.setVisibility(View.VISIBLE);
                line2TV.setText(avrcpManager.getMostRecentArtist() + " - " + avrcpManager.getMostRecentAlbum());
            }else{
                line2TV.setVisibility(View.GONE);
            }
        }else{
            line1TV.setText("Start Music on the Phone");
            line2TV.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(musicReceiver, new IntentFilter(AVRCPManager.ACTION_MUSIC_CONTENT_CHANGED));
        registerReceiver(musicReceiver, new IntentFilter(AVRCPManager.ACTION_MUSIC_PLAY_CHANGED));
        registerReceiver(musicReceiver, new IntentFilter("HUD_STATE_CHANGED"));
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        try {
            this.unregisterReceiver(musicReceiver);
        } catch (IllegalArgumentException e) {
            //ignore it
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
              if(!mPlayPauseBtn.isPressed()){
                  mPlayPauseBtn.invalidate();
                  mPlayPauseBtn.requestFocus();
                  mPlayPauseBtn.setSelected(true);
                  mPlayPauseBtn.setPressed(true);
              }
              break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if(!mPrevBtn.isPressed()){
                    mPrevBtn.invalidate();
                    mPrevBtn.requestFocus();
                    mPrevBtn.setSelected(true);
                    mPrevBtn.setPressed(true);
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if(!mNextBtn.isPressed()){
                    mNextBtn.invalidate();
                    mNextBtn.requestFocus();
                    mNextBtn.setSelected(true);
                    mNextBtn.setPressed(true);
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if(!mVolDownBtn.isPressed()){
                    mVolDownBtn.invalidate();
                    mVolDownBtn.requestFocus();
                    mVolDownBtn.setSelected(true);
                    mVolDownBtn.setPressed(true);
                }
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if(!mVolUpBtn.isPressed()){
                    mVolUpBtn.invalidate();
                    mVolUpBtn.requestFocus();
                    mVolUpBtn.setSelected(true);
                    mVolUpBtn.setPressed(true);
                }
                break;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Intent intent = new Intent(MusicService.ACTION_MUSIC_SERVICE);
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    mVolUpBtn.clearFocus();
                    mVolUpBtn.invalidate();
                    MusicPlayUtils.volUp(this.getApplicationContext());
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    mVolDownBtn.clearFocus();
                    mVolDownBtn.invalidate();
                    MusicPlayUtils.volDown(this.getApplicationContext());
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    mPrevBtn.clearFocus();
                    mPrevBtn.invalidate();
                    MusicPlayUtils.previous(this.getApplicationContext());
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mNextBtn.clearFocus();
                    mNextBtn.invalidate();
                    MusicPlayUtils.next(this.getApplicationContext());
                    break;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    mPlayPauseBtn.clearFocus();
                    mPlayPauseBtn.invalidate();
                    MusicPlayUtils.playOrPause(this.getApplicationContext());
                    break;
                }
        }
        return super.onKeyUp(keyCode, event);
    }
    
    private final BroadcastReceiver musicReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            String action = arg1.getAction();
            if (AVRCPManager.ACTION_MUSIC_CONTENT_CHANGED.equals(action)) {
                mTitle = arg1.getStringExtra(AVRCPManager.EXTRA_MUSIC_TITLE);
                mArtist = arg1.getStringExtra(AVRCPManager.EXTRA_MUSIC_ARTIST);
                mAlbum = arg1.getStringExtra(AVRCPManager.EXTRA_MUSIC_ALBUM);
                if(!mTitle.isEmpty()){
                    line1TV.setText(mTitle);
                    line2TV.setVisibility(View.VISIBLE);
                    if(!mArtist.isEmpty() || !mAlbum.isEmpty()){
                        line2TV.setText(mArtist + " - " + mAlbum);
                    }else{
                        line2TV.setVisibility(View.GONE);
                    }
                }else{
                    line1TV.setText("Start Music on the Phone");
                    line2TV.setVisibility(View.GONE);
                }
            }else if(AVRCPManager.ACTION_MUSIC_PLAY_CHANGED.equals(action)) {
                int state = arg1.getIntExtra(AVRCPManager.EXTRA_MUSIC_STATE, AVRCPManager.PLAY_STATE_STOP);
                if(state == AVRCPManager.PLAY_STATE_PLAY){
                    mPlayPauseBtn.setImageResource(R.drawable.controller_pause_selector);
                }else{
                    mPlayPauseBtn.setImageResource(R.drawable.controller_play_selector);
                }
            }else if("HUD_STATE_CHANGED".equals(action)) { //launch main screen to ask for smartphone connection
                int state = arg1.getIntExtra("state", 0);
                if (state == 0) { 
                    startActivity(new Intent(MusicPlayer.this, MainActivity.class));
                    finish();
                }
            }
        }
        
    };
}
