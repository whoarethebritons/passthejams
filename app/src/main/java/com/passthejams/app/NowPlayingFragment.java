package com.passthejams.app;


import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class NowPlayingFragment extends Fragment {


    public NowPlayingFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_now_playing, container, false);
        Toolbar toolbar = (Toolbar) root.findViewById(R.id.my_toolbar);
        // Set an OnMenuItemClickListener to handle menu item clicks
//        ;
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Handle the menu item
                return true;
            }
        });
        toolbar.setTitle("Now Playing");
        Log.d("now playing", String.valueOf(toolbar.canShowOverflowMenu()));
        //((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        return root;
    }


}
