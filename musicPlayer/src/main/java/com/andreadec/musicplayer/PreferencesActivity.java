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

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xmlpull.v1.*;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.preference.*;
import android.preference.Preference.*;
import android.support.v7.app.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.andreadec.musicplayer.models.*;

public class PreferencesActivity extends AppCompatActivity {
	private final static String DEFAULT_IMPORTEXPORT_FILENAME = Environment.getExternalStorageDirectory() + "/musicplayer_info.xml";
	
	private boolean needsRestart;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        PreferencesFragment preferencesFragment = new PreferencesFragment();
        fragmentTransaction.replace(android.R.id.content, preferencesFragment);
        fragmentTransaction.commit();
    }

    public static class PreferencesFragment extends PreferenceFragment implements OnPreferenceClickListener, OnPreferenceChangeListener {
        private SharedPreferences preferences;
        private Preference preferenceAbout, preferenceImport, preferenceExport, preferencePodcastsDirectory;
        private Preference preferenceDisableLockScreen, preferenceEnableGestures, preferenceShowPlaybackControls;
        private Preference preferenceRescanBaseFolder;
        private PreferencesActivity activity;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            activity = (PreferencesActivity)getActivity();

            preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

            preferenceAbout = findPreference("about");
            preferenceImport = findPreference("import");
            preferenceExport = findPreference("export");
            preferencePodcastsDirectory = findPreference("podcastsDirectory");

            preferenceAbout.setOnPreferenceClickListener(this);
            preferenceImport.setOnPreferenceClickListener(this);
            preferenceExport.setOnPreferenceClickListener(this);
            preferencePodcastsDirectory.setOnPreferenceClickListener(this);

            preferenceDisableLockScreen = findPreference("disableLockScreen");
            preferenceEnableGestures = findPreference("enableGestures");
            preferenceShowPlaybackControls = findPreference("showPlaybackControls");
            preferenceDisableLockScreen.setOnPreferenceChangeListener(this);
            preferenceEnableGestures.setOnPreferenceChangeListener(this);
            preferenceShowPlaybackControls.setOnPreferenceChangeListener(this);

            preferenceRescanBaseFolder = findPreference("rescanBaseFolder");
            preferenceRescanBaseFolder.setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if(preference.equals(preferenceAbout)) {
                startActivity(new Intent(activity, AboutActivity.class));
            } else if(preference.equals(preferenceImport)) {
                activity.doImport();
            } else if(preference.equals(preferenceExport)) {
                activity.doExport();
            } else if(preference.equals(preferencePodcastsDirectory)) {
                String podcastsDirectory = preferences.getString(Constants.PREFERENCE_PODCASTSDIRECTORY, null);
                if(podcastsDirectory==null || podcastsDirectory.equals("")) {
                    podcastsDirectory = Podcast.DEFAULT_PODCASTS_PATH;
                }
                DirectoryChooserDialog chooser = new DirectoryChooserDialog(activity, podcastsDirectory, new DirectoryChooserDialog.OnFileChosen() {
                    @Override
                    public void onFileChosen(String directory) {
                        if(directory==null) return;
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(Constants.PREFERENCE_PODCASTSDIRECTORY, directory);
                        editor.apply();
                    }
                });
                chooser.show();
            } else if(preference.equals(preferenceRescanBaseFolder)) {
                rescanBaseFolder();
            }
            return false;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if(preference.equals(preferenceDisableLockScreen) || preference.equals(preferenceEnableGestures) || preference.equals(preferenceShowPlaybackControls)) {
                activity.needsRestart = true;
            }
            return true;
        }

        private void rescanBaseFolder() {
            String baseFolder = preferences.getString(Constants.PREFERENCE_BASEFOLDER, Constants.DEFAULT_BASEFOLDER);
            if(baseFolder==null) {
                Toast.makeText(activity, R.string.baseFolderNotSetTitle, Toast.LENGTH_LONG).show();
                return;
            }
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
            } else {
                //activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
                ArrayList<String> files = new ArrayList<>();
                Stack<File> tmp = new Stack<>();
                tmp.push(new File(baseFolder));
                while(!tmp.isEmpty()) {
                    File file = tmp.pop();
                    if(file.isDirectory()) {
                        for(File f : file.listFiles()) {
                            tmp.push(f);
                        }
                    } else {
                        files.add(file.toString());
                    }
                }
                MediaScannerConnection.scanFile(activity, files.toArray(new String[0]), null, null);
            }
            Toast.makeText(activity, R.string.rescanStarted, Toast.LENGTH_SHORT).show();
        }
    }
	
	@Override
	public void onBackPressed() {
		close();
	}

	private void close() {
		final Intent intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if(needsRestart) {
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		}
		startActivity(intent);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			close();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void doImport() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.importMsg);
		builder.setMessage(getResources().getString(R.string.importConfirm, DEFAULT_IMPORTEXPORT_FILENAME));
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				doImport(DEFAULT_IMPORTEXPORT_FILENAME);
			}
		});
		builder.setNegativeButton(R.string.no, null);
		builder.show();
	}
	
	private void doImport(String filename) {
		if(filename==null) return;
		Log.i("Import file", filename);
		File file = new File(filename.replace("file://", ""));
		
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(file);
			doc.getDocumentElement().normalize();
	
			NodeList radios = doc.getElementsByTagName("radio");
			for(int i=0; i<radios.getLength(); i++) {
				Element radio = (Element)radios.item(i);
				String url = radio.getAttribute("url");
				String name = radio.getAttribute("name");
				if(url==null || url.equals("")) continue;
				if(name==null || name.equals("")) name = url;
				Radio.addRadio(new Radio(url, name));
			}
			
			NodeList podcasts = doc.getElementsByTagName("podcast");
			for(int i=0; i<podcasts.getLength(); i++) {
				Element podcast = (Element)podcasts.item(i);
				String url = podcast.getAttribute("url");
				String name = podcast.getAttribute("name");
				byte[] image = Base64.decode(podcast.getAttribute("image"), Base64.DEFAULT);
				if(url==null || url.equals("")) continue;
				if(name==null || name.equals("")) name = url;
				Podcast.addPodcast(this, url, name, image);
			}
			
			Toast.makeText(this, R.string.importSuccess, Toast.LENGTH_LONG).show();
		} catch(Exception e) {
			Toast.makeText(this, R.string.importError, Toast.LENGTH_LONG).show();
			Log.e("WebRadioAcitivity", "doImport", e);
		}
	}
	
	private void doExport() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.export);
		builder.setMessage(getResources().getString(R.string.exportConfirm, DEFAULT_IMPORTEXPORT_FILENAME));
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				doExport(DEFAULT_IMPORTEXPORT_FILENAME);
			}
		});
		builder.setNegativeButton(R.string.no, null);
		builder.show();
	}
	
	private void doExport(String filename) {
		ArrayList<Radio> radios = Radio.getRadios();
		ArrayList<Podcast> podcasts = Podcast.getPodcasts();
		
		File file = new File(filename);
		try {
			FileOutputStream fos = new FileOutputStream(file);
			XmlSerializer serializer = Xml.newSerializer();
			serializer.setOutput(fos, "UTF-8");
	        serializer.startDocument(null, true);
	        serializer.startTag(null, "info");
	        
	        serializer.startTag(null, "radios");
	        for(Radio radio : radios) {
	        	serializer.startTag(null, "radio");
	        	serializer.attribute(null, "url", radio.getUrl());
	        	serializer.attribute(null, "name", radio.getName());
		        serializer.endTag(null, "radio");
	        }
	        serializer.endTag(null, "radios");
	        
	        
	        serializer.startTag(null, "podcasts");
	        for(Podcast podcast : podcasts) {
	        	serializer.startTag(null, "podcast");
	        	serializer.attribute(null, "url", podcast.getUrl());
	        	serializer.attribute(null, "name", podcast.getName());
	        	serializer.attribute(null, "image", Base64.encodeToString(podcast.getImageBytes(), Base64.DEFAULT));
	        	serializer.endTag(null, "podcast");
	        }
	        serializer.endTag(null, "podcasts");
	        
	        serializer.endTag(null, "info");
	        serializer.endDocument();
	        serializer.flush();
			fos.close();
			
			Toast.makeText(this, R.string.exportSuccess, Toast.LENGTH_LONG).show();
		} catch(Exception e) {
			Toast.makeText(this, R.string.exportError, Toast.LENGTH_LONG).show();
			Log.e("WebRadioAcitivity", "doExport", e);
		}
	}
}
