//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.jetmusic;
import android.content.Context;
import android.content.Intent;

import com.reconinstruments.jetmusic.service.MusicService;

/**
 *  <code>MusicPlayUtils</code> provides the interface to interact with music service.
 *
 */
public class MusicPlayUtils {

    public static void volUp(Context context) {
        Intent intent = new Intent(MusicService.ACTION_MUSIC_SERVICE);
        intent.putExtra(MusicService.EXTRA_MUSIC_COMMAND, MusicService.MUSIC_COMMAND_VOLUP);
        context.startService(intent);
    }

    public static void volDown(Context context) {
        Intent intent = new Intent(MusicService.ACTION_MUSIC_SERVICE);
        intent.putExtra(MusicService.EXTRA_MUSIC_COMMAND, MusicService.MUSIC_COMMAND_VOLDOWN);
        context.startService(intent);
    }

    public static void previous(Context context) {
        Intent intent = new Intent(MusicService.ACTION_MUSIC_SERVICE);
        intent.putExtra(MusicService.EXTRA_MUSIC_COMMAND, MusicService.MUSIC_COMMAND_PREVIOUS);
        context.startService(intent);
    }

    public static void next(Context context) {
        Intent intent = new Intent(MusicService.ACTION_MUSIC_SERVICE);
        intent.putExtra(MusicService.EXTRA_MUSIC_COMMAND, MusicService.MUSIC_COMMAND_NEXT);
        context.startService(intent);
    }

    public static void playOrPause(Context context) {
        Intent intent = new Intent(MusicService.ACTION_MUSIC_SERVICE);
        intent.putExtra(MusicService.EXTRA_MUSIC_COMMAND, MusicService.MUSIC_COMMAND_PLAY_OR_PAUSE);
        context.startService(intent);
    }
    
}