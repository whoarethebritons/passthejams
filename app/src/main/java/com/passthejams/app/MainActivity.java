package com.passthejams.app;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TabHost;


public class MainActivity extends TabActivity implements BottomMusicFragment.OnFragmentInteractionListener,  Tab2Activity.Tab2Interface {
    final String TAG="main";

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

        // create the TabHost that will contain the Tabs
        TabHost tabHost = (TabHost)findViewById(android.R.id.tabhost);

        //Name the tag that each tab has
        TabHost.TabSpec tab1 = tabHost.newTabSpec("Albums");
        TabHost.TabSpec tab2 = tabHost.newTabSpec("Songs");

        // Set the Tab name and Activity
        // that will be opened when particular Tab will be selected
        tab1.setIndicator("Albums");
        tab1.setContent(new Intent(this, Tab1Activity.class));

        tab2.setIndicator("Songs");
        tab2.setContent(new Intent(this, Tab2Activity.class));

        //Add the tabs into the tabHost
        tabHost.addTab(tab1);
        tabHost.addTab(tab2);
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

    AdapterView.OnItemClickListener songListListener;
    //this is the override of the interface method that allows us to
    //give the main activity access to the itemclicklistener
    @Override
    public void onFragmentInteraction(AdapterView.OnItemClickListener fragmentService) {
        songListListener = fragmentService;
        Log.v(TAG, "changed listener");
    }

    @Override
    public Activity setActivity() {
        return this;
    }

    @Override
    public AdapterView.OnItemClickListener getListener() {
        return songListListener;
    }
}
