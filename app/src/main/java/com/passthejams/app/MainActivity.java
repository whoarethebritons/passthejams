package com.passthejams.app;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;


public class MainActivity extends Activity implements BottomMusicFragment.OnFragmentInteractionListener{
    //Cursor to list the music
    Cursor mCursor;
    final String TAG="main", PREFS_NAME = "PASSTHEJAMS_PREF", FIRST_TIME="FIRST_TIME_PREF";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if (settings.getBoolean(FIRST_TIME, true)) {
            //the app is being launched for first time, do something
            Log.d("Comments", "First time");

            // first time create queue

            ContentResolver contentResolver = this.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Audio.Playlists.NAME, "passthejams queue");
            int queue_id = 0;

            Uri mUri = contentResolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, contentValues);
            final String[] PROJECTION_PLAYLIST = new String[] {
                    MediaStore.Audio.Playlists._ID,
                    MediaStore.Audio.Playlists.NAME,
                    MediaStore.Audio.Playlists.DATA
            };
            if(mUri != null) {
                Cursor c = contentResolver.query(mUri, PROJECTION_PLAYLIST, null, null, null);
                System.out.println(c.getColumnCount());
                if(c != null) {
                    queue_id = c.getInt(c.getColumnIndex(MediaStore.Audio.Playlists._ID));
                    c.close();
                }
            }

            // record the fact that the app has been started at least once
            settings.edit().putBoolean(FIRST_TIME, false).commit();
            settings.edit().putInt("QUEUE_ID", queue_id).commit();
        }

        //database columns
        String[] mediaList = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ID};
        //Google Play Music URI
        //libUri = Uri.parse("content://com.google.android.music.MusicContent/media");
        //Regular file storage URI

        //query the database for all things that are "music"
        mCursor = managedQuery(Shared.libraryUri, mediaList, MediaStore.Audio.Media.IS_MUSIC + "!=0",
                null, null);

        for(String s: mCursor.getColumnNames()) {
            Log.v(TAG, s);
        }
        //get the view
        ListView lv = (ListView) findViewById(android.R.id.list);
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
        lv.setItemsCanFocus(false);
    }
    public class myCursorAdapter extends SimpleCursorAdapter {
        public myCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to);
        }
        @Override
        public void setViewImage(@NonNull ImageView v, String value) {
            v.setImageURI(Shared.getAlbumArt(getApplicationContext(), value));
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

        //start network settings activity
        if (id == R.id.action_network_test) {
            Intent oManager = new Intent(this, NetworkTest.class);
            startActivity(oManager);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //this is the override of the interface method that allows us to
    //give the main activity access to the itemclicklistener
    @Override
    public void onFragmentInteraction(AdapterView.OnItemClickListener fragmentService) {
        Log.v(TAG, "adding item click listener");
        ListView lv = (ListView) findViewById(android.R.id.list);
        lv.setOnItemClickListener(fragmentService);
    }

    @Override
    public Activity setActivity() {
        return this;
    }

    public void makeNewQueue() {
    }

}
