package com.example.musicplayer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;
import com.example.musicplayer.Song;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    public interface OnSongClickListener {
        void onSongClick(int position);
    }
    public interface OnSongLongClickListener {

        void onSongLongClick(Song song);
    }
    private final List<Song> songs;
    private final OnSongClickListener listener;
    private final OnSongLongClickListener longClickListener;

    public SongAdapter(List<Song> songs,OnSongClickListener listener,
                       OnSongLongClickListener longClickListener) {
        this.songs = songs;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_song,
                        parent,
                        false
                );
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull SongViewHolder holder,
            int position) {

        Song song = songs.get(position);

        holder.songName.setText(song.getTitle());
        holder.artistName.setText(song.getArtist());

        if (song.getCover() != null) {

            holder.songCover.setImageBitmap(
                    song.getCover()
            );

        } else {

            holder.songCover.setImageResource(
                    R.drawable.ic_music
            );
        }

        holder.itemView.setOnClickListener(v ->
                listener.onSongClick(position)
        );
        holder.itemView.setOnLongClickListener(v -> {

            int adapterPosition =
                    holder.getBindingAdapterPosition();

            if (adapterPosition !=
                    RecyclerView.NO_POSITION) {

                longClickListener.onSongLongClick(
                        songs.get(adapterPosition)
                );
            }

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class SongViewHolder
            extends RecyclerView.ViewHolder {

        ImageView songCover;
        TextView songName;
        TextView artistName;

        public SongViewHolder(View itemView) {

            super(itemView);

            songCover =
                    itemView.findViewById(
                            R.id.songCover
                    );

            songName =
                    itemView.findViewById(
                            R.id.songName
                    );

            artistName =
                    itemView.findViewById(
                            R.id.artistName
                    );
        }
    }
}
