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

import android.util.Log;

public class BalataUpdater extends Thread {
	private BalataNotifier mNotifier;
	private Boolean mRunFlag;
	private static final String TAG = "Background-Updater";

	static final int DELAY_BG = 10000;
	static final int DELAY_FG = 30000;

	public BalataUpdater(BalataNotifier notifier) {
		super("UpdaterService-Updater");
		mNotifier = notifier;
	}

	@Override
	public synchronized void start() {
		mRunFlag = true;
		super.start();
	}
	
	@Override
	public void interrupt() {
		mRunFlag = false;
		super.interrupt();
	}
	
	@Override
	public void run() {
		while (mRunFlag) {
			Log.d(TAG, "Updater running");
			String streamDetails = getStreamDetails();
			try {
				Log.d(TAG, "Update run");

				JSONObject songDetails = new JSONObject(streamDetails);
				Log.i(TAG, "Number of entries " + songDetails.length());

				int listeners = songDetails.getInt("listeners");
				String song_title = songDetails.getString("title");
				String song_artist = songDetails.getString("artist");

				Log.i(TAG, "Listeners " + Integer.toString(listeners));
				Log.i(TAG, "Artist " + song_artist);
				Log.i(TAG, "Title " + song_title);

				mNotifier.updateSongDetails(song_artist, song_title);
				Thread.sleep(DELAY_FG);
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				mRunFlag = false;
			}
		}
	}

	public String getStreamDetails() {
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(
				"http://balata.fm:4000/ajax/stream_info.json");
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