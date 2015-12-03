package com.passthejams.app;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

/**
 * Created by Eden on 11/8/2015.
 * CursorAdapter that allows album art to be retrieved from content provider
 * Also sets ImageView's tag to the position it is in the underlying Cursor
 */
class JamsCursorAdapter extends SimpleCursorAdapter {
    final String TAG = "JamsCursorAdapter";
    private Context mContext;


    public JamsCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
        super(context, layout, c, from, to);
        mContext = context;
    }
    @Override
    public void setViewImage(ImageView v, String value) {
        Shared.getAlbumArt(mContext, v, value);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);
        /*set view's tag to its position in the cursor so it can remember what position it is*/
        int pos = cursor.getPosition();
        View v = view.findViewById(R.id.imageButton);
        if(v != null) {
            v.setTag(pos);
        }
    }
}
