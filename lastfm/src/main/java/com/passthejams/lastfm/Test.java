package com.passthejams.lastfm;

import de.umass.lastfm.Artist;
import de.umass.lastfm.Chart;
import de.umass.lastfm.User;

import java.text.DateFormat;
import java.util.Collection;

/**
 * Created by eden on 9/11/15.
 */
public class Test {
    public static void main(String[] args) {
        String key="1d8a009cf65d2b94309cd1d52731b6d4";
        String user = "wongton9";
        Chart<Artist> chart = User.getWeeklyArtistChart(user, 10, key);
        DateFormat format = DateFormat.getDateInstance();
        String from = format.format(chart.getFrom());
        String to = format.format(chart.getTo());
        System.out.printf("Charts for %s for the week from %s to %s:%n", user, from, to);
        Collection<Artist> artists = chart.getEntries();
        for (Artist artist : artists) {
            System.out.println(artist.getName());
        }
    }
}
