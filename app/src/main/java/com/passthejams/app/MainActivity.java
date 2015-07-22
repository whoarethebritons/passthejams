package com.passthejams.app;

import android.accounts.*;
import android.app.Activity;
import android.app.Fragment;
import android.app.ListActivity;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.ViewSwitcher;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


public class MainActivity extends Activity implements BottomMusicFragment.OnFragmentInteractionListener{
    Cursor cursor;
    final int SONG_TITLE = 1;
    final String TAG="main";
    MusicPlaybackService musicPlaybackService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        //MediaStore.Audio.Playlists.
        String[] mediaList = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ID};
        //Google Play Music URI
        //libUri = Uri.parse("content://com.google.android.music.MusicContent/media");
        //Regular file storage URI

        cursor = managedQuery(Shared.libraryUri, mediaList, MediaStore.Audio.Media.IS_MUSIC + "!=0",
                null, null);
        String join = "select * from audio JOIN album ON (audio.ALBUM_ID = album._ID)";

        /*CursorJoiner joiner = new CursorJoiner(songcursor,
                new String[]{MediaStore.Audio.Media.ALBUM_ID},
                albumArt, new String[]{MediaStore.Audio.Albums._ID});*/


        for(String s: cursor.getColumnNames()) {
            Log.v(TAG, s);
        }
        ListView lv = (ListView) findViewById(android.R.id.list);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        String[] displayFields = new String[]{MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ID};
                //, getAlbumArt(cursor.getPosition())};
        int[] displayText = new int[] {R.id.songView, R.id.artistView, R.id.albumView, R.id.artView};
        SimpleCursorAdapter simpleCursorAdapter = new myCursorAdapter(this, R.layout.song_row, cursor,
                displayFields, displayText);

        lv.setAdapter(simpleCursorAdapter);
        lv.setItemsCanFocus(false);
        Fragment fragment = (BottomMusicFragment) getFragmentManager().findFragmentById(R.id.bottomBar);
        /*lv.setOnItemClickListener(BottomMusicFragment);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override

        });*/
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
        /*Intent i = new Intent(this, MusicPlaybackService.class);
        i.putExtra(Shared.OPTION, "destroy");
        startService(i);*/
        super.onDestroy();
    }


    @Override
    public void onFragmentInteraction(AdapterView.OnItemClickListener fragmentService) {
        Log.v(TAG, "adding item click listener");
        ListView lv = (ListView) findViewById(android.R.id.list);
        lv.setOnItemClickListener(fragmentService);
    }
}
