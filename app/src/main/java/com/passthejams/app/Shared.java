package com.passthejams.app;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

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

    public enum TabIntent { LAYOUT, LISTVIEW, ROWID, URI, PROJECTION_STRING, SELECTION_STRING, SELECTION_ARGS,
        DISPLAY_FIELDS, DISPLAY_TEXT }
    public enum TabType { SONG, ARTIST, ALBUM, PLAYLIST }
    static Uri libraryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    static Uri albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
    static Uri artistUri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
    final static String[] PROJECTION_PLAYLIST = new String[] {
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME,
            MediaStore.Audio.Playlists.DATA
    };
    final static String[] PROJECTION_ARTIST = new String[] {
            "DISTINCT "+MediaStore.Audio.Artists._ID + " as _id",
            MediaStore.Audio.Artists.ARTIST,

            MediaStore.Audio.Albums.ALBUM_ID
    };
    final static String[] PROJECTION_SONG = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ALBUM_ID};
    final static String[] PROJECTION_ALBUM = {
            "DISTINCT "+MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ARTIST};

    static void getAlbumArt(Context mContext, ImageView v, String album_id) {
        int id = Integer.parseInt(album_id);
        Uri test = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"), id);
        try {
            mContext.getContentResolver().openInputStream(test);
            v.setImageURI(test);
        }
        catch(FileNotFoundException e) {
            Log.e("Shared", e.getMessage());
            v.setImageResource(R.drawable.default_album);
        }
    }

    public static class ImageLoader extends AsyncTask<Object, String, Uri> {
        final String TAG = "ImageLoader";
        private View imageView;
        @Override
        protected Uri doInBackground(Object... params) {
            imageView = (View) params[0];
            String id = (String) params[1];
            Context context = (Context) params[2];

            Uri test = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), Integer.parseInt(id));
            try {
                context.getContentResolver().openInputStream((test));
            }
            catch(FileNotFoundException e) {
                Log.e("Shared", e.getMessage());
                test = null;
            }
            return test;
        }
        @Override
        protected void onPostExecute(Uri uri) {
            if(imageView != null) {
                ImageView albumArt = (ImageView) imageView;
                if(imageView.getTag() != null) {
                    //Log.d(TAG, imageView.toString() + ":" + imageView.getTag().toString());
                    albumArt.setImageURI((Uri)imageView.getTag());
                }
                else {
                    //Log.d(TAG, imageView.toString() + ":" + null);
                    albumArt.setImageResource(R.drawable.default_album);
                }
            }
        }
    }

}
