package com.studiosh.balata.fm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;

import com.studiosh.balata.fm.Eventbus.PlayerState;
import com.studiosh.balata.fm.Eventbus.SongInfo;

import de.greenrobot.event.EventBus;

public class BalataNotifier {
	private static final String TAG = "BalataNotifier";
	private final Handler handler = new Handler();
	public static final int NOTIFY_ID = 1345;

	private PlayerState mPlayerState = new PlayerState();
	private SongInfo mSongInfo;

    private Boolean mNotificationVisible = false;
	private NotificationCompat.Builder mNotifyBuild;

	private BalataController mController;
    private Service mService;
	
	public BalataNotifier(BalataController application, Service service) {
		mController = application;
        mService = service;
		EventBus.getDefault().registerSticky(this);
    }
	
	protected void destroy() {
		stopNotification();
		EventBus.getDefault().unregister(this);
    }

	public void onEvent(SongInfo songInfo) {
		mSongInfo = songInfo;
		updateNotification();
	}

	public void onEvent(PlayerState playerState) {
		mPlayerState = playerState;
		updateNotification();

        if (playerState.streamingState == PlayerState.StreamingState.STOPPED) {
            mService.stopForeground(false);
        } else {
            mService.startForeground(BalataNotifier.NOTIFY_ID, createNotification());
        }
	}

	/**
	 * Used to return from the notification to the main activity
	 *
	 * @return PendingIntent THe intent to create
	 */
	private PendingIntent getPendingIntent() {
		Intent intent = new Intent(mController.getApplicationContext(), MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NO_ANIMATION
                | Intent.FLAG_ACTIVITY_NEW_TASK);

		return PendingIntent.getActivity(mController.getApplicationContext(), 0,
					intent,PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private PendingIntent getBroadcastIntent(String command) {
		PendingIntent pi;

		Intent intent = new Intent(mController.getApplicationContext(),
				BalataNotifierService.class);
		intent.putExtra("COMMAND", command);

		pi = PendingIntent.getService(mController.getApplicationContext(), 12345,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		return pi;
	}

	private String getNotificationString() {
		if (mPlayerState.isBuffering()) {
			return mController.getString(R.string.buffering);
		} else {
			if (mSongInfo != null) {
				return mSongInfo.toString();
			} else {
				return mController.getString(R.string.retrieveing_song_details);
			}
		}
	}
	
	public Notification createNotification() {
		mNotifyBuild = new NotificationCompat.Builder(mController.getApplicationContext())
			.setContentTitle(mController.getString(R.string.balatafm))
			.setSmallIcon(R.drawable.ic_notification)
			.setContentIntent(getPendingIntent());

		mNotifyBuild.setContentText(getNotificationString());

		if (mPlayerState.isBuffering()) {
			mNotifyBuild.setProgress(0, 0, true);
		} else {
			if (mPlayerState.isPlaying()) {
				mNotifyBuild.addAction(R.drawable.player_stop, "Stop", getBroadcastIntent("stop"));
                mNotifyBuild.setOngoing(true);
			} else {
				mNotifyBuild.addAction(R.drawable.player_play, "Play", getBroadcastIntent("play"));
			}
		}

        mNotificationVisible = true;
		return mNotifyBuild.build();
	}
	
	public void updateNotification() {
        if (!mNotificationVisible) return;

		NotificationManager notify_manager =
		        (NotificationManager) mController.getApplicationContext().getSystemService(
		        		Context.NOTIFICATION_SERVICE);

		notify_manager.notify(NOTIFY_ID, createNotification());
	}
	
	public void stopNotification() {
		NotificationManager notify_manager =
		        (NotificationManager) mController.getApplicationContext().getSystemService(
		        		Context.NOTIFICATION_SERVICE);

		notify_manager.cancel(NOTIFY_ID);
	}
}
