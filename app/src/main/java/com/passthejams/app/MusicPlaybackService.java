package com.passthejams.app;

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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

/**
 * Created by Eden on 7/20/2015.
 */
public class MusicPlaybackService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener {
    /*
    Intents will have the following:
        position of song to begin playing
        action that is being requested
     */

    //used for binding service
    public class PlaybackBinder extends Binder {
        MusicPlaybackService getService() {
            return MusicPlaybackService.this;
        }
    }
    PlaybackBinder mBinder = new PlaybackBinder();

    //holds a paused song, the position of the song in the database and the position to seek to
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
    //holds the object to make the queue from
    class QueueObjectInfo {
        Cursor mCursor;
        int mStartPosition;
        boolean mShuffle;
        boolean mRepeat;
        public QueueObjectInfo(Cursor c, int start, boolean shuffle, boolean repeat) {
            mCursor = c;
            mStartPosition = start;
            mShuffle = shuffle;
            mRepeat = repeat;
        }
    }

    public class JamsQueue<K, V> extends TreeMap<K, V> {
        private NowPlayingFragment.OnQueueChangeListener mListener;

        public void setOnQueueChangeListener(NowPlayingFragment.OnQueueChangeListener e) {
            mListener = e;
        }
        public void doEvent() {
            if(mListener != null) {
                mListener.onEvent();
            }
        }
    }

    //the media player everything will be playing from
    MediaPlayer mMediaPlayer = new MediaPlayer();
    //a paused holder so that there is not more than one
    PausedSongHolder pausedSongHolder = new PausedSongHolder();
    //position for logs
    int cursorPosition;

    //position in treemap
    int playPosition;
    boolean mShuffle = false;
    //int mRepeat = 0; //where mRepeat=0 is off, 1 is repeat queue, 2 is repeat song

    //broadcast manager to update ui
    LocalBroadcastManager localBroadcastManager;
    BroadcastReceiver shuffleReceiver;//, repeatReceiver;

    JamsQueue<Integer, TrackInfo> songqueue = new JamsQueue<>();

    //for logging
    final String TAG = "Service";
    //these are possibly confusing
    final boolean PLAY = true, PAUSE=false;

    @Override
    public void onCreate() {
        super.onCreate();
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        songqueue.setOnQueueChangeListener(new NowPlayingFragment.OnQueueChangeListener() {
            @Override
            public void onEvent() {
                //sends album art to fragment to display the current artwork
                Intent queue = new Intent(Shared.Broadcasters.BROADCAST_QUEUE.name());
                Gson g = new Gson();
                queue.putExtra(Shared.Broadcasters.QUEUE_VALUE.name(), g.toJson(songqueue,
                        new TypeToken<JamsQueue<Integer, TrackInfo>>(){}.getType()));
                localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
                localBroadcastManager.sendBroadcast(queue);
            }
        });
        //create listeners for when the media player has loaded & finishes playing
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);

        /*button switching handling*/
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


        /*button switching handling*//*
        repeatReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "receiving repeat clicked");
                mRepeat = intent.getIntExtra(Shared.Service.REPEAT_VALUE.name(), 0);
            }
        };
        //actual registration of that listener
        localBroadcastManager.registerReceiver(repeatReceiver,
                new IntentFilter(Shared.Service.BROADCAST_REPEAT.name()));*/


    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return flags;
    }

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
    //get rid of paused song if it exists, move to next in cursor
    public void serviceOnNext() {
        discardPause();
        playPosition++;
        songqueue.doEvent();
        changeSong();
    }

    //get rid of paused song if it exists, move to previous in cursor
    public void serviceOnPrevious() {
        discardPause();
        playPosition--;
        songqueue.doEvent();
        changeSong();
    }

    /* takes QueueObjectInfo inputQ which allows it to create the queue
     * boolean discard which tells it whether to discard a pause or not
     * boolean discardQ which tells it whether to discard the queue or not
     *      if discardQ is false, the queue is discarded*/
    public void serviceOnPlay(QueueObjectInfo inputQ, boolean discard, boolean discardQ) {
        Log.v(TAG, "Cursor size:" + inputQ.mCursor.getCount());
        int temp = 0;

        int mPlayOrder = 0;

        //keeping queue
        if(!discardQ) {
            discard = true;
            songqueue.clear();
            ArrayList<Integer> playOrders = new ArrayList<Integer>();
            for(int i = 0; i < inputQ.mCursor.getCount(); i++) {
                playOrders.add(i);
            }
            if(mShuffle) {
                Collections.shuffle(playOrders);
            }
            //add all songs from the current cursor to the queue
            while (inputQ.mCursor.moveToPosition(temp)) {
                songqueue.put(playOrders.get(mPlayOrder), new TrackInfo(
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
        Log.d(TAG, songqueue.toString());
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

    public boolean changeSong(){
        int seekPosition=0;
        //if playPosition has not gone past the queue
        if(playPosition <= songqueue.lastKey() && playPosition >= songqueue.firstKey()) {
            Log.v(TAG, "position: " + playPosition);
            cursorPosition = playPosition;
            //get the TrackInfo
            TrackInfo trackInfo = songqueue.get(playPosition);
            //get the item's uri

            Uri contentUri = ContentUris.withAppendedId(Shared.libraryUri,
                    songqueue.get(playPosition).id);
            Log.v("Service", trackInfo.name);

            //sends album art to fragment to display the current artwork
            Intent albumArt = new Intent(Shared.Broadcasters.BROADCAST_ART.name());
            Log.v(TAG, "sending over album id: " + trackInfo);
            Gson g = new Gson();
            albumArt.putExtra(Shared.Broadcasters.ART_VALUE.name(), g.toJson(trackInfo, TrackInfo.class));

            localBroadcastManager.sendBroadcast(albumArt);

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

    //method to communicate to fragment about ui
    public void sendButtonValue(boolean value) {
        //create an intent so that the broadcast receiver can filter
        Intent playPauseUpdate = new Intent(Shared.Broadcasters.BROADCAST_BUTTON.name());
        playPauseUpdate.putExtra(Shared.Broadcasters.BUTTON_VALUE.name(), value);
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.sendBroadcast(playPauseUpdate);
    }
    public void discardPause() {
        Log.v(TAG, "discarded pause");
        pausedSongHolder = new PausedSongHolder();
    }

    //once media player is ready to play
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
        //queue.close();
        stopSelf();
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //here we need to play the next song in the queue
        serviceOnNext();
    }

    /*
        addSimilarToQueue takes an ArrayList of TrackInfo and adds it to the songqueue
        called after LastFm to add similar songs
     */
    public void addSimilarToQueue(ArrayList<TrackInfo> trackInfos) {
        TrackInfo baseSong = songqueue.get(playPosition);
        //clear the current queue
        songqueue.clear();
        int playOrder = 0;
        //add the original song
        songqueue.put(playOrder, baseSong);
        //add all of the similar songs
        for(TrackInfo t : trackInfos) {
            if(!t.equals(baseSong)) {
                songqueue.put(++playOrder, t);
            }
            else {
                Log.d(TAG, "added song is base song");
            }
        }
        discardPause();
        playPosition = 0;
        changeSong();
        Log.d(TAG, "insert size: " + trackInfos.size());
        Log.d(TAG, "Queue size: " + String.valueOf(songqueue.size()));
        songqueue.doEvent();
    }
    /* added to get TrackInfo to send to LastFm */
    public TrackInfo getCurrentPlaying() {
        return songqueue.get(playPosition);
    }

    public TreeMap<Integer, TrackInfo> getQueue() {
        return songqueue;
    }
}
