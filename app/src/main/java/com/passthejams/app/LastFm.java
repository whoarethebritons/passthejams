package com.passthejams.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;
import com.snappydb.SnappydbException;
import de.umass.lastfm.Caller;
import de.umass.lastfm.ResponseBuilder;
import de.umass.lastfm.Result;
import de.umass.lastfm.Track;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.snappydb.DB;
import com.snappydb.DBFactory;
import de.umass.lastfm.cache.FileSystemCache;
import de.umass.util.StringUtilities;

/**
 * Created by eden on 9/11/15.
 */
public class LastFm extends Activity{
    TrackInfo trackInfo;
    String key="1d8a009cf65d2b94309cd1d52731b6d4";
    final String TAG = "LastFm";
    DB snappydb;
    Context mContext;
    public LastFm(){}
    public LastFm(Context c, TrackInfo trackInfo) {
        this.trackInfo = trackInfo;
        mContext = c;
        try {
            snappydb = DBFactory.open(c, "passthejams"); //create or open an existing database using the default name
        } catch (SnappydbException e) {
        }
    }

    public ArrayList<TrackInfo> generateOrGetSimilar(ContentResolver contentResolver) {
        Cursor mCursor;
        int id = trackInfo.id;
        String song =trackInfo.name;
        String artist = trackInfo.artist;
        int album_id;
        ArrayList<TrackInfo> similar = new ArrayList<>();

        try {
            similar = snappydb.getObject(String.valueOf(id), similar.getClass());
            Log.d(TAG, "exists in db");
        } catch (SnappydbException e) {
            Log.d(TAG, "does not exist in db");
            FileSystemCache fileSystemCache = new FileSystemCache(mContext.getCacheDir());
            Caller caller = Caller.getInstance();
            caller.setCache(fileSystemCache);
            Map<String, String> params = new HashMap<String, String>();
            if (StringUtilities.isMbid(song)) {
                params.put("mbid", song);
            } else {
                params.put("artist", artist);
                params.put("track", song);
            }
            Result result = caller.call("track.getSimilar", key, params);
            Collection<Track> tracks = ResponseBuilder.buildCollection(result, Track.class);
            for( Track t: tracks) {
                Log.v(TAG, t.getName());
                mCursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Shared.PROJECTION_SONG,
                        MediaStore.Audio.Media.TITLE + "=?", new String[]{t.getName()}, null);
                if(mCursor.moveToFirst()) {
                    id = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Audio.Media._ID));
                    song = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    artist = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    album_id = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                    similar.add(new TrackInfo(id, album_id, song, artist));
                }
                mCursor.close();
            }
            try {
                snappydb.put(String.valueOf(id), similar);
            } catch (SnappydbException e1) {
                e1.printStackTrace();
            }
        }

        try {
            snappydb.close();
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
        return similar;
    }
}
