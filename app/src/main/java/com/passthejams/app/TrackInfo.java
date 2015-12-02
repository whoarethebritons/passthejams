package com.passthejams.app;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Created by Eden on 11/6/2015.
 */
public class TrackInfo{
    public String title;
    public String artist;
    public String album;
    public int _id;
    public int album_id;
    public String file_name;

    public TrackInfo() {
        title = null;
        artist = null;
        album = null;
        _id = -1;
        album_id = -1;
        file_name = null;
    }
    public TrackInfo(int id, int album_id, String name, String artist) {
        this._id = id;
        this.album_id = album_id;
        this.title = name;
        this.artist = artist;
    }

    public TrackInfo(JsonObject json) {
        title = json.get("title").getAsString();
        artist = json.get("artist").getAsString();
        album = json.get("album").getAsString();
        _id = json.get("_id").getAsInt();
        album_id = json.get("album_id").getAsInt();
        file_name = json.get("file_name").getAsString();
    }

    public TrackInfo(Uri uri, ContentResolver contentResolver) {
        loadFromUri(uri, contentResolver);
    }

    public void loadFromUri(Uri uri, ContentResolver contentResolver) {
        String columns[] = new String[]{
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.AudioColumns.ARTIST,
                MediaStore.Audio.AudioColumns.ALBUM,
                MediaStore.Audio.AudioColumns._ID,
                MediaStore.Audio.AudioColumns.ALBUM_ID,
                MediaStore.Audio.AudioColumns.DISPLAY_NAME
        };
        Cursor cursor =  contentResolver.query(uri, columns, null, null, null);
        if(cursor!=null) {
            if(cursor.moveToFirst()) {
                title = cursor.getString(0);
                artist = cursor.getString(1);
                album = cursor.getString(2);
                _id = cursor.getInt(3);
                album_id = cursor.getInt(4);
                file_name = cursor.getString(5);
            }
            cursor.close();
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("title",title);
        json.addProperty("artist",artist);
        json.addProperty("album",album);
        json.addProperty("_id",_id);
        json.addProperty("album_id",album_id);
        json.addProperty("file_name",file_name);
        return json;
    }

    public String toJsonString() {
        return new Gson().toJson(toJson());
    }

    public String getMimeType() {
        if(file_name == null) return "audio/*";
        int index = file_name.lastIndexOf(".")+1;
        String extension = file_name.substring(index).toLowerCase();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getMimeTypeFromExtension(extension);
    }

    @Override
    public boolean equals(Object o) {
        TrackInfo t2 = (TrackInfo) o;
        return t2._id == this._id && t2.title.equals(this.title);
    }
}