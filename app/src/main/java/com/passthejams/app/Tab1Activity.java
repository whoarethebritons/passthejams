package com.passthejams.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

/**
 * Created by Shadowbeing on 10/12/2015.
 */
public class Tab1Activity extends Activity
{
    Cursor mCursor;
    final String TAG="TAB1";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.album_layout);

        //database columns
        String[] mediaList = {"DISTINCT "+MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ARTIST};

        //query the database for all albums, different URI to remove duplicates
        mCursor = managedQuery(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, mediaList,
                null, null, (MediaStore.Audio.Media.ALBUM + " ASC"));

        for(String s: mCursor.getColumnNames()) {
            Log.v(TAG, s);
        }
        //get the view
        GridView gv = (GridView) findViewById(R.id.albumGrid);
        gv.setChoiceMode(GridView.CHOICE_MODE_SINGLE);

        //text to display
        String[] displayFields = new String[]{ MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums._ID};

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