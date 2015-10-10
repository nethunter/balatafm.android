package com.studiosh.balata.fm.Eventbus;

public class PlayerState {
    public enum StreamingState {
        STOPPED, BUFFERING, PLAYING, PAUSED
    }

    public StreamingState streamingState;

    public PlayerState() {
        this.streamingState = StreamingState.STOPPED;
    }

    public PlayerState(StreamingState streamingState) {
        this.streamingState = streamingState;
    }

    public boolean isPlaying() {
        return streamingState == StreamingState.PLAYING;
    }

    public boolean isBuffering() {
        return streamingState == StreamingState.BUFFERING;
    }
}
