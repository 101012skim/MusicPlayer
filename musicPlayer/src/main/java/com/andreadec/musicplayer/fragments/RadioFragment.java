/*
 * Copyright 2012-2015 Andrea De Cesare
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

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.andreadec.musicplayer.*;
import com.andreadec.musicplayer.adapters.*;
import com.andreadec.musicplayer.models.*;
import com.andreadec.musicplayer.viewholders.*;
import java.util.*;

public class RadioFragment extends MusicPlayerFragment {
    private ListsClickListener clickListener = new ListsClickListener() {
        @Override public void onHeaderClick() {}

        @Override
        public void onPlayableItemClick(PlayableItem item) {
            activity.playRadio((Radio) item);
        }

        @Override
        public void onPlayableItemMenuClick(PlayableItem item, int menuId) {
            switch(menuId) {
                case R.id.menu_edit:
                    editRadio((Radio)item);
                    break;
                case R.id.menu_delete:
                    deleteRadio((Radio)item);
                    break;
            }
        }

        @Override public void onCategoryClick(Object object) {}
        @Override public void onCategoryMenuClick(Object item, int menuId) {}
    };

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null) return null;
		View view = inflater.inflate(R.layout.layout_fragments, container, false);
		initialize(view);
        setFloatingButtonVisible(true);
		updateListView();
		return view;
	}
	
	@Override
	public void updateListView() {
        setEmptyViewText(R.string.noRadios);
		Radio playingRadio = null;
		if(activity.getCurrentPlayingItem() instanceof Radio) playingRadio = (Radio)activity.getCurrentPlayingItem();
        ArrayList<Object> items = new ArrayList<>();
        items.addAll(Radio.getRadios());
        recyclerView.setAdapter(new MusicPlayerAdapter(activity, items, playingRadio, emptyView, clickListener));
	}
	
	private void deleteRadio(final Radio radio) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(getResources().getString(R.string.deleteRadioConfirm));
		builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
		      public void onClick(DialogInterface dialog, int which) {
		    	  Radio.deleteRadio(radio);
		    	  updateListView();
		      }
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}
	
	private void editRadio(final Radio oldRadio) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		int title = oldRadio==null ? R.string.addRadio : R.string.edit;
		builder.setTitle(getResources().getString(title));
		final View view = getActivity().getLayoutInflater().inflate(R.layout.layout_editwebradio, null);
		builder.setView(view);
		
		final EditText editTextUrl = (EditText)view.findViewById(R.id.editTextUrl);
		final EditText editTextName = (EditText)view.findViewById(R.id.editTextName);
		if(oldRadio!=null) {
			editTextUrl.setText(oldRadio.getUrl());
			editTextName.setText(oldRadio.getName());
		}
		
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				String url = editTextUrl.getText().toString();
				String name = editTextName.getText().toString();
				if(url.equals("") || url.equals("http://")) {
					Toast.makeText(getActivity(), R.string.errorInvalidURL, Toast.LENGTH_SHORT).show();
					return;
				}
	        	if(name.equals("")) name = url;
	        	
	        	if(oldRadio!=null) {
	        		Radio.deleteRadio(oldRadio);
	        	}
				
	        	Radio newRadio = new Radio(url, name);
	        	Radio.addRadio(newRadio);
				updateListView();
			}
		});
		
		builder.setNegativeButton(R.string.cancel, null);
		AlertDialog dialog = builder.create();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dialog.show();
	}

	@Override
	public boolean onBackPressed() {
		return false;
	}

    @Override
	public void gotoPlayingItemPosition(PlayableItem playingItem) {
        int position = ((MusicPlayerAdapter)recyclerView.getAdapter()).getPlayableItemPosition(playingItem);
        layoutManager.scrollToPosition(position);
	}

    @Override
    public void onFloatingButtonClick() {
        editRadio(null);
    }
}
