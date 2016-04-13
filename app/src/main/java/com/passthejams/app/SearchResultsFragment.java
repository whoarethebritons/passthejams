package com.passthejams.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class SearchResultsFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String QUERY_PARAM = "QueryParam";
    private static final String TAG = "Search";
    private String mQuery;

    private OnFragmentInteractionListener mListener;

    public SearchResultsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param query Search Query.
     * @return A new instance of fragment SearchResultsFragment.
     */
    public static SearchResultsFragment newInstance(String query) {
        SearchResultsFragment fragment = new SearchResultsFragment();
        Bundle args = new Bundle();
        args.putString(QUERY_PARAM, query);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public void onStart() {
        super.onStart();
        if (getArguments() != null) {
            Log.d(TAG, "actually ran");
            Activity mActivity = getActivity();
            mQuery = getArguments().getString(QUERY_PARAM);
            Context context = getActivity().getApplicationContext();

            int row_id = R.layout.song_row;
            String[] projectionString = Shared.PROJECTION_SONG;

            String selectionString = MediaStore.Audio.Media.IS_MUSIC + "!=0 AND (instr(upper(" +
                    MediaStore.Audio.Media.TITLE + "),upper(?)))";
            String[] selectionArguments = new String[]{mQuery};
            Uri uri = Shared.libraryUri;
            String[] displayFields = new String[]{MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM,
                    MediaStore.Audio.Albums.ALBUM_ID};
            int[] displayText = new int[] {R.id.songView, R.id.artistView, R.id.albumView, R.id.artView};
            String sortOrder = (MediaStore.Audio.Media.ALBUM + " ASC, "+MediaStore.Audio.Media.TRACK+" ASC");

            //testing code
            //query the database given the passed items
            Cursor mCursor = mActivity.managedQuery(uri, projectionString, selectionString, selectionArguments, sortOrder);

            /*SimpleCursorAdapter simpleCursorAdapter = new JamsCursorAdapter(this, row_id, mCursor,
                    displayFields, displayText);*/

            //set adapter
            GridLayout lv = (GridLayout) mActivity.findViewById(R.id.songList);
            //lv.setAdapter(simpleCursorAdapter);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            for(int j = 0; j<20 && mCursor.moveToNext(); j++) {
                //while(mCursor.moveToNext()){
                Log.d(TAG, "song count:" + mCursor.getCount());
                View row = inflater.inflate(row_id,null);
                TextView t = (TextView) row.findViewById(R.id.songView);
                t.setText(mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                t = (TextView) row.findViewById(R.id.artistView);
                t.setText(mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                t = (TextView) row.findViewById(R.id.albumView);
                t.setText(mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
                ImageView i = (ImageView) row.findViewById(R.id.artView);
                Shared.getAlbumArt(context, i,
                        mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
                lv.addView(row);
            }

            row_id = R.layout.album_tile;projectionString = Shared.PROJECTION_ALBUM;
            uri = Shared.albumUri;
            displayFields = new String[]{
                    MediaStore.Audio.Albums.ARTIST,
                    MediaStore.Audio.Albums.ALBUM,
                    MediaStore.Audio.Albums._ID};
            displayText =  new int[] {R.id.artistView, R.id.albumTitle, R.id.artView};
            selectionString = "(instr(upper(" +
                    MediaStore.Audio.Media.ALBUM + "),upper(?)))";
            selectionArguments = new String[]{mQuery};
            sortOrder = (MediaStore.Audio.Media.ALBUM + " ASC");

            //testing code
            //query the database given the passed items
            mCursor = mActivity.managedQuery(uri, projectionString, selectionString, selectionArguments, sortOrder);

            /*simpleCursorAdapter = new JamsCursorAdapter(this, row_id, mCursor,
                    displayFields, displayText);*/

            //set adapter
            lv = (GridLayout) mActivity.findViewById(R.id.albumList);
            //lv.setAdapter(simpleCursorAdapter);
            for(int j = 0; j<8 && mCursor.moveToNext(); j++) {
                //while(mCursor.moveToNext()){
                Log.d(TAG, "album count:" + mCursor.getCount());
                View row = inflater.inflate(row_id,null);
                TextView t = (TextView) row.findViewById(R.id.artistView);
                t.setText(mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)));
                t = (TextView) row.findViewById(R.id.albumTitle);
                t.setText(mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)));
                ImageView i = (ImageView) row.findViewById(R.id.artView);
                Shared.getAlbumArt(context, i,
                        mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Albums._ID)));
                lv.addView(row);
            }

            row_id = R.layout.artist_tile;
            projectionString = Shared.PROJECTION_SONG;

            selectionString = "instr(upper(" +
                    MediaStore.Audio.Media.ARTIST + "),upper(?))) GROUP BY("+ MediaStore.Audio.Media.ARTIST_ID;
            selectionArguments = new String[]{mQuery};
            uri = Shared.libraryUri;
            displayFields = new String[]{
                    MediaStore.Audio.Artists.ARTIST,
                    MediaStore.Audio.Albums.ALBUM_ID};
            displayText =  new int[] {R.id.artistView, R.id.albumView};
            sortOrder = null;

            //testing code
            //query the database given the passed items
            mCursor = mActivity.managedQuery(uri, projectionString, selectionString, selectionArguments, sortOrder);

            /*simpleCursorAdapter = new JamsCursorAdapter(this, row_id, mCursor,
                    displayFields, displayText);*/

            //set adapter
            lv = (GridLayout) mActivity.findViewById(R.id.artistList);
            //lv.setAdapter(simpleCursorAdapter);
            for(int j = 0; j<6 && mCursor.moveToNext(); j++) {
                //while(mCursor.moveToNext()){
                Log.d(TAG, "artist count:" + mCursor.getCount());
                View row = inflater.inflate(row_id,null);
                TextView t = (TextView) row.findViewById(R.id.artistView);
                t.setText(mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)));
                ImageView i = (ImageView) row.findViewById(R.id.artView);
                Shared.getAlbumArt(context, i,
                        mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID)));
                lv.addView(row);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search_results, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    /*@Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }*/

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
