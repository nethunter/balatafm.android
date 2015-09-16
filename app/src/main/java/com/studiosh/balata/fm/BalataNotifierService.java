package com.studiosh.balata.fm;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class BalataNotifierService extends Service {
	private static final String TAG = "BalataNotifierService";

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    private BalataNotifier mNotifier;

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
    
	// General Service Logic
	@Override
	public void onCreate() {
		super.onCreate();
        Log.d(TAG, "Service created");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Service started");

        mNotifier = new BalataNotifier();
        startForeground(BalataController.NOTIFY_ID, mNotifier.createNotification());

		return START_STICKY;
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
        stopForeground(true);

		Log.d(TAG, "Service destroyed");
	}
	
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        BalataNotifierService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BalataNotifierService.this;
        }
    }
}
