package com.passthejams.app;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

import java.util.concurrent.ExecutionException;

/**
 * Created by Eden on 11/8/2015.
 */
public class JamsCursorAdapter extends SimpleCursorAdapter {
    final String TAG = "JamsCursorAdapter";
    Context mContext;
    int mLayout;


    public JamsCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
        super(context, layout, c, from, to);
        mContext = context;
        mLayout = layout;
    }
    @Override
    public void setViewImage(ImageView v, String value) {
        Shared.getAlbumArt(mContext, v, value);
    }
}
