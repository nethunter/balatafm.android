package com.studiosh.balata.fm;

import android.media.AudioManager;
import android.media.MediaPlayer;

import com.studiosh.balata.fm.Eventbus.PlayerCommand;
import com.studiosh.balata.fm.Eventbus.PlayerState;

import de.greenrobot.event.EventBus;

import static com.studiosh.balata.fm.Eventbus.PlayerState.StreamingState.BUFFERING;
import static com.studiosh.balata.fm.Eventbus.PlayerState.StreamingState.PAUSED;
import static com.studiosh.balata.fm.Eventbus.PlayerState.StreamingState.PLAYING;
import static com.studiosh.balata.fm.Eventbus.PlayerState.StreamingState.STOPPED;

public class BalataStreamer implements MediaPlayer.OnPreparedListener {
	private MediaPlayer mMediaPlayer;
	private Boolean mStreamStarted = false;
	private Boolean mPrevStreamState = false;
	private PlayerState mPlayerState = new PlayerState(STOPPED);
	private BalataController mController;

	private final String OGG_STREAM;
	private final String MP3_STREAM;

	public BalataStreamer(BalataController application) {
		mController = application;

		OGG_STREAM = mController.getString(R.string.balata_stream_url_ogg);
		MP3_STREAM = mController.getString(R.string.balata_stream_url_mp3);

		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		
		try {
			mMediaPlayer.setDataSource(MP3_STREAM);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mMediaPlayer.setOnPreparedListener(this);
		EventBus.getDefault().register(this);
	}

	public Boolean isStreamStarted() {
		return mStreamStarted;
	}

	public PlayerState getPlayerState() {
		return mPlayerState;
	}

	public void updatePlayerState(PlayerState.StreamingState streamingState) {
		mPlayerState = new PlayerState(streamingState);
		EventBus.getDefault().postSticky(mPlayerState);
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		if (mStreamStarted) {
			mp.start();
		}

		updatePlayerState(PLAYING);
	}

	public void play() {
		if (mPlayerState.streamingState != PLAYING) {
			mStreamStarted = true;
			mMediaPlayer.prepareAsync();
			updatePlayerState(BUFFERING);
		}
	}

	public void pause(Boolean pause) {
		if (mPlayerState.streamingState == PLAYING) {
			mPrevStreamState = mStreamStarted;
			mMediaPlayer.pause();
			updatePlayerState(PAUSED);
		} else if (mPlayerState.streamingState == PAUSED){
			if (mPrevStreamState) {
				mMediaPlayer.start();
				updatePlayerState(PLAYING);
			}
		}
	}

	public void stop() {
		if (mPlayerState.streamingState != STOPPED) {
			mStreamStarted = false;
			mMediaPlayer.stop();
			updatePlayerState(STOPPED);
		}
	}

	public void onEvent(PlayerCommand command) {
		switch (command.mCommand) {
			case PLAY:
				play();
				break;
			case PAUSE:
				pause(true);
				break;
			case STOP:
				stop();
				break;
		}
	}
	
	public void destroy() {
		EventBus.getDefault().unregister(this);
		stop();
		mMediaPlayer = null;
	}
}
