package com.studiosh.balata.fm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.studiosh.balata.fm.SongInfoService.LocalBinder;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";

	TextView mTextViewSongInfo;
	SeekBar mSeekBarVolume;

	private SongInfoService mSongInfoService;
	private Boolean mServiceStarted = false;
	private Boolean mBound = false;
	
	private ToggleButton mButtonPlayStop;
	private static Boolean mPlaying = true;

	private Intent mServiceIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Prepare the text views that will hold the artist details
		mTextViewSongInfo = (TextView) findViewById(R.id.tv_song_info);

		// Set the custom font for the text areas
		mTextViewSongInfo.setText(R.string.retrieveing_song_details);

		// Handle the Start/Stop Button
		mButtonPlayStop = (ToggleButton) findViewById(R.id.btn_play_stop);
		mButtonPlayStop.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				
				if (isChecked) {
					if (mSongInfoService.isStreamStarted()) {
						mSongInfoService.stopStream();
					}
				} else {
					if (!mSongInfoService.isStreamStarted()) {
						mSongInfoService.startStream();
					}
				}
			}
		});
		
		mButtonPlayStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "Click on Play/Stop Button");
			}
		});

		// Set the volume seekbar
		mSeekBarVolume = (SeekBar) findViewById(R.id.sb_volume);	
		final AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		
		// First let's handle the seek bar
		mSeekBarVolume.setMax(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
		mSeekBarVolume.setProgress(audio.getStreamVolume(AudioManager.STREAM_MUSIC));
		mSeekBarVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {}
			public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					audio.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
				}
			}
		});
		
		// Monitor system changes for volume change
		this.getApplicationContext().getContentResolver().registerContentObserver(
			android.provider.Settings.System.CONTENT_URI, true,
			new ContentObserver(new Handler()) {
				public void onChange(boolean selfChange) {
					super.onChange(selfChange);
					mSeekBarVolume.setProgress(audio
							.getStreamVolume(AudioManager.STREAM_MUSIC));
				}
			}
		);
		
		// Start the updates service
		Log.d(TAG, "About to start the service...");
		if (mServiceStarted == false) {
			mServiceIntent = new Intent(this, SongInfoService.class);
			startService(mServiceIntent);
			mServiceStarted = true;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
        Intent intent = new Intent(this, SongInfoService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);		
	}

	@Override
	protected void onStop() {
		super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Activity destroyed");

		if (mServiceStarted) {
			stopService(mServiceIntent);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(broadcastReceiver, new IntentFilter(
				SongInfoService.BROADCAST_ACTION));
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(broadcastReceiver);
	};

	protected void updatePlayStatus()
	{
	}
	
	private void updateUI(Intent intent) {
		String song_artist = intent.getStringExtra("song_artist");
		String song_title = intent.getStringExtra("song_title");
		int listeners = intent.getIntExtra("listeners", 0);

		mTextViewSongInfo.setText(song_artist + "\n" + song_title);
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateUI(intent);
		}
	};

	/* @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent intent = (Intent) new Intent(this, SettingsActivity.class);
			startActivity(intent);
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	} */
	
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mSongInfoService = binder.getService();
            mBound = true;
            
            mSongInfoService.broadcastSongDetails();
            mSongInfoService.startStream();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };	
}
