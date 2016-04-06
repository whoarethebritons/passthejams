package com.passthejams.app;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

public class SearchActivity extends Activity {
    String TAG = "Search";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        //handleIntent(getIntent());
    }


    public void handleIntent(View view) {
        Log.d(TAG, "searched");
        EditText e = (EditText) findViewById(R.id.searchQuery);
        String query = e.getEditableText().toString();

        int row_id = R.layout.song_row;
        String[] projectionString = Shared.PROJECTION_SONG;

        String selectionString = MediaStore.Audio.Media.IS_MUSIC + "!=0 AND (instr(upper(" +
                MediaStore.Audio.Media.TITLE + "),upper(?)))";
        String[] selectionArguments = new String[]{query};
        Uri uri = Shared.libraryUri;
        String[] displayFields = new String[]{MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ID};
        int[] displayText = new int[] {R.id.songView, R.id.artistView, R.id.albumView, R.id.artView};
        String sortOrder = (MediaStore.Audio.Media.ALBUM + " ASC, "+MediaStore.Audio.Media.TRACK+" ASC");

        //testing code
        //query the database given the passed items
        Cursor mCursor = managedQuery(uri, projectionString, selectionString, selectionArguments, sortOrder);

        SimpleCursorAdapter simpleCursorAdapter = new JamsCursorAdapter(this, row_id, mCursor,
                displayFields, displayText);

        //set adapter
        GridLayout lv = (GridLayout) findViewById(R.id.songList);
        LayoutInflater inflater = (LayoutInflater)getApplicationContext().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        while(mCursor.moveToNext()){
            Log.d(TAG, "song count:" + mCursor.getCount());
            View row = inflater.inflate(row_id,null);
            TextView t = (TextView) row.findViewById(R.id.songView);
            t.setText(mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
            t = (TextView) row.findViewById(R.id.artistView);
            t.setText(mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
            t = (TextView) row.findViewById(R.id.albumView);
            t.setText(mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
            ImageView i = (ImageView) row.findViewById(R.id.artView);
            Shared.getAlbumArt(getApplicationContext(), i,
                    mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
            lv.addView(row);
        }

        row_id = R.layout.album_tile;projectionString = Shared.PROJECTION_ALBUM;
        uri = Shared.albumUri;
        displayFields = new String[]{
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums._ID};
        displayText =  new int[] {R.id.artistView, R.id.albumTitle, R.id.artView};
        selectionString = "(instr(upper(" +
                MediaStore.Audio.Media.ALBUM + "),upper(?)))";
        selectionArguments = new String[]{query};
        sortOrder = (MediaStore.Audio.Media.ALBUM + " ASC");

        //testing code
        //query the database given the passed items
        mCursor = managedQuery(uri, projectionString, selectionString, selectionArguments, sortOrder);

        simpleCursorAdapter = new JamsCursorAdapter(this, row_id, mCursor,
                displayFields, displayText);

        //set adapter
        lv = (GridLayout) findViewById(R.id.albumList);
        while(mCursor.moveToNext()){
            Log.d(TAG, "album count:" + mCursor.getCount());
            View row = inflater.inflate(row_id,null);
            TextView t = (TextView) row.findViewById(R.id.artistView);
            t.setText(mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)));
            t = (TextView) row.findViewById(R.id.albumTitle);
            t.setText(mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)));
            ImageView i = (ImageView) row.findViewById(R.id.artView);
            Shared.getAlbumArt(getApplicationContext(), i,
                    mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Albums._ID)));
            lv.addView(row);
        }

        row_id = R.layout.artist_tile;
        projectionString = Shared.PROJECTION_SONG;

        selectionString = "instr(upper(" +
                MediaStore.Audio.Media.ARTIST + "),upper(?))) GROUP BY("+ MediaStore.Audio.Media.ARTIST_ID;
        selectionArguments = new String[]{query};
        uri = Shared.libraryUri;
        displayFields = new String[]{
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Albums.ALBUM_ID};
        displayText =  new int[] {R.id.artistView, R.id.albumView};
        sortOrder = null;

        //testing code
        //query the database given the passed items
        mCursor = managedQuery(uri, projectionString, selectionString, selectionArguments, sortOrder);

        simpleCursorAdapter = new JamsCursorAdapter(this, row_id, mCursor,
                displayFields, displayText);

        //set adapter
        lv = (GridLayout) findViewById(R.id.artistList);
        while(mCursor.moveToNext()){
            Log.d(TAG, "artist count:" + mCursor.getCount());
            View row = inflater.inflate(row_id,null);
            TextView t = (TextView) row.findViewById(R.id.artistView);
            t.setText(mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)));
            ImageView i = (ImageView) row.findViewById(R.id.artView);
            Shared.getAlbumArt(getApplicationContext(), i,
                    mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID)));
            lv.addView(row);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void search(View view) {
        EditText e = (EditText) findViewById(R.id.searchQuery);
        String queryval = e.getEditableText().toString();

        int layout_id = R.layout.song_layout;
        int row_id = R.layout.song_row;
        String[] projectionString = Shared.PROJECTION_SONG;

        String selectionString = MediaStore.Audio.Media.IS_MUSIC + "!=0 AND (instr(upper(" +
                MediaStore.Audio.Media.TITLE + "),upper(?)) OR instr(upper(" +
                MediaStore.Audio.Albums.ALBUM + "),upper(?)) OR instr(upper(" +
                MediaStore.Audio.Artists.ARTIST + "),upper(?)))";
        String[] selectionArguments = new String[]{queryval, queryval, queryval};
        Uri uri = Shared.libraryUri;
        String[] displayFields = new String[]{MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ID};
        int[] displayText = new int[] {R.id.songView, R.id.artistView, R.id.albumView, R.id.artView};
        String sortOrder = (MediaStore.Audio.Media.ALBUM + " ASC, "+MediaStore.Audio.Media.TRACK+" ASC");
        //query the database given the passed items
        Cursor mCursor = managedQuery(uri, projectionString, selectionString, selectionArguments, sortOrder);
        int i = 0;
        while(mCursor.moveToNext()){
            Log.d(TAG, mCursor.getString(i));
            i++;
            Log.d(TAG, mCursor.getString(i));
            i++;
            Log.d(TAG, mCursor.getString(i));
            i++;
            Log.d(TAG, mCursor.getString(i));
        }
        SimpleCursorAdapter simpleCursorAdapter = new JamsCursorAdapter(this, layout_id, mCursor,
                displayFields, displayText);

        //set adapter
        GridView lv = (GridView) findViewById(android.R.id.list);
        lv.setAdapter(simpleCursorAdapter);
    }
}
