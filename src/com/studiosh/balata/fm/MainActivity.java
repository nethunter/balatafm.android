package com.studiosh.balata.fm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
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
	SongInfoService songInfoService;
	
	private static Boolean serviceStarted = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Prepare the text views that will hold the artist details
		tv_listeners = (TextView) findViewById(R.id.tv_listeners);
		tv_song_info = (TextView) findViewById(R.id.tv_song_info);
		
		// Set the custom font for the text areas
		Typeface fontPressStart = Typeface.createFromAsset(getAssets(), "fonts/PressStart2P.ttf");
		tv_listeners.setTypeface(fontPressStart);
		tv_song_info.setTypeface(fontPressStart);
		tv_song_info.setText("Artist\nSong");
		
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
		if (MainActivity.serviceStarted == false) {
			startService(new Intent(this, SongInfoService.class));
			MainActivity.serviceStarted = true;
		}
	}

	@Override
	protected void onDestroy() {		
		Log.d(TAG, "Activity destroyed");
		System.exit(0);
		stopService(new Intent(this, SongInfoService.class));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
