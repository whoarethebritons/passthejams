package com.passthejams.app;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;


public class MainActivity extends ListActivity {
    Cursor cursor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String[] mediaList = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE};
        //Google Play Music URI
        Uri songUri = Uri.parse("content://com.google.android.music.MusicContent/audio");
        //Regular file storage URI
        //Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        cursor = managedQuery(songUri, mediaList, null, null, null);
        String[] displayFields = new String[]{MediaStore.Audio.Media.TITLE};
        int[] displayText = new int[] {android.R.id.text1};
        setListAdapter(new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor, displayFields,displayText));

    }

    public void onListItemClicked(ListView listView, View view, int position, long id) {
        if(cursor)
    }

    public void clickOn(View v) {
        Intent intent = new Intent();
        intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"),1);
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
