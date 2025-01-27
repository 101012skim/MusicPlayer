/*
 * Copyright 2012-2019 Andrea De Cesare
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
import java.net.*;
import java.util.*;

import android.Manifest;
import android.graphics.*;
import android.media.*;
import android.os.*;
import android.preference.*;

import com.google.android.material.navigation.NavigationView;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.PermissionChecker;
import androidx.core.view.GestureDetectorCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.widget.SeekBar.*;

import com.andreadec.musicplayer.fragments.*;
import com.andreadec.musicplayer.models.*;
import com.andreadec.musicplayer.ui.*;

public class MainActivity extends AppCompatActivity implements OnClickListener, OnSeekBarChangeListener {
	private static final String TAG = "MainActivity";
	public final static int PAGE_BROWSER=0, PAGE_PLAYLISTS=1; //, PAGE_RADIOS=2, PAGE_PODCASTS=3;
	private final static int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST=1;
	private final static int SECOND_SEEKBAR_DURATION = 600000; // 10 minutes, in milliseconds

    private MusicPlayerApplication app;
	
	private TextView textViewArtist, textViewTitle, textViewTime;
    private CheckableImageButton imageButtonPlayPause;
	private ImageButton imageButtonPrevious, imageButtonNext, imageButtonShowSeekbar2;
	private SeekBar seekBar1, seekBar2;
	private ImageView imageViewSongImage;
	private ImageView imageViewNavigationHeader;
	public MusicService musicService;
	private ServiceConnection serviceConnection;
	private Intent serviceIntent;
	private BroadcastReceiver broadcastReceiver;
	private SharedPreferences preferences;
    private SecondaryNavigationManager secondaryNavigationManager;
	
	private DrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	
	private static final int POLLING_INTERVAL = 450; // Refresh time of the seekbar
	private boolean pollingThreadRunning; // true if thread is active, false otherwise
	private boolean startPollingThread = true;
	private boolean showRemainingTime = false;
	
	// Variables used to reduce computing on polling thread
	private boolean isLengthAvailable = false;
	private String songDurationString = "";

	private String[] pages;
    private MusicPlayerFragment currentFragment;
	private FragmentManager fragmentManager;
	
	private String intentFile;
	private BrowserSong searchSong;
	private int switchNavigationHeaderImageIndex = 0;

    public int screenSizeX, screenSizeY;
	
	
	/* Initializes the activity. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

		if (Build.VERSION.SDK_INT >= 23 && PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
			requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_REQUEST);
		}
        
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(preferences.getBoolean(Preferences.PREFERENCE_DISABLELOCKSCREEN, Preferences.DEFAULT_DISABLELOCKSCREEN)) {
        	getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD); // Disable lock screen for this activity
        }

        setContentView(R.layout.activity_main);

        app = (MusicPlayerApplication)getApplication();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenSizeX = size.x;
        screenSizeY = size.y;

        pages = new String[4];
        pages[PAGE_BROWSER] = getResources().getString(R.string.browser);
        pages[PAGE_PLAYLISTS] = getResources().getString(R.string.playlist);
        //pages[PAGE_RADIOS] = getResources().getString(R.string.radio);
        //pages[PAGE_PODCASTS] = getResources().getString(R.string.podcasts);
        fragmentManager = getSupportFragmentManager();
        
        
        // MAIN NAVIGATION DRAWER

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //setTitle(pages[app.currentPage]);
            }
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //setTitle(getResources().getString(R.string.app_name));
			}
		};
        drawerLayout.setDrawerListener(drawerToggle);



        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        NavigationView navigationView = findViewById(R.id.navigationView);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.navigationBrowser:
                        openPage(PAGE_BROWSER);
                        break;
                    case R.id.navigationPlaylist:
                        openPage(PAGE_PLAYLISTS);
                        break;
					case R.id.about:
						startActivity(new Intent(MainActivity.this, AboutActivity.class));
						break;
                        /*
                    case R.id.navigationPodcast:
                        openPage(PAGE_PODCASTS);
                        break;
                    case R.id.navigationRadio:
                        openPage(PAGE_RADIOS);
                        break;
                         */
                    case R.id.navigationPreferences:
                        startActivity(new Intent(MainActivity.this, PreferencesActivity.class));
                        break;
                    case R.id.navigationQuit:
                        quitApplication();
                        return true;
                }
                menuItem.setChecked(true);
                drawerLayout.closeDrawers();
                return true;
            }
        });


        // RIGHT NAVIGATION DRAWER
        secondaryNavigationManager = new SecondaryNavigationManager(this, drawerLayout);
        
    	textViewArtist = findViewById(R.id.textViewArtist);
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewTime = findViewById(R.id.textViewTime);
        imageViewSongImage = findViewById(R.id.imageViewSongImage);
        imageButtonPrevious = findViewById(R.id.imageButtonPrevious);
        imageButtonPlayPause = findViewById(R.id.imageButtonPlayPause);
        imageButtonNext = findViewById(R.id.imageButtonNext);
        seekBar1 = findViewById(R.id.seekBar1);
        seekBar2 = findViewById(R.id.seekBar2);
        imageButtonShowSeekbar2 = findViewById(R.id.imageButtonShowSeekbar2);
        
        imageButtonShowSeekbar2.setOnClickListener(this);
        imageButtonPrevious.setOnClickListener(this);
        imageButtonPlayPause.setOnClickListener(this);
        imageButtonNext.setOnClickListener(this);
        seekBar1.setOnSeekBarChangeListener(this);
        seekBar1.setClickable(false);
        seekBar2.setOnSeekBarChangeListener(this);
        textViewTime.setOnClickListener(this);
        
        serviceIntent = new Intent(this, MusicService.class);

        if(app.currentPage==-1) { // App just launched
            if (preferences.getBoolean(Preferences.PREFERENCE_OPENLASTPAGEONSTART, Preferences.DEFAULT_OPENLASTPAGEONSTART)) {
                openPage(preferences.getInt(Preferences.PREFERENCE_LASTPAGE, Preferences.DEFAULT_LASTPAGE));
            } else {
                openPage(PAGE_BROWSER);
            }
        } else { // App already open (this happens when screen is rotated)
            openPage(app.currentPage);
        }
        loadSongFromIntent();
        
        View layoutPlaybackControls = findViewById(R.id.layoutPlaybackControls);
        if(preferences.getBoolean(Preferences.PREFERENCE_ENABLEGESTURES, Preferences.DEFAULT_ENABLEGESTURES)) {
	        final GestureDetectorCompat gestureDetector = new GestureDetectorCompat(this, new PlayerGestureListener());
	        View layoutTop = findViewById(R.id.layoutTop);
	        layoutTop.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return gestureDetector.onTouchEvent(event);
				}
              });
	        if(preferences.getBoolean(Preferences.PREFERENCE_SHOWPLAYBACKCONTROLS, Preferences.DEFAULT_SHOWPLAYBACKCONTROLS)) {
	        	layoutPlaybackControls.setVisibility(View.VISIBLE);
	        }
        } else {
        	layoutPlaybackControls.setVisibility(View.VISIBLE);
        }
	}
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }
    
    /* Activity comes foreground */
    @Override
    public void onStart() {
    	super.onStart();
    	// The service is bound to this activity
    	if(musicService==null) {
            startService(serviceIntent); // Starts the service if it is not running
			serviceConnection = new ServiceConnection() {
				@Override
				public void onServiceConnected(ComponentName className, IBinder service) {
					musicService = ((MusicService.MusicBinder)service).getService();
					startRoutine();
				}
				@Override
				public void onServiceDisconnected(ComponentName className) {
					musicService = null;
				}
			};
    		bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    	}
    }
    
    /* Called after onStart or when the screen is switched on. */
    @Override
    public void onResume() {
    	super.onResume();
    	if (musicService!=null) startRoutine();
    	
    	// Enable the broadcast receiver
    	IntentFilter intentFilter = new IntentFilter();
    	intentFilter.addAction("com.andreadec.musicplayer.newsong");
    	intentFilter.addAction("com.andreadec.musicplayer.playpausechanged");
    	intentFilter.addAction("com.andreadec.musicplayer.podcastdownloadcompleted");
    	intentFilter.addAction("com.andreadec.musicplayer.quitactivity");
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            	if(intent.getAction().equals("com.andreadec.musicplayer.newsong")) {
            		updatePlayingItem();
            	} else if(intent.getAction().equals("com.andreadec.musicplayer.playpausechanged")) {
            		updatePlayPauseButton();
            	/*
            	} else if(intent.getAction().equals("com.andreadec.musicplayer.podcastdownloadcompleted")) {
            		if(app.currentPage==PAGE_PODCASTS) {
            			PodcastsFragment podcastsFragment = (PodcastsFragment)currentFragment;
            			podcastsFragment.updateListView(true);
            		}
            	 */
            	} else if(intent.getAction().equals("com.andreadec.musicplayer.quitactivity")) {
            		finish(); // I don't call quitActivity() because the service closes himself after sending this broadcast
            	}

            }
        };
        registerReceiver(broadcastReceiver, intentFilter);
        updatePlayPauseButton();
    }

	@Override
	public void onRequestPermissionsResult (int requestCode, String[] permissions, int[] grantResults) {
		if(requestCode==READ_EXTERNAL_STORAGE_PERMISSION_REQUEST && grantResults[0]!=PermissionChecker.PERMISSION_GRANTED) {
			Utils.showMessageDialog(this, R.string.errorReadPermissionDenied);
		}
	}
    
    @Override
    public void setTitle(CharSequence title) {
    	super.setTitle(title);
    	getSupportActionBar().setTitle(title);
    }
    
    private void openPage(int page) {
    	MusicPlayerFragment fragment;
    	switch(page) {
    	case PAGE_BROWSER:
    		fragment = new BrowserFragment();
    		break;
    	case PAGE_PLAYLISTS:
    		fragment = new PlaylistFragment();
    		break;
    		/*
    	case PAGE_RADIOS:
    		fragment = new RadioFragment();
    		break;
    	case PAGE_PODCASTS:
    		fragment = new PodcastsFragment();
    		break;
    		 */
    	default:
    		return;
    	}
    	app.currentPage = page;
    	currentFragment = fragment;
    	FragmentTransaction transaction = fragmentManager.beginTransaction();
    	transaction.remove(currentFragment);
    	transaction.replace(R.id.page, fragment);
    	transaction.addToBackStack(null);
    	transaction.setTransition(FragmentTransaction.TRANSIT_NONE);
    	transaction.commit();
    	fragmentManager.executePendingTransactions();
    	setTitle(pages[app.currentPage]);
    }
    
    /* Called before onStop or when the screen is switched off. */
    @Override
    public void onPause() {
    	super.onPause();
    	stopPollingThread(); // Stop the polling thread
    	unregisterReceiver(broadcastReceiver); // Disable broadcast receiver
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	musicService = null;
    	unbindService(serviceConnection); // Unbinds from the service
    }
    
    /* Updates information about the current song. */
    private void updatePlayingItem() {
    	PlayableItem playingItem = musicService.getCurrentPlayingItem();
    	
    	if(playingItem!=null) {
    		// Song loaded
	    	textViewTitle.setText(playingItem.getTitle());
	    	textViewArtist.setText(playingItem.getArtist());
	    	seekBar1.setMax(musicService.getDuration());
	    	seekBar1.setProgress(musicService.getCurrentPosition());
	    	seekBar1.setClickable(true);
	    	isLengthAvailable = playingItem.isLengthAvailable();
	    	if(isLengthAvailable) {
	    		int duration = musicService.getDuration();
	    		songDurationString = "/" + Utils.formatTime(duration);
	    		seekBar1.setVisibility(View.VISIBLE);
	    		if(duration>SECOND_SEEKBAR_DURATION) {
	    			imageButtonShowSeekbar2.setVisibility(View.VISIBLE);
	    			imageButtonShowSeekbar2.setImageResource(R.drawable.expand);
	    		} else {
                    seekBar2.setVisibility(View.GONE);
	    			imageButtonShowSeekbar2.setVisibility(View.GONE);
	    		}
	    	} else {
	    		songDurationString = "";
	    		seekBar1.setVisibility(View.GONE);
                seekBar2.setVisibility(View.GONE);
	    		imageButtonShowSeekbar2.setVisibility(View.GONE);
	    	}

            imageViewSongImage.setVisibility(View.GONE);
            ((MusicPlayerApplication)getApplication()).imagesCache.getImageAsync(playingItem, imageViewSongImage);

			// NavigationHeader Image
			if (switchNavigationHeaderImageIndex > 10) {
				switchNavigationHeaderImageIndex = 0;
			} else {
				switchNavigationHeaderImageIndex += 1;
			}
			imageViewNavigationHeader = findViewById(R.id.imageViewNavigationHeader);
			if (imageViewNavigationHeader != null) {
				if (switchNavigationHeaderImageIndex == 0) {
					imageViewNavigationHeader.setImageResource(R.drawable.hy_0000);
				} else if (switchNavigationHeaderImageIndex == 1 ) {
				    imageViewNavigationHeader.setImageResource(R.drawable.hy_0001);
				} else if (switchNavigationHeaderImageIndex == 2 ) {
					imageViewNavigationHeader.setImageResource(R.drawable.hy_0002);
				} else if (switchNavigationHeaderImageIndex == 3 ) {
					imageViewNavigationHeader.setImageResource(R.drawable.hy_0003);
				} else if (switchNavigationHeaderImageIndex == 4 ) {
					imageViewNavigationHeader.setImageResource(R.drawable.hy_0004);
				} else if (switchNavigationHeaderImageIndex == 5 ) {
					imageViewNavigationHeader.setImageResource(R.drawable.hy_0005);
				} else if (switchNavigationHeaderImageIndex == 6 ) {
					imageViewNavigationHeader.setImageResource(R.drawable.hy_0006);
				} else if (switchNavigationHeaderImageIndex == 7 ) {
					imageViewNavigationHeader.setImageResource(R.drawable.hy_0007);
				} else if (switchNavigationHeaderImageIndex == 8 ) {
					imageViewNavigationHeader.setImageResource(R.drawable.hy_0008);
				} else if (switchNavigationHeaderImageIndex == 9 ) {
					imageViewNavigationHeader.setImageResource(R.drawable.hy_0009);
				} else if (switchNavigationHeaderImageIndex == 9 ) {
					imageViewNavigationHeader.setImageResource(R.drawable.hy_0010);
				}
			}
    	} else {
    		// No song loaded
    		textViewTitle.setText(R.string.noSong);
	    	textViewArtist.setText("");
	    	seekBar1.setMax(10);
	    	seekBar1.setProgress(0);
	    	seekBar1.setClickable(false);
	    	seekBar2.setVisibility(View.GONE);
	    	imageButtonShowSeekbar2.setVisibility(View.GONE);
	    	isLengthAvailable = true;
	    	songDurationString = "";
	    	seekBar1.setVisibility(SeekBar.GONE);
	    	imageViewSongImage.setVisibility(View.GONE);
    	}
    	updatePlayPauseButton();
    	updatePosition();

    	currentFragment.updateListView();
    }
    
    /* Updates the play/pause button status according to the playing song. */
    private void updatePlayPauseButton() {
    	imageButtonPlayPause.setChecked(musicService!=null && musicService.isPlaying());
    }
    
    /* Updates the seekbar and the position information according to the playing song. */
    private void updatePosition() {
		int progress = musicService.getCurrentPosition();
		int duration = musicService.getDuration();
		seekBar1.setProgress(progress);
		if(duration>SECOND_SEEKBAR_DURATION) {
			int progress2 = progress%SECOND_SEEKBAR_DURATION;
			
			int parts = duration/SECOND_SEEKBAR_DURATION;
			if(progress>parts*SECOND_SEEKBAR_DURATION) {
				seekBar2.setMax(duration-parts*SECOND_SEEKBAR_DURATION);
			} else {
				seekBar2.setMax(SECOND_SEEKBAR_DURATION);
			}
	    	seekBar2.setProgress(progress2);
		}
		String time;
		if(showRemainingTime && isLengthAvailable) {
			time = "-" + Utils.formatTime(musicService.getDuration()-progress);
		} else {
			time = Utils.formatTime(progress);
		}
		time += songDurationString;
		textViewTime.setText(time);
	}
    
    /* Called after the service has been bounded. */
    private void startRoutine() {
    	secondaryNavigationManager.update();
    	
    	// Opens the song from the search, if any
    	if(searchSong!=null) {
            playItem(searchSong);
            gotoPlayingItemPosition();
    		searchSong = null;
    	} else {
            updatePlayingItem();
        }
    	
    	// Opens the song from the intent, if necessary
    	if(intentFile!=null) {
    		BrowserSong song = new BrowserSong(intentFile);
    		playItem(song);
    		intentFile = null;
    	} else {
            updatePlayingItem();
        }

    	// Starts the thread to update the seekbar and position information
    	if(startPollingThread) startPollingThread();
    }
    
    /* Manages songs opened from an external application */
    @Override
    protected void onNewIntent(Intent newIntent) {
    	super.onNewIntent(newIntent);
    	setIntent(newIntent);
    	loadSongFromIntent();
    }
    private void loadSongFromIntent() {
    	Intent intent = getIntent();
    	if(intent!=null && intent.getAction()!=null && intent.getAction().equals(Intent.ACTION_VIEW)) {
        	try {
				intentFile = URLDecoder.decode(intent.getDataString(), "UTF-8");
				intentFile = intentFile.replace("file://", "");
			} catch (Exception e) {}
        }
    }
    
    /* Thread which updates song position polling the information from the service */
    private void startPollingThread() {
    	pollingThreadRunning = true;
        new Thread() {
        	public void run() {
        		while(pollingThreadRunning) {
        			runOnUiThread(new Runnable() {
						public void run() {
                            if(musicService!=null) {
                                updatePosition();
                            }
						}
					});
        			try{ Thread.sleep(POLLING_INTERVAL); } catch(Exception e) {}
        		}
        	}
        }.start();
    }
    private void stopPollingThread() {
    	pollingThreadRunning = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	if(app.currentPage==PAGE_BROWSER) {
    		menu.findItem(R.id.menu_setAsBaseFolder).setVisible(true);
    		menu.findItem(R.id.menu_gotoBaseFolder).setVisible(true);
			//menu.findItem(R.id.menu_gotoRootFolder).setVisible(true);
			menu.findItem(R.id.menu_gotoStorageFolder).setVisible(true);
    	} else {
    		menu.findItem(R.id.menu_setAsBaseFolder).setVisible(false);
    		menu.findItem(R.id.menu_gotoBaseFolder).setVisible(false);
			//menu.findItem(R.id.menu_gotoRootFolder).setVisible(false);
			menu.findItem(R.id.menu_gotoStorageFolder).setVisible(false);
    	}
    	/*
    	if(app.currentPage==PAGE_PODCASTS) {
    		menu.findItem(R.id.menu_removeAllEpisodes).setVisible(true);
    		menu.findItem(R.id.menu_removeDownloadedEpisodes).setVisible(true);
    	} else {
    	 */
    		menu.findItem(R.id.menu_removeAllEpisodes).setVisible(false);
    		menu.findItem(R.id.menu_removeDownloadedEpisodes).setVisible(false);
    	//}
    	if(musicService==null || musicService.getCurrentPlayingItem()==null) {
    		menu.findItem(R.id.menu_songInfo).setVisible(false);
    	} else {
    		menu.findItem(R.id.menu_songInfo).setVisible(true);
    	}
    	return true;
    }


	/* A menu item has been selected. */
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG,"onOptionsItemSelected");
    	if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
          }
		switch (item.getItemId()) {
		case R.id.menu_gotoPlayingSongDirectory:
			gotoPlayingItemPosition();
			return true;
		case R.id.menu_gotoBaseFolder:
			((BrowserFragment)currentFragment).gotoBaseFolder();
			return true;
			/*
		case R.id.menu_gotoRootFolder:
			((BrowserFragment)currentFragment).gotoRootFolder();
			return true;
			 */
		case R.id.menu_gotoStorageFolder:
			((BrowserFragment)currentFragment).gotoStorageFolder();
			return true;
		case R.id.menu_search:
			startActivityForResult(new Intent(this, SearchActivity.class), 1);
			return true;
		case R.id.menu_songInfo:
			showItemInfo(musicService.getCurrentPlayingItem());
			return true;
		case R.id.menu_setAsBaseFolder:
			setBaseFolder(((MusicPlayerApplication) getApplication()).getCurrentDirectory().getDirectory());
			return true;
		case R.id.menu_removeAllEpisodes:
			//((PodcastsFragment)currentFragment).removeAllEpisodes();
			return true;
		case R.id.menu_removeDownloadedEpisodes:
			//((PodcastsFragment)currentFragment).removeDownloadedEpisodes();
			return true;
        case R.id.menu_quit:
            quitApplication();
            return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
    
    @Override
	public boolean onContextItemSelected(MenuItem item) {
    	return currentFragment.onContextItemSelected(item);
    }

    /* Button click handler. */
	@Override
	public void onClick(View view) {
		if (view.equals(imageButtonPlayPause)) {
			musicService.playPause();
			updatePlayPauseButton();
		} else if(view.equals(imageButtonNext)) {
			musicService.nextItem();
		} else if(view.equals(imageButtonPrevious))  {
			musicService.previousItem(false);
		} else if(view.equals(textViewTime)) {
			showRemainingTime = !showRemainingTime;
		} else if(view.equals(imageButtonShowSeekbar2)) {
            if (seekBar2.getVisibility() == View.VISIBLE) {
                imageButtonShowSeekbar2.setImageResource(R.drawable.expand);
                seekBar2.setVisibility(View.GONE);
            } else {
                imageButtonShowSeekbar2.setImageResource(R.drawable.collapse);
                seekBar2.setVisibility(View.VISIBLE);
            }
        }
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// Called when SearchActivity returns
		super.onActivityResult(requestCode, resultCode, intent);
		if (resultCode == 1) { // If result code is 0, the user canceled the operation
			BrowserSong song = (BrowserSong)intent.getSerializableExtra("song");
			File songDirectory = new File(song.getPlayableUri()).getParentFile();
			BrowserDirectory browserDirectory = new BrowserDirectory(songDirectory);
			song.setBrowser(browserDirectory);
			searchSong = song;
		}
	}
	
	/* ALWAYS CALL THIS FUNCTION TO COMPLETELY CLOSE THE APPLICATION */
	public void quitApplication() {
		if(preferences.getBoolean(Preferences.PREFERENCE_OPENLASTPAGEONSTART, Preferences.DEFAULT_OPENLASTPAGEONSTART)) {
			SharedPreferences.Editor editor = preferences.edit();
			editor.putInt(Preferences.PREFERENCE_LASTPAGE, app.currentPage);
			editor.apply();
		}
        app.currentPage = -1;
		stopService(serviceIntent); // Stop the service!
		finish();
	}
	
	public void playItem(PlayableItem item) {
		boolean ok = musicService.playItem(item);
		if(!ok) Utils.showMessageDialog(this, R.string.errorSong, R.string.errorSongMessage);
	}
	
	public void playRadio(Radio radio) {
		//new PlayRadioTask(radio).execute();
	}
	
	public void playPodcastEpisodeStreaming(PodcastEpisode episode) {
		//new PlayPodcastEpisodeStreamingTask(episode).execute();
	}
	
	public PlayableItem getCurrentPlayingItem() {
		if(musicService==null) return null;
		return musicService.getCurrentPlayingItem();
	}
	
	public void gotoPlayingItemPosition() {
		final PlayableItem playingItem = musicService.getCurrentPlayingItem();
		if(playingItem==null) return;
		if(playingItem instanceof BrowserSong) {
			openPage(PAGE_BROWSER);
		} else if(playingItem instanceof PlaylistSong) {
			openPage(PAGE_PLAYLISTS);
			/*
		} else if(playingItem instanceof Radio) {
			openPage(PAGE_RADIOS);
		} else if(playingItem instanceof PodcastEpisode) {
			openPage(PAGE_PODCASTS);
			 */
		}
		currentFragment.gotoPlayingItemPosition(playingItem);
	}
	
	public void setBaseFolder(final File folder) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.setAsBaseFolder);
		builder.setMessage(R.string.setBaseFolderConfirm);
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
		      public void onClick(DialogInterface dialog, int which) {
                  SharedPreferences.Editor editor = preferences.edit();
                  editor.putString(Preferences.PREFERENCE_BASEFOLDER, folder.getAbsolutePath());
                  editor.apply();
                  /*String[] paths = {folder.getAbsolutePath()};
                  MediaScannerConnection.scanFile(MainActivity.this, paths, null, null);*/
		      }
		});
		builder.setNegativeButton(R.string.no, null);
		builder.show();
	}
	
	/* Back button click handler. Overwrites default behaviour. */
	boolean backPressedOnce = false; // Necessary to implement double-tap-to-quit-app
	@Override
	public void onBackPressed() {
		if(backPressedOnce) {
			quitApplication();
			return;
		}
		boolean executed = currentFragment.onBackPressed();
		if(!executed && preferences.getBoolean(Preferences.PREFERENCE_ENABLEBACKDOUBLEPRESSTOQUITAPP, Preferences.DEFAULT_ENABLEBACKDOUBLEPRESSTOQUITAPP)) {
			backPressedOnce = true;
			Toast.makeText(this, R.string.pressAgainToQuitApp, Toast.LENGTH_SHORT).show();
			new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    backPressedOnce = false;
                }
            }, 2000);
		}
	}

	/* Seekbar click handler. */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if(fromUser) { // Event is triggered only if the seekbar position was modified by the user
			if(seekBar.equals(seekBar1)) {
				musicService.seekTo(progress);
			} else if(seekBar.equals(seekBar2)) {
				int progress2 = (seekBar1.getProgress()/SECOND_SEEKBAR_DURATION)*SECOND_SEEKBAR_DURATION;
				musicService.seekTo(progress2+progress);
			}
			updatePosition();
		}
	}
	@Override public void onStartTrackingTouch(SeekBar seekBar) {}
	@Override public void onStopTrackingTouch(SeekBar seekBar) {}
	
	
	
	private class PlayRadioTask extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog progressDialog;
		private Radio radio;
		public PlayRadioTask(Radio radio) {
			this.radio = radio;
			progressDialog = new ProgressDialog(MainActivity.this);
		}
		@Override
		protected void onPreExecute() {
	        progressDialog.setIndeterminate(true);
	        progressDialog.setCancelable(true);
	        progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					cancel(true);
				}
			});
	        progressDialog.setCanceledOnTouchOutside(false);
	        progressDialog.setMessage(MainActivity.this.getString(R.string.loadingRadio, radio.getTitle()));
			progressDialog.show();
			startPollingThread = false;
			stopPollingThread(); // To prevent polling thread activation
	    }
		@Override
		protected Boolean doInBackground(Void... params) {
			return musicService.playItem(radio);
		}
		@Override
	    protected void onCancelled() {
			musicService.playItem(null);
	    }
		@Override
		protected void onPostExecute(final Boolean success) {
			updatePlayingItem();
			if(progressDialog.isShowing()) {
				progressDialog.dismiss();
	        }
			startPollingThread = true;
			if(!pollingThreadRunning) startPollingThread();
			
			if(!success) {
				Utils.showMessageDialog(MainActivity.this, R.string.errorWebRadio, R.string.errorWebRadioMessage);
			}
		}
	}
	
	
	private class PlayPodcastEpisodeStreamingTask extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog progressDialog;
		private PodcastEpisode episode;
		public PlayPodcastEpisodeStreamingTask(PodcastEpisode episode) {
			this.episode = episode;
			progressDialog = new ProgressDialog(MainActivity.this);
		}
		@Override
		protected void onPreExecute() {
	        progressDialog.setIndeterminate(true);
	        progressDialog.setCancelable(true);
	        progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					cancel(true);
				}
			});
	        progressDialog.setCanceledOnTouchOutside(false);
	        progressDialog.setMessage(MainActivity.this.getString(R.string.loadingPodcastEpisode, episode.getTitle()));
			progressDialog.show();
			startPollingThread = false;
			stopPollingThread(); // To prevent polling thread activation
	    }
		@Override
		protected Boolean doInBackground(Void... params) {
			return musicService.playItem(episode);
		}
		@Override
	    protected void onCancelled() {
			musicService.playItem(null);
	    }
		@Override
		protected void onPostExecute(final Boolean success) {
			updatePlayingItem();
			if(progressDialog.isShowing()) {
				progressDialog.dismiss();
	        }
			startPollingThread = true;
			if(!pollingThreadRunning) startPollingThread();
			
			if(!success) {
				Utils.showMessageDialog(MainActivity.this, R.string.error, R.string.errorSong);
			}
		}
	}
	
	private void showItemInfo(PlayableItem item) {
		if(item==null || item.getInformation()==null) return;
		ArrayList<Information> information = item.getInformation();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.songInfo);
		View view = getLayoutInflater().inflate(R.layout.layout_songinfo, null, false);
		LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.linearLayoutInformation);
		
		Bitmap image = item.getImage();
		if(image!=null) {
			ImageView imageView = new ImageView(this);
			imageView.setImageBitmap(image);
			linearLayout.addView(imageView);
		}
		
		for(Information info : information) {
			TextView info1 = new TextView(this);
			info1.setTextAppearance(this, android.R.style.TextAppearance_Medium);
			info1.setText(getResources().getString(info.key));
			TextView info2 = new TextView(this);
			info2.setText(info.value);
			info2.setPadding(0, 0, 0, 10);
			linearLayout.addView(info1);
			linearLayout.addView(info2);
		}
		
		builder.setView(view);
		builder.setPositiveButton(R.string.ok, null);
		builder.show();
	}
	
	private class PlayerGestureListener extends GestureDetector.SimpleOnGestureListener {
    	@Override
        public boolean onDown(MotionEvent event) { 
            return true;
        }

		@Override
		public boolean onFling(MotionEvent event1, MotionEvent event2,  float velocityX, float velocityY) {
			if(event1.getX()<event2.getX()) musicService.previousItem(false);
			else if(event1.getX()>event2.getX()) musicService.nextItem();
			return true;
		}
		
		@Override
	    public boolean onSingleTapConfirmed(MotionEvent event) {
			musicService.playPause();
	        return true;
	    }
	}
}
