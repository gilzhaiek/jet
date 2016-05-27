package com.reconinstruments.jetmusic;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
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

public class JetMusicPlayer extends Activity{

    private TextView line1TV;
    private TextView line2TV;
    private TextView mplayPauseTV;
    
    private String mTitle;
    private String mArtist;
    private String mAlbum;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_jet);
        startService(new Intent(MusicService.ACTION_MUSIC_SERVICE));
        line1TV = (TextView) findViewById(R.id.line1);
        line2TV = (TextView) findViewById(R.id.line2);
        mplayPauseTV = (TextView) findViewById(R.id.play_pause);
        AVRCPManager avrcpManager = AVRCPManager.getInstance(this.getApplicationContext());
        if(avrcpManager.getMostRecentPlayStatus() == PlayStatus.PLAYING){
            line1TV.setTextColor(Color.parseColor("#ffffff"));
            mplayPauseTV.setText("PAUSE");
        }else{
            line1TV.setTextColor(Color.parseColor("#808080"));
            mplayPauseTV.setText("PLAY");
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
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    MusicPlayUtils.volUp(this.getApplicationContext());
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    MusicPlayUtils.volDown(this.getApplicationContext());
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    MusicPlayUtils.previous(this.getApplicationContext());
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    MusicPlayUtils.next(this.getApplicationContext());
                    break;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
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
                    line1TV.setTextColor(Color.parseColor("#ffffff"));
                    mplayPauseTV.setText("PAUSE");
                }else{
                    line1TV.setTextColor(Color.parseColor("#808080"));
                    mplayPauseTV.setText("PLAY");
                }
            }else if("HUD_STATE_CHANGED".equals(action)) { //launch main screen to ask for smartphone connection
                int state = arg1.getIntExtra("state", 0);
                if (state == 0) { 
                    startActivity(new Intent(JetMusicPlayer.this, MainActivity.class));
                    finish();
                }
            }
        }
        
    };
}
