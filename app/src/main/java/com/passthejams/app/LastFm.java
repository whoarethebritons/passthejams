package com.passthejams.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;
import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;
import de.umass.lastfm.Caller;
import de.umass.lastfm.ResponseBuilder;
import de.umass.lastfm.Result;
import de.umass.lastfm.Track;
import de.umass.lastfm.cache.FileSystemCache;
import de.umass.util.StringUtilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by eden on 9/11/15.
 * LastFm is used to retrieve similar songs from the Last.Fm database
 * then check if the user owns those songs
 * and finally add them to the app's personal database of similar songs
 */
public class LastFm extends Activity{
    final String TAG = "LastFm";
    private TrackInfo trackInfo;
    //TODO: better way to store API key from Last.Fm
    private String key="1d8a009cf65d2b94309cd1d52731b6d4";

    private DB snappydb;
    private Context mContext;
    public LastFm(Context c, TrackInfo trackInfo) {
        this.trackInfo = trackInfo;
        mContext = c;
        try {
            snappydb = DBFactory.open(c, "passthejams"); //create or open an existing database using the default name
        } catch (SnappydbException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public ArrayList<TrackInfo> generateOrGetSimilar(ContentResolver contentResolver) {
        Cursor mCursor;
        //get current trackinfo
        int id = trackInfo._id;
        String song = trackInfo.title;
        String artist = trackInfo.artist;
        int album_id;
        ArrayList<TrackInfo> similar = new ArrayList<TrackInfo>();

        //TODO: limit application to at most 5 requests per second
        try {
            //if it already exists, get the ArrayList from the database
            similar = snappydb.getObject(String.valueOf(id), similar.getClass());
            Log.d(TAG, "exists in db");
        } catch (SnappydbException e) {
            /*overriding setup of Caller from LastFm API mainly by adding Android's cache directory*/
            FileSystemCache fileSystemCache = new FileSystemCache(mContext.getCacheDir());
            Caller caller = Caller.getInstance();
            caller.setCache(fileSystemCache);
            Map<String, String> params = new HashMap<>();
            if (StringUtilities.isMbid(song)) {
                params.put("mbid", song);
            } else {
                params.put("artist", artist);
                params.put("track", song);
            }
            Result result = caller.call("track.getSimilar", key, params);
            Collection<Track> tracks = ResponseBuilder.buildCollection(result, Track.class);

            //see if each track exists on the device
            for( Track t: tracks) {
                Log.v(TAG, t.getName());
                mCursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Shared.PROJECTION_SONG,
                        MediaStore.Audio.Media.TITLE + "=?", new String[]{t.getName()}, null);
                if(mCursor.moveToFirst()) {
                    id = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Audio.Media._ID));
                    song = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    artist = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    album_id = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                    //if it does add it
                    similar.add(new TrackInfo(id, album_id, song, artist));
                }
                mCursor.close();
            }
            try {
                //write this information to the database
                snappydb.put(String.valueOf(id), similar);
            } catch (SnappydbException e1) {
                Log.e(TAG, e1.getMessage());
            }
        }

        try {
            snappydb.close();
        } catch (SnappydbException e) {
            Log.e(TAG, e.getMessage());
        }
        //return ArrayList that Fragment will add to queue
        return similar;
    }
}
