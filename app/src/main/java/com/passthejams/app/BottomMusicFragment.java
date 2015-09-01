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

public class BottomMusicFragment extends Fragment {
    //boolean value to hold whether the service is bound or not
    boolean mBound;
    //variable to hold the music service so that methods can be directly run
    MusicPlaybackService mService;
    //broadcast receivers for ui updates from service
    BroadcastReceiver receiver, artReceiver;
    //unused right now
    Activity holderActivity;

    //for interacting with holder activities
    private OnFragmentInteractionListener mListener;

    final String TAG="fragment";

    public static BottomMusicFragment newInstance(Activity inActivity) {
        return new BottomMusicFragment();
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
                //gets the boolean value of whether to switch or not
                boolean playPause = intent.getBooleanExtra(Shared.BUTTON_VALUE, true);
                Button play = (Button) getActivity().findViewById(R.id.playButton);
                Button pause = (Button) getActivity().findViewById(R.id.pauseButton);
                //if that value is false, then mediaplayer is not playing
                if(!playPause) {
                    play.setVisibility(View.VISIBLE);
                    pause.setVisibility(View.INVISIBLE);
                }
                //otherwise it is playing and show the pause button
                else {
                    play.setVisibility(View.INVISIBLE);
                    pause.setVisibility(View.VISIBLE);
                }

            }
        };
        //actual registration of that listener
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(Shared.BROADCAST_BUTTON));

        /*album art change handling*/
        artReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "receiving");
                String artLocation = intent.getStringExtra(Shared.ART_VALUE);
                ImageView play = (ImageView) getActivity().findViewById(R.id.currentAlbumArt);
                //method that will set the image to whatever is at the uri
                //Shared.getAlbumArt(String s) will resolve the uri
                play.setImageURI(Shared.getAlbumArt(artLocation));
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(artReceiver,
                new IntentFilter(Shared.BROADCAST_ART));

        /*
        the onclick listeners that will go to the music service
         */

        //this one has true so that by default it will not discard pause
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
            //specifies where we are sending this to
            Intent playSong = new Intent(getActivity(), MusicPlaybackService.class);
            //send over the position
            playSong.putExtra(Shared.POSITION, pos);
            Log.v("list listener", " " + pos);
            //send that we are going to "play"
            playSong.putExtra(Shared.OPTION, Shared.PLAY);
            //send that we will discard any stored pause
            playSong.putExtra(Shared.DISCARD_PAUSE, true);

            //call the play method on the service
            mService.serviceOnPlay(playSong.getIntExtra(Shared.POSITION, -1),
                    playSong.getBooleanExtra(Shared.DISCARD_PAUSE, true));
        }
    };

    //method for making intents for the button listeners
    public Intent makeActionIntent(String option, int position, boolean discard) {
        Log.v(TAG, option);
        Intent intent = new Intent(getActivity(), MusicPlaybackService.class);
        //specifies what action we will be performing
        intent.putExtra(Shared.OPTION, option);
        //specifies position in database of the song
        intent.putExtra(Shared.POSITION, position);
        //whether we are unpausing or skipping
        intent.putExtra(Shared.DISCARD_PAUSE, !discard);
        return intent;
    }
    //button listeners
    public View.OnClickListener buttonListeners(final String button, final boolean discard) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, button + " clicked");
                //create the intent
                Intent intent = makeActionIntent(button, v.getVerticalScrollbarPosition(), discard);
                //switch around which action we're planning on doing

                Log.d(TAG, button + " clicked");
                switch(button) {
                    case "play":
                        mService.serviceOnPlay(intent.getIntExtra(Shared.POSITION, 0),
                                intent.getBooleanExtra(Shared.DISCARD_PAUSE, true));
                        break;
                    case "next":
                        mService.serviceOnNext();
                        break;
                    case "previous":
                        mService.serviceOnPrevious();
                        break;
                    case "pause":
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
        //remove the broadcasters
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

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(AdapterView.OnItemClickListener s);
        //unused right now
        Activity setActivity();
    }

}
