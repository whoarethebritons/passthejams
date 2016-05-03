package com.passthejams.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import com.google.gson.Gson;

import java.util.ArrayList;

public class BottomMusicFragment extends Fragment{
    //boolean value to hold whether the service is bound or not
    boolean mBound;
    //variable to hold the music service so that methods can be directly run
    MusicPlaybackService mService;
    //broadcast receivers for ui updates from service
    private BroadcastReceiver receiver, artReceiver, notificationReceiver, externalArtReceiver;

    //for interacting with holder activities
    private OnFragmentInteractionListener mListener;

    final String TAG="BottomMusicFragment";

    public BottomMusicFragment() {}

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
                    //setViewVisibility(int viewId, int visibility)

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
                Gson g = new Gson();
                String artLocation = intent.getStringExtra(Shared.Broadcasters.ART_VALUE.name());
                TrackInfo t = g.fromJson(artLocation, TrackInfo.class);
                ImageView play = (ImageView) getActivity().findViewById(R.id.currentAlbumArt);
                mListener.setImageVal(String.valueOf(t.album_id));
                //method that will set the image to whatever is at the uri
                //Shared.getAlbumArt(String s) will resolve the uri
                Shared.getAlbumArt(context, play, String.valueOf(t.album_id));
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

        /* the LastFm button */
        Button lastFmShuffle = (Button) getActivity().findViewById(R.id.lastfmButton);
        lastFmShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //must be on a new thread because of NetworkOnMainException
                Thread newThread = new Thread(new Runnable() {
                    public void run() {
                        Looper.prepare();
                        if (mService.getCurrentPlaying() != null) {
                            //get the ArrayList from LastFm
                            ArrayList<TrackInfo> test = new LastFm(getActivity().getApplicationContext(),
                                    mService.getCurrentPlaying())
                                    .generateOrGetSimilar(getActivity().getContentResolver());
                            //call the service passing the ArrayList
                            mService.addSimilarToQueue(test);
                        }
                    }
                });
                newThread.start();

            }
        });

        /*the shuffle button*/
        Button shuffle = (Button) getActivity().findViewById(R.id.shuffleButton);
        //the tag contains a boolean to decide whether to shuffle or not
        shuffle.setTag(false);
        shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //retrieve tag
                boolean shuffleVal = (boolean) v.getTag();
                Log.d(TAG, "shuffleVal was " + shuffleVal);
                //invert tag and set to new
                v.setTag(!shuffleVal);
                //hold new tag
                shuffleVal = (boolean) v.getTag();
                Log.d(TAG, "shuffleVal is now " + shuffleVal);
                //create an intent so that the broadcast receiver can filter
                Intent shuffleUpdate = new Intent(Shared.Service.BROADCAST_SHUFFLE.name());
                //send over the shuffle boolean value
                shuffleUpdate.putExtra(Shared.Service.SHUFFLE_VALUE.name(), shuffleVal);
                //broadcast it
                LocalBroadcastManager localBroadcastManager =
                        LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
                localBroadcastManager.sendBroadcast(shuffleUpdate);
                //calls play
                Button play = (Button) getActivity().findViewById(R.id.playButton);
                play.performClick();
            }
        });
    }

    /**
    ItemClickListener, will probably be reused and maybe in a different fragment
    that specifically uses the listviews to play the songs
    */
    private AdapterView.OnItemClickListener listItemClick = new AdapterView.OnItemClickListener() {
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
                            playSong.getIntExtra(Shared.Main.POSITION.name(), -1));
            mService.serviceOnPlay(queueObjectInfo,
                    playSong.getBooleanExtra(Shared.Main.DISCARD_PAUSE.name(), true), false);
        }
    };

    //method for making intents for the button listeners
    private Intent makeActionIntent(String option, int position, boolean discard) {
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
    private View.OnClickListener buttonListeners(final Shared.Service button, final boolean discard) {
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
                        if(mListener.currentViewCursor() != null) {
                            mService.pressPlay();
                        }
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
    private ServiceConnection mServiceConnection = new ServiceConnection() {
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
            mService.isPlaying();
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
        getActivity().unbindService(mServiceConnection);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(artReceiver);
        mBound = false;
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mService != null) {
            mService.isPlaying();
        }
    }

    /**
     * @return TreeMap for use in Queue fragment
     */
    public ArrayList<TrackInfo> queue() {
        return mService.getQueue();
    }

    /**
     * @return TrackInfo for use in NowPlaying & Queue fragments
     */
    public TrackInfo currentSong() {
        return mService.getCurrentPlaying();
    }


    public interface OnFragmentInteractionListener {
        /**
         * @param s sends OnItemClickListener
         */
        void onFragmentInteraction(AdapterView.OnItemClickListener s);

        /**
         * @return Cursor from query that is currently displayed
         */
        Cursor currentViewCursor();

        /**
         * @param albumid passes the Album Art's id when song changes
         *                then when NowPlaying is opened it has the art id
         */
        void setImageVal(String albumid);
    }

}
