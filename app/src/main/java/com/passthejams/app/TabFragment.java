package com.passthejams.app;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;


/**
 * A simple {@link Fragment} subclass.
 */
public class TabFragment extends Fragment {


    public TabFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tab, container, false);
    }
    @Override
    public void onStart() {
        super.onStart();
        // create the TabHost that will contain the Tabs
        TabHost tabHost = (TabHost) getActivity().findViewById(android.R.id.tabhost);
        if(tabHost.getCurrentView() == null) {
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
                break;
            case ALBUM:
                layout_id = R.layout.album_layout;
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
