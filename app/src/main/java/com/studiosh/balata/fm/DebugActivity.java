package com.studiosh.balata.fm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.studiosh.balata.fm.Eventbus.SongInfo;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;


public class DebugActivity extends Activity {
    final static String TAG = DebugActivity.class.toString();
    private BalataController mController;

    @Bind(R.id.text_song_name) TextView textSongDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        ButterKnife.bind(this);

        mController = (BalataController) getApplication();
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    public void onEvent(SongInfo songInfoUpdate) {
        textSongDetails.setText(songInfoUpdate.streamTitle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    public void startService(View v) {
        startService(new Intent(this, BalataNotifierService.class));
    }

    public void startStream(View v) {
        mController.getStreamer().play();
    }

    public void play(View v) {
        mController.getStreamer().play();
    }

    public void pause(View v) {
        mController.getStreamer().pause(true);
    }

    public void stop(View v) {
        mController.getStreamer().stop();
    }

    public void resume(View v) {
        mController.getStreamer().pause(false);
    }


}
