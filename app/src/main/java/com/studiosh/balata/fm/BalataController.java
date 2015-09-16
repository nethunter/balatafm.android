package com.studiosh.balata.fm;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

/**
 * Created by nethunter on 9/16/15.
 */
public class BalataController extends Application {
    public static final String TAG = BalataController.class.getSimpleName();

    public static final String SONG_DETAILS_UPDATE =
            "com.studiosh.balata.fm.SONG_DETAILS_UPDATE";
    public static final String STREAMING_STATE_UPDATE =
            "com.studiosh.balata.fm.STREAMING_STATE_UPDATE";
    public static final String COMMAND_ACTION =
            "com.studiosh.balata.fm.COMMAND";

    private static BalataController mInstance;

    private static BalataNotifierService mSongInfoService;

    private static BalataUpdater mBalataUpdater;
    private static BalataStreamer mBalataStreamer;
    private static BalataNotifier mBalataNotifier;

    public static final int NOTIFY_ID = 1345;

    private String mSongArtist;
    private String mSongTitle;

    public static final String PREFS_NAME = "BalataPrefs";

    private PhoneStateListener mPhoneStateListener;

    private Intent mServiceIntent;
    private static Boolean mServiceStarted = false;
    private Boolean mBound = false;

    public enum StreamingState {
        STOPPED, BUFFERING, PLAYING, PAUSED
    }

    private StreamingState mStreamingState;

    private BroadcastReceiver mCommandReciever = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(COMMAND_ACTION)) {
                String command = intent.getStringExtra("COMMAND");

                if (command.equals("play")) {
                    startStream();
                } else if (command.equals("stop")) {
                    stopStream();
                }
            }
        }
    };

    public static synchronized BalataController getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        registerReceiver(mCommandReciever, new IntentFilter(COMMAND_ACTION));
        registerPauseOnPhoneCall();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean is_playing = settings.getBoolean("is_playing", false);
        if (is_playing) {
            startStream();
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterReceiver(mCommandReciever);
    }

    public StreamingState getStreamingState() {
        return mStreamingState;
    }

    public void setStreamingState(StreamingState streamingState) {
        if (streamingState != mStreamingState) {
            mStreamingState = streamingState;

            Intent intent = new Intent(STREAMING_STATE_UPDATE);
            intent.putExtra("state", streamingState.ordinal());

            sendBroadcast(intent);
        }
    }

    public boolean isStreamStarted() {
        StreamingState streamingState = getStreamingState();

        return streamingState != StreamingState.STOPPED;
    }

    public void startStream() {
        startBalataNotifierService();
        getUpdater().start();
        getStreamer().play();
    }

    public void pauseStream(boolean pause) {
        getStreamer().pause(pause);
    }

    public void stopStream() {
        getStreamer().stop();
        stopBalataNotifierService();
        stopBalataUpdaterThread();
    }

    public void startBalataNotifierService() {
        mServiceIntent = new Intent(this, BalataNotifierService.class);
        startService(mServiceIntent);
        mServiceStarted = true;
    }

    public void stopBalataNotifierService() {
        if (mServiceStarted && getStreamer().isStreamStarted()) {
            stopService(new Intent(this, BalataNotifierService.class));
            mServiceStarted = false;
        }
    }

    public void stopBalataUpdaterThread() {
        mBalataUpdater.interrupt();
        mBalataUpdater = null;
    }

    /**
     * Update the song details in the
     */
    public synchronized void updateSongDetails(String songArtist, String songTitle) {
        if (!songArtist.equals(mSongArtist) || !songTitle.equals(mSongArtist)) {
            mSongArtist = songArtist;
            mSongTitle = songTitle;


            sendBroadcast(getSongDetailsIntent());
        }
    }

    public Intent getSongDetailsIntent() {
        Intent intent = new Intent(SONG_DETAILS_UPDATE);
        intent.putExtra("song_artist", mSongArtist);
        intent.putExtra("song_title", mSongTitle);

        return intent;
    }

    public BalataStreamer getStreamer() {
        if (mBalataStreamer == null) {
            mBalataStreamer = new BalataStreamer();
        }

        return mBalataStreamer;
    }

    public BalataUpdater getUpdater() {
        if (mBalataUpdater == null) {
            mBalataUpdater = new BalataUpdater();
        }

        return mBalataUpdater;
    }

    public void registerPauseOnPhoneCall()
    {
        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    pauseStream(true);
                } else if(state == TelephonyManager.CALL_STATE_IDLE) {
                    pauseStream(false);
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
