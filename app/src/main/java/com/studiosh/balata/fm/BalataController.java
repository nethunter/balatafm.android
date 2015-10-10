package com.studiosh.balata.fm;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.squareup.leakcanary.LeakCanary;
import com.studiosh.balata.fm.Eventbus.PlayerState;
import com.studiosh.balata.fm.Eventbus.PlayerState.StreamingState;

import de.greenrobot.event.EventBus;

public class BalataController extends Application {
    public static final String TAG = BalataController.class.getSimpleName();

    public static final String COMMAND_ACTION =
            "com.studiosh.balata.fm.COMMAND";

    private BalataStreamer mBalataStreamer;

    public static final String PREFS_NAME = "BalataPrefs";
    private PhoneStateListener mPhoneStateListener;

    private BalataNotifierService mService;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((BalataNotifierService.LocalBinder)service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
        EventBus.getDefault().registerSticky(this);
        registerPauseOnPhoneCall();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Boolean isPlaying = prefs.getBoolean("is_playing", false);

        if (isPlaying) {
            getStreamer().play();
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        EventBus.getDefault().unregister(this);
    }

    public void onEvent(PlayerState playerState) {
        if (playerState.streamingState != StreamingState.STOPPED) {
            bindService(new Intent(this, BalataNotifierService.class),
                    mServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            if (mService != null && getStreamer().isStreamStarted()) {
                unbindService(mServiceConnection);
            }
        }

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean("is_playing", playerState.streamingState != StreamingState.STOPPED);
        editor.commit();
    }

    public BalataStreamer getStreamer() {
        if (mBalataStreamer == null) {
            mBalataStreamer = new BalataStreamer(this);
        }

        return mBalataStreamer;
    }

    public void registerPauseOnPhoneCall()
    {
        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (mBalataStreamer.getPlayerState().streamingState != StreamingState.STOPPED) {
                    if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
                        mBalataStreamer.pause(true);
                    } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                        mBalataStreamer.pause(false);
                    }
                }
                super.onCallStateChanged(state, incomingNumber);
            }
        };
        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if(mgr != null) {
            mgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }
}
