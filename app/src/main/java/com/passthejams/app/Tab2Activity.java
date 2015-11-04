package com.passthejams.app;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * Created by Shadowbeing on 10/12/2015.
 */
public class Tab2Activity extends Activity
{
    Cursor mCursor;
    final String TAG="main";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.song_layout);

        //database columns
        String[] mediaList = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ID};

        //query the database for all things that are "music"
        mCursor = managedQuery(Shared.libraryUri, mediaList, MediaStore.Audio.Media.IS_MUSIC + "!=0",
                null, null);

        for(String s: mCursor.getColumnNames()) {
            Log.v(TAG, s);
        }
        //get the view
        ListView lv = (ListView) findViewById(R.id.songList);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        //text to display
        String[] displayFields = new String[]{MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ID};
        //fields to display text in
        int[] displayText = new int[] {R.id.songView, R.id.artistView, R.id.albumView, R.id.artView};
        SimpleCursorAdapter simpleCursorAdapter = new myCursorAdapter(this, R.layout.song_row, mCursor,
                displayFields, displayText);

        //set adapter
        lv.setAdapter(simpleCursorAdapter);
        lv.setItemsCanFocus(true);

    }
    @Override
    public void onContentChanged() {
        Log.v(TAG, "on content changed");
        ListView lv = (ListView) findViewById(R.id.songList);
        Activity a = getParent();
        Tab2Interface ef = (Tab2Interface) a;
        lv.setOnItemClickListener(ef.getListener());
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

    public interface Tab2Interface {
        AdapterView.OnItemClickListener getListener();
    }
}