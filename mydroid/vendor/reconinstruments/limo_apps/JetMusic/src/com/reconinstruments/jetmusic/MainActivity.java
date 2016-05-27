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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reconinstruments.jetmusic.service.AVRCPManager;
import com.reconinstruments.jetmusic.service.MusicService;
import com.reconinstruments.utils.BTHelper;
import com.stonestreetone.bluetopiapm.AVRCP.PlayStatus;
import com.reconinstruments.dashelement1.ColumnElementActivity;
import com.reconinstruments.utils.DeviceUtils;

public class MainActivity extends ColumnElementActivity{
    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView titleTV;
    private RelativeLayout twoLinesLayout;
    private TextView line1TV;
    private TextView line2TV;
    private TextView bodyTextTV;
    private TextView actionTextTV;
    private ImageView actionIV;
    private LinearLayout actionLayout;

    private BTHelper mBTHelper;
    
    private AVRCPManager mAVRCPManager;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(MusicService.ACTION_MUSIC_SERVICE));
        mBTHelper = BTHelper.getInstance(getApplicationContext());
        registerReceiver(musicStateReceiver, new IntentFilter(AVRCPManager.ACTION_MUSIC_SERVICE_CHANGED));
        registerReceiver(musicStateReceiver, new IntentFilter(AVRCPManager.ACTION_MUSIC_PLAY_CHANGED));
        mAVRCPManager = AVRCPManager.getInstance(this.getApplicationContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
        
        if(mBTHelper.getBTConnectionState() == 2 && (mAVRCPManager.getCurrentPlayStatus() == PlayStatus.PLAYING || mAVRCPManager.getCurrentPlayStatus() == PlayStatus.PAUSED)){
            if(DeviceUtils.isSun()){
                startActivity(new Intent(this, JetMusicPlayer.class));
            }else{
                startActivity(new Intent(this, MusicPlayer.class));
            }
            finish();
            return;
        }


        setContentView(R.layout.main);
        titleTV = (TextView) findViewById(R.id.title);
        twoLinesLayout = (RelativeLayout) findViewById(R.id.two_lines_layout);
        actionLayout = (LinearLayout) findViewById(R.id.action);
        line1TV = (TextView) findViewById(R.id.line1);
        line2TV = (TextView) findViewById(R.id.line2);
        bodyTextTV = (TextView) findViewById(R.id.body_text);
        actionTextTV = (TextView) findViewById(R.id.actoin_text);
        actionIV = (ImageView) findViewById(R.id.action_img);
        if(DeviceUtils.isSun()){
            actionIV.setImageResource(R.drawable.jet_select);
        }else{
            actionIV.setImageResource(R.drawable.select);
        }
        if(mBTHelper.getBTConnectionState() == 2){
            titleTV.setText("MUSIC PLAYER");
            twoLinesLayout.setVisibility(View.GONE);
            bodyTextTV.setVisibility(View.VISIBLE);
            bodyTextTV.setText("No Music Playing.\nStart a song from your favorite smartphone music app.");
            actionLayout.setVisibility(View.GONE);
        }else{
            titleTV.setText("MUSIC PLAYER");
            twoLinesLayout.setVisibility(View.GONE);
            bodyTextTV.setVisibility(View.VISIBLE);
            bodyTextTV.setText("You must have a smartphone connected to use this feature.");
            actionLayout.setVisibility(View.VISIBLE);
            actionTextTV.setText("CONNECT PHONE");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            this.unregisterReceiver(musicStateReceiver);
        } catch (IllegalArgumentException e) {
            //ignore it
        }
    }

    @Override
    //implement the key events the same as music player to enhance the UX on issue JAS-1895
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
                    if(mBTHelper.getBTConnectionState() != 2){
                        startActivity(new Intent("com.reconinstruments.connectdevice.CONNECT"));
                    }else if(mBTHelper.getBTConnectionState() == 2){
                        MusicPlayUtils.playOrPause(this.getApplicationContext());
                    }
                    break;
                }
        }
        return super.onKeyUp(keyCode, event);
    }
    
    private final BroadcastReceiver musicStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (AVRCPManager.ACTION_MUSIC_SERVICE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(AVRCPManager.EXTRA_MUSIC_STATE, AVRCPManager.MUSIC_STATE_DISCONNECTED);
                if (state == AVRCPManager.MUSIC_STATE_DISCONNECTED) { 
                    Log.d(TAG, "MUSIC_STATE_DISCONNECTED");
                } else if (state == AVRCPManager.MUSIC_STATE_CONNECTED) {
                    if(mAVRCPManager.getCurrentPlayStatus() == PlayStatus.PLAYING || mAVRCPManager.getCurrentPlayStatus() == PlayStatus.PAUSED){
                        if(DeviceUtils.isSun()){
                            startActivity(new Intent(context, JetMusicPlayer.class));
                        }else{
                            startActivity(new Intent(context, MusicPlayer.class));
                        }
                        finish();
                    }else{
                        titleTV.setText("MUSIC PLAYER");
                        twoLinesLayout.setVisibility(View.GONE);
                        bodyTextTV.setVisibility(View.VISIBLE);
                        bodyTextTV.setText("No Music Playing.\nStart a song from your favorite smartphone music app.");
                        actionLayout.setVisibility(View.GONE);
                    }
                }
            }else if(AVRCPManager.ACTION_MUSIC_PLAY_CHANGED.equals(action)) {
                int state = intent.getIntExtra(AVRCPManager.EXTRA_MUSIC_STATE, AVRCPManager.PLAY_STATE_STOP);
                if(state != AVRCPManager.PLAY_STATE_STOP){
                    if(DeviceUtils.isSun()){
                        startActivity(new Intent(context, JetMusicPlayer.class));
                    }else{
                        startActivity(new Intent(context, MusicPlayer.class));
                    }
                    finish();
                }
            }
        }
    };
}
