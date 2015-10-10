package com.studiosh.balata.fm;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.studiosh.balata.fm.Eventbus.PlayerCommand;
import com.studiosh.balata.fm.Eventbus.SongInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import de.greenrobot.event.EventBus;

public class BalataNotifierService extends Service {
	private static final String TAG = "BalataNotifierService";

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
	private BalataController mController;
    private BalataNotifier mNotifier;
    private SongInfo mSongInfo;

	private Socket mSocket;

	private Emitter.Listener onMetadataRecieved = new Emitter.Listener() {
		@Override
		public void call(Object... args) {
            JSONObject jsonMetadata = (JSONObject) args[0];
            String streamTitle;
            try {
                streamTitle = jsonMetadata.getString("StreamTitle");
                mSongInfo = new SongInfo(streamTitle);
                EventBus.getDefault().postSticky(mSongInfo);

                Log.d(TAG, streamTitle);
            } catch (JSONException e) {
                e.printStackTrace();
            }
		}
	};

    @Override
	public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service started");

        if (mSocket == null) {
            mSocket = getSocket();
            mSocket.on("metadata", onMetadataRecieved);
            mSocket.connect();
        }

        if (mNotifier == null) {
            mNotifier = new BalataNotifier(mController, this);
        }

        return mBinder;
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        stopSelf();

        Log.d(TAG, "Started service");

        if (intent != null) {
            String command = intent.getStringExtra("COMMAND");

            if (command.equals("play")) {
                EventBus.getDefault().post(new PlayerCommand(PlayerCommand.CommandEnum.PLAY));
            } else if (command.equals("stop")) {
                EventBus.getDefault().post(new PlayerCommand(PlayerCommand.CommandEnum.STOP));
            }
        }

        return 0;
    }

    // General Service Logic
	@Override
	public void onCreate() {
		super.onCreate();
        Log.d(TAG, "Service created");

		mController = (BalataController) getApplication();
	}

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    @Override
	public void onDestroy() {
		super.onDestroy();

        stopForeground(true);
        mNotifier = null;

        mSocket.disconnect();
		mSocket.off("metadata", onMetadataRecieved);
        mSocket = null;

		Log.d(TAG, "Service destroyed");
	}

    public Socket getSocket() {
        if (mSocket == null) {
            try {
                mSocket = IO.socket(getString(R.string.balata_url));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        return mSocket;
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
