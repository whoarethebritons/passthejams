package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;


public class JamsServer implements Runnable {
    private static ServerSocket serverSocky;
    private static int nextSongId = 1;
    private static int nextAlbumId = 1;
    private static HashMap<JsonObject,Integer> idLookup = new HashMap<>();
    private static HashMap<Integer,Song> songLookup = new HashMap<>();
    private static HashMap<String,Integer> albumIdLookup = new HashMap<>();
    private Socket socky;
    public static final int BUFFER_SIZE = 1024;

    private static JsonArray songs = null;

    public static void main(String args[]) throws IOException {
        serverSocky = new ServerSocket(8080);
        //System.out.println(new JamsServer(null).getSongs());
        loadSongs();
        while(true) {
            Socket s = serverSocky.accept();
            JamsServer server = new JamsServer(s);
            (new Thread(server)).run();
        }

    }

    private static int getSongId(String title, String artist, String album) {
        JsonObject song = new JsonObject();
        if(title == null) return 0;
        if(artist == null) artist = "";
        if(album == null) album = "";
        song.addProperty("title",title);
        song.addProperty("artist",artist);
        song.addProperty("album", album);
        return getSongId(song);

    }

    private static int getSongId(JsonObject key) {
        //ensure that there aren't any extra properties
        JsonObject tempKey = new JsonObject();
        tempKey.add("title", key.get("title"));
        tempKey.add("artist", key.get("artist"));
        tempKey.add("album", key.get("album"));
        int id = 0;
        if(idLookup.containsKey(tempKey)) {
            id = idLookup.get(tempKey);
        } else {
            id = nextSongId++;
            idLookup.put(tempKey,id);
        }
        return id;
    }

    private static int getAlbumId(String album) {
        //TODO: will fail at some point, multiple albums can have the same name
        if(albumIdLookup.containsKey(album)) {
            return  albumIdLookup.get(album);
        } else {
            int id = nextAlbumId++;
            albumIdLookup.put(album,id);
            return id;
        }
    }

    private static void loadSongs() {
        songs = loadSongs(new File("."));
    }

    private static JsonArray loadSongs(File dir) {
        JsonArray songs = new JsonArray();
        for(File f:dir.listFiles()) {
            if(f.isDirectory()) {
                songs.addAll(loadSongs(f));
            } else {
                String name = f.getName();
                if(name.matches("(.*mp3$)|(.*m4p$)|(.*m4a$)|(.*ogg$)")) {
                    Song song = new Song(f);
                    songLookup.put(song._id,song);
                    songs.add(song.toJson());
                }
            }
        }
        return songs;
    }

    public JamsServer(Socket s) {
        socky = s;
    }

    @Override
    public void run() {
        try(
        BufferedReader in = new BufferedReader(new InputStreamReader(socky.getInputStream()));
        PrintWriter out = new PrintWriter(socky.getOutputStream());
        ) {
            String line = in.readLine();
            if(line==null) {
                System.out.println("Socket input null");
                return;
            }
            line = line.trim();
            System.out.println("Command: "+line);
            if(line.equals("getSongs")) {
                out.print(getSongs());
                out.flush();
            } else if(line.contains("getSong")) {
                int songId = Integer.parseInt(line.substring(8));
                Song song = songLookup.get(songId);
                copyFile(song.file);
            } else if(line.equals("getAlbums")) {
                out.print("Unimplemented");
            } else if(line.equals("getPlaylists")) {
                out.print("Unimplemented");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(socky != null) {
                    socky.close();
                    socky = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Done");
    }

    private String getSongs() {
        return new Gson().toJson(songs);
    }

    private void copyFile(File f) throws IOException {
        System.out.println("Serving file: " + f.getName());
        byte buffer[] = new byte[BUFFER_SIZE];
        try(FileInputStream in = new FileInputStream(f)) {
            OutputStream out = socky.getOutputStream();
            int read = 0;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }
        System.out.println("Served file: " + f.getName());
    }

    private static class Song {
        public String title = null;
        public String artist = null;
        public String album = null;
        public int _id = 0;
        public int album_id = 0;
        public File file = null;

        public Song() {

        }

        public Song(String title, String artist, String album, File file) {
            this.title = title;
            this.artist = artist;
            this.album = album;
            this._id = getSongId(title, artist,album);
            this.album_id = getAlbumId(album);
            this.file = file;
        }

        public Song(File file) {
            AudioFile ff = null;
            try {
                ff = AudioFileIO.read(file);
                Tag tag = ff.getTag();
                title = tag.getFirst(FieldKey.TITLE);
                artist = tag.getFirst(FieldKey.ARTIST);
                album = tag.getFirst(FieldKey.ALBUM);
                _id = getSongId(title, artist, album);
                album_id = getAlbumId(album);
                this.file = file;
            } catch (CannotReadException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TagException e) {
                e.printStackTrace();
            } catch (ReadOnlyFileException e) {
                e.printStackTrace();
            } catch (InvalidAudioFrameException e) {
                e.printStackTrace();
            }
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("title",title);
            obj.addProperty("artist",artist);
            obj.addProperty("album",album);
            obj.addProperty("_id",_id);
            obj.addProperty("album_id",album_id);
            obj.addProperty("file_name",file.getName());
            return obj;
        }

        public String toString() {
            return new Gson().toJson(toJson());
        }
    }
}
