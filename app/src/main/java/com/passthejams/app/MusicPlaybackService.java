package com.passthejams.app;

import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;

/**
 * Created by Eden on 7/20/2015.
 */
public class MusicPlaybackService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    /*
    Intents will have the following:
        position of song to begin playing
        action that is being requested
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
    MediaPlayer mMediaPlayer = new MediaPlayer();
    PausedSongHolder pausedSongHolder = new PausedSongHolder();
    int cursorPosition;
    Uri libraryURI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    Cursor queue = null;
    LocalBroadcastManager localBroadcastManager;
    final boolean TOGGLE = true;
    //for logging
    final String TAG = "Service";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        String[] mediaList = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE};
        //repeat query
        queue = getContentResolver().query(libraryURI, mediaList, MediaStore.Audio.Media.IS_MUSIC + "!=0",
                null, null);
        String s = intent.getStringExtra(MainActivity.OPTION);
        switch(s) {
            case "play":
                Log.v("service", "play clicked");
                serviceOnPlay(intent.getIntExtra(MainActivity.POSITION, -1),
                        intent.getBooleanExtra(MainActivity.DISCARD_PAUSE, true));
                break;
            case "next":
                Log.v("service", "next clicked");
                serviceOnNext();
                break;
            case "previous":
                Log.v("service", "previous clicked");
                serviceOnPrevious();
                break;
            case "pause":
                Log.v("service", "pause clicked");
                serviceOnPause();
                break;
            default:
                break;
        }
        return flags;
    }

    public void serviceOnPause() {
        if(mMediaPlayer.isPlaying()) {
            pausedSongHolder.setIndex(cursorPosition);
            pausedSongHolder.setSeekTo(mMediaPlayer.getCurrentPosition());
            Log.v(TAG, pausedSongHolder.getIndex() + " seek to " + pausedSongHolder.getSeekTo());
            mMediaPlayer.pause();
            //eventually switch out the buttons
            Intent playPauseUpdate = new Intent("button-event");
            playPauseUpdate.putExtra("value", TOGGLE);
            localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
            localBroadcastManager.sendBroadcast(playPauseUpdate);
        }
    }
    public boolean serviceOnNext() {
        return changeSong(queue.moveToPosition(cursorPosition + 1));
    }
    public void serviceOnPrevious() {
        changeSong(queue.moveToPosition(cursorPosition - 1));
    }
    private void serviceOnPlay(int pos, boolean discard) {
        if(discard) {
            Log.v(TAG, "discarded pause");
            pausedSongHolder = new PausedSongHolder();
            changeSong(queue.moveToPosition(pos));
        }
        else {
            changeSong(queue.moveToPosition(pausedSongHolder.getIndex()));
            int seekPosition = pausedSongHolder.getSeekTo();
            Log.v(TAG, "seek: " + seekPosition);
        }
        Intent playPauseUpdate = new Intent("button-event");
        playPauseUpdate.putExtra("value", TOGGLE);
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.sendBroadcast(playPauseUpdate);
    }

    public boolean changeSong(boolean input) {
        int seekPosition =0;
        //if the cursor was able to move to this item
        if(input) {
            Log.v(TAG, "position: " + queue.getPosition());
            cursorPosition = queue.getPosition();
            //get the item's uri
            Uri contentUri = ContentUris.withAppendedId(libraryURI,
                    queue.getLong(0));
            Log.v("Service", queue.getString(1));
            try {
                //make sure to clear any player that is already playing
                if(mMediaPlayer != null) {
                    Log.v("Service", mMediaPlayer.toString());
                    mMediaPlayer.reset();
                    mMediaPlayer.setDataSource(getApplicationContext(), contentUri);
                    mMediaPlayer.prepareAsync();
                    Log.v(TAG, "seeking to " + seekPosition);
                } /*else {
                    mMediaPlayer = MediaPlayer.create(getApplicationContext(), contentUri);
                    Log.v("Service", "new player: " + mMediaPlayer.toString());
                }*/
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return mMediaPlayer.isPlaying();
        }
        else {
            mMediaPlayer.release();
            stopSelf();
        }
        return input;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.seekTo(pausedSongHolder.getSeekTo());
        mp.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //here we need to play the next song in the queue
        serviceOnNext();
    }
}
