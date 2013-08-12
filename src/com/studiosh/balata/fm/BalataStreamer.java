package com.studiosh.balata.fm;

import android.media.AudioManager;
import android.media.MediaPlayer;

public class BalataStreamer implements MediaPlayer.OnPreparedListener {
	private MediaPlayer mMediaPlayer;
	private BalataNotifier mBalataNotifier;
	
	private Boolean mStreamStarted = false;
	private Boolean mPrevStreamState = false;
	
	private final String OGG_STREAM = "http://stream.balata.fm/stream.ogg";
	private final String MP3_STREAM = "http://stream.balata.fm/stream.mp3";

	public BalataStreamer(BalataNotifier balataNotifier) {
		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		
		mBalataNotifier = balataNotifier;
		
		try {
			mMediaPlayer.setDataSource(MP3_STREAM);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mMediaPlayer.setOnPreparedListener(this);
	}

	public Boolean isStreamStarted() {
		return mStreamStarted;
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		if (mStreamStarted == true) {
			mp.start();
		}
		mBalataNotifier.setBuffering(false);
	}

	public void play() {
		if (!mMediaPlayer.isPlaying()) {
			mStreamStarted = true;
			mBalataNotifier.setPlaying(true);
			mBalataNotifier.setBuffering(true);
			mMediaPlayer.prepareAsync();
		}
	}

	public void pause(Boolean pause) {
		if (pause == true) {
			mPrevStreamState = mStreamStarted;
			mMediaPlayer.pause();
		} else {
			if (mPrevStreamState == true) {
				mMediaPlayer.start();
			}
		}
	}

	public void stop() {
		mStreamStarted = false;		
		mBalataNotifier.setBuffering(false);
		mBalataNotifier.setPlaying(false);
		mMediaPlayer.stop();		
	}
	
	public void destroy() {
		stop();
		mMediaPlayer = null;
	}
}
