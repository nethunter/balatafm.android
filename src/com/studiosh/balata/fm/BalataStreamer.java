package com.studiosh.balata.fm;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;

public class BalataStreamer implements MediaPlayer.OnPreparedListener {
	private MediaPlayer media_player;
	
	private final String OGG_STREAM = "http://stream.balata.fm/stream.ogg";
	private final String MP3_STREAM = "http://stream.balata.fm/stream.mp3";

	public BalataStreamer() {
		media_player = new MediaPlayer();
		media_player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		
		try {
			media_player.setDataSource(OGG_STREAM);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		media_player.setOnPreparedListener(this);
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		mp.start();
	}

	public void play() {
		if (!media_player.isPlaying()) {
			media_player.prepareAsync();
		}
	}

	public void pause() {
		media_player.pause();
	}

	public void stop() {
		media_player.stop();
	}
	
	public void destroy() {
		stop();
		media_player = null;
	}
}
