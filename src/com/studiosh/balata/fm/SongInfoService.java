package com.studiosh.balata.fm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class SongInfoService extends Service {
	private static final String TAG = "SongInfoService";

	static final int NOTIFY_ID = 1345;

	private static BackgroundUpdater mUpdater;
		
	private Boolean mGotSongInfo = false;
	private String mSongTitle;
	private String mSongArtist;
	private int mListeners;
	
	private static BalataStreamer mBalataStreamer;
	private Boolean mStreamStarted = false;

	private NotificationCompat.Builder notify_build;
	
    public static final String BROADCAST_ACTION = "com.studiosh.balata.fm.SONG_DETAILS_UPDATE";
    private final Handler handler = new Handler();
    
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    private Boolean mBound = false;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        SongInfoService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SongInfoService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
    	mBound = true;
    	
    	if (mGotSongInfo) {
    		broadcastSongDetails();
    	}
    	
        return mBinder;
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
    	mBound = false;
    	return super.onUnbind(intent);
    }

	// General Service Logic
	@Override
	public void onCreate() {
		super.onCreate();
		
		if (mUpdater == null) {
			mUpdater = new BackgroundUpdater(this);
		}
		
		if (mBalataStreamer == null) {
			mBalataStreamer = new BalataStreamer();
		}
		
		Log.d(TAG, "Service created");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Service started");

		if (!mUpdater.isAlive()) {
			mUpdater.start();
		}
				
		handler.removeCallbacks(sendUpdatesToUI);
		
		startNotification();

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mStreamStarted) {
			stopStream();
		}
		
		if (mBalataStreamer != null) {
			mBalataStreamer.destroy();
			mBalataStreamer = null;
		}
		
		mUpdater.interrupt();
		mUpdater = null;

		Log.d(TAG, "Service destroyed");
	}
	
	public void startNotification() {
		if (notify_build == null) {
			Intent intent = new Intent(getApplicationContext(), MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK 
					| Intent.FLAG_ACTIVITY_NO_ANIMATION 
					| Intent.FLAG_ACTIVITY_NEW_TASK);
			
			PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, 
					intent,PendingIntent.FLAG_UPDATE_CURRENT);
			
			notify_build = new NotificationCompat.Builder(this)
				.setContentTitle(getString(R.string.balatafm))
				.setContentText(getString(R.string.retrieving_song_data))
				.setSmallIcon(R.drawable.ic_launcher)
				.setOngoing(true)
				.setContentIntent(pi);
			
			startForeground(NOTIFY_ID, notify_build.build());
		}
	}
	
	public void startStream() {
		if (!mStreamStarted) {
			mBalataStreamer.play();
			mStreamStarted = true;
		}
	}
	
	public void stopStream() {
		if (mStreamStarted) {
			mBalataStreamer.stop();
			mStreamStarted = false;
		}
	}
	
	public boolean isStreamStarted()
	{
		return mStreamStarted;
	}
	
	/**
	 * Update the system tray notification to show the song info
	 */
	public void updateNotification() {
		notify_build.setContentText(mSongArtist + " - " + mSongTitle);
		
		NotificationManager notify_manager =
		        (NotificationManager) getSystemService(
		        		Context.NOTIFICATION_SERVICE);
		
		notify_manager.notify(NOTIFY_ID, notify_build.build());
	}
	
	/**
	 * Update the song details in the 
	 */
	public void updateSongDetails(String song_artist, String song_title, int listeners) {
		mGotSongInfo = true;
		this.mSongArtist = song_artist;
		this.mSongTitle = song_title;
		this.mListeners = listeners;
		
		updateNotification();
		
		if (mBound) {
			handler.postDelayed(sendUpdatesToUI, 1000);
		}
	}

	public void broadcastSongDetails() {
		if (mGotSongInfo) {
			Intent intent = new Intent(BROADCAST_ACTION);
			intent.putExtra("song_artist", mSongArtist);
			intent.putExtra("song_title", mSongTitle);
			intent.putExtra("listeners", mListeners);
			intent.putExtra("is_playing", mStreamStarted);
			
			sendBroadcast(intent);
		}
	}
	
    private Runnable sendUpdatesToUI = new Runnable() {
    	public void run() {
    		broadcastSongDetails();
    		Log.d(TAG, "Updating UI with song details");
    	}
    };    
}
