package com.passthejams.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;


/**
 * Created by Francisco on 11/15/2015.
 * Given a name of a song list and the type(album or playlist)
 * Return a view with the songs in the album or playlist and
 * have the songs playable when the song is clicked.
 */
public class PlaylistSongList extends Fragment {
    DragSortListView listView;
    SimpleCursorAdapter simpleCursorAdapter;
    Cursor mCursor;
    final String TAG = "PlaylistSongList";
    String listTitle;
    int playlistID;

    private Cursor rebuildCursor(Cursor toRebuild)
    {
        Cursor passCursor;
        //Check to make sure cursor isn't empty
        if (toRebuild.moveToFirst()) {
            //get the first song's id
            String audioID = toRebuild.getString(toRebuild.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID));
            //temp cursor should only return 1 song
            Cursor temp = getActivity().getContentResolver().query(Shared.libraryUri,
                    Shared.PROJECTION_SONG,
                    MediaStore.Audio.Media._ID + " = " + "'" + audioID + "'",
                    null, (MediaStore.Audio.Media.TRACK + " ASC"));
            // initialize passcursor with first item
            passCursor = temp;
            // get any other items in mCursor
            while (toRebuild.moveToNext()) {
                //get the rest of the song ids
                audioID = toRebuild.getString(toRebuild.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID));
                temp = getActivity().getContentResolver().query(Shared.libraryUri,
                        Shared.PROJECTION_SONG,
                        MediaStore.Audio.Media._ID + " = " + "'" + audioID + "'",
                        null, (MediaStore.Audio.Media.TRACK + " ASC"));
                //cursor array for merge cursor, passCursor needs to be first
                Cursor[] toMerge = new Cursor[]{passCursor, temp};
                //passcursor gains rows 1 by 1 as long as there are rows left in mCursor
                passCursor = new MergeCursor(toMerge);
            }
            //passCursor is now built like mCursor but with proper URI and columns to play songs
            return passCursor;
        }
        else return mCursor;
    }
    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener()
    {
        @Override
        public void drop(int from, int to)
        {
            if (from != to)
            {
                //Updating the playlist
                ContentResolver resolver = getActivity().getContentResolver();
                MediaStore.Audio.Playlists.Members.moveItem(resolver, playlistID, from, to);
                Log.v(TAG, "Moved song " + from + " to position "+ to + " in playlist: "+listTitle);
                //Updating the cursor for the listview
                Uri playlistMembers = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistID);
                String[] proj = {MediaStore.Audio.Playlists.Members.AUDIO_ID,
                        MediaStore.Audio.Playlists.Members.ARTIST,
                        MediaStore.Audio.Playlists.Members.TITLE,
                        MediaStore.Audio.Playlists.Members.ALBUM,
                        MediaStore.Audio.Playlists.Members.ALBUM_ID,
                        MediaStore.Audio.Playlists.Members._ID
                };
                Cursor cursor = getActivity().getContentResolver().query(playlistMembers, proj, null, null,
                        MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);
                //Rebuilding using shared URI
                mCursor = rebuildCursor(cursor);
                //Updating listview with new cursor
                simpleCursorAdapter.changeCursor(mCursor);
                simpleCursorAdapter.notifyDataSetChanged();
                //Pass cursor to media player
                Activity a = getActivity();
                genericTabInterface e = (genericTabInterface) a;
                listView.setOnItemClickListener(e.getListener());
                e.passCursor(mCursor, Shared.TabType.SONG.name());

            }
        }
    };




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.playlist_song_layout, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle bundle = this.getArguments();
        //Name of the song or playlist
        listTitle = bundle.getString("TITLE");
        int row_layout = R.layout.song_row;
        listView = (DragSortListView) getView().findViewById(R.id.plistView);
        Log.v(TAG, "Getting songs from playlist: " + listTitle);

        //get the playlist id from db given playlist name
        Cursor cursor = getActivity().getContentResolver().query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                Shared.PROJECTION_PLAYLIST,
                MediaStore.Audio.Playlists.NAME + " = " + "'" + listTitle.replace("'", "''") + "'",
                null, null);
        if (cursor.moveToFirst()) {
            playlistID = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
        }
        //columns for song info for songs in playlist
        String[] proj = {MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.ARTIST,
                MediaStore.Audio.Playlists.Members.TITLE,
                MediaStore.Audio.Playlists.Members.ALBUM,
                MediaStore.Audio.Playlists.Members.ALBUM_ID,
                MediaStore.Audio.Playlists.Members._ID
        };
        //Gets table of songs that are in the specified playlist
        Uri playlistMembers = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistID);

        //cursor with songs from given playlist sorted by insertion order
        mCursor = getActivity().getContentResolver().query(playlistMembers, proj, null, null,
                MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);
        //Rebuilding using shared URI
        mCursor = rebuildCursor(mCursor);

        //text to display
        String[] displayFields = new String[]{MediaStore.Audio.Playlists.Members.TITLE,
                MediaStore.Audio.Playlists.Members.ARTIST, MediaStore.Audio.Playlists.Members.ALBUM,
                MediaStore.Audio.Playlists.Members.ALBUM_ID};
        //fields to display text in
        int[] displayText = new int[]{R.id.songView, R.id.artistView, R.id.albumView, R.id.artView};

         simpleCursorAdapter = new JamsCursorAdapter(getActivity(), row_layout, mCursor,
                displayFields, displayText);


        listView.setAdapter(simpleCursorAdapter);
        listView.setDropListener(onDrop);

        DragSortController controller = new DragSortController(listView);
        controller.setDragHandleId(R.id.artView);
        controller.setRemoveEnabled(false);
        controller.setSortEnabled(true);
        controller.setDragInitMode(1);

        listView.setFloatViewManager(controller);
        listView.setOnTouchListener(controller);
        listView.setDragEnabled(true);



        //set adapter
        Activity a = getActivity();
        genericTabInterface e = (genericTabInterface) a;
        //get click listener
        listView.setOnItemClickListener(e.getListener());
        listView.setOnItemLongClickListener(deleteSong());
        //pass cursor
        e.passCursor(mCursor, Shared.TabType.SONG.name());




    }

    //use onResume so that the cursor gets updated each time the tab is switched
    @Override
    public void onResume() {
        Log.v(TAG, "onResume: On tab changed");
        //get the AbsListView
        //ListView lv = (ListView) getActivity().findViewById(R.id.plistView);
        Activity a = getActivity();
        genericTabInterface ef = (genericTabInterface) a;
        //set click listener
        listView.setOnItemClickListener(ef.getListener());
        listView.setOnItemLongClickListener(deleteSong());
        //pass cursor
        ef.passCursor(mCursor, Shared.TabType.SONG.name());
        //call super to perform other actions
        super.onResume();
    }



    AdapterView.OnItemLongClickListener deleteSong() {
            return new AdapterView.OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick (AdapterView<?> parent,
                                                View v, int position, long id){
                    final String songTitle = (String) ((TextView) v.findViewById(R.id.songView)).getText();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Remove Song: "+songTitle+"");
                    builder.setMessage("Are you sure you want to delete this song?");
                    Log.v(TAG, "longclick set");
                    final int songPos = position;


                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ContentResolver resolver = getActivity().getContentResolver();
                            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistID);
                            String[] proj = {MediaStore.Audio.Playlists.Members.AUDIO_ID,
                                    MediaStore.Audio.Playlists.Members.ARTIST,
                                    MediaStore.Audio.Playlists.Members.TITLE,
                                    MediaStore.Audio.Playlists.Members.ALBUM,
                                    MediaStore.Audio.Playlists.Members.ALBUM_ID,
                                    MediaStore.Audio.Playlists.Members._ID};
                            //get the playlist id from db given playlist name
                            Cursor cursor = getActivity().getContentResolver().query(uri, proj, null, null,
                                    MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);
                            cursor.moveToPosition(songPos);
                            int songID = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members._ID));
                            //take position into consideration rather than song id in case of multiple of same song in playlist
                            //int songID = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                            String where = MediaStore.Audio.Playlists.Members._ID + " =? ";
                            String[] selectionArgs = {Integer.toString(songID)};
                            int rowsDeleted = resolver.delete(uri, where, selectionArgs);
                            Log.v(TAG, "Deleted song with "+rowsDeleted+" rows removed.");
                            mCursor = getActivity().getContentResolver().query(uri, proj, null, null,
                                    MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);
                            mCursor = rebuildCursor(mCursor);
                            String[] displayFields = new String[]{MediaStore.Audio.Playlists.Members.TITLE,
                                    MediaStore.Audio.Playlists.Members.ARTIST, MediaStore.Audio.Playlists.Members.ALBUM,
                                    MediaStore.Audio.Playlists.Members.ALBUM_ID};
                            int row_layout = R.layout.song_row;
                            //fields to display text in
                            int[] displayText = new int[]{R.id.songView, R.id.artistView, R.id.albumView, R.id.artView};

                            simpleCursorAdapter = new JamsCursorAdapter(getActivity(), row_layout, mCursor,
                                    displayFields, displayText);


                            listView.setAdapter(simpleCursorAdapter);
                            //simpleCursorAdapter.changeCursor(mCursor);
                            //simpleCursorAdapter.notifyDataSetChanged();
                            genericTabInterface ef = (genericTabInterface) getActivity();
                            ef.passCursor(mCursor, Shared.TabType.SONG.name());
                            //int duration = Toast.LENGTH_LONG;
                            //Toast toast = Toast.makeText(context, "Song was removed", duration);
                            //toast.show();
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

    public interface genericTabInterface {
        AdapterView.OnItemClickListener getListener();
        void passCursor(Cursor c, String s);

    }


}
