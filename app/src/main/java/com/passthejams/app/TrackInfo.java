package com.passthejams.app;

/**
 * Created by Eden on 11/6/2015.
 */
public class TrackInfo{
    int id, album_id;
    String name, artist;
    public TrackInfo() {}
    public TrackInfo(int id, int album_id, String name, String artist) {
        this.id = id;
        this.album_id = album_id;
        this.name = name;
        this.artist = artist;
    }

    @Override
    public boolean equals(Object o) {
        TrackInfo t2 = (TrackInfo) o;
        return t2.id == this.id && t2.name.equals(this.name);
    }
}
