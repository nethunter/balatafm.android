package com.studiosh.balata.fm;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class BalataUpdater extends Thread {
	public static final String SONG_INFO_UPDATE = "com.studiosh.balata.fm.SONGINFOUPDATE";

	static final int DELAY_BG = 10000;
	static final int DELAY_FG = 30000;
	private static final String TAG = "Background-Updater";
	BalataController mContoller;
	private Boolean mRunFlag;

	public BalataUpdater() {
		super("UpdaterService-Updater");
		mContoller = BalataController.getInstance();
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

				mContoller.updateSongDetails(song_artist, song_title);
				Thread.sleep(DELAY_FG);
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				mRunFlag = false;
			}
		}
	}

	/**
	 * @todo Get JSON from server
	 * @return String JSON from the server
	 */
	public String getStreamDetails() {
		return "{\"listeners\": 5, \"title\": \"Blah song\", \"artist\": \"Another blah\"}";
	}
}