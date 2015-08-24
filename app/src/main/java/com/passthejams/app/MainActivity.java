package com.passthejams.app;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;


public class MainActivity extends Activity implements BottomMusicFragment.OnFragmentInteractionListener{
    Cursor cursor;
    final int SONG_TITLE = 1;
    final String TAG="main";
    MusicPlaybackService musicPlaybackService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        String[] mediaList = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ID};
        //Google Play Music URI
        //libUri = Uri.parse("content://com.google.android.music.MusicContent/media");
        //Regular file storage URI

        cursor = managedQuery(Shared.libraryUri, mediaList, MediaStore.Audio.Media.IS_MUSIC + "!=0",
                null, null);

        for(String s: cursor.getColumnNames()) {
            Log.v(TAG, s);
        }
        ListView lv = (ListView) findViewById(android.R.id.list);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        String[] displayFields = new String[]{MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ID};
        int[] displayText = new int[] {R.id.songView, R.id.artistView, R.id.albumView, R.id.artView};
        SimpleCursorAdapter simpleCursorAdapter = new myCursorAdapter(this, R.layout.song_row, cursor,
                displayFields, displayText);

        lv.setAdapter(simpleCursorAdapter);
        lv.setItemsCanFocus(false);
    }
    public class myCursorAdapter extends SimpleCursorAdapter {

        public myCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to);
        }
        @Override
        public void setViewImage(ImageView v, String value) {
            v.setImageURI(Shared.getAlbumArt(value));
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

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }


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
}
