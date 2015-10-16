package com.passthejams.app;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

/**
 * Created by Shadowbeing on 10/12/2015.
 */
public class Tab1Activity extends Activity
{
    Cursor mCursor;
    final String TAG="main";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.album_layout);

        //database columns
        String[] mediaList = {MediaStore.Audio.Media._ID, MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ID};

        //query the database for all things that are "music"
        mCursor = managedQuery(Shared.libraryUri, mediaList, MediaStore.Audio.Media.IS_MUSIC + "!=0",
                null, null);

        for(String s: mCursor.getColumnNames()) {
            Log.v(TAG, s);
        }
        //get the view
        GridView gv = (GridView) findViewById(R.id.albumGrid);
        gv.setChoiceMode(GridView.CHOICE_MODE_SINGLE);

        //text to display
        String[] displayFields = new String[]{ MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ID};

        //fields to display text in
        int[] displayText = new int[] {R.id.artistName, R.id.albumTitle, R.id.albumView};
        SimpleCursorAdapter simpleCursorAdapter = new myCursorAdapter(this, R.layout.album_tile, mCursor,
                displayFields, displayText);

        //set adapter
        gv.setAdapter(simpleCursorAdapter);
        gv.setFocusable(false);
    }
    public class myCursorAdapter extends SimpleCursorAdapter {
        public myCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to);
        }
        @Override
        public void setViewImage(ImageView v, String value) {
            v.setImageURI(Shared.getAlbumArt(getApplicationContext(), value));
        }
    }
}