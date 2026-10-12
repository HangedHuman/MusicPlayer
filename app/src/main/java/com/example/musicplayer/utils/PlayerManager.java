package com.example.musicplayer.utils;

import android.content.Context;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.musicplayer.Song;

import java.util.ArrayList;
import java.util.List;

public class PlayerManager {

    private static PlayerManager instance;

    private final ExoPlayer player;
    private List<Song> playlist = new ArrayList<>();

    private PlayerManager(Context context) {

        player = new ExoPlayer.Builder(
                context.getApplicationContext()
        ).build();
    }

    public static synchronized PlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new PlayerManager(context);
        }
        return instance;
    }

    public ExoPlayer getPlayer() {
        return player;
    }

    public void setPlaylist(List<Song> songs) {

        playlist = songs;

        player.clearMediaItems();

        for (Song song : playlist) {

            MediaMetadata metadata =
                    new MediaMetadata.Builder()
                            .setTitle(song.getTitle())
                            .setArtist(song.getArtist())
                            .setAlbumTitle(song.getAlbum())
                            .build();

            MediaItem item =
                    new MediaItem.Builder()
                            .setUri(song.getUri())
                            .setMediaMetadata(metadata)
                            .build();

            player.addMediaItem(item);
        }

        player.prepare();
    }

    public Song getCurrentSong() {
        if (playlist.isEmpty()) {
            return null;
        }

        int index = player.getCurrentMediaItemIndex();
        if (index < 0 || index >= playlist.size()) {
            return null;
        }
        return playlist.get(index);
    }

    public boolean playPause() {
        if (player.isPlaying()) {
            player.pause();
            return false;
        }
        else {
            player.play();
            return true;
        }
    }

    public void next() {
        if (playlist.isEmpty()) {
            return;
        }
        int current = player.getCurrentMediaItemIndex();
        int next = current + 1;
        if (next >= playlist.size()) {
            next = 0;
        }
        player.seekTo(next, 0);
        player.play();
    }

    public void prev() {
        if (playlist.isEmpty()) {
            return;
        }
        int current = player.getCurrentMediaItemIndex();
        int prev = current - 1;
        if (prev < 0) {
            prev = playlist.size() - 1;
        }
        player.seekTo(prev, 0);
        player.play();
    }
    public void playTrack(int position) {

        if (position < 0 || position >= playlist.size()) {
            return;
        }

        player.seekTo(position, 0);
        player.play();
    }
}
