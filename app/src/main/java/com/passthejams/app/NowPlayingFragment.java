package com.passthejams.app;


import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.TreeMap;


/**
 * A simple {@link Fragment} subclass.
 * NowPlayingFragment displays the information about the current song
 * Queue fragment displays the songs that are going to be played next or were played previously
 */
public class NowPlayingFragment extends Fragment {
    private BroadcastReceiver artReceiver;
    private ImageView album;
    private Toolbar toolbar;
    private final String TAG = "Now Playing";

    public NowPlayingFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root =  inflater.inflate(R.layout.fragment_now_playing, container, false);
        String id = getArguments().getString(Shared.Broadcasters.ART_VALUE.name());
        album = (ImageView) root.findViewById(R.id.nowPlayingArt);

        //TODO: Add a menu to the toolbar to get rid of Queue button
        //add the toolbar that displays the currently playing song
        toolbar = (Toolbar) root.findViewById(R.id.my_toolbar);
        toolbar.inflateMenu(R.menu.menu_now_playing);
        toolbar.setTitle("Now Playing");

        Log.d(TAG, String.valueOf(toolbar.canShowOverflowMenu()));
        Log.d(TAG, album.toString());

        //retrieve the BottomMusicFragment so that we can see the currently playing song
        FragmentManager fragmentManager = getFragmentManager();
        BottomMusicFragment f = (BottomMusicFragment) fragmentManager.findFragmentById(R.id.bottomBar);
        TrackInfo current = f.currentSong();

        //set the album art of the ImageView
        setNowPlayingArt(current);
        Log.d(TAG, "sent : " + id + "to imageview");
        return root;
    }

    @Override
    public void onResume() {
        FragmentManager fragmentManager = getFragmentManager();
        BottomMusicFragment f = (BottomMusicFragment) fragmentManager.findFragmentById(R.id.bottomBar);
        TrackInfo current = f.currentSong();
        setNowPlayingArt(current);
        super.onResume();

        getActivity().findViewById(R.id.lastfmButton).setVisibility(View.VISIBLE);

        /*album art change handling*/
        artReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "receiving");
                Gson g = new Gson();
                String artLocation = intent.getStringExtra(Shared.Broadcasters.ART_VALUE.name());
                TrackInfo t = g.fromJson(artLocation, TrackInfo.class);
                setNowPlayingArt(t);
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(artReceiver,
                new IntentFilter(Shared.Broadcasters.BROADCAST_ART.name()));
        super.onResume();
    }
    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(artReceiver);
        //getActivity().findViewById(R.id.lastfmButton).setVisibility(View.INVISIBLE);
        super.onPause();
    }

    /**
     * @param artLocation takes in TrackInfo that will be used to retrieve the album_id so that
     *                    the album art can be displayed
     *                    also sets the toolbar to the song name & artist
     */
    private void setNowPlayingArt(TrackInfo artLocation) {
        Log.v(TAG, "trackinfo changed");
        //method that will set the image to whatever is at the uri
        //Shared.getAlbumArt(String s) will resolve the uri
        if(artLocation != null) {
            Shared.getAlbumArt(getActivity().getApplicationContext(), album, String.valueOf(artLocation.album_id));
            toolbar.setTitle(artLocation.title + " -- " + artLocation.artist);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }
    @Override
    public void onStop() {
        //remove the broadcaster
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(artReceiver);
        super.onStop();
    }


    public static class Queue extends Fragment {
        BroadcastReceiver queueReceiver;
        JamsArrayAdapter mListAdapter;
        View mRoot;
        final String TAG = "Queue";
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View root =  inflater.inflate(R.layout.song_layout, container, false);
            FragmentManager fragmentManager = getFragmentManager();

            /*
            BottomMusicFragment has access to the MusicPlaybackService
            retrieved so communication can happen easier
            */
            BottomMusicFragment f = (BottomMusicFragment) fragmentManager.findFragmentById(R.id.bottomBar);
            MusicPlaybackService.JamsQueue<Integer, TrackInfo> tempq = (MusicPlaybackService
                    .JamsQueue<Integer,TrackInfo>) f.queue();

            /* set the list adapter for the view */
            mListAdapter = new JamsArrayAdapter(getActivity().getApplicationContext(),
                    R.layout.song_row, R.id.songView, tempq);
            ListView lv = (ListView) root.findViewById(android.R.id.list);
            lv.setAdapter(mListAdapter);
            mRoot = root;
            return root;
        }
        @Override
        public void onResume() {
            super.onResume();
            /*queue change handling*/
            queueReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.v(TAG, "receiving");
                    String jsonQueue = intent.getStringExtra(Shared.Broadcasters.QUEUE_VALUE.name());
                    Log.d(TAG, jsonQueue);

                    //convert the json string to JamsQueue<Integer,TrackInfo>
                    Gson g = new Gson();
                    TreeMap<Integer, TrackInfo> t = (g.fromJson(jsonQueue,
                            new TypeToken<MusicPlaybackService.JamsQueue<Integer, TrackInfo>>(){}.getType()));

                    //update the queue
                    mListAdapter.updateQueue(t);
                }
            };
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(queueReceiver,
                    new IntentFilter(Shared.Broadcasters.BROADCAST_QUEUE.name()));

        }
        @Override
        public void onPause() {
            super.onPause();
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(queueReceiver);
        }
    }

    public static class JamsArrayAdapter extends ArrayAdapter {
        TreeMap<Integer, TrackInfo> queue;
        private class TrackHolder {
            TextView song;
            TextView artist;
        }
        public JamsArrayAdapter(Context context, int resource, int textViewResourceId, TreeMap q) {
            super(context, resource, textViewResourceId, q.values().toArray());
            queue = q;
        }
        @Override
        public int getCount() {
            return queue.size();
        }

        @Override
        public Object getItem(int position) {
            return queue.values().toArray()[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        /**
         * @param position of the item
         * @param convertView the view that is null or being recycled if it exists
         * @param parent the ViewGroup that convertView is a part of
         * @return the inflated View with the correct TrackHolder values set as its tag
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.queue_row, null);
            }

            TrackHolder trackHolder = new TrackHolder();

            trackHolder.song = (TextView) convertView.findViewById(R.id.songView);
            TrackInfo temp = (TrackInfo) getItem(position);
            trackHolder.song.setText(temp.title);

            trackHolder.artist = (TextView) convertView.findViewById(R.id.artistView);
            trackHolder.artist.setText(temp.artist);

            convertView.setTag(trackHolder);

            return convertView;
        }

        /**
         * @param queue changes the data source
         * then notifies data changed
         */
        public void updateQueue(TreeMap<Integer, TrackInfo> queue) {
            this.queue = queue;
            notifyDataSetChanged();
        }
    }

    public interface OnQueueChangeListener {
        /**
         * Fires an event when the queue is changed
         */
        void onEvent();
    }

}
