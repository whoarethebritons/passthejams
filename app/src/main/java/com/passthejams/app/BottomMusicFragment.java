package com.passthejams.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BottomMusicFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BottomMusicFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BottomMusicFragment extends Fragment {
    int id = 0;
    boolean mBound;
    MusicPlaybackService mService;
    BroadcastReceiver receiver, artReceiver;
    Activity holderActivity;

    private OnFragmentInteractionListener mListener;


    final String TAG="fragment";

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment BottomMusicFragment.
     */
    public static BottomMusicFragment newInstance(Activity inActivity) {
        BottomMusicFragment fragment = new BottomMusicFragment();
        return fragment;
    }

    public BottomMusicFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }
    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), MusicPlaybackService.class);

        // /binds the music service to whatever activity is containing the fragment
        getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        /*button switching handling*/
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "receiving");
                boolean playPause = intent.getBooleanExtra(Shared.BUTTON_VALUE, true);
                Button play = (Button) getActivity().findViewById(R.id.playButton);
                Button pause = (Button) getActivity().findViewById(R.id.pauseButton);
                if(!playPause) {
                    play.setVisibility(View.VISIBLE);
                    pause.setVisibility(View.INVISIBLE);
                }
                else {
                    play.setVisibility(View.INVISIBLE);
                    pause.setVisibility(View.VISIBLE);
                }

            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(Shared.BROADCAST_BUTTON));

        /*album art change handling*/
        artReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "receiving");
                String artLocation = intent.getStringExtra(Shared.ART_VALUE);
                ImageView play = (ImageView) getActivity().findViewById(R.id.currentAlbumArt);
                play.setImageURI(Shared.getAlbumArt(artLocation));
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(artReceiver,
                new IntentFilter(Shared.BROADCAST_ART));

        /*
        the onclick listeners that will go to the music service
         */

        Button play = (Button) getActivity().findViewById(R.id.playButton);
        play.setOnClickListener(buttonListeners(Shared.PLAY, true));
        Button pause = (Button) getActivity().findViewById(R.id.pauseButton);
        pause.setOnClickListener(buttonListeners(Shared.PAUSE, false));
        Button next = (Button) getActivity().findViewById(R.id.nextButton);
        next.setOnClickListener(buttonListeners(Shared.NEXT, false));
        Button previous = (Button) getActivity().findViewById(R.id.previousButton);
        previous.setOnClickListener(buttonListeners(Shared.PREV, false));

    }

    /*
    ItemClickListener, will probably be reused and maybe in a different fragment
    that specifically uses the listviews to play the songs
    */
    AdapterView.OnItemClickListener listItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int pos = parent.getPositionForView(view);
            Log.v("list listener", "list item position: " + parent.getPositionForView(view));
            Intent playSong = new Intent(getActivity(), MusicPlaybackService.class);
            playSong.putExtra(Shared.POSITION, pos);
            Log.v("list listener", " " + pos);
            playSong.putExtra(Shared.OPTION, "play");
            playSong.putExtra(Shared.DISCARD_PAUSE, true);
            mService.serviceOnPlay(playSong.getIntExtra(Shared.POSITION, -1),
                    playSong.getBooleanExtra(Shared.DISCARD_PAUSE, true));
        }
    };
    public Intent makeActionIntent(String option, int position, boolean discard) {
        Log.v(TAG, option);
        Intent intent = new Intent(getActivity(), MusicPlaybackService.class);
        intent.putExtra(Shared.OPTION, option);
        intent.putExtra(Shared.POSITION, position);
        intent.putExtra(Shared.DISCARD_PAUSE, !discard);
        return intent;
    }
    //button listeners
    public View.OnClickListener buttonListeners(final String button, final boolean discard) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, button + " clicked");
                Intent intent = makeActionIntent(button, v.getVerticalScrollbarPosition(), discard);
                switch(button) {
                    case "play":
                        Log.v(TAG, "play clicked");
                        mService.serviceOnPlay(intent.getIntExtra(Shared.POSITION, 0),
                                intent.getBooleanExtra(Shared.DISCARD_PAUSE, true));
                        break;
                    case "next":
                        Log.v(TAG, "next clicked");
                        mService.serviceOnNext();
                        break;
                    case "previous":
                        Log.v(TAG, "previous clicked");
                        mService.serviceOnPrevious();
                        break;
                    case "pause":
                        Log.v(TAG, "pause clicked");
                        mService.serviceOnPause();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    //gets the service if we can successfully bind to it
    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "bind success");
            mBound = true;
            MusicPlaybackService.PlaybackBinder binder =
                    (MusicPlaybackService.PlaybackBinder) service;
            //this is the service we use in the button listeners
            mService = binder.getService();
            //allow activity to set item click listener now that service is initialized
            mListener.onFragmentInteraction(listItemClick);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "disconnected");
            mBound = false;
        }
    };

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(artReceiver);
        super.onStop();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bottom_music, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
            //holderActivity = mListener.setActivity();
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
    @Override
    public void onDestroy() {
        Log.v(TAG, "destroyed");
        getActivity().unbindService(mServiceConnection);
        mBound = false;
        super.onDestroy();
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
        void onFragmentInteraction(AdapterView.OnItemClickListener s);
        public Activity setActivity();
    }

}
