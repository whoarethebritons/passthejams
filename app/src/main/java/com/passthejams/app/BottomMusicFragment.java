package com.passthejams.app;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
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
import android.widget.RemoteViews;
import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class BottomMusicFragment extends Fragment{
    //boolean value to hold whether the service is bound or not
    boolean mBound;
    //variable to hold the music service so that methods can be directly run
    MusicPlaybackService mService;
    //broadcast receivers for ui updates from service
    private BroadcastReceiver receiver, artReceiver, notificationReceiver, externalArtReceiver;

    //for interacting with holder activities
    private OnFragmentInteractionListener mListener;

    final String TAG="fragment";

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

                createNotification(true, playPause, false, null);
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
                createNotification(true, false, true, t);
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(artReceiver,
                new IntentFilter(Shared.Broadcasters.BROADCAST_ART.name()));

        externalArtReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "external art received");
                Gson g = new Gson();
                String artLocation = intent.getStringExtra(Shared.Broadcasters.ART_VALUE.name());
                TrackInfo t = g.fromJson(artLocation, TrackInfo.class);
                createNotification(true, false, true, t);
            }
        };

        getActivity().registerReceiver(externalArtReceiver,
                new IntentFilter(Shared.Broadcasters.BROADCAST_ART.name()));
        /*notification handling*/
        notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "receiving notification");
                if(intent.getBooleanExtra(Shared.Service.PLAY.name(), false)) {
                    Log.v(TAG, "from play");
                    mService.pressPlay();
                    createNotification(true, true, false, null);
                }
                else if(intent.getBooleanExtra(Shared.Service.PREVIOUS.name(), false)) {
                    Log.v(TAG, "from previous");
                    servicePrevious();
                }
                else if(intent.getBooleanExtra(Shared.Service.NEXT.name(), false)) {
                    Log.v(TAG, "from next");
                    serviceNext();
                }
                else if(intent.getBooleanExtra(Shared.Service.PAUSE.name(), false)) {
                    Log.v(TAG, "from pause");
                    servicePause();
                    createNotification(true, false, false, null);
                }
                else if(intent.getBooleanExtra(Shared.Service.SHUFFLE_VALUE.name(), false)) {
                    Log.v(TAG, "from shuffle");
                }
                else {
                    Log.v(TAG, "not a button");
                }

            }
        };

        getActivity().registerReceiver(notificationReceiver,
                new IntentFilter(Shared.Broadcasters.BROADCAST_NOTIFY.name()));
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
                            servicePlay(intent);
                        }
                        break;
                    case NEXT:
                        serviceNext();
                        break;
                    case PREVIOUS:
                        servicePrevious();
                        break;
                    case PAUSE:
                        servicePause();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    public void servicePlay(Intent intent) {
        //QueueObjectInfo containing the cursor and position of song
        //when play is pressed assume it is position 0 we are requesting
        /*MusicPlaybackService.QueueObjectInfo queueObjectInfo =
                new MusicPlaybackService().new QueueObjectInfo(mListener.currentViewCursor(),
                        intent.getIntExtra(Shared.Main.POSITION.name(), 0));
        //TODO: pressing play always discards queue
        mService.serviceOnPlay(queueObjectInfo,
                intent.getBooleanExtra(Shared.Main.DISCARD_PAUSE.name(), true), false);*/
        mService.pressPlay();
    }

    public void servicePause() {
        mService.serviceOnPause();
    }

    public void servicePrevious() {
        mService.serviceOnPrevious();
    }
    public void serviceNext() {
        mService.serviceOnNext();
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
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(externalArtReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(notificationReceiver);
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
            createNotification(false, false, false, null);
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
    /**
     * This sample demonstrates notifications with custom content views.
     *
     * <p>On API level 16 and above a big content view is also defined that is used for the
     * 'expanded' notification. The notification is created by the NotificationCompat.Builder.
     * The expanded content view is set directly on the {@link android.app.Notification} once it has been build.
     * (See {@link android.app.Notification#bigContentView}.) </p>
     *
     * <p>The content views are inflated as {@link android.widget.RemoteViews} directly from their XML layout
     * definitions using {@link android.widget.RemoteViews#RemoteViews(String, int)}.</p>
     */
    private void createNotification(boolean updateButton, boolean playPause, boolean songChange, TrackInfo artLocation) {
        // BEGIN_INCLUDE(notificationCompat)
        android.support.v4.app.NotificationCompat.Builder builder = new android.support.v4.app.NotificationCompat.Builder(getActivity().getApplicationContext());
        // END_INCLUDE(notificationCompat)

        // BEGIN_INCLUDE(intent)
        //Create Intent to launch this Activity again if the notification is clicked.
        Intent i = new Intent(getActivity().getApplicationContext(), BottomMusicFragment.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        /*PendingIntent intent = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);*/
        //builder.setContentIntent(intent);
        // END_INCLUDE(intent)

        // BEGIN_INCLUDE(ticker)
        // Sets the ticker text
        builder.setTicker(getResources().getString(R.string.custom_notification));

        // Sets the small icon for the ticker
        builder.setSmallIcon(R.drawable.ic_launcher);
        // END_INCLUDE(ticker)

        // BEGIN_INCLUDE(buildNotification)
        // Cancel the notification when clicked
        builder.setAutoCancel(true);

        // Build the notification
        Notification notification = builder.build();
        // END_INCLUDE(buildNotification)

        // BEGIN_INCLUDE(customLayout)
        // Inflate the notification layout as RemoteViews
        RemoteViews contentView = new RemoteViews(getActivity().getPackageName(), R.layout.notification);

        // Set text on a TextView in the RemoteViews programmatically.
        final String time = DateFormat.getTimeInstance().format(new Date()).toString();
        final String text = getResources().getString(R.string.collapsed, time);
        contentView.setTextViewText(R.id.textView, text);

        /* Workaround: Need to set the content view here directly on the notification.
         * NotificationCompatBuilder contains a bug that prevents this from working on platform
         * versions HoneyComb.
         * See https://code.google.com/p/android/issues/detail?id=30495
         */
        notification.contentView = contentView;

        // Add a big content view to the notification if supported.
        // Support for expanded notifications was added in API level 16.
        // (The normal contentView is shown when the notification is collapsed, when expanded the
        // big content view set here is displayed.)
        //if (Build.VERSION.SDK_INT >= 16) {
        // Inflate and set the layout for the expanded notification view
        RemoteViews expandedView =
                new RemoteViews(getActivity().getPackageName(), R.layout.notification_expanded);

        if(updateButton) {
            //if that value is false, then mediaplayer is not playing
            if(!playPause) {
                expandedView.setViewVisibility(R.id.notifyplayButton, View.VISIBLE);
                expandedView.setViewVisibility(R.id.notifypauseButton, View.INVISIBLE);
            }
            //otherwise it is playing and show the pause button
            else {
                expandedView.setViewVisibility(R.id.notifyplayButton, View.INVISIBLE);
                expandedView.setViewVisibility(R.id.notifypauseButton, View.VISIBLE);
            }
        }
        if(songChange) {
            Log.v(TAG, "trackinfo changed");
            //method that will set the image to whatever is at the uri
            //Shared.getAlbumArt(String s) will resolve the uri
            if(artLocation != null) {
                int id = artLocation.album_id;
                Uri test = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), id);
                try {
                    getActivity().getApplicationContext().getContentResolver().openInputStream(test);
                    expandedView.setImageViewUri(R.id.notifycurrentAlbumArt, test);
                }
                catch(FileNotFoundException e) {
                    Log.e("Shared", e.getMessage());
                    expandedView.setImageViewResource(R.id.notifycurrentAlbumArt, R.drawable.default_album);
                }

                expandedView.setTextViewText(R.id.notifyArtistName, artLocation.artist);
                expandedView.setTextViewText(R.id.notifySongName, artLocation.title);
            }
        }
        Intent play = new Intent(Shared.Broadcasters.BROADCAST_NOTIFY.name());
        play.putExtra(Shared.Service.PLAY.name(), true);

        Intent pause = new Intent(Shared.Broadcasters.BROADCAST_NOTIFY.name());
        pause.putExtra(Shared.Service.PAUSE.name(), true);

        Intent next = new Intent(Shared.Broadcasters.BROADCAST_NOTIFY.name());
        next.putExtra(Shared.Service.NEXT.name(), true);

        Intent prev = new Intent(Shared.Broadcasters.BROADCAST_NOTIFY.name());
        prev.putExtra(Shared.Service.PREVIOUS.name(), true);

        Intent shuffle = new Intent(Shared.Broadcasters.BROADCAST_NOTIFY.name());
        shuffle.putExtra(Shared.Service.SHUFFLE_VALUE.name(), true);


        PendingIntent playpendingIntent  = PendingIntent.getBroadcast(getActivity().getApplicationContext(), 1235, play,
                PendingIntent.FLAG_UPDATE_CURRENT);
        expandedView.setOnClickPendingIntent(R.id.notifyplayButton, playpendingIntent);

        PendingIntent pausependingIntent  = PendingIntent.getBroadcast(getActivity().getApplicationContext(), 1236, pause,
                PendingIntent.FLAG_UPDATE_CURRENT);
        expandedView.setOnClickPendingIntent(R.id.notifypauseButton, pausependingIntent);

        PendingIntent nextpendingIntent  = PendingIntent.getBroadcast(getActivity().getApplicationContext(), 1237, next,
                PendingIntent.FLAG_UPDATE_CURRENT);
        expandedView.setOnClickPendingIntent(R.id.notifynextButton, nextpendingIntent);

        PendingIntent previouspendingIntent  = PendingIntent.getBroadcast(getActivity().getApplicationContext(), 1238, prev,
                PendingIntent.FLAG_UPDATE_CURRENT);
        expandedView.setOnClickPendingIntent(R.id.notifypreviousButton, previouspendingIntent);

        PendingIntent shufflependingIntent  = PendingIntent.getBroadcast(getActivity().getApplicationContext(), 1239, shuffle,
                PendingIntent.FLAG_UPDATE_CURRENT);
        expandedView.setOnClickPendingIntent(R.id.notifyshuffleButton, shufflependingIntent);

        notification.bigContentView = expandedView;
        //}
        // END_INCLUDE(customLayout)

        // START_INCLUDE(notify)
        // Use the NotificationManager to show the notification
        NotificationManager nm = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(1340, notification);
        // END_INCLUDE(notify)
    }
}
