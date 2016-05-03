package com.passthejams.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * Created by Eden on 7/20/2015.
 * MusicPlaybackService is a Service running in the background during
 * the lifetime of the application. It is bound to the BottomMusicFragment.
 * BottomMusicFragment (and other fragments/activities that utilize its methods)
 * allows the user to interact with the MusicPlaybackService and access its methods.
 */
public class MusicPlaybackService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener {
    /**
     * Intents will have the following:
     * position of song to begin playing
     * action that is being requested
     */

    public class PlaybackBinder extends Binder {
        /**
         * @return MusicPlaybackService for binding
         */
        MusicPlaybackService getService() {
            return MusicPlaybackService.this;
        }
    }
    private PlaybackBinder mBinder = new PlaybackBinder();

    /**
     * holds a paused song, the position of the song in the database and the position to seek to
     */
    class PausedSongHolder {
        int index;
        int seekTo;
        public PausedSongHolder() {
            index=-1;
            seekTo=0;
        }
        public void setIndex(int i) {index=i;}
        public void setSeekTo(int i){seekTo=i;}
        public int getIndex(){return index;}
        public int getSeekTo(){return seekTo;}
    }
    /**
     *  holds the object to make the queue from
     */
    class QueueObjectInfo {
        Cursor mCursor;
        int mStartPosition;
        public QueueObjectInfo(Cursor c, int start) {
            mCursor = c;
            mStartPosition = start;
        }
    }

    public class JamsQueue<K> extends ArrayList<K> {
        private NowPlayingFragment.OnQueueChangeListener mListener;

        /**
         * @param e sets callback listener
         */
        public void setOnQueueChangeListener(NowPlayingFragment.OnQueueChangeListener e) {
            mListener = e;
        }

        /**
         * performs callback
         */
        public void doEvent() {
            if(mListener != null) {
                mListener.onEvent();
            }
        }
        public int lastKey() {
            return this.size();
        }
        public int firstKey() {
            return 0;
        }
    }

    //the media player everything will be playing from
    private MediaPlayer mMediaPlayer = new MediaPlayer();
    //a paused holder so that there is not more than one
    private PausedSongHolder pausedSongHolder = new PausedSongHolder();
    //position for logs
    private int cursorPosition;

    //position in TreeMap
    private int playPosition;
    private boolean mShuffle = false;

    //broadcast manager to update ui
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver shuffleReceiver;

    private JamsQueue<TrackInfo> songQueue = new JamsQueue<>();

    //for logging
    final String TAG = "Service";
    //TODO: these are possibly confusing
    private final boolean PLAY = true, PAUSE=false;

    @Override
    public void onCreate() {
        super.onCreate();
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        songQueue.setOnQueueChangeListener(new NowPlayingFragment.OnQueueChangeListener() {
            @Override
            public void onEvent() {
                //sends album art to fragment to display the current artwork
                Intent queue = new Intent(Shared.Broadcasters.BROADCAST_QUEUE.name());
                Gson g = new Gson();
                queue.putExtra(Shared.Broadcasters.QUEUE_VALUE.name(), g.toJson(songQueue,
                        new TypeToken<JamsQueue<TrackInfo>>() {
                        }.getType()));
                localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
                localBroadcastManager.sendBroadcast(queue);
            }
        });
        //create listeners for when the media player has loaded & finishes playing
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);

