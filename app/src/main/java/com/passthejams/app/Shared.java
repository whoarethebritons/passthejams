package com.passthejams.app;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

/**
 * Created by Eden on 7/21/2015.
 * The purpose of this class is for shared variables and methods
 * mainly for final Strings so you can just change them once
 */
public class Shared {
    //broadcast values
    final static String BROADCAST_BUTTON="button-event", BROADCAST_ART="album-art",
            BUTTON_VALUE="value", ART_VALUE="art",
            //Strings for Fragment
            NEXT="next", PLAY="play", PREV="previous", PAUSE="pause", SERVICE="service",
            //Strings for MainActivity
            POSITION = "position", OPTION="option", DISCARD_PAUSE="discard";

    static Uri libraryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

    static Uri getAlbumArt(String album_id) {
        int id = Integer.parseInt(album_id);
        Uri test = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"), id);
        Log.v("Shared", test.toString());
        return test;
    }
}
