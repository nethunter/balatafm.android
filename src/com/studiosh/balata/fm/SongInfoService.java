package com.studiosh.balata.fm;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

public class SongInfoService extends Service {
	private static final String TAG = "SongInfoService";

	private static BalataUpdater mUpdater;
		
	private Boolean mGotSongInfo = false;
	private String mSongTitle;
	private String mSongArtist;
	
	private static BalataStreamer mBalataStreamer;
	private static BalataNotifier mBalataNotifier;
		
	private PhoneStateListener mPhoneStateListener;
	    
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    private static Boolean mBound = false;

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
    	SongInfoService.mBound = true;
    	
    	Log.d(TAG, "Bound service");
    	
        return mBinder;
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
    	mBound = false;
    	Log.d(TAG, "Unbound service");
    	return super.onUnbind(intent);
    }

	// General Service Logic
	@Override
	public void onCreate() {
		super.onCreate();
		
		if (mUpdater == null) {
			mUpdater = new BalataUpdater(this);
		}
				
		if (mBalataNotifier == null) {
			mBalataNotifier = new BalataNotifier(this);
		}

		if (mBalataStreamer == null) {
			mBalataStreamer = new BalataStreamer(mBalataNotifier);
		}
		
		Log.d(TAG, "Service created");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Service started");

		if (!mUpdater.isAlive()) {
			mUpdater.start();
		}

		mBalataNotifier.startNotification();
		
		pauseOnPhoneCall();
		
		return START_STICKY;
	}
	
	public void pauseOnPhoneCall()
	{
		mPhoneStateListener = new PhoneStateListener() {
		    @Override
		    public void onCallStateChanged(int state, String incomingNumber) {
		        if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
		        	mBalataStreamer.pause(true);
		        } else if(state == TelephonyManager.CALL_STATE_IDLE) {
		        	mBalataStreamer.pause(false);
		        }
		        super.onCallStateChanged(state, incomingNumber);
		    }
		};
		TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		if(mgr != null) {
		    mgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		}

	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mBalataStreamer != null) {
			mBalataStreamer.destroy();
			mBalataStreamer = null;
		}
		
		// Stop phone state listener
		TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		if(mgr != null) {
		    mgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
		}
		
		mUpdater.interrupt();
		mUpdater = null;

		Log.d(TAG, "Service destroyed");
	}
	
	public BalataStreamer getStreamer() {
		return mBalataStreamer;
	}
	
	/**
	 * Update the song details in the 
	 */
	public void updateSongDetails(String song_artist, String song_title) {
		
		mBalataNotifier.setSongDetails(mSongArtist, mSongTitle);
	}
}
