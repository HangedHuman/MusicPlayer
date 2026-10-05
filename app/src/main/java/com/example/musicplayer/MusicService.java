package com.example.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;

import com.example.musicplayer.utils.PlayerManager;

public class MusicService extends Service {

    public static final String CHANNEL_ID = "music_channel";
    private static final int NOTIFICATION_ID = 1;
    private PlayerManager manager;

    @Override
    public void onCreate() {
        super.onCreate();

        android.util.Log.d("MUSIC_SERVICE", "SERVICE STARTED");

        manager = PlayerManager.getInstance(this);
        createChannel();
        manager.getPlayer().addListener(
                playerListener
        );
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    private void createChannel() {

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Music",
                        NotificationManager.IMPORTANCE_LOW
                );

        channel.setDescription(
                "Управление воспроизведением музыки"
        );
        NotificationManager manager =
                getSystemService(NotificationManager.class);

        manager.createNotificationChannel(channel);
    }

    private Notification buildNotification() {

        Song song =
                manager.getCurrentSong();

        String title =
                "Music Player";

        String artist =
                "Нет воспроизведения";

        Bitmap cover = null;

        if (song != null) {

            if (song.getTitle() != null &&
                    !song.getTitle().isEmpty()) {

                title = song.getTitle();
            }

            if (song.getArtist() != null &&
                    !song.getArtist().isEmpty()) {

                artist = song.getArtist();
            }

            cover = song.getCover();
        }

        // PLAY / PAUSE
        Intent playPauseIntent =
                new Intent(
                        this,
                        NotificationReceiver.class
                );

        playPauseIntent.setAction(
                NotificationReceiver.ACTION_PLAY_PAUSE
        );

        PendingIntent playPausePendingIntent =
                PendingIntent.getBroadcast(
                        this,
                        100,
                        playPauseIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

        // NEXT
        Intent nextIntent =
                new Intent(
                        this,
                        NotificationReceiver.class
                );

        nextIntent.setAction(
                NotificationReceiver.ACTION_NEXT
        );

        PendingIntent nextPendingIntent =
                PendingIntent.getBroadcast(
                        this,
                        101,
                        nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

        // PREVIOUS
        Intent previousIntent =
                new Intent(
                        this,
                        NotificationReceiver.class
                );

        previousIntent.setAction(
                NotificationReceiver.ACTION_PREV
        );

        PendingIntent previousPendingIntent =
                PendingIntent.getBroadcast(
                        this,
                        102,
                        previousIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

        Intent openAppIntent =
                new Intent(
                        this,
                        MainActivity.class
                );

        PendingIntent openAppPendingIntent =
                PendingIntent.getActivity(
                        this,
                        103,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )

                        .setSmallIcon(
                                R.drawable.ic_music
                        )

                        .setContentTitle(title)

                        .setContentText(artist)

                        .setSubText("Music Player")

                        .setContentIntent(
                                openAppPendingIntent
                        )

                        .setOngoing(true)

                        .setOnlyAlertOnce(true)

                        .setVisibility(
                                NotificationCompat.VISIBILITY_PUBLIC
                        )

                        .addAction(
                                R.drawable.ic_prev,
                                "Previous",
                                previousPendingIntent
                        )

                        .addAction(
                                manager.getPlayer().isPlaying()
                                        ? R.drawable.ic_pause
                                        : R.drawable.ic_play,
                                manager.getPlayer().isPlaying()
                                        ? "Pause"
                                        : "Play",
                                playPausePendingIntent
                        )

                        .addAction(
                                R.drawable.ic_next,
                                "Next",
                                nextPendingIntent
                        );

        if (cover != null) {

            builder.setLargeIcon(cover);
        }

        return builder.build();
    }

    public void updateNotification() {

        NotificationManager notificationManager =
                getSystemService(
                        NotificationManager.class
                );

        notificationManager.notify(
                NOTIFICATION_ID,
                buildNotification()
        );
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        updateNotification();

        return START_STICKY;
    }

    private final Player.Listener playerListener =
            new Player.Listener() {

                @Override
                public void onMediaItemTransition(
                        MediaItem mediaItem,
                        int reason
                ) {

                    updateNotification();
                }

                @Override
                public void onIsPlayingChanged(
                        boolean isPlaying
                ) {

                    updateNotification();
                }
            };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}