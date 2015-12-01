package com.passthejams.app;

import android.app.Activity;
import android.app.Fragment;
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
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Admiral Sandvich on 11/15/2015.
 */
public class SelectedSongList extends Fragment {
    Cursor mCursor;
    final String TAG= "SongList";
    String listTitle;
    int playlistID;
    boolean albumSongList;//1 for album list 0 for playlist list

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.song_layout, container, false);
    }
    @Override
    public void onStart()
    {
        super.onStart();

        int layout_id = R.layout.song_layout;
        Bundle bundle = this.getArguments();
        albumSongList = bundle.getBoolean("SONGLISTTYPE");
        listTitle = bundle.getString("TITLE");
        int row_layout = R.layout.song_row;
        //if null then we get a list from an album
        if (albumSongList) {
            Log.v(TAG, "Getting songs from album: " + listTitle);


            //query the database for album sorted by track number
            mCursor = getActivity().managedQuery(Shared.libraryUri, Shared.PROJECTION_SONG,
                    MediaStore.Audio.Media.IS_MUSIC + "!=0 AND " +
                    MediaStore.Audio.Albums.ALBUM + " = " + "'" + listTitle.replace("'","''") + "'",
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

            //get click listener
            lv.setOnItemClickListener(e.getListener());
            //pass cursor
            e.passCursor(mCursor, Shared.TabType.SONG.name());
        }
        //else we got a playlist
        else{
            Log.v(TAG, "Getting songs from playlist: " + listTitle);
            //get the playlist id from db given playlist name
            Cursor cursor = getActivity().managedQuery(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    Shared.PROJECTION_PLAYLIST,
                    MediaStore.Audio.Playlists.NAME + " = " + "'" + listTitle + "'",
                    null, null);
            if (cursor.moveToFirst()){
                playlistID = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
            }
            //columns for song info for songs in playlist
            String[] proj = {   MediaStore.Audio.Playlists.Members.AUDIO_ID,
                    MediaStore.Audio.Playlists.Members.ARTIST,
                    MediaStore.Audio.Playlists.Members.TITLE,
                    MediaStore.Audio.Playlists.Members.ALBUM,
                    MediaStore.Audio.Playlists.Members.ALBUM_ID,
                    MediaStore.Audio.Playlists.Members._ID
            };
            //Gets table of songs that are in the specified playlist
            Uri playlistMembers = MediaStore.Audio.Playlists.Members.getContentUri("external",playlistID);
            //cursor with songs from given playlist
            mCursor = getActivity().managedQuery(playlistMembers, proj, null, null,
                    MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);
            //Start rebuilding using shared URI
            //Cursor where we merge cursors
            Cursor passCursor;
            //Check to make sure cursor isn't empty
            if (mCursor.moveToFirst()){
                    //get the first song's id
                    String data = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID));
                    //temp cursor should only return 1 song
                    Cursor temp = getActivity().managedQuery(Shared.libraryUri,
                            Shared.PROJECTION_SONG,
                            MediaStore.Audio.Media._ID +" = "+"'"+data+"'",
                            null, (MediaStore.Audio.Media.TRACK + " ASC"));
                    // initialize passcursor with first item
                    passCursor = temp;
                    // get any other items in mCursor
                    while(mCursor.moveToNext()){
                        //get the rest of the song ids
                        data = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID));
                        temp = getActivity().managedQuery(Shared.libraryUri,
                            Shared.PROJECTION_SONG,
                            MediaStore.Audio.Media._ID + " = " + "'" + data + "'",
                            null, (MediaStore.Audio.Media.TRACK + " ASC"));
                        //cursor array for merge cursor, passCursor probably needs to be first
                        Cursor[] toMerge = new Cursor[]{passCursor,temp};
                        //passcursor gains rows 1 by 1 as long as there are rows left in mCursor
                        passCursor = new MergeCursor(toMerge);
                     }
                mCursor = passCursor;
                }

            ListView lv = (ListView) getView().findViewById(android.R.id.list);
            lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            //text to display
            String[] displayFields = new String[]{MediaStore.Audio.Playlists.Members.TITLE,
                    MediaStore.Audio.Playlists.Members.ARTIST, MediaStore.Audio.Playlists.Members.ALBUM,
                    MediaStore.Audio.Playlists.Members.ALBUM_ID};
            //fields to display text in
            int[] displayText = new int[]{R.id.songView, R.id.artistView, R.id.albumView, R.id.artView};

            SimpleCursorAdapter simpleCursorAdapter = new JamsCursorAdapter(getActivity(), row_layout, mCursor,
                    displayFields, displayText);

            //set adapter
            lv.setAdapter(simpleCursorAdapter);
            genericTabInterface e = (genericTabInterface) getActivity();
            //get click listener
            lv.setOnItemClickListener(e.getListener());
            //pass cursor
            e.passCursor(mCursor, Shared.TabType.SONG.name());
        }
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
