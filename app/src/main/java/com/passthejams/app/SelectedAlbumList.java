package com.passthejams.app;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

/**
 * Created by Francisco on 11/15/2015.
 * Given an artist name provide a list of albums that artist has appeared on.
 * Clicking on an album then provides a new view with the songs in the selected album.
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
        //Get passed parameters
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
        //necessary so it shows albums that an artist is in but not the author of EX:compilations
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
        //Here is where we take the name of the album of the selected album and create a new SelectedSongList
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
        //Take the cursor an fill in the view with the appropriate info from the columns
        SimpleCursorAdapter simpleCursorAdapter = new JamsCursorAdapter(getActivity(), row_layout, mCursor,
                displayFields, displayText);

        //set adapter
        lv.setAdapter(simpleCursorAdapter);

    }
}
