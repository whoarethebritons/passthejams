package com.passthejams.app;


import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.TreeMap;


/**
 * A simple {@link Fragment} subclass.
 */
public class NowPlayingFragment extends Fragment {
    BroadcastReceiver artReceiver;
    ImageView album;
    final String TAG = "Now Playing";

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
        Log.d(TAG, album.toString());
        setNowPlayingArt(id);
        Log.d(TAG, "sent : " + id + "to imageview");
        return root;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {

        /*album art change handling*/
        artReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "receiving");
                String artLocation = intent.getStringExtra(Shared.Broadcasters.ART_VALUE.name());
                setNowPlayingArt(artLocation);
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(artReceiver,
                new IntentFilter(Shared.Broadcasters.BROADCAST_ART.name()));
        super.onCreate(savedInstanceState);
    }

    public void setNowPlayingArt(String artLocation) {
        //method that will set the image to whatever is at the uri
        //Shared.getAlbumArt(String s) will resolve the uri
        if(artLocation != null) {
            Shared.getAlbumArt(getActivity().getApplicationContext(), album, artLocation);
        }
    }

    @Override
    public void onStart() {
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.my_toolbar);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Handle the menu item
                return true;
            }
        });
        toolbar.setTitle("Now Playing");
        Log.d(TAG, String.valueOf(toolbar.canShowOverflowMenu()));
        super.onStart();
    }
    @Override
    public void onStop() {
        //remove the broadcaster
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(artReceiver);
        super.onStop();
    }


    public static class Queue extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View root =  inflater.inflate(R.layout.song_layout, container, false);
            FragmentManager fragmentManager = getFragmentManager();

            BottomMusicFragment f = (BottomMusicFragment) fragmentManager.findFragmentById(R.id.bottomBar);
            TreeMap<Integer, TrackInfo> tempq = f.queue();
            ListAdapter listAdapter = new JamsBaseAdapter(getActivity().getApplicationContext(),
                    R.layout.song_row, R.id.songView, tempq.values().toArray());
            ListView lv = (ListView) root.findViewById(android.R.id.list);
            lv.setAdapter(listAdapter);
            return root;
        }
        @Override
        public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);
        }
    }

}
