/*
 * Copyright 2015-2019 Andrea De Cesare
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
import android.view.*;
import android.widget.*;

import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import static com.andreadec.musicplayer.MusicService.*;

class SecondaryNavigationManager implements NavigationView.OnNavigationItemSelectedListener {
    private MainActivity activity;
    private DrawerLayout drawerLayout;
    private MenuItem shuffle, repeat, repeatAll, bass, shake;

    public SecondaryNavigationManager(MainActivity activity, DrawerLayout drawerLayout) {
        this.activity = activity;
        this.drawerLayout = drawerLayout;

        NavigationView navigationRight = activity.findViewById(R.id.navigationRight);
        navigationRight.setNavigationItemSelectedListener(this);

        Menu menu = navigationRight.getMenu();
        shuffle = menu.findItem(R.id.shuffle);
        repeat = menu.findItem(R.id.repeat);
        repeatAll = menu.findItem(R.id.repeatAll);
        bass = menu.findItem(R.id.bass);
        shake = menu.findItem(R.id.shake);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        int currentPlayMode = activity.musicService.getPlayMode();
        switch (menuItem.getItemId()) {
            case R.id.shuffle:
                activity.musicService.setPlayMode(currentPlayMode==PLAY_MODE_SHUFFLE ? PLAY_MODE_NORMAL : PLAY_MODE_SHUFFLE);
                update();
                break;
            case R.id.repeat:
                activity.musicService.setPlayMode(currentPlayMode==PLAY_MODE_REPEAT_ONE ? PLAY_MODE_NORMAL : PLAY_MODE_REPEAT_ONE);
                update();
                break;
            case R.id.repeatAll:
                activity.musicService.setPlayMode(currentPlayMode==PLAY_MODE_REPEAT_ALL ? PLAY_MODE_NORMAL : PLAY_MODE_REPEAT_ALL);
                update();
                break;
            case R.id.bass:
                bassBoostSettings();
                break;
            case R.id.shake:
                activity.musicService.toggleShake();
                update();
                break;
        }
        drawerLayout.closeDrawers();
        return true;
    }

    public void update() {
        int currentPlayMode = activity.musicService.getPlayMode();
        shuffle.setChecked(currentPlayMode==PLAY_MODE_SHUFFLE);
        repeat.setChecked(currentPlayMode==PLAY_MODE_REPEAT_ONE);
        repeatAll.setChecked(currentPlayMode==PLAY_MODE_REPEAT_ALL);
        bass.setChecked(activity.musicService.getBassBoostEnabled());
        shake.setChecked(activity.musicService.isShakeEnabled());
    }

    private void bassBoostSettings() {
        if(!activity.musicService.getBassBoostAvailable()) {
            Utils.showMessageDialog(activity, R.string.error, R.string.errorBassBoost);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.bassBoost);
        View view = activity.getLayoutInflater().inflate(R.layout.layout_bassboost, null);
        builder.setView(view);

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                update();
            }
        });
        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                update();
            }
        });

        CheckBox checkBoxBassBoostEnable = (CheckBox)view.findViewById(R.id.checkBoxBassBoostEnabled);
        checkBoxBassBoostEnable.setChecked(activity.musicService.getBassBoostEnabled());
        checkBoxBassBoostEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                activity.musicService.toggleBassBoost();
                update();
            }
        });

        SeekBar seekbar = view.findViewById(R.id.seekBarBassBoostStrength);
        seekbar.setMax(1000);
        seekbar.setProgress(activity.musicService.getBassBoostStrength());
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    activity.musicService.setBassBoostStrength(seekBar.getProgress());
                }
            }
            @Override public void onStartTrackingTouch(SeekBar arg0) {}
            @Override public void onStopTrackingTouch(SeekBar arg0) {}
        });

        builder.show();
    }
}
