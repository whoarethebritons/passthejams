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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class JamsServer implements Runnable {
    private static ServerSocket serverSocky;
    private Socket socky;

    public static void main(String args[]) throws IOException {
        serverSocky = new ServerSocket(8080);
        //System.out.println(new JamsServer(null).getSongs());
        while(true) {
            Socket s = serverSocky.accept();
            JamsServer server = new JamsServer(s);
            (new Thread(server)).run();
        }

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
            if(line.equals("getSongs")) {
                out.print(getSongs());
            } else if(line.contains("getSong")) {
                out.print("Unimplemented");
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
    }

    private String getSongs() {
        return new Gson().toJson(getSongs(new File(".")));
    }

    private JsonArray getSongs(File dir) {
        JsonArray songs = new JsonArray();
        for(File f:dir.listFiles()) {
            if(f.isDirectory()) {
                songs.addAll(getSongs(f));
            } else {
                String name = f.getName();
                if(name.matches("(.*mp3$)|(.*m4p$)|(.*m4a$)|(.*ogg$)")) {
                    JsonObject song = new JsonObject();
                    //song.addProperty("FileName",name);
                    try {
                        AudioFile ff = AudioFileIO.read(f);
                        Tag tag = ff.getTag();
                        song.addProperty("Title",tag.getFirst(FieldKey.TITLE));
                        song.addProperty("Album",tag.getFirst(FieldKey.ALBUM));
                        song.addProperty("Artist",tag.getFirst(FieldKey.ARTIST));
                    } catch (TagException e) {
                        e.printStackTrace();
                    } catch (ReadOnlyFileException e) {
                        e.printStackTrace();
                    } catch (CannotReadException e) {
                        e.printStackTrace();
                    } catch (InvalidAudioFrameException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println(song);
                    songs.add(song);
                }
            }
        }
        return songs;
    }
}
