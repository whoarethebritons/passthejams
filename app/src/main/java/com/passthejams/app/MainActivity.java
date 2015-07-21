package com.passthejams.app;

import android.accounts.*;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.ViewSwitcher;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;


public class MainActivity extends ActionBarActivity implements BottomMusicFragment.OnFragmentInteractionListener{
    Cursor cursor;
    final int SONG_TITLE = 1;
    final static String POSITION = "position", OPTION="option";
    MusicPlaybackService musicPlaybackService;
    Uri libUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        //MediaStore.Audio.Playlists.
        String[] mediaList = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE};
        //Google Play Music URI
        //libUri = Uri.parse("content://com.google.android.music.MusicContent/media");
        //Regular file storage URI
        libUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        cursor = managedQuery(libUri, mediaList, MediaStore.Audio.Media.IS_MUSIC + "!=0",
                null, null);

        ListView lv = (ListView) findViewById(android.R.id.list);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        String[] displayFields = new String[]{MediaStore.Audio.Media.TITLE};
        int[] displayText = new int[] {android.R.id.text1};
        SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor,
                displayFields, displayText);

        lv.setAdapter(simpleCursorAdapter);
        lv.setItemsCanFocus(false);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                View p = (View) view.getParent();
                int pos = ((ListView) findViewById(android.R.id.list)).getPositionForView(view);
                Intent playSong = new Intent(getApplicationContext(), MusicPlaybackService.class);
                playSong.putExtra(POSITION, pos);
                Log.v("main", " " + pos);
                playSong.putExtra(OPTION, "play");
                startService(playSong);
            }
        });
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
    public void onFragmentInteraction(Uri uri) {

    }
    public void onForward(View v) {
        Log.v("fragment", "forward clicked");
        Intent i = new Intent();
        i.putExtra(OPTION, "forward");
    }
    public void onBackward(View v) {
        Log.v("fragment", "back clicked");
        Intent i = new Intent();
        i.putExtra(OPTION, "backward");
    }
}
