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
import android.support.design.widget.*;
import android.support.v4.widget.*;
import android.view.*;
import android.widget.*;

class SecondaryNavigationManager implements NavigationView.OnNavigationItemSelectedListener {
    private MainActivity activity;
    private DrawerLayout drawerLayout;
    private MenuItem shuffle, repeat, repeatAll, bass, equalizer, shake;

    public SecondaryNavigationManager(MainActivity activity, DrawerLayout drawerLayout) {
        this.activity = activity;
        this.drawerLayout = drawerLayout;

        NavigationView navigationRight = (NavigationView)activity.findViewById(R.id.navigationRight);
        navigationRight.setNavigationItemSelectedListener(this);

        Menu menu = navigationRight.getMenu();
        shuffle = menu.findItem(R.id.shuffle);
        repeat = menu.findItem(R.id.repeat);
        repeatAll = menu.findItem(R.id.repeatAll);
        bass = menu.findItem(R.id.bass);
        equalizer = menu.findItem(R.id.equalizer);
        shake = menu.findItem(R.id.shake);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.shuffle:
                activity.musicService.setShuffle(!activity.musicService.getShuffle());
                update();
                break;
            case R.id.repeat:
                activity.musicService.setRepeat(!activity.musicService.getRepeat());
                update();
                break;
            case R.id.repeatAll:
                activity.musicService.setRepeatAll(!activity.musicService.getRepeatAll());
                update();
                break;
            case R.id.bass:
                bassBoostSettings();
                break;
            case R.id.equalizer:
                equalizerSettings();
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
        shuffle.setChecked(activity.musicService.getShuffle());
        repeat.setChecked(activity.musicService.getRepeat());
        repeatAll.setChecked(activity.musicService.getRepeatAll());
        bass.setChecked(activity.musicService.getBassBoostEnabled());
        equalizer.setChecked(activity.musicService.getEqualizerEnabled());
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

        SeekBar seekbar = (SeekBar)view.findViewById(R.id.seekBarBassBoostStrength);
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

    private void equalizerSettings() {
        if(!activity.musicService.getEqualizerAvailable()) {
            Utils.showMessageDialog(activity, R.string.error, R.string.errorEqualizer);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.equalizer);
        View view = activity.getLayoutInflater().inflate(R.layout.layout_equalizer, null);
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

        CheckBox checkBoxEqualizerEnabled = (CheckBox)view.findViewById(R.id.checkBoxEqualizerEnabled);
        checkBoxEqualizerEnabled.setChecked(activity.musicService.getEqualizerEnabled());
        checkBoxEqualizerEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                activity.musicService.toggleEqualizer();
                update();
            }
        });

        String[] availablePresets = activity.musicService.getEqualizerAvailablePresets();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, availablePresets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinnerEqualizerPreset = (Spinner)view.findViewById(R.id.spinnerEqualizerPreset);
        spinnerEqualizerPreset.setAdapter(adapter);
        spinnerEqualizerPreset.setSelection(activity.musicService.getEqualizerPreset());

        spinnerEqualizerPreset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activity.musicService.setEqualizerPreset(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        builder.show();
    }
}
