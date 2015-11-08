package com.passthejams.app;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

/**
 * Created by Eden on 11/6/2015.
 */
public class GenericTabActivity<T extends AbsListView> extends Activity {
    Cursor mCursor;
    final String TAG= "Generic Tab";
    int list_id;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        int layout_id = getIntent().getIntExtra(Shared.TabIntent.LAYOUT.name(), R.layout.song_layout);
        list_id = getIntent().getIntExtra(Shared.TabIntent.LISTVIEW.name(), android.R.id.list);

        int row_id = getIntent().getIntExtra(Shared.TabIntent.ROWID.name(), android.R.id.list);
        setContentView(layout_id);

        String[] projectionString = getIntent().getStringArrayExtra(Shared.TabIntent.PROJECTION_STRING.name());
        String selectionString = getIntent().getStringExtra(Shared.TabIntent.SELECTION_STRING.name());
        String[] selectionArguments = getIntent().getStringArrayExtra(Shared.TabIntent.SELECTION_ARGS.name());
        Uri uri = Uri.parse(getIntent().getStringExtra(Shared.TabIntent.URI.name()));

        //query the database given the passed items
        mCursor = managedQuery(uri, projectionString, selectionString, selectionArguments, null);

        //get the view
        T lv = (T) findViewById(list_id);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        //text to display
        String[] displayFields = getIntent().getStringArrayExtra(Shared.TabIntent.DISPLAY_FIELDS.name());
        //fields to display text in
        int[] displayText = getIntent().getIntArrayExtra(Shared.TabIntent.DISPLAY_TEXT.name());

        SimpleCursorAdapter simpleCursorAdapter = new myCursorAdapter(this, row_id, mCursor,
                displayFields, displayText);

        //set adapter
        lv.setAdapter(simpleCursorAdapter);
        genericTabInterface ef = (genericTabInterface) getParent();
        //lv.setOnItemClickListener(ef.getListener());
        ef.passCursor(mCursor);
    }
    @Override
    public void onResume() {
        Log.v(TAG, "on content changed");
        T lv = (T) findViewById(list_id);
        Activity a = getParent();
        genericTabInterface ef = (genericTabInterface) a;
        lv.setOnItemClickListener(ef.getListener());
        ef.passCursor(mCursor);
        super.onResume();
    }
    public class myCursorAdapter extends SimpleCursorAdapter {
        public myCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to);
        }
        @Override
        public void setViewImage(ImageView v, String value) {
            Shared.getAlbumArt(getApplicationContext(), v, value);
        }
    }

    public interface genericTabInterface {
        AdapterView.OnItemClickListener getListener();
        void passCursor(Cursor c);
    }
}
