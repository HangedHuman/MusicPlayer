package com.example.musicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import com.example.musicplayer.utils.PlayerManager;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String ACTION_PLAY_PAUSE =
            "com.example.musicplayer.PLAY_PAUSE";

    public static final String ACTION_NEXT =
            "com.example.musicplayer.NEXT";

    public static final String ACTION_PREV =
            "com.example.musicplayer.PREV";

    @Override
    public void onReceive(Context context, Intent intent) {

        PlayerManager manager =
                PlayerManager.getInstance(context);

        String action = intent.getAction();

        if (ACTION_PLAY_PAUSE.equals(action)) {
            manager.playPause();
        } else if (ACTION_NEXT.equals(action)) {
            manager.next();
        } else if (ACTION_PREV.equals(action)) {
            manager.prev();
        }

        Intent serviceIntent =
                new Intent(
                        context,
                        MusicService.class
                );
        ContextCompat.startForegroundService(
                context,
                serviceIntent
        );
    }
}
