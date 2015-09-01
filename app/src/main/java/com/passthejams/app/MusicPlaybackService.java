package com.passthejams.app;

import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
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
    //the media player everything will be playing from
    MediaPlayer mMediaPlayer = new MediaPlayer();
    //a paused holder so that there is not more than one
    PausedSongHolder pausedSongHolder = new PausedSongHolder();
    //position for logs
    int cursorPosition;
    //cursor to move through database
    Cursor queue = null;
    //broadcast manager to update ui
    LocalBroadcastManager localBroadcastManager;

    //for logging
    final String TAG = "Service";
    //these are possibly confusing
    final boolean PLAY = true, PAUSE=false;
    final int SONGID=0, SONGTITLE = 1, ALBUMID=2;

    @Override
    public void onCreate() {
        super.onCreate();
        //create listeners for when the media player has loaded & finishes playing
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);

        //columns of database the cursor is selecting
        String[] mediaList = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM_ID};
        //repeat query so media player has a version separate from the ui
        queue = getContentResolver().query(Shared.libraryUri, mediaList,
                MediaStore.Audio.Media.IS_MUSIC + "!=0", null, null);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return flags;
    }

    public void serviceOnPause() {
        if(mMediaPlayer.isPlaying()) {
            //set the paused song holder to contain the position of the cursor and seek position
            pausedSongHolder.setIndex(cursorPosition);
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
        changeSong(queue.moveToPosition(cursorPosition + 1));
    }

    //get rid of paused song if it exists, move to previous in cursor
    public void serviceOnPrevious() {
        discardPause();
        changeSong(queue.moveToPosition(cursorPosition - 1));
    }

    public void serviceOnPlay(int pos, boolean discard) {
        Log.v(TAG, "Position:" + pos);
        //handle whether this is a new click or not
        if(discard||pausedSongHolder.index == -1) {
            discardPause();
            changeSong(queue.moveToPosition(pos));
        }
        else {
            changeSong(queue.moveToPosition(pausedSongHolder.getIndex()));
            int seekPosition = pausedSongHolder.getSeekTo();
            Log.v(TAG, "seek: " + seekPosition);
        }
    }

    public boolean changeSong(boolean input) {
        int seekPosition=0;
        //if the cursor was able to move to this item
        if(input) {
            Log.v(TAG, "position: " + queue.getPosition());
            cursorPosition = queue.getPosition();
            //get the item's uri
            Uri contentUri = ContentUris.withAppendedId(Shared.libraryUri,
                    queue.getLong(SONGID));
            Log.v("Service", queue.getString(SONGTITLE));

            //sends album art to fragment to display the current artwork
            Intent albumArt = new Intent(Shared.BROADCAST_ART);
            Log.v(TAG, "sending over album id: " + queue.getString(ALBUMID));
            albumArt.putExtra(Shared.ART_VALUE, queue.getString(ALBUMID));
            localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
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
            return input;
        }
    }

    //method to communicate to fragment about ui
    public void sendButtonValue(boolean value) {
        //create an intent so that the broadcast receiver can filter
        Intent playPauseUpdate = new Intent(Shared.BROADCAST_BUTTON);
        playPauseUpdate.putExtra(Shared.BUTTON_VALUE, value);
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.sendBroadcast(playPauseUpdate);
    }
    public void discardPause() {
        Log.v(TAG, "discarded pause");
        pausedSongHolder = new PausedSongHolder();
    }

     /*added for media player control*/

    public int getSongPosition() {
        return mMediaPlayer.getCurrentPosition();
    }
    public void onSeekTo(int position) {
        mMediaPlayer.seekTo(position);
    }
    public boolean isMediaPlaying() {
        return mMediaPlayer.isPlaying();
    }/* //functionality later
    public boolean canGoBack() {
        int positionHolder = cursorPosition;
        boolean ret = queue.moveToPosition(cursorPosition -1);
        queue.moveToPosition(positionHolder);
        return ret;
    }
    public boolean canGoNext() {
        int positionHolder = cursorPosition;
        boolean ret = queue.moveToPosition(cursorPosition +1);
        queue.moveToPosition(positionHolder);
        return ret;
    }*/
    public int getDuration() {
        return mMediaPlayer.getDuration();
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
        queue.close();
        stopSelf();
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //here we need to play the next song in the queue
        serviceOnNext();
    }
}
