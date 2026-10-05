package com.example.musicplayer;
import android.graphics.Bitmap;
import android.net.Uri;
public class Song {

    private final long id;
    private final String title;
    private final String artist;
    private final String album;
    private final String genre;
    private String year;
    private String duration;

    private final Uri uri;
    private final Bitmap cover;


    public Song(
            long id,
            String title,
            String artist,
            String album,
            String genre,
            String year,
            String duration,
            Uri uri,
            Bitmap cover) {

        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.year = year;
        this.duration = duration;

        this.uri = uri;
        this.cover = cover;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getGenre() {
        return genre;
    }

    public String getYear() { return year; }

    public String getDuration() { return duration; }

    public Uri getUri() {
        return uri;
    }

    public Bitmap getCover() {
        return cover;
    }
}