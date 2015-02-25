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

import android.support.v7.widget.*;
import android.view.*;
import com.andreadec.musicplayer.*;
import com.andreadec.musicplayer.models.*;
import com.andreadec.musicplayer.viewholders.*;
import java.io.File;
import java.util.ArrayList;

public class MusicPlayerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private MainActivity activity;
    private ArrayList<Object> items;
    private LayoutInflater inflater;
    private ImagesCache imagesCache;
    private PlayableItem playingItem;
    private ListsClickListener clickListener;
    private View emptyView;

    private final static int TYPE_HEADER=0, TYPE_DIRECTORY=1, TYPE_SONG=2, TYPE_PLAYLIST=3, TYPE_RADIO=4, TYPE_PODCAST=5, TYPE_PODCAST_EPISODE=6;

    public MusicPlayerAdapter(MainActivity activity, ArrayList<Object> items, PlayableItem playingItem, View emptyView, ListsClickListener clickListener) {
        this.activity = activity;
        this.items = items;
        this.playingItem = playingItem;
        this.emptyView = emptyView;
        this.clickListener = clickListener;
        inflater = activity.getLayoutInflater();
        imagesCache = ((MusicPlayerApplication)activity.getApplication()).imagesCache;
        checkEmpty();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if(items.get(position) instanceof String) return TYPE_HEADER;
        else if(item instanceof File) return TYPE_DIRECTORY;
        else if(item instanceof BrowserSong || item instanceof PlaylistSong) return TYPE_SONG;
        else if(item instanceof Radio) return TYPE_RADIO;
        else if(item instanceof Podcast) return TYPE_PODCAST;
        else if(item instanceof PodcastEpisode) return TYPE_PODCAST_EPISODE;
        else if(item instanceof Playlist) return TYPE_PLAYLIST;
        return -1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_HEADER:
                return new HeaderViewHolder(inflater.inflate(R.layout.list_header, null), clickListener);
            case TYPE_DIRECTORY:
                return new DirectoryViewHolder(inflater.inflate(R.layout.folder_item, null), activity, clickListener);
            case TYPE_SONG:
                return new SongViewHolder(inflater.inflate(R.layout.song_item, null), imagesCache, clickListener);
            case TYPE_PLAYLIST:
                return new PlaylistViewHolder(inflater.inflate(R.layout.playlist_item, null), activity, clickListener);
            case TYPE_RADIO:
                return new RadioViewHolder(inflater.inflate(R.layout.radio_item, null), activity, clickListener);
            case TYPE_PODCAST:
                return new PodcastViewHolder(inflater.inflate(R.layout.folder_item, null), activity, clickListener);
            case TYPE_PODCAST_EPISODE:
                return new PodcastEpisodeViewHolder(inflater.inflate(R.layout.podcast_item, null), activity, clickListener);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if(holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).update((String)item);
        } else if(holder instanceof DirectoryViewHolder) {
            ((DirectoryViewHolder) holder).update((File)item);
        } else if(holder instanceof SongViewHolder) {
            ((SongViewHolder) holder).update((PlayableItem)item, playingItem);
        } else if(holder instanceof RadioViewHolder) {
            ((RadioViewHolder) holder).update((Radio)item, (Radio)playingItem);
        } else if(holder instanceof PodcastViewHolder) {
            ((PodcastViewHolder) holder).update((Podcast)item);
        } else if(holder instanceof PodcastEpisodeViewHolder) {
            ((PodcastEpisodeViewHolder) holder).update((PodcastEpisode)item, (PodcastEpisode)playingItem);
        } else if(holder instanceof PlaylistViewHolder) {
            ((PlaylistViewHolder) holder).update((Playlist)item);
        }
    }

    public int getPlayableItemPosition(PlayableItem item) {
        return items.indexOf(item);
    }

    public void swapItems(int from, int to) {
        Object item = items.remove(from);
        items.add(to, item);
        notifyItemMoved(from, to);
    }

    public Object deleteItem(int position) {
        Object item = items.remove(position);
        notifyItemRemoved(position);
        return item;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void checkEmpty() {
        if(emptyView!=null) {
            if(getItemCount()==0 || (getItemCount()==1 && getItemViewType(0)==TYPE_HEADER)) {
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
            }
        }
    }
}
