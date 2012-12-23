package com.studiosh.balata.fm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.studiosh.balata.fm.SongInfoService.LocalBinder;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";

	TextView mtvListeners;
	TextView mtvSongInfo;

	private SongInfoService mSongInfoService;
	private Boolean mServiceStarted = false;
	private Boolean mBound = false;
	
	private static Boolean mPlaying = true;

	private Intent mServiceIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Prepare the text views that will hold the artist details
		mtvListeners = (TextView) findViewById(R.id.tv_listeners);
		mtvSongInfo = (TextView) findViewById(R.id.tv_song_info);

		// Set the custom font for the text areas
		Typeface fontPressStart = Typeface.createFromAsset(getAssets(),
				"fonts/PressStart2P.ttf");
		mtvListeners.setTypeface(fontPressStart);
		mtvSongInfo.setTypeface(fontPressStart);
		mtvSongInfo.setText(R.string.retrieveing_song_details);

		// Handle the Start/Stop Button
		Button btn_play_stop = (Button) findViewById(R.id.btn_play_stop);
		btn_play_stop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "Click on Play/Stop Button");
			}
		});

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

		mtvSongInfo.setText(song_artist + "\n" + song_title);
		mtvListeners.setText("Balata.FM [" + Integer.toString(listeners) + "]");
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateUI(intent);
		}
	};

	@Override
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
	}
	
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
