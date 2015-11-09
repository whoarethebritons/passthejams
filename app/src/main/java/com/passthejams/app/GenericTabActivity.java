package com.passthejams.app;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;

/**
 * Created by Eden on 11/6/2015.
 * GenericTabActivity extends AbsListView so that either a GridView
 * or ListView can be used with it.
 * An Intent is passed to it with the following parameters:
 * *the layout id
 * *the list id
 * *the layout of the row
 * *the projection string
 * *the selection string
 * *the selection arguments
 * *the uri
 * *the display fields
 * *the display text
 */
public class GenericTabActivity<T extends AbsListView> extends Activity {
    Cursor mCursor;
    final String TAG= "Generic Tab";
    int list_id;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //the fields to make the layout
        int layout_id = getIntent().getIntExtra(Shared.TabIntent.LAYOUT.name(), R.layout.song_layout);
        list_id = getIntent().getIntExtra(Shared.TabIntent.LISTVIEW.name(), android.R.id.list);
        int row_layout = getIntent().getIntExtra(Shared.TabIntent.ROWID.name(), R.layout.song_layout);

        //set the content view
        setContentView(layout_id);

        //all the fields to create the query
        String[] projectionString = getIntent().getStringArrayExtra(Shared.TabIntent.PROJECTION_STRING.name());
        String selectionString = getIntent().getStringExtra(Shared.TabIntent.SELECTION_STRING.name());
        String[] selectionArguments = getIntent().getStringArrayExtra(Shared.TabIntent.SELECTION_ARGS.name());
        Uri uri = Uri.parse(getIntent().getStringExtra(Shared.TabIntent.URI.name()));

        //query the database given the passed items
        mCursor = managedQuery(uri, projectionString, selectionString, selectionArguments, null);

        //get the view as a generic that extends AbsListView (i.e. GridView, ListView)
        T lv = (T) findViewById(list_id);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        //text to display
        String[] displayFields = getIntent().getStringArrayExtra(Shared.TabIntent.DISPLAY_FIELDS.name());
        //fields to display text in
        int[] displayText = getIntent().getIntArrayExtra(Shared.TabIntent.DISPLAY_TEXT.name());

        SimpleCursorAdapter simpleCursorAdapter = new JamsCursorAdapter(this, row_layout, mCursor,
                displayFields, displayText);

        //set adapter
        lv.setAdapter(simpleCursorAdapter);
        //get click listener
        genericTabInterface ef = (genericTabInterface) getParent();
        lv.setOnItemClickListener(ef.getListener());
        //send over cursor
        ef.passCursor(mCursor);
    }
    //use onResume so that the cursor gets updated each time the tab is switched
    @Override
    public void onResume() {
        Log.v(TAG, "on tab changed");
        //get the AbsListView
        T lv = (T) findViewById(list_id);
        Activity a = getParent();
        genericTabInterface ef = (genericTabInterface) a;
        //set click listener
        lv.setOnItemClickListener(ef.getListener());
        //pass cursor
        ef.passCursor(mCursor);
        //call super to perform other actions
        super.onResume();
    }

    public interface genericTabInterface {
        AdapterView.OnItemClickListener getListener();
        void passCursor(Cursor c);
    }
}
