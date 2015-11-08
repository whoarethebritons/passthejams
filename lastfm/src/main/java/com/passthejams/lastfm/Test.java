package com.passthejams.lastfm;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import de.umass.lastfm.*;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;

import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

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
        HashMap<String, Object> hmap = new HashMap<String, Object>();

        hmap.put("Similar Tracks", Track.getSimilar("Led Zeppelin","Houses of the Holy", key) );
        hmap.put("_id", "trackid");

        CouchDbProperties properties = new CouchDbProperties()
                .setDbName("passthejams")
                .setCreateDbIfNotExist(true)
                .setProtocol("http")
                .setHost("127.0.0.1")
                .setPort(5984)
                .setMaxConnections(100)
                .setConnectionTimeout(0);
        CouchDbClient dbClient = new CouchDbClient(properties);
        Gson g = new Gson();
        try {
            HashMap<String, Object> update = dbClient.find(HashMap.class, "newfancyid");
            /*Collection<Tag> tr = Track.getTopTags("Led Zeppelin", "Houses of the Holy", key);
            String ret = g.toJson(tr, tr.getClass());
            update.put("LastFM Tags", tr);*/
            dbClient.update(update);
        }catch(org.lightcouch.NoDocumentException e) {
            hmap.put("_id", "newfancyid");
            /*Collection<Tag> tr = Track.getTopTags("Led Zeppelin", "Houses of the Holy", key);
            String ret = g.toJson(tr, tr.getClass());
            hmap.put("LastFM Tags", tr);*/
            dbClient.save(hmap);
        }

        JsonObject jsonObject = g.fromJson(dbClient.find(JsonObject.class, "newfancyid"), JsonObject.class);

        System.out.println(jsonObject);
        System.out.println(jsonObject.getAsJsonArray("Similar Tracks"));
        for(JsonElement j : jsonObject.getAsJsonArray("Similar Tracks")) {
            System.out.println(j);
            Track test = g.fromJson(j, Track.class);
            System.out.println(test);
        }
    }
}
