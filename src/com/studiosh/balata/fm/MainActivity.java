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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";

	TextView tv_listeners;
	TextView tv_song_info;
	
	private static SongInfoService mSongInfoService;
	private static Boolean mServiceStarted = false;
	
	private Intent mServiceIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Prepare the text views that will hold the artist details
		tv_listeners = (TextView) findViewById(R.id.tv_listeners);
		tv_song_info = (TextView) findViewById(R.id.tv_song_info);

		// Set the custom font for the text areas
		Typeface fontPressStart = Typeface.createFromAsset(getAssets(),
				"fonts/PressStart2P.ttf");
		tv_listeners.setTypeface(fontPressStart);
		tv_song_info.setTypeface(fontPressStart);
		tv_song_info.setText(R.string.retrieveing_song_details);

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

	private void updateUI(Intent intent) {
		String song_artist = intent.getStringExtra("song_artist");
		String song_title = intent.getStringExtra("song_title");
		int listeners = intent.getIntExtra("listeners", 0);

		tv_song_info.setText(song_artist + "\n" + song_title);
		tv_listeners.setText("Balata.FM [" + Integer.toString(listeners) + "]");
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateUI(intent);
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
		
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.activity_main, menu);
		// return true;
	}

}
