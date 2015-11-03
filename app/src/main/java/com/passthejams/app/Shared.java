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

    public static class ImageLoader extends AsyncTask<Object, String, Uri> {
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
            //Bitmap bitmap = new Bitmap(uri);

            return test;
        }
        @Override
        protected void onPostExecute(Uri uri) {
            if(uri != null && imageView != null) {
                ImageView albumArt = (ImageView) imageView;
                albumArt.setImageURI(uri);
            }
        }
    }

}
