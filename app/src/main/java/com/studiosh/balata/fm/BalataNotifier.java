package com.studiosh.balata.fm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;

public class BalataNotifier {
	public static final String SONG_DETAILS_ACTION = "com.studiosh.balata.fm.SONG_DETAILS_UPDATE";
	static final int NOTIFY_ID = 1345;
	private static final String TAG = "BalataNotifier";
	private final Handler handler = new Handler();
	private SongInfoService mSongInfoService;
	private MainActivity mMainActivity;
	private String mSongArtist;
	private String mSongTitle;
	private Boolean mBuffering = false;
	private Boolean mPlaying = false;
	private NotificationCompat.Builder mNotifyBuild;
	private Runnable sendUpdatesToUI = new Runnable() {
		public void run() {
			broadcastSongDetails();
		}
	};
	
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
	
	private PendingIntent getPendingIntent() {
		Intent intent = new Intent(mSongInfoService.getApplicationContext(), MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
				| Intent.FLAG_ACTIVITY_NO_ANIMATION
				| Intent.FLAG_ACTIVITY_NEW_TASK);

		PendingIntent pi = PendingIntent.getActivity(mSongInfoService.getApplicationContext(), 0,
					intent,PendingIntent.FLAG_UPDATE_CURRENT);

		return pi;
	}
	
	private PendingIntent getBroadcastIntent(String command) {
		PendingIntent pi;

		Intent intent = new Intent();
		intent.setAction(SongInfoService.COMMAND_ACTION);
		intent.putExtra("COMMAND", command);

		pi = PendingIntent.getBroadcast(mSongInfoService.getApplicationContext(), 12345,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		return pi;
	}
	
	private String getNotificationString() {
		if (mBuffering) {
			return mSongInfoService.getString(R.string.buffering);
		} else {
			if (mSongArtist != null && mSongTitle != null) {
				return mSongArtist + " - " + mSongTitle;
			} else {
				return mSongInfoService.getString(R.string.retrieveing_song_details);
			}
		}
	}
	
	public void startNotification() {
		mNotifyBuild = new NotificationCompat.Builder(mSongInfoService)
			.setContentTitle(mSongInfoService.getString(R.string.balatafm))
			.setSmallIcon(R.drawable.ic_notification)
			.setOngoing(true)
			.setContentIntent(getPendingIntent());

		mNotifyBuild.setContentText(getNotificationString());

		if (mBuffering) {
			mNotifyBuild.setProgress(0, 0, true);
		} else {
			mNotifyBuild.setProgress(0, 0, false);

			if (mPlaying) {
				mNotifyBuild.addAction(R.drawable.player_stop, "Stop", getBroadcastIntent("stop"));
			} else {
				mNotifyBuild.addAction(R.drawable.player_play, "Play", getBroadcastIntent("play"));
			}
		}

		mSongInfoService.startForeground(NOTIFY_ID, mNotifyBuild.build());
	}
	
	public void updateNotification() {
		mNotifyBuild.setContentText(getNotificationString());

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
		if (songArtist.equals(mSongArtist) || songTitle.equals(mSongArtist)) {
			mSongTitle = songTitle;
			mSongArtist = songArtist;
			updateNotification();
			updateUI();
		}
	}
	
	public void setBuffering(Boolean buffering) {
		if (buffering != mBuffering) {
			mBuffering = buffering;
			startNotification();
			updateUI();
		}
	}
	
	public void setPlaying(Boolean playing) {
		if (playing != mPlaying) {
			mPlaying = playing;
			startNotification();
			updateUI();
		}
	}

	public void updateUI() {
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
	
	/**
	 * Update the song details in the 
	 */
	public void updateSongDetails(String songArtist, String songTitle) {
		mSongArtist = songArtist;
		mSongTitle = songTitle;
		
		setSongDetails(mSongArtist, mSongTitle);
	}
}
