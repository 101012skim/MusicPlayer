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

package com.andreadec.musicplayer.models;

import java.io.*;
import java.util.*;
import android.content.*;
import android.database.*;
import android.preference.*;
import android.provider.*;

import com.andreadec.musicplayer.*;
import com.andreadec.musicplayer.filters.*;

public class BrowserDirectory {
	private File directory;
	private ArrayList<File> subdirs;
	private ArrayList<BrowserSong> songs;
	private SharedPreferences preferences;
	private static ContentResolver mediaResolver;

	private final static String[] projection = {
			MediaStore.Audio.Media.ARTIST,
			MediaStore.Audio.Media.TITLE,
			MediaStore.Audio.Media.TRACK,
			MediaStore.Audio.Media.DATA
	};
	
	public BrowserDirectory(File directory) {
		Context context = MusicPlayerApplication.getContext();
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		this.directory = directory;
		mediaResolver = context.getContentResolver();
		subdirs = getSubfoldersInDirectory(directory);
		songs = getSongsInDirectory(directory, preferences.getString(Constants.PREFERENCE_SONGSSORTINGMETHOD, Constants.DEFAULT_SONGSSORTINGMETHOD), this);
	}
	
	public File getDirectory() {
		return directory;
	}
	
	public ArrayList<File> getSubdirs() {
		return subdirs;
	}
	
	public ArrayList<BrowserSong> getSongs() {
		return songs;
	}

	public static ArrayList<BrowserSong> getSongsInDirectory(File directory, String sortingMethod, BrowserDirectory browserDirectory) {
		ArrayList<BrowserSong> songs = new ArrayList<>();

		String path = directory.getAbsolutePath();
		if(!path.endsWith("/")) path+="/";

		//String where = MediaStore.MediaColumns.DATA + " REGEXP \"" + path + "[^/]+\"";
        String where = MediaStore.MediaColumns.DATA + " LIKE \"" + path + "%\" AND " + MediaStore.MediaColumns.DATA + " NOT LIKE \"" + path + "%/%\"";
        Cursor musicCursor = mediaResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, where, null, getSortOrder(sortingMethod));
        int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int uriColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        int trackColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
        if(musicCursor!=null && musicCursor.moveToFirst()) {
			do {
				String title = musicCursor.getString(titleColumn);
				String artist = musicCursor.getString(artistColumn);
				String uri = musicCursor.getString(uriColumn);
				String trackNumber = musicCursor.getString(trackColumn);
				songs.add(new BrowserSong(uri, artist, title, trackNumber, browserDirectory));
			} while (musicCursor.moveToNext());
		}
		musicCursor.close();

		return songs;
	}

	private static String getSortOrder(String sortingMethod) {
        switch (sortingMethod) {
            case "nat":
                return MediaStore.Audio.Media.TRACK+","+MediaStore.Audio.Media.ARTIST+","+MediaStore.Audio.Media.TITLE;
            case "at":
                return MediaStore.Audio.Media.ARTIST+","+MediaStore.Audio.Media.TITLE;
            case "ta":
                return MediaStore.Audio.Media.TITLE+","+MediaStore.Audio.Media.ARTIST;
            case "f":
                return MediaStore.Audio.Media.DATA;
        }
		return null;
	}
	
	// Lists all the subfolders of a given directory
	private ArrayList<File> getSubfoldersInDirectory(File directory) {
		ArrayList<File> subfolders = new ArrayList<>();
		File files[] = directory.listFiles(new DirectoryFilter());
		for (File file : files) {
			subfolders.add(file);
		}
		Collections.sort(subfolders);
		return subfolders;
	}
}
