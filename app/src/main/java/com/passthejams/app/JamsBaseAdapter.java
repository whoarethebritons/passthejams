package com.passthejams.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by eden on 11/20/15.
 */
public class JamsBaseAdapter extends ArrayAdapter {
    Object[] queue;

    private class TrackHolder {
        TextView song;
        TextView artist;
    }

    public JamsBaseAdapter(Context context, int resource, int textViewResourceId, Object[] objects) {
        super(context, resource, textViewResourceId, objects);
        queue = objects;
    }

    @Override
    public int getCount() {
        return queue.length;
    }

    @Override
    public Object getItem(int position) {
        return queue[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.queue_row, null);
        }

        TrackHolder trackHolder = new TrackHolder();

        trackHolder.song = (TextView) convertView.findViewById(R.id.songView);
        TrackInfo temp = (TrackInfo) getItem(position);
        trackHolder.song.setText(temp.title);

        trackHolder.artist = (TextView) convertView.findViewById(R.id.artistView);
        trackHolder.artist.setText(temp.artist);

        convertView.setTag(trackHolder);

        return convertView;
    }
}
