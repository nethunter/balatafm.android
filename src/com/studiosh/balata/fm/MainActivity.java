package com.studiosh.balata.fm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements MediaPlayer.OnPreparedListener {
	private static final String TAG = "MainActivity";
	private static final String OGG_STREAM = "http://stream.balata.fm/stream.ogg";
	private static final String MP3_STREAM = "http://stream.balata.fm/stream.mp3";

	private String mSongArtist;
	private String mSongTitle;
	private int mListeners;
	
	TextView tv_listeners;
	TextView tv_song_info;
	
	private Boolean mRunFlag;
	private Boolean mBuffering = false;
	private Updater mUpdater;
	private static MediaPlayer mMediaPlayer;
	
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
				
			}
		});
				
		// Create the media player
		String url = MP3_STREAM; // your URL here
		mMediaPlayer = new MediaPlayer();
		try {
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mMediaPlayer.setDataSource(url);
		} catch (IOException e) {
			
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Activity destroyed");		
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		mRunFlag = true;
		// Create updater thread
		if (mUpdater == null) {
			mUpdater = new Updater();
		}

		if (!mUpdater.isAlive()) {
			mUpdater.start();
		}
		
		mMediaPlayer.setOnPreparedListener(this);
		tv_song_info.setText(R.string.buffering);
		mBuffering = true;
		mMediaPlayer.prepareAsync();
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		mp.start();
		mBuffering = false;
		updateSongDisplay();
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		mRunFlag = false;
		mUpdater = null;
		mMediaPlayer.stop();
	};
	
	public void updateSongDetails(String song_artist, String song_title, int listeners) {
		mSongArtist = song_artist;
		mSongTitle = song_title;
		mListeners = listeners;
		
		if (!mBuffering) {
			updateSongDisplay();
		}
	}
	
	public void updateSongDisplay() {
		tv_song_info.setText(mSongArtist + "\n" + mSongTitle);
		tv_listeners.setText("Balata.FM [" + Integer.toString(mListeners) + "]");
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	private class Updater extends Thread {
		private static final String TAG = "MainActivity.Updater";
		static final int DELAY = 10000;
		
		public Updater() {
			super("UpdaterService-Updater");
		}

		@Override
		public void run() {
			while (mRunFlag) {
				Log.d(TAG, "Updater running");
				String streamDetails = getStreamDetails();
				try {
					Log.d(TAG, "Update run");

					JSONObject songDetails = new JSONObject(streamDetails);
					Log.i(TAG, "Number of entries "
							+ songDetails.length());
					
					final int listeners = songDetails.getInt("listeners");
					final String song_title = songDetails.getString("title");
					final String song_artist = songDetails.getString("artist");
					
					Log.i(TAG, "Listeners " + Integer.toString(listeners));
					Log.i(TAG, "Artist " + song_artist);
					Log.i(TAG, "Title " + song_title);
					
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							updateSongDetails(song_artist, song_title, listeners);							
						}
					});
					
					Thread.sleep(DELAY);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			Log.d(TAG, "Exiting main loop");
		}

		public String getStreamDetails() {
			StringBuilder builder = new StringBuilder();
			HttpClient client = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(
					"http://www.balata.fm/ajax/stream_info.json");
			try {
				HttpResponse response = client.execute(httpGet);
				StatusLine statusLine = response.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if (statusCode == 200) {
					HttpEntity entity = response.getEntity();
					InputStream content = entity.getContent();
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(content));
					String line;
					while ((line = reader.readLine()) != null) {
						builder.append(line);
					}
				} else {
					Log.e(TAG, "Failed to retrieve JSON File");
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return builder.toString();
		}
	}	
}
