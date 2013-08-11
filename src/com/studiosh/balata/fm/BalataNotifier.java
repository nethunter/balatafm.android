package com.studiosh.balata.fm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.TextView;

public class BalataNotifier {
	private static final String TAG = "BalataNotifier";
	
	private SongInfoService mSongInfoService;
	private String mSongArtist;
	private String mSongTitle;
	private Boolean mGotSongInfo = false;
	private Boolean mBuffering = false;
	private Boolean mPlaying = false;
	private Boolean mBound = false;
	
    public static final String SONG_DETAILS_ACTION = "com.studiosh.balata.fm.SONG_DETAILS_UPDATE";
    private final Handler handler = new Handler();
	
	static final int NOTIFY_ID = 1345;	
	
	private NotificationCompat.Builder mNotifyBuild;
	
	public BalataNotifier(SongInfoService songInfoService) {
		mSongInfoService = songInfoService;
	}
	
	public void setActivity(MainActivity activity) {
		TextView tv = (TextView)activity.findViewById(R.id.tv_song_info);
		tv.setText(mSongTitle + " - " + mSongArtist);
	}	

	public void startNotification() {
		handler.removeCallbacks(sendUpdatesToUI);
		
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
	
	/**
	 * Update the system tray notification to show the song info
	 */
	public void setSongDetails(String songArtist, String songTitle) {
		mSongTitle = songTitle;
		mSongArtist = songArtist;
		
		updateNotification(songArtist + " - " + songTitle);
		
		if (mBound) {
			handler.postDelayed(sendUpdatesToUI, 1000);
		}		
	}
	
	public void setBuffering(Boolean buffering) {
		mBuffering = buffering;
		
		if (buffering == true) {
			updateNotification(mSongInfoService.getString(R.string.buffering));
		} else {
			updateNotification(mSongArtist + " - " + mSongTitle);
		}
	}
	
	public void setPlaying(Boolean playing) {
		mPlaying = playing;
	}
	
	public void setBound(Boolean bound) {
		mBound = bound;
	}

	public void broadcastSongDetails() {
		if (mGotSongInfo) {
			Intent intent = new Intent(SONG_DETAILS_ACTION);
			intent.putExtra("song_artist", mSongArtist);
			intent.putExtra("song_title", mSongTitle);
			intent.putExtra("is_playing", mPlaying);
			
			mSongInfoService.sendBroadcast(intent);
		}
	}
	
    private Runnable sendUpdatesToUI = new Runnable() {
    	public void run() {
    		broadcastSongDetails();
    		Log.d(TAG, "Updating UI with song details");
    	}
    };	
}
