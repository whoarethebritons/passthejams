package com.passthejams.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TabHost;

import java.io.IOException;
import java.util.ArrayList;

public class BottomMusicFragment extends Fragment{
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
                boolean playPause = intent.getBooleanExtra(Shared.Broadcasters.BUTTON_VALUE.name(), true);
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
                new IntentFilter(Shared.Broadcasters.BROADCAST_BUTTON.name()));

        /*album art change handling*/
        artReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "receiving");
                String artLocation = intent.getStringExtra(Shared.Broadcasters.ART_VALUE.name());
                ImageView play = (ImageView) getActivity().findViewById(R.id.currentAlbumArt);
                //method that will set the image to whatever is at the uri
                //Shared.getAlbumArt(String s) will resolve the uri
                Shared.getAlbumArt(context, play, artLocation);
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(artReceiver,
                new IntentFilter(Shared.Broadcasters.BROADCAST_ART.name()));

        /*
        the onclick listeners that will go to the music service
         */

        //this one has true so that by default it will not discard pause
        Button play = (Button) getActivity().findViewById(R.id.playButton);
        play.setOnClickListener(buttonListeners(Shared.Service.PLAY, true));
        Button pause = (Button) getActivity().findViewById(R.id.pauseButton);
        pause.setOnClickListener(buttonListeners(Shared.Service.PAUSE, false));
        Button next = (Button) getActivity().findViewById(R.id.nextButton);
        next.setOnClickListener(buttonListeners(Shared.Service.NEXT, false));
        Button previous = (Button) getActivity().findViewById(R.id.previousButton);
        previous.setOnClickListener(buttonListeners(Shared.Service.PREVIOUS, false));

        Button shuffle = (Button) getActivity().findViewById(R.id.shuffleButton);
        shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread newThread = new Thread(new Runnable() {
                    public void run() {
                        Looper.prepare();
                        ArrayList<TrackInfo> test = new LastFm(getActivity().getApplicationContext(),
                                mService.getCurrentPlaying()).generateOrGetSimilar(getActivity().getContentResolver());
                        mService.addSimilarToQueue(test);
                        /*for (TrackInfo t: test) {
                            Log.v(TAG, t.name);
                        }*/
                    }
                });
                newThread.start();

            }
        });

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
            playSong.putExtra(Shared.Main.POSITION.name(), pos);
            Log.v("list listener", " " + pos);
            //send that we are going to "play"
            playSong.putExtra(Shared.Main.OPTION.name(), Shared.Service.PLAY);
            //send that we will discard any stored pause
            playSong.putExtra(Shared.Main.DISCARD_PAUSE.name(), true);

            //call the play method on the service
            MusicPlaybackService.QueueObjectInfo queueObjectInfo =
                    new MusicPlaybackService(). new QueueObjectInfo(mListener.currentViewCursor(),
                            playSong.getIntExtra(Shared.Main.POSITION.name(), -1),
                            false, false);
           mService.serviceOnPlay(queueObjectInfo,
                    playSong.getBooleanExtra(Shared.Main.DISCARD_PAUSE.name(), true), false);
        }
    };

    //method for making intents for the button listeners
    public Intent makeActionIntent(String option, int position, boolean discard) {
        Log.v(TAG, option);
        Intent intent = new Intent(getActivity(), MusicPlaybackService.class);
        //specifies what action we will be performing
        intent.putExtra(Shared.Main.OPTION.name(), option);
        //specifies position in database of the song
        intent.putExtra(Shared.Main.POSITION.name(), position);
        //whether we are unpausing or skipping
        intent.putExtra(Shared.Main.DISCARD_PAUSE.name(), !discard);
        return intent;
    }
    //button listeners
    public View.OnClickListener buttonListeners(final Shared.Service button, final boolean discard) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, button.name() + " clicked");
                //create the intent
                Intent intent = makeActionIntent(button.name(), v.getVerticalScrollbarPosition(), discard);
                //switch around which action we're planning on doing

                Log.d(TAG, button + " clicked");
                switch(button) {
                    case PLAY:
                        MusicPlaybackService.QueueObjectInfo queueObjectInfo =
                                new MusicPlaybackService(). new QueueObjectInfo(mListener.currentViewCursor(),
                                        intent.getIntExtra(Shared.Main.POSITION.name(), 0),
                                        false, false);
                        mService.serviceOnPlay(queueObjectInfo,
                                intent.getBooleanExtra(Shared.Main.DISCARD_PAUSE.name(), true), true);
                        break;
                    case NEXT:
                        mService.serviceOnNext();
                        break;
                    case PREVIOUS:
                        mService.serviceOnPrevious();
                        break;
                    case PAUSE:
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
        Cursor currentViewCursor();
        //unused right now
        Activity setActivity();
    }

}
