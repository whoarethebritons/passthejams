package com.passthejams.app;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.TabHost;


public class MainActivity extends TabActivity implements BottomMusicFragment.OnFragmentInteractionListener,
        GenericTabActivity.genericTabInterface {
    final String TAG="main";
    Cursor returnCursor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create the TabHost that will contain the Tabs
        TabHost tabHost = (TabHost)findViewById(android.R.id.tabhost);

        //Name the tag that each tab has
        TabHost.TabSpec albumTab = tabHost.newTabSpec("Albums");
        TabHost.TabSpec songTab = tabHost.newTabSpec("Songs");
        TabHost.TabSpec artistTab = tabHost.newTabSpec("Artists");
        TabHost.TabSpec playlistTab = tabHost.newTabSpec("Playlists");

        // Set the Tab name and Activity
        // that will be opened when particular Tab will be selected
        albumTab.setIndicator("Albums");
        Intent albumIntent = generateTabIntent(Shared.TabType.ALBUM);
        albumTab.setContent(albumIntent);

        songTab.setIndicator("Songs");
        Intent songIntent = generateTabIntent(Shared.TabType.SONG);
        songTab.setContent(songIntent);

        artistTab.setIndicator("Artists");
        Intent artistIntent = generateTabIntent(Shared.TabType.ARTIST);
        artistTab.setContent(artistIntent);

        playlistTab.setIndicator("Playlists");
        Intent playlistIntent = generateTabIntent(Shared.TabType.PLAYLIST);
        playlistTab.setContent(playlistIntent);

        //Add the tabs into the tabHost
        tabHost.addTab(albumTab);
        tabHost.addTab(songTab);
        tabHost.addTab(artistTab);
        tabHost.addTab(playlistTab);

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
    public Cursor currentViewCursor() {
        return returnCursor;
    }

    @Override
    public Activity setActivity() {
        return this;
    }

    @Override
    public AdapterView.OnItemClickListener getListener() {
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        switch(tabHost.getCurrentTabTag()) {
            case "Songs":
                return songListListener;
            case "Albums":
                return null;
            case "Playlists":
                return null;
            case "Artists":
                return null;
            default:
                return null;
        }
    }

    @Override
    public void passCursor(Cursor c) {
        returnCursor = c;
    }
    public Intent generateTabIntent(final Shared.TabType type) {
        Intent retIntent = new Intent(this, GenericTabActivity.class);
        int layout_id = 0, list_id = 0, row_id = 0;
        String[] projectionString = new String[0], selectionArguments = new String[0], displayFields = new String[0];
        String selectionString = null;
        Uri uri = null;
        int[] displayText = new int[0];

        switch (type) {
            case SONG:
                layout_id = R.layout.song_layout;
                row_id = R.layout.song_row;
                list_id = R.id.songList;
                projectionString = Shared.PROJECTION_SONG;
                selectionString = MediaStore.Audio.Media.IS_MUSIC + "!=0";
                selectionArguments = null;
                uri = Shared.libraryUri;
                displayFields = new String[]{MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                        MediaStore.Audio.Albums.ALBUM_ID};
                displayText = new int[] {R.id.songView, R.id.artistView, R.id.albumView, R.id.artView};
                break;
            case ALBUM:
                layout_id = R.layout.album_layout;
                list_id = R.id.albumGrid;
                row_id = R.layout.album_tile;
                projectionString = Shared.PROJECTION_ALBUM;
                selectionString = MediaStore.Audio.Media.IS_MUSIC + "!=0";
                selectionArguments = null;
                uri = Shared.libraryUri;
                displayFields = new String[]{
                        MediaStore.Audio.Albums.ARTIST,
                        MediaStore.Audio.Albums.ALBUM,
                        MediaStore.Audio.Albums.ALBUM_ID};
                displayText =  new int[] {R.id.artistName, R.id.albumTitle, R.id.albumView};
                break;
            case ARTIST:
                layout_id = R.layout.artist_layout;
                list_id = R.id.artistGrid;
                row_id = R.layout.artist_tile;
                projectionString = Shared.PROJECTION_ARTIST;
                selectionString = MediaStore.Audio.Media.IS_MUSIC + "!=0";
                selectionArguments = null;
                uri = Shared.libraryUri;
                displayFields = new String[]{
                        MediaStore.Audio.Artists.ARTIST,
                        MediaStore.Audio.Albums.ALBUM_ID};
                displayText =  new int[] {R.id.artistName, R.id.albumView};
                break;
            case PLAYLIST:
                layout_id = R.layout.album_layout;
                list_id = R.id.albumGrid;
                row_id = R.layout.album_tile;
                projectionString = Shared.PROJECTION_PLAYLIST;
                selectionString = null;
                selectionArguments = null;
                uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
                displayFields = new String[]{
                        MediaStore.Audio.Playlists.NAME,};
                displayText =  new int[] {R.id.artistName};
                break;
            default:
                break;
        }
        retIntent.putExtra(Shared.TabIntent.LAYOUT.name(), layout_id);
        retIntent.putExtra(Shared.TabIntent.LISTVIEW.name(), list_id);
        retIntent.putExtra(Shared.TabIntent.ROWID.name(), row_id);
        retIntent.putExtra(Shared.TabIntent.PROJECTION_STRING.name(), projectionString);
        retIntent.putExtra(Shared.TabIntent.SELECTION_STRING.name(), selectionString);
        retIntent.putExtra(Shared.TabIntent.SELECTION_ARGS.name(), selectionArguments);
        retIntent.putExtra(Shared.TabIntent.URI.name(), uri.toString());
        retIntent.putExtra(Shared.TabIntent.DISPLAY_FIELDS.name(), displayFields);
        retIntent.putExtra(Shared.TabIntent.DISPLAY_TEXT.name(), displayText);
        return retIntent;
    }
}
