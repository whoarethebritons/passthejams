package com.passthejams.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Admiral Sandvich on 11/15/2015.
 */
public class SelectedAlbumList extends Fragment {
    Cursor mCursor;
    final String TAG= "AlbumList";
    String artistName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.album_layout, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        //the fields to make the layout
        int layout_id = R.layout.album_layout;
        //artistName = getIntent().getStringExtra("ARTISTNAME");
        Bundle bundle = this.getArguments();
        artistName = bundle.getString("ARTISTNAME");
        int row_layout = R.layout.album_tile;
        Log.v(TAG, "Getting albums from " + artistName);

        //query to get the artistID of a given artist name
        Cursor cursor = getActivity().managedQuery(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Artists.ARTIST, MediaStore.Audio.Artists._ID},
                MediaStore.Audio.Artists.ARTIST + "=" + "'" + artistName + "'",
                null, null);
        int artistID = 0;
        if (cursor.moveToFirst()){
            artistID = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists._ID));
        }

        //Uri returns table with all albums containing an artist
        Uri artistAlbums = MediaStore.Audio.Artists.Albums.getContentUri("external", artistID);
        //query to get albums for a given artistID
        //necessary so it shows albums that an artist is in but no the author of EX:compilations
        mCursor = getActivity().managedQuery(artistAlbums, null, null, null,
                MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);

        GridView lv = (GridView) getView().findViewById(android.R.id.list);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        //text to display
        String[] displayFields = new String[]{
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums._ID};

        //fields to display text in
        int[] displayText = new int[]{R.id.artistName, R.id.albumTitle, R.id.albumView};
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent,
                                    View v, int position, long id) {
                String albumTitle = (String) ((TextView) v.findViewById(R.id.albumTitle)).getText();
                SelectedSongList al = new SelectedSongList();
                Bundle bundle = new Bundle();
                bundle.putString("TITLE", albumTitle);
                bundle.putBoolean("SONGLISTTYPE",true);
                al.setArguments(bundle);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.mainContent, al); // f1_container is your FrameLayout container
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.addToBackStack(null);
                ft.commit();
            }
        });
        SimpleCursorAdapter simpleCursorAdapter = new JamsCursorAdapter(getActivity(), row_layout, mCursor,
                displayFields, displayText);

        //set adapter
        lv.setAdapter(simpleCursorAdapter);

    }
}
