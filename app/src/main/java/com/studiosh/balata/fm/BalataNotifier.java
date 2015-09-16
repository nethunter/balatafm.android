package com.studiosh.balata.fm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;

import com.studiosh.balata.fm.BalataController.StreamingState;

public class BalataNotifier {
	private static final String TAG = "BalataNotifier";
	private final Handler handler = new Handler();

    private String mSongArtist;
    private String mSongTitle;
    private Boolean mBuffering = false;
    private Boolean mPlaying = false;

    private Boolean mNotificationVisible = false;

    private BalataController.StreamingState mStreamingState;
	private NotificationCompat.Builder mNotifyBuild;

    private BroadcastReceiver mSongDetailsReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mSongArtist = intent.getStringExtra("song_artist");
            mSongTitle = intent.getStringExtra("song_title");

            updateNotification();
        }
    };

    private BroadcastReceiver mStreamingStateReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            StreamingState streamingState =
                    BalataController.StreamingState.values()[intent.getIntExtra("state", 0)];

            boolean mBuffering = (streamingState == StreamingState.BUFFERING);
            boolean mPlaying = (streamingState == StreamingState.PLAYING);

            updateNotification();
        }
    };

	private BalataController mController;
	
	public BalataNotifier() {
		mController = BalataController.getInstance();

        mController.registerReceiver(mSongDetailsReciever, new IntentFilter(BalataController.SONG_DETAILS_UPDATE));
        mController.registerReceiver(mStreamingStateReciever, new IntentFilter(BalataController.STREAMING_STATE_UPDATE));
    }
	
	protected void destroy() {
		stopNotification();

        mController.unregisterReceiver(mSongDetailsReciever);
        mController.unregisterReceiver(mStreamingStateReciever);
    }

	private PendingIntent getPendingIntent() {
		Intent intent = new Intent(mController.getApplicationContext(), MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
				| Intent.FLAG_ACTIVITY_NO_ANIMATION
				| Intent.FLAG_ACTIVITY_NEW_TASK);

		PendingIntent pi = PendingIntent.getActivity(mController.getApplicationContext(), 0,
					intent,PendingIntent.FLAG_UPDATE_CURRENT);

		return pi;
	}
	
	private PendingIntent getBroadcastIntent(String command) {
		PendingIntent pi;

		Intent intent = new Intent();
		intent.setAction(BalataController.COMMAND_ACTION);
		intent.putExtra("COMMAND", command);

		pi = PendingIntent.getBroadcast(mController.getApplicationContext(), 12345,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		return pi;
	}
	
	private String getNotificationString() {
        boolean buffering = (mStreamingState == BalataController.StreamingState.BUFFERING);

		if (buffering) {
			return mController.getString(R.string.buffering);
		} else {
			if (mSongArtist != null && mSongTitle != null) {
				return mSongArtist + " - " + mSongTitle;
			} else {
				return mController.getString(R.string.retrieveing_song_details);
			}
		}
	}
	
	public Notification createNotification() {
		mNotifyBuild = new NotificationCompat.Builder(mController.getApplicationContext())
			.setContentTitle(mController.getString(R.string.balatafm))
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

        mNotificationVisible = true;
		return mNotifyBuild.build();
	}
	
	public void updateNotification() {
        if (!mNotificationVisible) return;

		mNotifyBuild.setContentText(getNotificationString());

		NotificationManager notify_manager =
		        (NotificationManager) mController.getApplicationContext().getSystemService(
		        		Context.NOTIFICATION_SERVICE);

		notify_manager.notify(BalataController.NOTIFY_ID, mNotifyBuild.build());
	}
	
	public void stopNotification() {
		NotificationManager notify_manager =
		        (NotificationManager) mController.getApplicationContext().getSystemService(
		        		Context.NOTIFICATION_SERVICE);

		notify_manager.cancel(BalataController.NOTIFY_ID);
	}
}
