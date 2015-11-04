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
