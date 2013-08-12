package com.studiosh.balata.fm;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SongInfoService extends Service {
	private static final String TAG = "SongInfoService";

	private static BalataUpdater mUpdater;
			
	private static BalataStreamer mBalataStreamer;
	private static BalataNotifier mBalataNotifier;
		
	private PhoneStateListener mPhoneStateListener;
	    
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

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
		return mBinder;
	}
    
	// General Service Logic
	@Override
	public void onCreate() {
		super.onCreate();
						
		if (mBalataNotifier == null) {
			mBalataNotifier = new BalataNotifier(this);
		}

		if (mUpdater == null) {
			mUpdater = new BalataUpdater(mBalataNotifier);
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
		
		// Stop phone state listener
		TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		if(mgr != null) {
		    mgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
		}
				
		mUpdater.interrupt();
		mUpdater = null;

		if (mBalataStreamer != null) {
			mBalataStreamer.destroy();
			mBalataStreamer = null;
		}
		
		if (mBalataNotifier != null) {
			mBalataNotifier.destroy();
			mBalataNotifier = null;
		}
		
		Log.d(TAG, "Service destroyed");
	}
	
	public BalataStreamer getStreamer() {
		return mBalataStreamer;
	}
	
	public BalataNotifier getNotifier() {
		return mBalataNotifier;
	}
}
