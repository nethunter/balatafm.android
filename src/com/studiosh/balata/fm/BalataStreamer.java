package com.studiosh.balata.fm;

import java.io.IOException;

import android.media.AudioManager;
import android.media.MediaPlayer;

public class BalataStreamer {
	private MediaPlayer media_player;
	
	private final String OGG_STREAM = "http://stream.balata.fm/stream.ogg";
	private final String MP3_STREAM = "http://stream.balata.fm/stream.mp3";
	
	public BalataStreamer() {
		try {
			media_player = new MediaPlayer();
			media_player.setAudioStreamType(AudioManager.STREAM_MUSIC);
			media_player.setDataSource(OGG_STREAM);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void play() {
		try {
			media_player.prepare();  // might take long! (for buffering, etc)
			media_player.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void stop() {
		
	}
}
