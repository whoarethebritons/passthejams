package com.passthejams.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ViewSwitcher;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BottomMusicFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BottomMusicFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BottomMusicFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    BroadcastReceiver receiver;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;


    final String NEXT="next", PLAY="play", PREV="previous", PAUSE="pause", VALUE="value",
            TAG="fragment";

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BottomMusicFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BottomMusicFragment newInstance(String param1, String param2) {
        BottomMusicFragment fragment = new BottomMusicFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        //args.put
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public BottomMusicFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        /*receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "receiving");
                boolean playPause = intent.getBooleanExtra(VALUE, true);
                Button play = (Button) getActivity().findViewById(R.id.playButton);
                Button pause = (Button) getActivity().findViewById(R.id.pauseButton);
                int visible = play.getVisibility();
                if(playPause) {
                    play.setVisibility(pause.getVisibility());
                    pause.setVisibility(visible);
                }
            }
        };
        Log.v(TAG, BottomMusicFragment.class.getCanonicalName());
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(BottomMusicFragment.class.getCanonicalName()));*/

        /*
        the onclick listeners that will go to the music service
         */

        Button play = (Button) getActivity().findViewById(R.id.playButton);
        //temporary
        play.setOnClickListener(buttonListeners(PAUSE));
        Button pause = (Button) getActivity().findViewById(R.id.pauseButton);
        pause.setOnClickListener(buttonListeners(PAUSE));
        Button next = (Button) getActivity().findViewById(R.id.nextButton);
        next.setOnClickListener(buttonListeners(NEXT));
        Button previous = (Button) getActivity().findViewById(R.id.previousButton);
        previous.setOnClickListener(buttonListeners(PREV));
    }
    public View.OnClickListener buttonListeners(final String button) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, button + " clicked");
                Intent i = new Intent(getActivity(),MusicPlaybackService.class);
                i.putExtra(MainActivity.OPTION, button);
                i.putExtra(MainActivity.POSITION, v.getVerticalScrollbarPosition());
                getActivity().startService(i);
            }
        };
    }
    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        super.onStop();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bottom_music, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

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
        public void onFragmentInteraction(Uri uri);
    }

}
