package com.example.musicplayer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.adapter.SongAdapter;
import com.example.musicplayer.utils.PlayerManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_AUDIO_PERMISSION = 100;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 101;
    private ImageView cover;
    private TextView title;

    private Button btnPlay;
    private Button btnNext;
    private Button btnPrev;
    private SeekBar seekBar;
    private TextView tvCurrentTime, tvTotalTime;
    private RecyclerView recyclerView;
    private PlayerManager manager;

    private final Handler handler = new Handler();

    private final List<Song> playlist = new ArrayList<>();
    private Runnable seekBarRunnable;

    private String formatDuration(String milliseconds) {

        if (milliseconds == null) {
            return "Неизвестно";
        }

        try {

            long ms =
                    Long.parseLong(milliseconds);

            long totalSeconds =
                    ms / 1000;

            long minutes =
                    totalSeconds / 60;

            long seconds =
                    totalSeconds % 60;

            return String.format(
                    "%02d:%02d",
                    minutes,
                    seconds
            );

        } catch (Exception e) {

            return "Неизвестно";
        }
    }

    private String valueOrUnknown(String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "Неизвестно";
        }

        return value;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();
        manager = PlayerManager.getInstance(this);
        manager.getPlayer().addListener(playerListener);
        if (checkAudioPermission()) {
            scanMusicFolder();
            loadMusic();
            checkAndRequestNotification();
        } else {
            requestAudioPermission();
        }

        setupButtons();
        setupSeekBar();
        startSeekBarUpdater();
        startMusicService();
    }

    private void initializeViews() {

        cover = findViewById(R.id.imgCover);
        title = findViewById(R.id.txtSong);
        btnPlay = findViewById(R.id.btnPlay);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );
    }

    private boolean checkAudioPermission() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            return checkSelfPermission(
                    Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED;

        } else {

            return checkSelfPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestAudioPermission() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.READ_MEDIA_AUDIO
                    },
                    REQUEST_AUDIO_PERMISSION
            );

        } else {

            requestPermissions(
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    REQUEST_AUDIO_PERMISSION
            );
        }
    }
    private void checkAndRequestNotification() {
        if (checkNotificationPermission()) {
            android.util.Log.d("MusicPlayer", "Уведомления разрешены");
        } else {
            requestNotification();
        }
    }

    private boolean checkNotificationPermission() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU)

            return checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        return true;
    }
    private void requestNotification() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    REQUEST_NOTIFICATION_PERMISSION
            );

        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == REQUEST_AUDIO_PERMISSION) {

            if (grantResults.length > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d(
                        "MusicPlayer",
                        "Разрешение получено"
                );
                loadMusic();
            } else {
                android.util.Log.e(
                        "MusicPlayer",
                        "Разрешение НЕ получено"
                );
            }
        }

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                android.util.Log.e("MusicPlayer", "Уведомления разрешены");

            } else {

                android.util.Log.e("MusicPlayer", "Уведомления запрещены");
            }
        }
    }

    private void loadMusic() {

        playlist.clear();

        Uri collection =
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;



        String[] projection = {

                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA
        };

        String selection =
                MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String sortOrder =
                MediaStore.Audio.Media.TITLE + " ASC";

        try (Cursor cursor =
                     getContentResolver().query(
                             collection,
                             projection,
                             selection,
                             null,
                             sortOrder
                     )) {

            if (cursor == null) {
                android.util.Log.e(
                        "MusicPlayer",
                        "MediaStore вернул NULL"
                );
                return;
            }

            android.util.Log.d(
                    "MusicPlayer",
                    "Найдено файлов: " + cursor.getCount()
            );

            int idColumn =
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media._ID
                    );

            while (cursor.moveToNext()) {

                long id = cursor.getLong(idColumn);

                Uri songUri =
                        ContentUris.withAppendedId(
                                collection,
                                id
                        );

                android.util.Log.d(
                        "MusicPlayer",
                        "Файл: " + songUri
                );

                Song song =
                        readSongMetadata(songUri);

                if (song != null) {

                    playlist.add(song);
                }
            }
        } catch (Exception e) {

            android.util.Log.e(
                    "MusicPlayer",
                    "Ошибка загрузки музыки",
                    e
            );
        }
        android.util.Log.d(
                "MusicPlayer",
                "В playlist: " + playlist.size()
        );
        manager.setPlaylist(playlist);

        recyclerView.setAdapter(
                new SongAdapter(

                        playlist,

                        // Обычное нажатие
                        position -> {

                            manager.playTrack(position);

                            updateUI();

                            btnPlay.setText("PAUSE");
                        },

                        // Долгое нажатие
                        song -> {

                            showSongInfoDialog(song);
                        }
                )
        );

        if (!playlist.isEmpty()) {

            updateUI();
        } else { title.setText("Музыку нету");}
    }

    private void scanMusicFolder() {

        File musicDir =
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MUSIC
                );

        if (!musicDir.exists()) {
            return;
        }

        File[] files =
                musicDir.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {

            if (file.isFile()) {

                String name =
                        file.getName().toLowerCase();

                if (name.endsWith(".mp3") ||
                        name.endsWith(".wav") ||
                        name.endsWith(".m4a") ||
                        name.endsWith(".flac")) {

                    MediaScannerConnection.scanFile(
                            this,
                            new String[]{
                                    file.getAbsolutePath()
                            },
                            null,
                            (path, uri) -> {

                                android.util.Log.d(
                                        "MusicPlayer",
                                        "Просканирован: "
                                                + path
                                );

                                runOnUiThread(() ->
                                        loadMusic()
                                );
                            }
                    );
                }
            }
        }
    }

    private Song readSongMetadata(Uri uri) {

        MediaMetadataRetriever retriever =
                new MediaMetadataRetriever();

        try {
            retriever.setDataSource(
                    this,
                    uri
            );

            String title =
                    retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_TITLE
                    );

            String artist =
                    retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_ARTIST
                    );

            String album =
                    retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_ALBUM
                    );

            String genre =
                    retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_GENRE
                    );
            String year =
                    retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_YEAR
                    );

            String duration =
                    retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION
                    );

            byte[] image =
                    retriever.getEmbeddedPicture();

            Bitmap coverBitmap = null;

            if (image != null) {

                coverBitmap =
                        BitmapFactory.decodeByteArray(
                                image,
                                0,
                                image.length
                        );
            }

            if (title == null ||
                    title.trim().isEmpty()) {

                title = "Неизвестный трек";
            }

            if (artist == null ||
                    artist.trim().isEmpty()) {

                artist = "Неизвестный исполнитель";
            }

            if (album == null ||
                    album.trim().isEmpty()) {

                album = "Неизвестный альбом";
            }

            if (genre == null ||
                    genre.trim().isEmpty()) {

                genre = "Неизвестный жанр";
            }

            return new Song(
                    uri.hashCode(),
                    title,
                    artist,
                    album,
                    genre,
                    year,
                    formatDuration(duration),
                    uri,
                    coverBitmap
            );

        } catch (Exception e) {

            android.util.Log.e(
                    "MusicPlayer",
                    "Ошибка чтения: " + uri,
                    e
            );
            return null;

        } finally {

            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    private void setupButtons() {

        btnPlay.setOnClickListener(v -> {

            boolean playing =
                    manager.playPause();

            if (playing) {

                btnPlay.setText("PAUSE");

            } else {

                btnPlay.setText("PLAY");
            }
        });

        btnNext.setOnClickListener(v -> {

            manager.next();

            updateUI();

            btnPlay.setText("PAUSE");
        });

        btnPrev.setOnClickListener(v -> {

            manager.prev();

            updateUI();

            btnPlay.setText("PAUSE");
        });
    }

    private void setupSeekBar() {

        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        if (fromUser) {

                            manager.getPlayer()
                                    .seekTo(progress);

                            tvCurrentTime.setText(
                                    formatTime(progress)
                            );
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar) {
                    }
                }
        );
    }

    private void startSeekBarUpdater() {

        seekBarRunnable =
                new Runnable() {

                    @Override
                    public void run() {

                        if (manager != null &&
                                manager.getPlayer() != null) {

                            long duration =
                                    manager.getPlayer()
                                            .getDuration();

                            long position =
                                    manager.getPlayer()
                                            .getCurrentPosition();

                            if (duration > 0) {

                                seekBar.setMax(
                                        (int) duration
                                );

                                seekBar.setProgress(
                                        (int) position
                                );

                                tvCurrentTime.setText(
                                        formatTime(position)
                                );

                                tvTotalTime.setText(
                                        formatTime(duration)
                                );
                            }
                        }

                        handler.postDelayed(
                                this,
                                500
                        );
                    }
                };

        handler.post(
                seekBarRunnable
        );
    }

    private void updateUI() {

        Song song = manager.getCurrentSong();

        if (song == null) {
            return;
        }

        String displayTitle =
                song.getTitle();

        if (song.getArtist() != null &&
                !song.getArtist().isEmpty()) {

            displayTitle =
                    song.getArtist()
                            + " — "
                            + song.getTitle();
        }

        title.setText(displayTitle);

        if (song.getCover() != null) {

            cover.setImageBitmap(
                    song.getCover()
            );

        } else {

            cover.setImageResource(
                    R.drawable.ic_music
            );
        }

        tvCurrentTime.setText(
                "00:00"
        );

        long duration =
                manager.getPlayer()
                        .getDuration();

        if (duration > 0) {

            tvTotalTime.setText(
                    formatTime(duration)
            );
        } else {

            tvTotalTime.setText(
                    "00:00"
            );
        }

        seekBar.setProgress(0);

        title.setSelected(true);
    }

    private String formatTime(
            long milliseconds) {

        if (milliseconds < 0) {
            return "00:00";
        }

        int totalSeconds =
                (int) (milliseconds / 1000);

        int minutes =
                (totalSeconds / 60) % 60;

        int seconds =
                totalSeconds % 60;

        return String.format(
                "%02d:%02d",
                minutes,
                seconds
        );
    }

    private final Player.Listener playerListener =
            new Player.Listener() {

                @Override
                public void onMediaItemTransition(
                        MediaItem mediaItem,
                        int reason) {

                    runOnUiThread(() -> {

                        updateUI();

                        if (manager.getPlayer().isPlaying()) {
                            btnPlay.setText("PAUSE");
                        } else {
                            btnPlay.setText("PLAY");
                        }
                    });
                }

                @Override
                public void onIsPlayingChanged(
                        boolean isPlaying) {

                    runOnUiThread(() -> {

                        if (isPlaying) {
                            btnPlay.setText("PAUSE");
                        } else {
                            btnPlay.setText("PLAY");
                        }
                    });
                }
            };

    private void startMusicService() {

        Intent intent =
                new Intent(
                        this,
                        MusicService.class
                );

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            startForegroundService(intent);

        } else {

            startService(intent);
        }
    }

    private void showSongInfoDialog(Song song) {

        View view =
                getLayoutInflater().inflate(
                        R.layout.dialog_song_inf,
                        null
                );

        ImageView cover =
                view.findViewById(
                        R.id.dialogCover
                );

        TextView title =
                view.findViewById(
                        R.id.dialogTitle
                );

        TextView artist =
                view.findViewById(
                        R.id.dialogArtist
                );

        TextView album =
                view.findViewById(
                        R.id.dialogAlbum
                );

        TextView genre =
                view.findViewById(
                        R.id.dialogGenre
                );

        TextView year =
                view.findViewById(
                        R.id.dialogYear
                );

        TextView duration =
                view.findViewById(
                        R.id.dialogDuration
                );

        Button close =
                view.findViewById(
                        R.id.btnCloseDialog
                );

        if (song.getCover() != null) {
            cover.setImageBitmap(
                    song.getCover()
            );

        } else {
            cover.setImageResource(
                    R.drawable.ic_music
            );
        }


        title.setText(
                "Название: " +
                        valueOrUnknown(
                                song.getTitle()
                        )
        );

        artist.setText(
                "Исполнитель: " +
                        valueOrUnknown(
                                song.getArtist()
                        )
        );

        album.setText(
                "Альбом: " +
                        valueOrUnknown(
                                song.getAlbum()
                        )
        );

        genre.setText(
                "Жанр: " +
                        valueOrUnknown(
                                song.getGenre()
                        )
        );

        year.setText(
                "Год: " +
                        valueOrUnknown(
                                song.getYear()
                        )
        );

        duration.setText(
                "Длительность: " +
                        valueOrUnknown(
                                song.getDuration()
                        )
        );


        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(view)
                        .create();


        close.setOnClickListener(
                v -> dialog.dismiss()
        );

        dialog.show();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (seekBarRunnable != null) {

            handler.removeCallbacks(
                    seekBarRunnable
            );
        }
    }
}