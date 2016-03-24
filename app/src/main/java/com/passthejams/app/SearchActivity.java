package com.passthejams.app;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.SimpleCursorAdapter;

public class SearchActivity extends Activity {
    String TAG = "Search";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
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
        GridView lv = (GridView) findViewById(R.id.gridView);
        lv.setAdapter(simpleCursorAdapter);
    }
}
