/*
 * Copyright 2015-2016 Andrea De Cesare
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

package com.andreadec.musicplayer.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andreadec.musicplayer.*;
import com.andreadec.musicplayer.models.*;

public class PodcastEpisodeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private MainActivity activity;
    private ListsClickListener clickListener;
    private PodcastEpisode episode;
    private TextView title, info, status, description;
    private ImageView image, imageStatus, menu;
    private View card;

    public PodcastEpisodeViewHolder(View view, MainActivity activity, ListsClickListener clickListener) {
        super(view);
        this.activity = activity;
        this.clickListener = clickListener;
        title = (TextView)view.findViewById(R.id.textViewPodcastTitle);
        info = (TextView)view.findViewById(R.id.textViewPodcastInfo);
        status = (TextView)view.findViewById(R.id.textViewPodcastStatus);
        description = (TextView)view.findViewById(R.id.textViewPodcastDescription);
        image = (ImageView)view.findViewById(R.id.imageViewItemImage);
        imageStatus = (ImageView)view.findViewById(R.id.imageViewPodcastStatus);
        card = view.findViewById(R.id.card);
        menu = (ImageButton)view.findViewById(R.id.buttonMenu);
        view.setOnClickListener(this);
    }

    public void update(final PodcastEpisode episode, PodcastEpisode currentEpisode) {
        this.episode = episode;
        title.setText(episode.getTitle());
        String duration = episode.getDuration();
        if(duration!=null) info.setText(duration);
        else info.setVisibility(View.GONE);
        status.setText(episode.getStatusString());
        switch(episode.getStatus()) {
            case PodcastEpisode.STATUS_NEW:
                imageStatus.setImageResource(R.drawable.accept);
                break;
            case PodcastEpisode.STATUS_DOWNLOADING:
                imageStatus.setImageResource(R.drawable.download);
                break;
            case PodcastEpisode.STATUS_DOWNLOADED:
                imageStatus.setImageResource(R.drawable.save);
                break;
            default:
                imageStatus.setImageDrawable(null);
        }
		if(episode.getDescription()!=null) {
			description.setText(episode.getDescription());
		}
        if(episode.equals(currentEpisode)) {
            card.setBackgroundResource(R.drawable.card_playing);
            image.setImageResource(R.drawable.play_orange);
        } else {
            card.setBackgroundResource(R.drawable.card);
            image.setImageResource(R.drawable.audio);
        }
        menu.setOnClickListener(this);
        menu.setFocusable(false);
    }

    @Override
    public void onClick(View view) {
        if(view.equals(menu)) {
            final PopupMenu popup = new PopupMenu(activity, menu);
            popup.getMenuInflater().inflate(R.menu.contextmenu_editdelete, popup.getMenu());
            popup.getMenu().removeItem(R.id.menu_edit);
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    clickListener.onPlayableItemMenuClick(episode, item.getItemId());
                    return true;
                }
            });
            popup.show();
        } else {
            clickListener.onPlayableItemClick(episode);
        }
    }
}
