package com.studiosh.balata.fm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.studiosh.balata.fm.BalataController.StreamingState;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	private static BalataNotifierService mSongInfoService;

	private BalataController mController;
	private String mSongArtist;
	private String mSongTitle;
	private BalataController.StreamingState mStreamingState;

	private BroadcastReceiver mSongDetailsReciever = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mSongArtist = intent.getStringExtra("song_artist");
			mSongTitle = intent.getStringExtra("song_title");

			updateUI();
		}
	};

	private BroadcastReceiver mStreamingStateReciever = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mStreamingState = StreamingState.values()[intent.getIntExtra("state", 0)];

			updateUI();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mController = BalataController.getInstance();

		// Handle the Start/Stop Button
		ToggleButton btnPlayStop = (ToggleButton) findViewById(R.id.btn_play_stop);
		btnPlayStop.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ToggleButton tb = (ToggleButton) v;
				Boolean isChecked = tb.isChecked();

				BalataStreamer streamer = mController.getStreamer();

				if (!isChecked) {
					if (mController.isStreamStarted()) {
						mController.stopStream();
					}
				} else {
					if (!mController.isStreamStarted()) {
						mController.startStream();
					}
				}

	            SharedPreferences settings = getSharedPreferences(
                        BalataController.PREFS_NAME, 0);
	            SharedPreferences.Editor editor = settings.edit();
	            editor.putBoolean("is_playing", streamer.isStreamStarted());
	            editor.commit();
			}
		});

		// Set the volume seek bar
		final SeekBar sbVolume = (SeekBar) findViewById(R.id.sb_volume);
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
		getApplicationContext().getContentResolver().registerContentObserver(
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
	protected void onStart() {
		super.onStart();

        // Start the updates service
		if (!mController.isStreamStarted()) {
			// Set the custom font for the text areas
			TextView tvSongInfo = (TextView) findViewById(R.id.tv_song_info);
			tvSongInfo.setText(R.string.retrieveing_song_details);
		}

	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}

	protected void onResume() {
		super.onResume();
		registerReceiver(mSongDetailsReciever, new IntentFilter(BalataController.SONG_DETAILS_UPDATE));
		registerReceiver(mStreamingStateReciever, new IntentFilter(BalataController.STREAMING_STATE_UPDATE));
	}
	
	protected void noPause() {
		super.onPause();
		unregisterReceiver(mSongDetailsReciever);
        unregisterReceiver(mStreamingStateReciever);
	}
	
	public void updateUI() {
		TextView tvSongInfo = (TextView) findViewById(R.id.tv_song_info);
		ToggleButton btnPlayStop = (ToggleButton) findViewById(R.id.btn_play_stop);
		ImageView imgBalataLogo = (ImageView) findViewById(R.id.balata_logo);
		ProgressBar pbBuffering = (ProgressBar) findViewById(R.id.buffering);

		boolean buffering = (mStreamingState == BalataController.StreamingState.BUFFERING);
		boolean playing = (mStreamingState == BalataController.StreamingState.PLAYING);

		btnPlayStop.setChecked(playing);
		// btnPlayStop.setClickable(!buffering);
		btnPlayStop.setEnabled(buffering);

		if (buffering) {
			tvSongInfo.setText(getString(R.string.buffering));
			Animation animFade = AnimationUtils.loadAnimation(getApplicationContext(),
					R.anim.balata_logo_fades);
			imgBalataLogo.startAnimation(animFade);
			pbBuffering.setVisibility(View.VISIBLE);
		} else {
			tvSongInfo.setText(mSongArtist + "\n" + mSongTitle);

			imgBalataLogo.setAnimation(null);
			pbBuffering.setVisibility(View.INVISIBLE);
		}
	}
}
