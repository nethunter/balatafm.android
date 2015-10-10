package com.studiosh.balata.fm.Eventbus;

public class SongInfo {
    public final String streamTitle;

    public SongInfo(String streamTitle) {
        this.streamTitle = streamTitle;
    }

    @Override
    public String toString() {
        return streamTitle;
    }
}
