package com.passthejams.app;

import android.accounts.*;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;


public class MainActivity extends ListActivity {
    Cursor cursor;
    final int SONG_TITLE = 1;
    MediaPlayer mMediaPlayer;
    Uri libUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        String[] mediaList = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE};
        //Google Play Music URI
        //libUri = Uri.parse("content://com.google.android.music.MusicContent/media");
        //Regular file storage URI
        libUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        mMediaPlayer = new MediaPlayer();

        cursor = managedQuery(libUri, mediaList, MediaStore.Audio.Media.CONTENT_TYPE + "='" + MediaStore.Audio.Media.IS_MUSIC +"'", null, null);
        for(String s : cursor.getColumnNames()) {
            System.out.println(s);
        }
        String[] displayFields = new String[]{MediaStore.Audio.Media.TITLE};
        int[] displayText = new int[] {android.R.id.text1};
        setListAdapter(new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor, displayFields, displayText));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if(cursor.moveToPosition(position)) {
            Uri contentUri = ContentUris.withAppendedId(libUri.normalizeScheme(),
                    cursor.getLong(0));
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mMediaPlayer.setDataSource(this, contentUri);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onListItemClick(l, v, position, id);
    }
    public void clickOn(View v) {
        Intent intent = new Intent();
        intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Music"),1);
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
}
