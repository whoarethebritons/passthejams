package com.passthejams.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
    private Cursor mCursor;
    final String TAG= "Generic Tab";
    private int list_id;
    private String tabType;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        tabType = getIntent().getStringExtra(Shared.TabIntent.TYPE.name());

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
        String sortOrder = getIntent().getStringExtra(Shared.TabIntent.SORT_ORDER.name());
        Uri uri = Uri.parse(getIntent().getStringExtra(Shared.TabIntent.URI.name()));

        //query the database given the passed items
        mCursor = managedQuery(uri, projectionString, selectionString, selectionArguments, sortOrder);

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
        ef.passCursor(mCursor, tabType);
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
        ef.passCursor(mCursor, tabType);
        //call super to perform other actions
        super.onResume();
    }

    public void showMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_song, popup.getMenu());

        final int p = (int) view.getTag();
        Log.v(TAG, String.valueOf(p));
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_last_fm:
                        Log.d(TAG, "lastfm clicked");
                        BottomMusicFragment f = (BottomMusicFragment)
                                getParent().getFragmentManager().findFragmentById(R.id.bottomBar);
                        //Intent intent = makeActionIntent(button.name(), v.getVerticalScrollbarPosition(), discard);
                        Intent intent = new Intent(getParent(), MusicPlaybackService.class);
                        //specifies what action we will be performing
                        intent.putExtra(Shared.Main.OPTION.name(), Shared.Service.PLAY.name());
                        //specifies position in database of the song
                        intent.putExtra(Shared.Main.POSITION.name(), p);
                        //whether we are unpausing or skipping
                        intent.putExtra(Shared.Main.DISCARD_PAUSE.name(), false);
                        //requests position of song in list given current cursor
                        MusicPlaybackService.QueueObjectInfo queueObjectInfo =
                                new MusicPlaybackService().new QueueObjectInfo(mCursor,
                                        intent.getIntExtra(Shared.Main.POSITION.name(), 0));
                        //calls play
                        f.mService.serviceOnPlay(queueObjectInfo,
                                intent.getBooleanExtra(Shared.Main.DISCARD_PAUSE.name(), true), false);
                        //so that it can then run the lastfm method
                        Button lastfm = (Button) getParent().findViewById(R.id.lastfmButton);
                        lastfm.performClick();
                        return true;
                    case R.id.action_add_to_queue:
                        BottomMusicFragment fr = (BottomMusicFragment)
                                getParent().getFragmentManager().findFragmentById(R.id.bottomBar);
                        ListView lv = (ListView) findViewById(android.R.id.list);
                        //get item at requested position and add it to queue using BottomMusicFragment
                        fr.mService.addToQueue((Cursor)lv.getAdapter().getItem(p));
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }
    public interface genericTabInterface {
        /**
         * @return OnItemClickListener from observer
         */
        AdapterView.OnItemClickListener getListener();

        /**
         * @param c sends current Cursor to observer
         * @param type sends type (i.e. song, album, artist) of Cursor to observer
         */
        void passCursor(Cursor c, String type);
    }
}
