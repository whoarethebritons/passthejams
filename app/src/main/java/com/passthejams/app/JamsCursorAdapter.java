package com.passthejams.app;

import android.content.Context;
import android.database.Cursor;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

/**
 * Created by Eden on 11/8/2015.
 */
public class JamsCursorAdapter extends SimpleCursorAdapter {
    Context mContext;
    public JamsCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
        super(context, layout, c, from, to);
        mContext = context;
    }
    @Override
    public void setViewImage(ImageView v, String value) {
        Shared.getAlbumArt(mContext, v, value);
    }
}
