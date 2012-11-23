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
import org.json.JSONException;
import org.json.JSONObject;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class SongInfoService extends Service {
	private static final String TAG = "SongInfoService";

	static final int DELAY_BG = 10000;
	static final int DELAY_FG = 30000;
	static final int NOTIFY_ID = 1345;

	private boolean runFlag = false;
	private static Updater updater;
		
	private String song_title;
	private String song_artist;
	private int listeners;
	
	private BalataStreamer balata_streamer;

	private NotificationCompat.Builder notify_build;
	
    public static final String BROADCAST_ACTION = "com.studiosh.balata.fm.SONG_DETAILS_UPDATE";
    Intent intent;
    private final Handler handler = new Handler();
    
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		intent = new Intent(BROADCAST_ACTION);
		
		if (updater == null) {
			updater = new Updater();
		}
		
		Log.d(TAG, "Service created");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Service started");

		runFlag = true;
		if (!updater.isAlive()) {
			updater.start();
		}
		
		handler.removeCallbacks(sendUpdatesToUI);
		
		startNotification();
		startStream();

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		runFlag = false;
		updater.interrupt();

		Log.d(TAG, "Service destroyed");
	}
	
	public void startNotification() {
		if (notify_build == null) {
			PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
			                new Intent(getApplicationContext(), MainActivity.class),
			                PendingIntent.FLAG_UPDATE_CURRENT);
			
			notify_build = new NotificationCompat.Builder(this)
				.setContentTitle(getString(R.string.balatafm))
				.setContentText(getString(R.string.retrieving_song_data))
				.setSmallIcon(R.drawable.ic_launcher)
				.setOngoing(true)
				.setContentIntent(pi);
			
			startForeground(NOTIFY_ID, notify_build.build());
		}
	}
	
	private void startStream() {
		balata_streamer.play();
	}
	
	/**
	 * Update the system tray notification to show the song info
	 */
	public void updateNotification() {
		notify_build.setContentText(song_artist + " - " + song_title);
		
		NotificationManager notify_manager =
		        (NotificationManager) getSystemService(
		        		Context.NOTIFICATION_SERVICE);
		
		notify_manager.notify(NOTIFY_ID, notify_build.build());
	}
	
	/**
	 * Update the song details in the 
	 */
	public void updateSongDetails(String song_artist, String song_title, int listeners) {
		this.song_artist = song_artist;
		this.song_title = song_title;
		this.listeners = listeners;
		
		updateNotification();
		handler.postDelayed(sendUpdatesToUI, 1000);
	}

	private void broadcastSongDetails() {
		intent.putExtra("song_artist", song_artist);
		intent.putExtra("song_title", song_title);
		intent.putExtra("listeners", listeners);
		
		sendBroadcast(intent);		
	}
	
    private Runnable sendUpdatesToUI = new Runnable() {
    	public void run() {
    		broadcastSongDetails();
    		Log.d(TAG, "Updating UI with song details");
    	}
    };

	private class Updater extends Thread {
		public Updater() {
			super("UpdaterService-Updater");
		}

		@Override
		public void run() {
			SongInfoService songInfoService = SongInfoService.this;
			while (songInfoService.runFlag) {
				Log.d(TAG, "Updater running");
				String streamDetails = getStreamDetails();
				try {
					Log.d(TAG, "Update run");

					JSONObject songDetails = new JSONObject(streamDetails);
					Log.i(TAG, "Number of entries "
							+ songDetails.length());
					
					int listeners = songDetails.getInt("listeners");
					String song_title = songDetails.getString("title");
					String song_artist = songDetails.getString("artist");
					
					Log.i(TAG, "Listeners " + Integer.toString(listeners));
					Log.i(TAG, "Artist " + song_artist);
					Log.i(TAG, "Title " + song_title);
					
					songInfoService.updateSongDetails(song_artist, song_title, listeners);
					Thread.sleep(DELAY_FG);
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					songInfoService.runFlag = false;
				}
			}
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
