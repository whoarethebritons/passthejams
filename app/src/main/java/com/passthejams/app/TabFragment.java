package com.passthejams.app;


import android.app.Activity;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;


/**
 * A simple {@link Fragment} subclass.
 */
public class TabFragment extends Fragment {

    final String TAG="TabFrag";

    public TabFragment() {
        // Bundle to save tab, can be used for other info
        setArguments(new Bundle());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.v(TAG, "Create tabFragment view");
        return inflater.inflate(R.layout.fragment_tab, container, false);

    }

    @Override
    //Called when a fragment starts a new fragment
    public void onPause() {
        super.onPause();
        TabHost tabHost = (TabHost) getActivity().findViewById(android.R.id.tabhost);
        getArguments().putInt("mMyCurrentTab", tabHost.getCurrentTab());
        Log.v(TAG, "Saving tab on pause:" + tabHost.getCurrentTab());
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalActivityManager lam = new LocalActivityManager(getActivity(),true);
        lam.dispatchCreate(lam.saveInstanceState());
        lam.dispatchResume();
        // create the TabHost that will contain the Tabs
        TabHost tabHost = (TabHost) getActivity().findViewById(android.R.id.tabhost);
        tabHost.setup(lam);
        Log.v(TAG,"Found tabhost");
        if(tabHost.getCurrentView() == null) {
            Log.v(TAG,"tabhost was null");
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
            //Set tab
            Log.v(TAG, "Getting tab:" + getArguments().getInt("mMyCurrentTab", 0));
            tabHost.setCurrentTab(getArguments().getInt("mMyCurrentTab", 0));
            Log.v(TAG, "Set tab to:" + tabHost.getCurrentTab());

        }
    }

    /*
     *This method generates an Intent for GenericTabActivity with the following:
     * *the layout id
     * *the list id
     * *the layout of the row
     * *the projection string
     * *the selection string
     * *the selection arguments
     * *the uri
     * *the display fields
     * *the display text
     */
    public Intent generateTabIntent(final Shared.TabType type) {
        Intent retIntent = new Intent(getActivity(), GenericTabActivity.class);
        int layout_id = 0, list_id = 0, row_id = 0;
        String[] projectionString = new String[0], selectionArguments = new String[0], displayFields = new String[0];
        String selectionString = null;
        String sortOrder = null;
        Uri uri = null;
        int[] displayText = new int[0];

        list_id = android.R.id.list;
        switch (type) {
            case SONG:
                layout_id = R.layout.song_layout;
                row_id = R.layout.song_row;
                projectionString = Shared.PROJECTION_SONG;
                selectionString = MediaStore.Audio.Media.IS_MUSIC + "!=0";
                selectionArguments = null;
                uri = Shared.libraryUri;
                displayFields = new String[]{MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                        MediaStore.Audio.Albums.ALBUM_ID};
                displayText = new int[] {R.id.songView, R.id.artistView, R.id.albumView, R.id.artView};
                sortOrder = (MediaStore.Audio.Media.ALBUM + " ASC, "+MediaStore.Audio.Media.TRACK+" ASC");
                break;
            case ALBUM:
                layout_id = R.layout.album_layout;
                row_id = R.layout.album_tile;
                projectionString = Shared.PROJECTION_ALBUM;
                selectionString = null;
                selectionArguments = null;
                uri = Shared.albumUri;
                displayFields = new String[]{
                        MediaStore.Audio.Albums.ARTIST,
                        MediaStore.Audio.Albums.ALBUM,
                        MediaStore.Audio.Albums._ID};
                displayText =  new int[] {R.id.artistName, R.id.albumTitle, R.id.albumView};
                sortOrder = (MediaStore.Audio.Media.ALBUM + " ASC");
                break;
            case ARTIST:
                layout_id = R.layout.artist_layout;
                row_id = R.layout.artist_tile;
                projectionString = Shared.PROJECTION_ARTIST;
                selectionString = MediaStore.Audio.Media.IS_MUSIC + "!=0) GROUP BY( "+ MediaStore.Audio.Media.ARTIST_ID;
                selectionArguments = null;
                uri = Shared.libraryUri;
                displayFields = new String[]{
                        MediaStore.Audio.Artists.ARTIST,
                        MediaStore.Audio.Albums.ALBUM_ID};
                displayText =  new int[] {R.id.artistName, R.id.albumView};
                sortOrder = (MediaStore.Audio.Artists.ARTIST + " ASC");
                break;
            case PLAYLIST:
                layout_id = R.layout.artist_layout;
                row_id = R.layout.artist_tile;
                projectionString = Shared.PROJECTION_PLAYLIST;
                selectionString = null;
                selectionArguments = null;
                uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
                displayFields = new String[]{
                        MediaStore.Audio.Playlists.NAME,MediaStore.Audio.Playlists._ID};
                displayText =  new int[] {R.id.artistName, R.id.albumView};
                sortOrder = (MediaStore.Audio.Playlists.NAME + " ASC");
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
        retIntent.putExtra(Shared.TabIntent.SORT_ORDER.name(), sortOrder);
        retIntent.putExtra(Shared.TabIntent.URI.name(), uri.toString());
        retIntent.putExtra(Shared.TabIntent.DISPLAY_FIELDS.name(), displayFields);
        retIntent.putExtra(Shared.TabIntent.DISPLAY_TEXT.name(), displayText);
        return retIntent;
    }

}
