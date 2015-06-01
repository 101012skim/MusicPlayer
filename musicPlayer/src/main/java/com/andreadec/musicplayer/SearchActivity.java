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

package com.andreadec.musicplayer;

import java.io.File;
import java.util.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.view.View.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.andreadec.musicplayer.adapters.*;
import com.andreadec.musicplayer.database.*;
import com.andreadec.musicplayer.models.*;

public class SearchActivity extends ActionBarActivity implements OnClickListener, OnKeyListener {
	private EditText editTextSearch;
	private ImageButton buttonSearch;
	private RecyclerView recyclerViewSearch;
	private SharedPreferences preferences;
	private MusicPlayerApplication application;
	private String lastSearch;
	private InputMethodManager inputMethodManager;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
		inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        
        if(preferences.getBoolean(Constants.PREFERENCE_SHOWHELPOVERLAYINDEXING, true) && preferences.getString(Constants.PREFERENCE_BASEFOLDER, "/").equals("/")) {
        	final FrameLayout frameLayout = new FrameLayout(this);
        	LayoutInflater layoutInflater = getLayoutInflater();
        	layoutInflater.inflate(R.layout.activity_search, frameLayout);
        	layoutInflater.inflate(R.layout.layout_helpoverlay_indexing, frameLayout);
        	final View overlayView = frameLayout.getChildAt(1);
        	overlayView.setOnClickListener(new OnClickListener() {
				@Override public void onClick(View v) {
					frameLayout.removeView(overlayView);
					SharedPreferences.Editor editor = preferences.edit();
					editor.putBoolean("showHelpOverlayIndexing", false);
					editor.commit();
				}
             	});
        	setContentView(frameLayout);
        } else {
        	setContentView(R.layout.activity_search);
        }
        
        editTextSearch = (EditText)findViewById(R.id.editTextSearch);
        editTextSearch.setOnKeyListener(this);
		editTextSearch.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean hasFocus) {
				if(!hasFocus) {
					inputMethodManager.hideSoftInputFromWindow(editTextSearch.getWindowToken(), 0);
				}
			}
		});
        buttonSearch = (ImageButton)findViewById(R.id.buttonSearch);
        buttonSearch.setOnClickListener(this);
        recyclerViewSearch = (RecyclerView)findViewById(R.id.recyclerViewSearch);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerViewSearch.setLayoutManager(llm);
        
        application = (MusicPlayerApplication)getApplication();
        lastSearch = application.getLastSearch();
        
        setResult(0, getIntent());
	}

	@Override
	public void onResume() {
		super.onResume();
		editTextSearch.requestFocus();
		//inputMethodManager.showSoftInput(editTextSearch, InputMethodManager.SHOW_FORCED);
		inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }
	
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
		if(lastSearch==null || lastSearch.equals("")) {
			menu.findItem(R.id.menu_repeatLastSearch).setVisible(false);
        }
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_repeatLastSearch:
			if(lastSearch==null) return true;
			editTextSearch.setText(lastSearch);
			search();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onClick(View view) {
		if(view.equals(buttonSearch)) {
			editTextSearch.clearFocus();
			search();
		}
	}
	
	private void search() {
		String text = editTextSearch.getText().toString();
		search(text);
		application.setLastSearch(text);
	}
	
	private void search(String str) {
		str = str.replace("\"", "");
		str = str.trim();
		ArrayList<BrowserSong> results = new ArrayList<BrowserSong>();
		
		SongsDatabase songsDatabase = new SongsDatabase();
		SQLiteDatabase db = songsDatabase.getWritableDatabase();
		
		Cursor cursor = db.rawQuery("SELECT uri, artist, title, trackNumber, hasImage FROM Songs WHERE artist LIKE \"%"+str+"%\" OR title LIKE \"%"+str+"%\"", null);
		while(cursor.moveToNext()) {
			String uri = cursor.getString(0);
			String artist = cursor.getString(1);
			String title = cursor.getString(2);
        	Integer trackNumber = cursor.getInt(3);
        	if(trackNumber==-1) trackNumber=null;
        	boolean hasImage = cursor.getInt(4)==1;
            if(new File(uri).exists()) {
                BrowserSong song = new BrowserSong(uri, artist, title, trackNumber, hasImage, null);
                results.add(song);
            } else {
                deleteSongFromCache(uri);
            }
        }
		db.close();
		
		if(results.size()==0) {
			Utils.showMessageDialog(this, R.string.noResultsFoundTitle, R.string.noResultsFoundMessage);
		} else {
			recyclerViewSearch.setAdapter(new SearchResultsAdapter(this, results));
		}
	}
	
	private void deleteSongFromCache(String uri) {
		SongsDatabase songsDatabase = new SongsDatabase();
		SQLiteDatabase db = songsDatabase.getWritableDatabase();
		db.delete("Songs", "uri=\""+uri+"\"", null);
		db.close();
	}

    public void songSelected(BrowserSong song) {
        Intent intent = getIntent();
        intent.putExtra("song", song);
        File songFile = new File(song.getUri());
        if(!songFile.exists()) {
            Utils.showMessageDialog(this, R.string.notFound, R.string.songNotFound);
            deleteSongFromCache(song.getUri());
            return;
        }
        setResult(1, intent);
        finish();
    }

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		// Manage "enter" key on keyboard
		if(event.getAction()==KeyEvent.ACTION_DOWN && keyCode==KeyEvent.KEYCODE_ENTER) {
			search();
			return true;
		}
		return false;
	}
}
