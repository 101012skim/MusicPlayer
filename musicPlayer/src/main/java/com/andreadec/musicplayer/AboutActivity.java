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

import android.content.pm.PackageManager.*;
import android.content.res.*;
import android.os.*;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.method.*;
import android.widget.*;

public class AboutActivity extends ActionBarActivity {
	private TextView textViewAbout;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);       
        setContentView(R.layout.activity_about);
        textViewAbout = (TextView)findViewById(R.id.textViewAbout);
        textViewAbout.setMovementMethod(LinkMovementMethod.getInstance());
        Resources resources = getResources();
        
        String version = "";
        try {
			version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {}
        
        String about = "<h1>"+resources.getString(R.string.app_name)+"</h1>";
        about += "<p>"+resources.getString(R.string.version, version)+"</p>";
        about += "<p>&copy; 2012-2016 Andrea De Cesare</p>";
        about += "<p><a href=\"https://github.com/andreadec/MusicPlayer\">https://github.com/andreadec/MusicPlayer</a></p>";
        
        about += "<h2>&nbsp;</h2>";
        about += "<h2>"+resources.getString(R.string.license)+"</h2>";
        about += resources.getString(R.string.apacheLicense);
        
        about += "<h2>&nbsp;</h2>";
        about += "<h2>"+resources.getString(R.string.libraries)+"</h2>";
        about += "<p>"+resources.getString(R.string.librariesUsed)+"</p>";
        
        about += "<h5>Android Support Library</h5>";
        about += "<p>Copyright (c) 2005-2014, The Android Open Source Project<br>";
        about += resources.getString(R.string.apacheLicense);
        
        about += "<h5>"+resources.getString(R.string.artworks)+"</h5>";
        about += "<p>Some icons are from the <a href=\"http://tango.freedesktop.org/\">Tango Desktop Project</a>, released into the Public Domain.</p>";
        about += "<p>Some icons are from <a href=\"https://www.google.com/design/icons/\">Material icons</a>, licensed under Creative Commons Attribution 4.0 International license.</p>";
        about += "<p>Some UI elements were generated using <a href=\"http://android-holo-colors.com\">Android Holo Colors</a> by Jérôme Van Der Linden, licensed under Creative Commons Attribution 3.0 Unported License.</p>";

        about += "<h2>&nbsp;</h2>";
        about += "<h2>"+resources.getString(R.string.specialThanks)+"</h2>";
        about += "<p>Spierpa</p>";
        about += "<p>Matteo</p>";
        textViewAbout.setText(Html.fromHtml(about));
	}
}
