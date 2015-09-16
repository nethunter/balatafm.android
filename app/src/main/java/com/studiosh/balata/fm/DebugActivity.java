package com.studiosh.balata.fm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class DebugActivity extends Activity {
    private BalataController mController;

    private BroadcastReceiver mBroadcastReciever = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BalataController.STREAMING_STATE_UPDATE:
                    TextView text_streaming_state = (TextView) findViewById(R.id.text_streaming_state);
                    int state = intent.getIntExtra("state", 0);

                    text_streaming_state.setText(Integer.toString(state));
                    break;
                case BalataController.SONG_DETAILS_UPDATE:
                    TextView text_song_details = (TextView) findViewById(R.id.text_song_name);
                    String songArtist = intent.getStringExtra("song_artist");
                    String songTitle = intent.getStringExtra("song_title");
                    String songName = songArtist.concat(" - ").concat(songTitle);

                    text_song_details.setText(songName);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        mController = BalataController.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBroadcastReciever, new IntentFilter(BalataController.SONG_DETAILS_UPDATE));
        registerReceiver(mBroadcastReciever, new IntentFilter(BalataController.STREAMING_STATE_UPDATE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReciever);
    }

    public void startService(View v) {

    }

    public void startStream(View v) {
        mController.startStream();
    }

    public void play(View v) {
        mController.startStream();
    }

    public void pause(View v) {
        mController.pauseStream(true);
    }

    public void stop(View v) {
        mController.stopStream();
    }

    public void resume(View v) {
        mController.pauseStream(false);
    }
}
