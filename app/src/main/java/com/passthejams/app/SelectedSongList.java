package com.passthejams.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * Created by Francisco on 11/15/2015.
 * Given a name of a song list and the type(album or playlist)
 * Return a view with the songs in the album or playlist and
 * have the songs playable when the song is clicked.
 */
public class SelectedSongList extends Fragment {
    Cursor mCursor;
    final String TAG = "SongList";
    String listTitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.song_layout, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle bundle = this.getArguments();
        //Name of the song or playlist
        listTitle = bundle.getString("TITLE");
        int row_layout = R.layout.song_row;
        //Different queries required to display an album vs a playlist

        Log.v(TAG, "Getting songs from album: " + listTitle);

        //query the database for album sorted by track number
        //replace required to query albums with apostrophes
        mCursor = getActivity().managedQuery(Shared.libraryUri,
                Shared.PROJECTION_SONG,
                MediaStore.Audio.Media.IS_MUSIC + "!=0 AND " +
                        MediaStore.Audio.Albums.ALBUM + " = " + "'" + listTitle.replace("'", "''") + "'",
                null, (MediaStore.Audio.Media.TRACK + " ASC"));

        //get the view as a ListView
        ListView lv = (ListView) getView().findViewById(android.R.id.list);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        //text to display
        String[] displayFields = new String[]{MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ID};
        //fields to display text in
        int[] displayText = new int[]{R.id.songView, R.id.artistView, R.id.albumView, R.id.artView};

        SimpleCursorAdapter simpleCursorAdapter = new JamsCursorAdapter(getActivity(), row_layout, mCursor,
                displayFields, displayText);

        //set adapter
        lv.setAdapter(simpleCursorAdapter);
        genericTabInterface e = (genericTabInterface) getActivity();

        //get click listener to play song
        lv.setOnItemClickListener(e.getListener());
        //pass cursor
        e.passCursor(mCursor, Shared.TabType.SONG.name());


    }

    //use onResume so that the cursor gets updated each time the tab is switched
    @Override
    public void onResume() {
        Log.v(TAG, "onResume: On tab changed");
        //get the AbsListView
        ListView lv = (ListView) getActivity().findViewById(android.R.id.list);
        Activity a = getActivity();
        genericTabInterface ef = (genericTabInterface) a;
        //set click listener
        lv.setOnItemClickListener(ef.getListener());
        //pass cursor
        ef.passCursor(mCursor, Shared.TabType.SONG.name());
        //call super to perform other actions
        super.onResume();
    }

    public interface genericTabInterface {
        AdapterView.OnItemClickListener getListener();

        void passCursor(Cursor c, String s);
    }


}
