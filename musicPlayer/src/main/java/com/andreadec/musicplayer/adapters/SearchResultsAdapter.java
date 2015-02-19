/*
 * Copyright 2015 Andrea De Cesare
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andreadec.musicplayer.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.andreadec.musicplayer.*;
import com.andreadec.musicplayer.models.*;

import java.util.ArrayList;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.SearchResultsViewHolder> {
    private SearchActivity activity;
    private ArrayList<BrowserSong> songs;
    private LayoutInflater inflater;
    private ImagesCache imagesCache;

    public SearchResultsAdapter(SearchActivity activity, ArrayList<BrowserSong> songs) {
        this.activity = activity;
        this.songs = songs;
        inflater = activity.getLayoutInflater();
        imagesCache = ((MusicPlayerApplication)activity.getApplication()).imagesCache;
    }

    @Override
    public SearchResultsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.song_item, null);
        SearchResultsViewHolder viewHolder = new SearchResultsViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(SearchResultsViewHolder holder, int position) {
        final BrowserSong song = songs.get(position);
        holder.song = song;
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());
        holder.image.setImageResource(R.drawable.audio);
        imagesCache.getImageAsync(song, holder.image);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public class SearchResultsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView artist;
        public TextView title;
        public ImageView image;
        public ImageButton menu;
        private BrowserSong song;

        public SearchResultsViewHolder(View view) {
            super(view);
            title = (TextView)view.findViewById(R.id.textViewSongItemTitle);
            artist = (TextView)view.findViewById(R.id.textViewSongItemArtist);
            image = (ImageView)view.findViewById(R.id.imageViewItemImage);
            menu = (ImageButton)view.findViewById(R.id.buttonMenu);
            view.setOnClickListener(this);
            menu.setOnClickListener(this);
            menu.setFocusable(false);
        }

        @Override
        public void onClick(View view) {
            if(view.equals(menu)) {
                AddToPlaylistDialog.showDialog(activity, song);
            } else {
                activity.songSelected(song);
            }
        }
    }
}
