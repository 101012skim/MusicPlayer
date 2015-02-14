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

package com.andreadec.musicplayer;

import android.app.*;
import android.content.*;
import android.os.AsyncTask;
import android.preference.*;
import android.view.*;
import android.widget.*;
import com.andreadec.musicplayer.models.*;
import java.io.*;
import java.util.*;

public class AddToPlaylistDialog {
    public static void showDialog(final Activity activity, final Object item) {
        LayoutInflater inflater = activity.getLayoutInflater();

        ArrayList<Playlist> playlists = Playlists.getPlaylists();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.addToPlaylist);
        ListView list = (ListView)inflater.inflate(R.layout.add_to_playlist, null, false);
        builder.setView(list);
        final AlertDialog dialog = builder.create();
        final ArrayAdapter<Playlist> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, android.R.id.text1, playlists);
        list.setAdapter(adapter);

        View header = inflater.inflate(android.R.layout.simple_list_item_1, null, false);
        TextView headerText = (TextView)header.findViewById(android.R.id.text1);
        headerText.setText(R.string.newPlaylist);
        list.addHeaderView(header);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if(position==0) {
                    addToNewPlaylist(activity, item);
                } else {
                    Playlist playlist = adapter.getItem(position - 1);
                    addToPlaylist(activity, playlist, item);
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private static void addToPlaylist(final Context context, final Playlist playlist, final Object item) {
        if (item instanceof File) {
            new AddFolderToPlaylistTask(context, playlist, (File) item).execute();
        } else {
            playlist.addSong((BrowserSong) item);
        }
    }

    private static void addToNewPlaylist(final Activity activity, final Object item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.newPlaylist);
        final View view = activity.getLayoutInflater().inflate(R.layout.layout_editplaylist, null);
        builder.setView(view);

        final EditText editTextName = (EditText)view.findViewById(R.id.editTextPlaylistName);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String name = editTextName.getText().toString();
                if(name==null || name.equals("")) {
                    Utils.showMessageDialog(activity, R.string.error, R.string.errorPlaylistName);
                    return;
                }
                Playlist playlist = Playlists.addPlaylist(name);
                addToPlaylist(activity, playlist, item);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private static class AddFolderToPlaylistTask extends AsyncTask<Void, Void, Void> {
        private Context context;
        private ProgressDialog progressDialog;
        private Playlist playlist;
        private File folder;
        private SharedPreferences preferences;

        public AddFolderToPlaylistTask(Context context, Playlist playlist, File folder) {
            this.context = context;
            this.playlist = playlist;
            this.folder = folder;
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(context.getResources().getString(R.string.addingSongsToPlaylist));
            progressDialog.show();
        }
        @Override
        protected Void doInBackground(Void... params) {
            List<BrowserSong> songs = BrowserDirectory.getSongsInDirectory(folder, preferences.getString(Constants.PREFERENCE_SONGSSORTINGMETHOD, Constants.DEFAULT_SONGSSORTINGMETHOD), null);
            for(BrowserSong song : songs) {
                playlist.addSong(song);
            }
            return null;
        }
        @Override
        protected void onPostExecute(final Void success) {
            if(progressDialog.isShowing()) progressDialog.dismiss();
        }
    }
}
