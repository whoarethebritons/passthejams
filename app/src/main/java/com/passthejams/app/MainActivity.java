package com.passthejams.app;

import android.app.*;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;


public class MainActivity extends Activity implements BottomMusicFragment.OnFragmentInteractionListener,
        GenericTabActivity.genericTabInterface, SelectedSongList.genericTabInterface, PlaylistSongList.genericTabInterface{
    final String TAG="main";
    private Cursor returnCursor;
    final int NOTIFICATION_ID = 24;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    String[] mDrawerItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = shared.getString("pref_themevalues", "Default");
        int choice = R.style.AppTheme;
        switch(theme) {
            case "Red":
                choice = R.style.RedTheme;
                break;
            case "Green":
                choice = R.style.GreenTheme;
                break;
            case "Blue":
                choice = R.style.BlueTheme;
                break;
            default:
                break;
        }
        Log.v(TAG, "THEME HAS BEEN CHANGED TO " + theme);
        setTheme(choice);
        setContentView(R.layout.activity_main);

        /*start network service*/
        Context context = getApplicationContext();
        Intent intent = new Intent(this,NetworkService.class);
        context.startService(intent);

        /*insert tab fragment into layout*/
        TabFragment tf = new TabFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.mainContent, tf); // mainContent is the container for the fragments
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.addToBackStack(null);
        ft.commit();

        //the items that go in the drawer menu
        mDrawerItems = new String[]{"Queue", "Network", "Search", "Theme"};

        mTitle = mDrawerTitle = getTitle();
        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ListView mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, mDrawerItems));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if(getActionBar() != null) {
                    getActionBar().setTitle(mTitle);
                }
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if(getActionBar() != null) {
                    getActionBar().setTitle(mDrawerTitle);
                }
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // enable ActionBar app icon to behave as action to toggle nav drawer
        if(getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
        //createNotification();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            Log.d(TAG, "searched");
            String query = intent.getStringExtra(SearchManager.QUERY);

            int layout_id = R.layout.song_layout;
            int row_id = R.layout.song_row;
            String[] projectionString = Shared.PROJECTION_SONG;

            String selectionString = MediaStore.Audio.Media.IS_MUSIC + "!=0 AND (instr(upper(" +
                    MediaStore.Audio.Media.TITLE + "),upper(?)) OR instr(upper(" +
                    MediaStore.Audio.Albums.ALBUM + "),upper(?)) OR instr(upper(" +
                    MediaStore.Audio.Artists.ARTIST + "),upper(?)))";
            String[] selectionArguments = new String[]{query, query, query};
            Uri uri = Shared.libraryUri;
            String[] displayFields = new String[]{MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                    MediaStore.Audio.Albums.ALBUM_ID};
            int[] displayText = new int[] {R.id.songView, R.id.artistView, R.id.albumView, R.id.artView};
            String sortOrder = (MediaStore.Audio.Media.ALBUM + " ASC, "+MediaStore.Audio.Media.TRACK+" ASC");

            //testing code
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
            //GridView lv = (GridView) findViewById(R.id.gridView);
            Intent retIntent = new Intent(this, GenericTabActivity.class);
            int list_id = android.R.id.list;
            retIntent.putExtra(Shared.TabIntent.LAYOUT.name(), layout_id);
            retIntent.putExtra(Shared.TabIntent.LISTVIEW.name(), list_id);
            retIntent.putExtra(Shared.TabIntent.ROWID.name(), row_id);
            retIntent.putExtra(Shared.TabIntent.PROJECTION_STRING.name(), projectionString);
            retIntent.putExtra(Shared.TabIntent.SELECTION_STRING.name(), selectionString);
            retIntent.putExtra(Shared.TabIntent.SELECTION_ARGS.name(), selectionArguments);
            retIntent.putExtra(Shared.TabIntent.SORT_ORDER.name(), sortOrder);
            retIntent.putExtra(Shared.TabIntent.URI.name(), uri.toString());
            retIntent.putExtra(Shared.TabIntent.DISPLAY_FIELDS.name(), displayFields);
            retIntent.putExtra(Shared.TabIntent.DISPLAY_TEXT.name(), displayText);
            retIntent.putExtra(Shared.TabIntent.TYPE.name(), "Search");
            ListFragment fragment = new ListFragment();
            fragment.setListAdapter(simpleCursorAdapter);
            FragmentTransaction ftnew = getFragmentManager().beginTransaction();
            ftnew.replace(R.id.mainContent, fragment); // mainContent is the container for the fragments
            ftnew.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ftnew.addToBackStack(null);
            ftnew.commit();
        }
    }

    @Override
    protected void onResume(){
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = shared.getString("pref_themevalues", "Default");
        int choice = R.style.AppTheme;
        switch(theme) {
            case "Red":
                choice = R.style.RedTheme;
                break;
            case "Green":
                choice = R.style.GreenTheme;
                break;
            case "Blue":
                choice = R.style.BlueTheme;
                break;
            default:
                break;
        }
        Log.v(TAG, "THEME HAS BEEN CHANGED TO " + theme);
        setTheme(choice);
        super.onResume();
        TabFragment tfnew = new TabFragment();
        FragmentTransaction ftnew = getFragmentManager().beginTransaction();
        ftnew.replace(R.id.mainContent, tfnew); // mainContent is the container for the fragments
        ftnew.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ftnew.addToBackStack(null);
        ftnew.commit();
    }
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            //TODO: have the drawer items open their respective views
            System.out.println("item click");
            Log.d(TAG, "clicked position: " + position);
            switch(mDrawerItems[position]) {
                case "Queue":
                    Log.v(TAG, "drawer menu clicked position: " + mDrawerItems[position]);
                    break;
                case "Network":
                    Log.v(TAG, "drawer menu clicked position: " + mDrawerItems[position]);
                    break;
                case "Search": //currently the Search
                    Log.v(TAG, "drawer menu clicked position: " + mDrawerItems[position]);
                    Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
                    startActivity(intent);
                    break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent oManager = new Intent(this, SettingsActivity.class);
            startActivity(oManager);
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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private AdapterView.OnItemClickListener songListListener;

    /**
     * @param fragmentService gives the main activity access to the itemclicklistener
     */
    @Override
    public void onFragmentInteraction(AdapterView.OnItemClickListener fragmentService) {
        songListListener = fragmentService;
        Log.v(TAG, "changed listener");
    }

    /**
     * @return returnCursor passes to the fragment */
    @Override
    public Cursor currentViewCursor() {
        return returnCursor;
    }

    @Override
    public AdapterView.OnItemLongClickListener getLongListener() {
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        if(tabHost.getCurrentTabTag().equals("Playlists")) {
            return new AdapterView.OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick (AdapterView<?> parent,
                                                View v, int position, long id){
                    final String playlistTitle = (String) ((TextView) v.findViewById(R.id.playlistName)).getText();
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Remove Playlist: "+playlistTitle+"");
                    builder.setMessage("Are you sure you want to delete this playlist?");
                    Log.v(TAG, "longclick set");


                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ContentResolver resolver = getContentResolver();
                            Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
                            //get the playlist id from db given playlist name
                            Cursor cursor = getContentResolver().query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                    Shared.PROJECTION_PLAYLIST,
                                    MediaStore.Audio.Playlists.NAME + " = " + "'" + playlistTitle.replace("'", "''") + "'",
                                    null, null);
                            cursor.moveToFirst();
                            int playlistID = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));

                            int rowsDeleted = resolver.delete(uri.buildUpon().appendPath(String.valueOf(playlistID)).build(), null, null);
                            Log.v(TAG, "Deleted "+playlistTitle+" with id: "+playlistID+". "+rowsDeleted+" rows removed.");
                            Context context = getApplicationContext();
                            int duration = Toast.LENGTH_LONG;

                            Toast toast = Toast.makeText(context, ("Playlist " + playlistTitle + " was removed!"), duration);
                            toast.show();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                    return  true;
                }
            };
        }
        else
            return null;
    }

    /**
     * @return OnItemClickListener to pass to the Tab
     * getListener takes the Listener from the Fragment and passes it to the Tab
     */
    @Override
    public AdapterView.OnItemClickListener getListener() {
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        if(tabHost == null) {
            FragmentManager fragmentManager = getFragmentManager();
            Fragment f = fragmentManager.findFragmentById(R.id.mainContent);
            if((f instanceof SelectedSongList || f instanceof PlaylistSongList)) {
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
        if(tabHost == null) {
            return null;
        }
        switch(tabHost.getCurrentTabTag()) {
            case "Songs":
                return songListListener;
            case "Search":
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
                        String playlistTitle = (String)((TextView) v.findViewById(R.id.playlistName)).getText();
                        PlaylistSongList pl = new PlaylistSongList();
                        Bundle bundle = new Bundle();
                        bundle.putString("TITLE", playlistTitle);
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
                        String artistName = (String)((TextView) v.findViewById(R.id.artistView)).getText();
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

    /**
     * @param c is a Cursor from one of the Tab views
     * @param type is what the Cursor is displaying (song, album, artist)
     * passCursor allows Tabs to send their cursor over to Main
     * so that it may send it to the Fragment
     */
    @Override
    public void passCursor(Cursor c, String type) {
        if(type.equals(Shared.TabType.SONG.name())) {
            returnCursor = c;
        }
    }

    private String albumid;

    /**
     * @param i allows MainActivity to pass the String albumid to NowPlayingFragment
     *          this is so it can load the album art onCreateView
     */
    @Override
    public void setImageVal(String i) {
        albumid = i;
    }

    /**
     * @param view is a view that the user clicks on
     *             that opens the NowPlayingFragment if one is not open
     */
    public void nowPlaying(View view) {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment f = fragmentManager.findFragmentById(R.id.mainContent);
        Log.v(TAG, "backstack val: " + fragmentManager.getBackStackEntryCount());
        if(! (f instanceof NowPlayingFragment)) {
            Bundle b = new Bundle();
            b.putString(Shared.Broadcasters.ART_VALUE.name(), albumid);
            Log.v(TAG, String.valueOf(f.isVisible()));
            Log.v(TAG, "added a now playing");
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            NowPlayingFragment fragment = new NowPlayingFragment();
            fragment.setArguments(b);
            fragmentTransaction.replace(R.id.mainContent, fragment);
            fragmentTransaction.addToBackStack("nowplaying");
            fragmentTransaction.commit();
        }

        Log.v(TAG, "backstack val: " + fragmentManager.getBackStackEntryCount());
    }

    /**
     * @param view is a view that the user clicks on
     *             that opens the Queue fragment if one is not open
     */
    public void showQueue(View view) {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment f = fragmentManager.findFragmentById(R.id.mainContent);
        Log.v(TAG, "backstack val: " + fragmentManager.getBackStackEntryCount());
        //if Queue is not already open
        if(! (f instanceof NowPlayingFragment.Queue)) {
            Log.v(TAG, String.valueOf(f.isVisible()));
            Log.v(TAG, "added a now playing");
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            NowPlayingFragment.Queue fragment = new NowPlayingFragment.Queue();
            fragmentTransaction.replace(R.id.mainContent, fragment);
            fragmentTransaction.addToBackStack("queue");
            fragmentTransaction.commit();
        }

        Log.v(TAG, "backstack val: " + fragmentManager.getBackStackEntryCount());
    }

    /**
     * Create and show a notification with a custom layout.
     * This callback is defined through the 'onClick' attribute of the
     * 'Show Notification' button in the XML layout.
     *
     * @param v
     */
    /*public void showNotificationClicked(View v) {
        createNotification();
    }*/
}