package com.studiosh.balata.fm;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.studiosh.balata.fm.Eventbus.PlayerState;
import com.studiosh.balata.fm.Eventbus.SongInfo;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
    private SongInfo mSongInfo;
    private PlayerState mPlayerState = new PlayerState();

    @Bind(R.id.tv_song_info) TextView tvSongInfo;
    @Bind(R.id.btn_play_stop) ToggleButton btnPlayStop;
    @Bind(R.id.balata_logo) ImageView imgBalataLogo;
    @Bind(R.id.buffering) ProgressBar pbBuffering;
    @Bind(R.id.sb_volume) SeekBar sbVolume;

	private BalataController mController;

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

    @OnClick(R.id.btn_play_stop) void onPlayStopClick(ToggleButton tb) {
		Boolean isChecked = tb.isChecked();

		if (!isChecked) {
			if (mPlayerState.isPlaying()) {
				mController.getStreamer().stop();
			}
		} else {
			if (!mPlayerState.isPlaying()) {
				mController.getStreamer().play();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mController = (BalataController) getApplication();
		ButterKnife.bind(this);

		// Set the volume seek bar
        final AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // First let's handle the seek bar
		sbVolume.setMax(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
		sbVolume.setProgress(audio.getStreamVolume(AudioManager.STREAM_MUSIC));
		sbVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if (fromUser) {
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                }
            }
        });

		// Monitor system changes for volume change
		getContentResolver().registerContentObserver(
                android.provider.Settings.System.CONTENT_URI, true,
                new ContentObserver(new Handler()) {
                    public void onChange(boolean selfChange) {
                        super.onChange(selfChange);
                        sbVolume.setProgress(audio
                                .getStreamVolume(AudioManager.STREAM_MUSIC));
                    }
                }
        );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    protected void onResume() {
		super.onResume();
        EventBus.getDefault().registerSticky(this);

        // Initialize the state of the player
        mPlayerState = mController.getStreamer().getPlayerState();
        bindService(new Intent(this, BalataNotifierService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE);

        // Start the updates service
        if (mSongInfo == null) {
            tvSongInfo.setText(R.string.retrieveing_song_details);
        } else {
            tvSongInfo.setText(mSongInfo.streamTitle);
        }
	}
	
	protected void onPause() {
		super.onPause();
        EventBus.getDefault().unregister(this);
        unbindService(mServiceConnection);
    }

    public void onEventMainThread(SongInfo songInfo) {
        mSongInfo = songInfo;
        updateUI();
    }

    public void onEventMainThread(PlayerState playerState) {
        mPlayerState = playerState;
        updateUI();
    }

    public void updateUI() {
		btnPlayStop.setChecked(mPlayerState.streamingState != PlayerState.StreamingState.STOPPED);
		// btnPlayStop.setClickable(!buffering);
		btnPlayStop.setEnabled(!mPlayerState.isBuffering());

		if (mPlayerState.isBuffering()) {
			tvSongInfo.setText(getString(R.string.buffering));
			Animation animFade = AnimationUtils.loadAnimation(getApplicationContext(),
					R.anim.balata_logo_fades);
			imgBalataLogo.startAnimation(animFade);
			pbBuffering.setVisibility(View.VISIBLE);
		} else {
			tvSongInfo.setText(mSongInfo.streamTitle);

			imgBalataLogo.setAnimation(null);
			pbBuffering.setVisibility(View.INVISIBLE);
		}
	}
}
