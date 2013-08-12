package com.studiosh.balata.fm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;

public class BalataNotifier {
	private static final String TAG = "BalataNotifier";
	
	private SongInfoService mSongInfoService;
	private MainActivity mMainActivity;
	private String mSongArtist;
	private String mSongTitle;
	private Boolean mBuffering = false;
	private Boolean mPlaying = false;
		
	public static final String SONG_DETAILS_ACTION = "com.studiosh.balata.fm.SONG_DETAILS_UPDATE";
	private final Handler handler = new Handler();
	
	static final int NOTIFY_ID = 1345;	
	
	private NotificationCompat.Builder mNotifyBuild;
	
	public BalataNotifier(SongInfoService songInfoService) {
		mSongInfoService = songInfoService;
		
		handler.removeCallbacks(sendUpdatesToUI);
	}
	
	protected void destroy() {
		stopNotification();
	}
	
	public void setMainActivity(MainActivity activity) {
		mMainActivity = activity;
	}
	
	public void clearMainActivity() {
		mMainActivity = null;
	}

	public void startNotification() {	
		if (mNotifyBuild == null) {
			Intent intent = new Intent(mSongInfoService.getApplicationContext(), MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK 
					| Intent.FLAG_ACTIVITY_NO_ANIMATION 
					| Intent.FLAG_ACTIVITY_NEW_TASK);
			
			PendingIntent pi = PendingIntent.getActivity(mSongInfoService.getApplicationContext(), 0, 
					intent,PendingIntent.FLAG_UPDATE_CURRENT);
			
			mNotifyBuild = new NotificationCompat.Builder(mSongInfoService)
				.setContentTitle(mSongInfoService.getString(R.string.balatafm))
				.setContentText(mSongInfoService.getString(R.string.retrieving_song_data))
				.setSmallIcon(R.drawable.ic_notification)
				.setOngoing(true)
				.setContentIntent(pi);
			
			mSongInfoService.startForeground(NOTIFY_ID, mNotifyBuild.build());
		}
	}
	
	public void updateNotification(String notification) {
		mNotifyBuild.setContentText(notification);
		
		NotificationManager notify_manager =
		        (NotificationManager) mSongInfoService.getApplicationContext().getSystemService(
		        		Context.NOTIFICATION_SERVICE);
		
		notify_manager.notify(NOTIFY_ID, mNotifyBuild.build());		
	}
	
	public void stopNotification() {
		NotificationManager notify_manager =
		        (NotificationManager) mSongInfoService.getApplicationContext().getSystemService(
		        		Context.NOTIFICATION_SERVICE);

		notify_manager.cancel(NOTIFY_ID);
	}
	
	/**
	 * Update the system tray notification to show the song info
	 */
	public void setSongDetails(String songArtist, String songTitle) {
		mSongTitle = songTitle;
		mSongArtist = songArtist;
		
		updateNotification(songArtist + " - " + songTitle);
		updateUI();
	}
	
	public void setBuffering(Boolean buffering) {
		mBuffering = buffering;	
		updateUI();
	}
	
	public void setPlaying(Boolean playing) {
		mPlaying = playing;
		updateUI();
	}
	
	public void updateUI() {
		if (mBuffering == true) {
			updateNotification(mSongInfoService.getString(R.string.buffering));
		} else {
			updateNotification(mSongArtist + " - " + mSongTitle);
		}
		
		if (mMainActivity != null) {
			mMainActivity.runOnUiThread(sendUpdatesToUI);
		}
	}

	public void broadcastSongDetails() {
		Intent intent = new Intent(SONG_DETAILS_ACTION);
		intent.putExtra("song_artist", mSongArtist);
		intent.putExtra("song_title", mSongTitle);
		intent.putExtra("buffering", mBuffering);
		intent.putExtra("playing", mPlaying);
		
		mSongInfoService.sendBroadcast(intent);		
	}
	
	private Runnable sendUpdatesToUI = new Runnable() {
		public void run() {
			broadcastSongDetails();
		}
	};
	
	/**
	 * Update the song details in the 
	 */
	public void updateSongDetails(String songArtist, String songTitle) {
		mSongArtist = songArtist;
		mSongTitle = songTitle;
		
		setSongDetails(mSongArtist, mSongTitle);
	}
}
