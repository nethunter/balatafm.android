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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class SongInfoService extends Service {
	private static final String TAG = "SongInfoService";

	static final int DELAY_BG = 10000;
	static final int DELAY_FG = 30000;
	static final int NOTIFY_ID = 1345;

	private boolean runFlag = false;
	private Updater updater;
		
	private String song_title;
	private String song_artist;
	private int listeners;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.updater = new Updater();
		Log.d(TAG, "Service created");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Service started");

		this.runFlag = true;
		this.updater.start();
		
		startNotification();

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		this.runFlag = false;
		this.updater.interrupt();
		this.updater = null;

		Log.d(TAG, "Service destroyed");
	}
	
	public void startNotification() {
		// String songName;
		// assign the song name to songName
		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
		                new Intent(getApplicationContext(), MainActivity.class),
		                PendingIntent.FLAG_UPDATE_CURRENT);
		Notification notification = new Notification.Builder(this)
			.setContentTitle("Balata.fm")
			.setContentText("Test notification")
			.setSmallIcon(R.drawable.ic_launcher)
			.setOngoing(true)
			.setContentIntent(pi)
			.build();
		startForeground(NOTIFY_ID, notification);
	}

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
					
					songInfoService.listeners = songDetails.getInt("listeners");
					songInfoService.song_title = songDetails.getString("title");
					songInfoService.song_artist = songDetails.getString("artist");
					
					Log.i(TAG, "Listeners " + 
							Integer.toString(songInfoService.listeners));
					
					Log.i(TAG, "Artist " + 
							songInfoService.song_artist);

					Log.i(TAG, "Title " + 
							songInfoService.song_title);
					
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
