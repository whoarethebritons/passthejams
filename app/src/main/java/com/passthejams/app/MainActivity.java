package com.passthejams.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TabHost;
import android.widget.TextView;


public class MainActivity extends Activity implements BottomMusicFragment.OnFragmentInteractionListener,
        GenericTabActivity.genericTabInterface, SelectedSongList.genericTabInterface{
    final String TAG="main";
    Cursor returnCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Context context = getApplicationContext();
        Intent intent = new Intent(this,NetworkService.class);
        context.startService(intent);


        TabFragment tf = new TabFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.mainContent, tf); // f1_container is your FrameLayout container
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.addToBackStack(null);
        ft.commit();

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
            /*SettingFragment s1 = new SettingFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.mainContent, s1); // f1_container is your FrameLayout container
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.addToBackStack(null);
            ft.commit();*/
            return true;
        }

        //start network settings activity
        if (id == R.id.action_network_list) {
            Intent oManager = new Intent(this, NetworkListActivity.class);
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
    /*this is the override of the interface method that allows us to
    give the main activity access to the itemclicklistener*/
    @Override
    public void onFragmentInteraction(AdapterView.OnItemClickListener fragmentService) {
        songListListener = fragmentService;
        Log.v(TAG, "changed listener");
    }

    /* currentViewCursor passes the current cursor to the fragment */
    @Override
    public Cursor currentViewCursor() {
        return returnCursor;
    }

    @Override
    public Activity setActivity() {
        return this;
    }

    /* getListener takes the Listener from the Fragment and passes it to the Tab */
    @Override
    public AdapterView.OnItemClickListener getListener() {
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        if(tabHost == null) {
            FragmentManager fragmentManager = getFragmentManager();
            Fragment f = fragmentManager.findFragmentById(R.id.mainContent);
            if((f instanceof SelectedSongList)) {
                return songListListener;
            }
            else if(f instanceof SelectedAlbumList) {
                return new AdapterView.OnItemClickListener()
                {
                    public void onItemClick(AdapterView<?> parent,
                                            View v, int position, long id)
                    {
                        String albumTitle = (String)((TextView) v.findViewById(R.id.albumTitle)).getText();
                        SelectedSongList al = new SelectedSongList();
                        Bundle bundle = new Bundle();
                        bundle.putString("TITLE", albumTitle);
                        bundle.putBoolean("SONGLISTTYPE",true);
                        al.setArguments(bundle);
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.replace(R.id.mainContent, al);
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                        ft.addToBackStack(null);
                        ft.commit();
                    }
                };
            }
        }
        switch(tabHost.getCurrentTabTag()) {
            case "Songs":
                return songListListener;
            case "Albums":
                return new AdapterView.OnItemClickListener()
                {
                    public void onItemClick(AdapterView<?> parent,
                                            View v, int position, long id)
                    {
                        String albumTitle = (String)((TextView) v.findViewById(R.id.albumTitle)).getText();
                        SelectedSongList al = new SelectedSongList();
                        Bundle bundle = new Bundle();
                        bundle.putString("TITLE", albumTitle);
                        bundle.putBoolean("SONGLISTTYPE",true);
                        al.setArguments(bundle);
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.replace(R.id.mainContent, al);
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                        ft.addToBackStack(null);
                        ft.commit();
                    }
                };
            case "Playlists":
                return new AdapterView.OnItemClickListener()
                {
                    public void onItemClick(AdapterView<?> parent,
                                            View v, int position, long id)
                    {
                        String playlistTitle = (String)((TextView) v.findViewById(R.id.artistName)).getText();
                        SelectedSongList pl = new SelectedSongList();
                        Bundle bundle = new Bundle();
                        bundle.putString("TITLE", playlistTitle);
                        bundle.putBoolean("SONGLISTTYPE", false);
                        pl.setArguments(bundle);
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.replace(R.id.mainContent, pl);
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                        ft.addToBackStack(null);
                        ft.commit();
                    }
                };
            case "Artists":
                return new AdapterView.OnItemClickListener()
                {
                    public void onItemClick(AdapterView<?> parent,
                                            View v, int position, long id)
                    {
                        String artistName = (String)((TextView) v.findViewById(R.id.artistName)).getText();
                        SelectedAlbumList albl = new SelectedAlbumList();
                        Bundle bundle = new Bundle();
                        bundle.putString("ARTISTNAME", artistName);
                        albl.setArguments(bundle);
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.replace(R.id.mainContent, albl);
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                        ft.addToBackStack(null);
                        ft.commit();

                    }
                };
            default:
                return null;
        }
    }

    /*
    passCursor allows Tabs to send their cursor over to Main
    so that it may send it to the Fragment
     */
    @Override
    public void passCursor(Cursor c, String type) {
        if(type.equals(Shared.TabType.SONG.name())) {
            returnCursor = c;
        }
    }

    public void nowPlaying(View view) {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment f = fragmentManager.findFragmentById(R.id.mainContent);
        Log.v(TAG, "backstack val: " + fragmentManager.getBackStackEntryCount());
        if(! (f instanceof NowPlayingFragment)) {
            Log.v(TAG, String.valueOf(f.isVisible()));
            Log.v(TAG, "added a now playing");
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            NowPlayingFragment fragment = new NowPlayingFragment();
            fragmentTransaction.replace(R.id.mainContent, fragment);
            fragmentTransaction.addToBackStack("nowplaying");
            fragmentTransaction.commit();
        }

        Log.v(TAG, "backstack val: " + fragmentManager.getBackStackEntryCount());
    }
}
