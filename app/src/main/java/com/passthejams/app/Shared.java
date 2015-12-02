package com.passthejams.app;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by Eden on 7/21/2015.
 * The purpose of this class is for shared variables and methods
 * mainly for final Strings so you can just change them once
 */
public class Shared {
    public enum Broadcasters { BROADCAST_BUTTON, BROADCAST_ART, BUTTON_VALUE, ART_VALUE,
        BROADCAST_SONG, SONG_VALUE, BROADCAST_QUEUE, QUEUE_VALUE}
    public enum Main { POSITION, OPTION, DISCARD_PAUSE}
    public enum Service { NEXT, PLAY, PREVIOUS, PAUSE, BROADCAST_SHUFFLE, BROADCAST_REPEAT, SHUFFLE_VALUE,
        REPEAT_VALUE }

    public enum TabIntent { LAYOUT, LISTVIEW, ROWID, URI, PROJECTION_STRING, SELECTION_STRING, SELECTION_ARGS,
        SORT_ORDER,DISPLAY_FIELDS, DISPLAY_TEXT, TYPE }
    public enum TabType { SONG, ARTIST, ALBUM, PLAYLIST }
    static Uri libraryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    static Uri albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
    final static String[] PROJECTION_PLAYLIST = new String[] {
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME,
            MediaStore.Audio.Playlists.DATA
    };
    final static String[] PROJECTION_ARTIST = new String[] {
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Albums.ALBUM_ID
    };
    final static String[] PROJECTION_SONG = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ALBUM_ID
    };
    final static String[] PROJECTION_ALBUM = {
            "DISTINCT "+MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ARTIST
    };

    private static HashMap<Integer,Uri> albumArtLookup = new HashMap<>();

    static void getAlbumArt(Context mContext, ImageView v, String album_id) {
        int id = Integer.parseInt(album_id);
        if(albumArtLookup.containsKey(id)) {
            Uri uri = albumArtLookup.get(id);
            if(uri != null) {
                v.setImageURI(uri);
            } else {
                v.setImageResource(R.drawable.default_album);
            }
        } else {
            Uri test = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), id);
            InputStream in = null;
            try {
                in = mContext.getContentResolver().openInputStream(test);
                albumArtLookup.put(id, test);
                v.setImageURI(test);
            } catch (FileNotFoundException e) {
                Log.e("Shared", e.getMessage());
                albumArtLookup.put(id, null);
                v.setImageResource(R.drawable.default_album);
            } finally {
                if (in != null) try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

}