        /*shuffle switching handling*/
        shuffleReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "receiving shuffle clicked");
                mShuffle = intent.getBooleanExtra(Shared.Service.SHUFFLE_VALUE.name(), false);
                Log.d(TAG, String.valueOf(mShuffle));
            }
        };
        //actual registration of that listener
        localBroadcastManager.registerReceiver(shuffleReceiver,
                new IntentFilter(Shared.Service.BROADCAST_SHUFFLE.name()));
        createNotification(false, false, false, null);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "receiving notification");
        if(intent.getBooleanExtra(Shared.Service.PLAY.name(), false)) {
            Log.v(TAG, "from play");
            pressPlay();
            createNotification(true, true, false, null);
        }
        else if(intent.getBooleanExtra(Shared.Service.PREVIOUS.name(), false)) {
            Log.v(TAG, "from previous");
            serviceOnPrevious();
        }
        else if(intent.getBooleanExtra(Shared.Service.NEXT.name(), false)) {
            Log.v(TAG, "from next");
            serviceOnNext();
        }
        else if(intent.getBooleanExtra(Shared.Service.PAUSE.name(), false)) {
            Log.v(TAG, "from pause");
            serviceOnPause();
            createNotification(true, false, false, null);
        }
        else if(intent.getBooleanExtra(Shared.Service.SHUFFLE_VALUE.name(), false)) {
            Log.v(TAG, "from shuffle");
        }
        else {
            Log.v(TAG, "not a button");
        }
        return flags;
    }

    /**
     * pauses current song
     * saves it to the pausedSongHolder
     * sends update information to UI to display the play button
     */
    public void serviceOnPause() {
        if(mMediaPlayer.isPlaying()) {
            //set the paused song holder to contain the position of the cursor and seek position
            pausedSongHolder.setIndex(playPosition);
            pausedSongHolder.setSeekTo(mMediaPlayer.getCurrentPosition());

            Log.v(TAG, pausedSongHolder.getIndex() + " seek to " + pausedSongHolder.getSeekTo());

            //actually perform the pause
            mMediaPlayer.pause();

            //switch out the buttons
            sendButtonValue(PAUSE);
        }
    }

    /**
     * gets rid of paused song if it exists
     * moves to next in cursor
     * sends that the queue should be updated with currently playing song
     */
    public void serviceOnNext() {
        Log.d(TAG, "service on next");
        discardPause();
        if(playPosition < songQueue.lastKey()) {
            playPosition++;
        }
        songQueue.doEvent();
        changeSong();
    }

    /**
     * gets rid of paused song if it exists
     * moves to previous in cursor
     * sends that the queue should be updated with currently playing song
     */
    public void serviceOnPrevious() {
        discardPause();
        if(playPosition > songQueue.firstKey()) {
            playPosition--;
        }
        songQueue.doEvent();
        changeSong();
    }

    /**
     * @param inputQ type QueueObjectInfo holds cursor and current position
     * @param discard type boolean: when true will discard a previous pause
     * @param discardQ type boolean: when false will discard a previous queue
     */
    public void serviceOnPlay(QueueObjectInfo inputQ, boolean discard, boolean discardQ) {
        Log.v(TAG, "Cursor size:" + inputQ.mCursor.getCount());
        int temp = 0;

        int mPlayOrder = 0;

        //keeping queue
        if(!discardQ) {
            discard = true;
            songQueue.clear();
            ArrayList<Integer> playOrders = new ArrayList<>();
            for(int i = 0; i < inputQ.mCursor.getCount(); i++) {
                playOrders.add(i);
            }
            if(mShuffle) {
                Collections.shuffle(playOrders);
            }
            //add all songs from the current cursor to the queue
            while (inputQ.mCursor.moveToPosition(temp)) {
                songQueue.add(playOrders.get(mPlayOrder), new TrackInfo(
                        inputQ.mCursor.getInt(inputQ.mCursor.getColumnIndex(MediaStore.Audio.Media._ID)),
                        inputQ.mCursor.getInt(inputQ.mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                        inputQ.mCursor.getString(inputQ.mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                        inputQ.mCursor.getString(inputQ.mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))));
                Log.v(TAG, "inserted: " + inputQ.mCursor.getInt(inputQ.mCursor.getColumnIndex(MediaStore.Audio.Media._ID))
                        + " at: " + mPlayOrder);
                mPlayOrder++;
                temp++;
            }

        }
        Log.d(TAG, songQueue.toString());
        //handle whether this is a new click or not
        if(discard || pausedSongHolder.index == -1) {
            discardPause();
            //set playPosition to the song that was clicked on
            playPosition = inputQ.mStartPosition;
            changeSong();
        }
        else {
            //set playPosition to the paused song position
            playPosition = pausedSongHolder.getIndex();
            changeSong();
            int seekPosition = pausedSongHolder.getSeekTo();
            Log.v(TAG, "seek: " + seekPosition);
        }

    }

    /**
     * @return boolean representing whether song change was successful
     */
    private boolean changeSong(){
        int seekPosition=0;
        //if playPosition has not gone past the queue
        if(playPosition < songQueue.lastKey() && playPosition >= songQueue.firstKey()) {
            Log.v(TAG, "position: " + playPosition);
            cursorPosition = playPosition;
            //get the TrackInfo
            TrackInfo trackInfo = songQueue.get(playPosition);
            //get the item's uri

            Uri contentUri = ContentUris.withAppendedId(Shared.libraryUri,
                    songQueue.get(playPosition)._id);
            Log.v("Service", trackInfo.title);

            //sends album art to fragment to display the current artwork
            Intent albumArt = new Intent(Shared.Broadcasters.BROADCAST_ART.name());
            Log.v(TAG, "sending over album id: " + trackInfo);
            Gson g = new Gson();
            albumArt.putExtra(Shared.Broadcasters.ART_VALUE.name(), g.toJson(trackInfo, TrackInfo.class));
            createNotification(true, false, true, trackInfo);
            localBroadcastManager.sendBroadcast(albumArt);
            sendBroadcast(albumArt);

            try {
                //make sure to clear any player that is already playing
                if(mMediaPlayer != null) {
                    Log.v("Service", mMediaPlayer.toString());
                    mMediaPlayer.reset();
                    //tell it which uri to play
                    mMediaPlayer.setDataSource(getApplicationContext(), contentUri);
                    //prepare async in case it takes a while
                    mMediaPlayer.prepareAsync();
                    Log.v(TAG, "seeking to " + seekPosition);
                } else {
                    mMediaPlayer = MediaPlayer.create(getApplicationContext(), contentUri);
                    Log.v("Service", "new player: " + mMediaPlayer.toString());
                }
                //set type to play
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return mMediaPlayer.isPlaying();
        }
        else {
            Log.v(TAG, "No input to changeSong");
            mMediaPlayer.reset();
            sendButtonValue(PAUSE);
            stopSelf();
            return false;
        }
    }

    /**
     * method to communicate to fragment about ui
     */
    private void sendButtonValue(boolean value) {
        //create an intent so that the broadcast receiver can filter
        Intent playPauseUpdate = new Intent(Shared.Broadcasters.BROADCAST_BUTTON.name());
        playPauseUpdate.putExtra(Shared.Broadcasters.BUTTON_VALUE.name(), value);
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.sendBroadcast(playPauseUpdate);
        createNotification(true, value, false, null);
    }
    private void discardPause() {
        Log.v(TAG, "discarded pause");
        pausedSongHolder = new PausedSongHolder();
    }

    /**
     * once media player is ready to play
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        //seek to paused song seek (or 0 if there is no paused song)
        mp.seekTo(pausedSongHolder.getSeekTo());
        //start player
        mp.start();
        //send that it is playing so it shows the pause button
        sendButtonValue(PLAY);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        mMediaPlayer.release();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(shuffleReceiver);
        stopSelf();
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //here we need to play the next song in the queue
        serviceOnNext();
    }

    /**
     * @param trackInfos type ArrayList<TrackInfo>
     * is added to the songQueue
     * called after LastFm to add similar songs
     */
    public void addSimilarToQueue(ArrayList<TrackInfo> trackInfos) {
        TrackInfo baseSong = songQueue.get(playPosition);
        //clear the current queue
        songQueue.clear();
        int playOrder = 0;
        //add the original song
        songQueue.add(playOrder, baseSong);
        //add all of the similar songs
        for(TrackInfo t : trackInfos) {
            if(!t.equals(baseSong)) {
                songQueue.add(++playOrder, t);
            }
            else {
                Log.d(TAG, "added song is base song");
            }
        }
        discardPause();
        playPosition = 0;
        changeSong();
        Log.d(TAG, "insert size: " + trackInfos.size());
        Log.d(TAG, "Queue size: " + String.valueOf(songQueue.size()));
        songQueue.doEvent();
    }
    /**
     *  @return TrackInfo to send to LastFm
     */
    public TrackInfo getCurrentPlaying() throws IndexOutOfBoundsException {
        return songQueue.get(playPosition);
    }

    /**
     * @return TreeMap of songs in the queue
     */
    public ArrayList<TrackInfo> getQueue() {
        return songQueue;
    }

    /**
     * @param c type Cursor contains one item
     */
    public void addToQueue(Cursor c) {
        int play = songQueue.size();
        songQueue.add(play, new TrackInfo(
                c.getInt(c.getColumnIndex(MediaStore.Audio.Media._ID)),
                c.getInt(c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST))));
    }

    /**
     * @param c type Cursor contains one item
     */
    public void playNext(Cursor c) {
        int play = songQueue.size();
        int position = 0;
        if(playPosition != 0) {
            position = playPosition;
        }
        songQueue.add(position, new TrackInfo(
                c.getInt(c.getColumnIndex(MediaStore.Audio.Media._ID)),
                c.getInt(c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST))));
    }

    public void pressPlay() {
        Log.v(TAG, "press play");
        if(pausedSongHolder.index == -1) {
            discardPause();
            changeSong();
        }
        else {
            //set playPosition to the paused song position
            playPosition = pausedSongHolder.getIndex();
            changeSong();
            int seekPosition = pausedSongHolder.getSeekTo();
            Log.v(TAG, "seek: " + seekPosition);
        }
    }

    public void isPlaying() {
        //send that it is playing so it shows the pause button
        if (mMediaPlayer.isPlaying()) {
            sendButtonValue(PLAY);
        }
        else {
            sendButtonValue(PAUSE);
        }
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
        android.support.v4.app.NotificationCompat.Builder builder =
                new android.support.v4.app.NotificationCompat.Builder(getApplicationContext());

        builder.setTicker(getResources().getString(R.string.custom_notification));

        // Sets the small icon for the ticker
        builder.setSmallIcon(R.drawable.ic_launcher);
        // Cancel the notification when clicked
        builder.setAutoCancel(true);

        // Build the notification
        Notification notification = builder.build();
        // END_INCLUDE(buildNotification)

        // BEGIN_INCLUDE(customLayout)
        // Inflate the notification layout as RemoteViews
        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification);

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
                new RemoteViews(getPackageName(), R.layout.notification_expanded);

        if(updateButton) {
            //if that value is false, then mediaplayer is not playing
            if(!playPause) {
                contentView.setViewVisibility(R.id.notifyplayButton, View.VISIBLE);
                contentView.setViewVisibility(R.id.notifypauseButton, View.INVISIBLE);
                expandedView.setViewVisibility(R.id.notifyplayButton, View.VISIBLE);
                expandedView.setViewVisibility(R.id.notifypauseButton, View.INVISIBLE);
            }
            //otherwise it is playing and show the pause button
            else {
                contentView.setViewVisibility(R.id.notifyplayButton, View.INVISIBLE);
                contentView.setViewVisibility(R.id.notifypauseButton, View.VISIBLE);
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
                    getApplicationContext().getContentResolver().openInputStream(test);
                    contentView.setImageViewUri(R.id.notifycurrentAlbumArt, test);
                    expandedView.setImageViewUri(R.id.notifycurrentAlbumArt, test);
                }
                catch(FileNotFoundException e) {
                    Log.e("Shared", e.getMessage());
                    contentView.setImageViewResource(R.id.notifycurrentAlbumArt, R.drawable.default_album);
                    expandedView.setImageViewResource(R.id.notifycurrentAlbumArt, R.drawable.default_album);
                }

                contentView.setTextViewText(R.id.notifyArtistName, artLocation.artist);
                contentView.setTextViewText(R.id.notifySongName, artLocation.title);
                expandedView.setTextViewText(R.id.notifyArtistName, artLocation.artist);
                expandedView.setTextViewText(R.id.notifySongName, artLocation.title);
            }
        }
        Intent play = new Intent(this, MusicPlaybackService.class);
        play.putExtra(Shared.Service.PLAY.name(), true);

        Intent pause = new Intent(this, MusicPlaybackService.class);
        pause.putExtra(Shared.Service.PAUSE.name(), true);

        Intent next = new Intent(this, MusicPlaybackService.class);
        next.putExtra(Shared.Service.NEXT.name(), true);

        Intent prev = new Intent(this, MusicPlaybackService.class);
        prev.putExtra(Shared.Service.PREVIOUS.name(), true);

        Intent shuffle = new Intent(this, MusicPlaybackService.class);
        shuffle.putExtra(Shared.Service.SHUFFLE_VALUE.name(), true);


        PendingIntent playpendingIntent  = PendingIntent.getService(this, 1039, play, 0);
        PendingIntent pausependingIntent  = PendingIntent.getService(this, 1040, pause, 0);
        PendingIntent nextpendingIntent  = PendingIntent.getService(this, 1041, next, 0);
        PendingIntent previouspendingIntent  = PendingIntent.getService(this, 1042, prev, 0);
        PendingIntent shufflependingIntent  = PendingIntent.getService(this, 1043, shuffle, 0);

        contentView.setOnClickPendingIntent(R.id.notifyplayButton, playpendingIntent);
        contentView.setOnClickPendingIntent(R.id.notifypauseButton, pausependingIntent);
        contentView.setOnClickPendingIntent(R.id.notifynextButton, nextpendingIntent);
        contentView.setOnClickPendingIntent(R.id.notifypreviousButton, previouspendingIntent);

        expandedView.setOnClickPendingIntent(R.id.notifyplayButton, playpendingIntent);
        expandedView.setOnClickPendingIntent(R.id.notifypauseButton, pausependingIntent);
        expandedView.setOnClickPendingIntent(R.id.notifynextButton, nextpendingIntent);
        expandedView.setOnClickPendingIntent(R.id.notifypreviousButton, previouspendingIntent);
        expandedView.setOnClickPendingIntent(R.id.notifyshuffleButton, shufflependingIntent);

        notification.bigContentView = expandedView;
        //}
        // END_INCLUDE(customLayout)

        // START_INCLUDE(notify)
        // Use the NotificationManager to show the notification
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(1340, notification);
        // END_INCLUDE(notify)
    }
}
