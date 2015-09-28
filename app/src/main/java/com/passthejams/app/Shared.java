package com.passthejams.app;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.FileNotFoundException;

/**
 * Created by Eden on 7/21/2015.
 * The purpose of this class is for shared variables and methods
 * mainly for final Strings so you can just change them once
 */
public class Shared {
    public enum Broadcasters { BROADCAST_BUTTON, BROADCAST_ART, BUTTON_VALUE, ART_VALUE }
    public enum Main { POSITION, OPTION, DISCARD_PAUSE}
    public enum Service { NEXT, PLAY, PREVIOUS, PAUSE }
    static Uri libraryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

    static Uri getAlbumArt(Context mContext, String album_id) {
        int id = Integer.parseInt(album_id);
        Uri test = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"), id);
        try {
            mContext.getContentResolver().openInputStream(test);
        }
        catch(FileNotFoundException e) {
            Log.e("Shared", e.getMessage());
            test = null;
        }
        //Log.v("Shared", test.toString());
        return test;
    }
}
