/*
 * Copyright 2013-2015 Andrea De Cesare
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

package com.andreadec.musicplayer.fragments;

import android.content.*;
import android.os.*;
import android.preference.*;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.*;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import com.andreadec.musicplayer.*;
import com.andreadec.musicplayer.models.*;
import com.andreadec.musicplayer.ui.DragDropTouchListener;

public abstract class MusicPlayerFragment extends Fragment {
    protected RecyclerView recyclerView;
    protected LinearLayoutManager layoutManager;
    protected MainActivity activity;
    private FloatingActionButton floatingButton;
    protected SharedPreferences preferences;
    protected TextView emptyView;

	public abstract boolean onBackPressed(); // Return false if no action was executed
	public abstract void gotoPlayingItemPosition(PlayableItem playingItem);
	public abstract void updateListView();
    public abstract void onFloatingButtonClick();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (MainActivity)getActivity();
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    public void initialize(View view) {
        recyclerView = (RecyclerView)view.findViewById(R.id.recyclerView);
        floatingButton = (FloatingActionButton)activity.findViewById(R.id.floatingButton);
        floatingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onFloatingButtonClick();
            }
        });
        emptyView = (TextView)view.findViewById(R.id.emptyView);

        layoutManager = new LinearLayoutManager(activity);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
    }

    public void setEmptyViewText(int text) {
        emptyView.setText(text);
    }

    public void enableSort(View view, int handler, DragDropTouchListener.OnItemMovedListener listener) {
        ImageView overlay = (ImageView) view.findViewById(R.id.imageViewOverlay);
        recyclerView.addOnItemTouchListener(new DragDropTouchListener(recyclerView, overlay, R.id.layoutHeader, handler, listener));
    }

    public void setFloatingButtonImage(int res) {
        floatingButton.setImageResource(res);
    }

    public void setFloatingButtonVisible(boolean visible) {
        floatingButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
