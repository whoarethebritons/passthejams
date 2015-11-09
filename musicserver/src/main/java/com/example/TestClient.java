package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by david on 11/4/15.
 */
public class TestClient {
    public static String HOST = "localhost";
    public static int PORT = 8080;
    public static void main(String args[]) throws IOException {
        Socket s = new Socket(HOST,PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        PrintWriter out = new PrintWriter(s.getOutputStream());
        out.println("getSongs");
        out.flush();
        //System.out.println("Songs: "+in.readLine());
        String line = in.readLine();
        in.close(); in = null;
        out.close(); out = null;
        s.close(); s = null;
        JsonArray a = new JsonParser().parse(line).getAsJsonArray();
        for(JsonElement o:a) {
            JsonObject obj = o.getAsJsonObject();
            getSong(obj.get("_id").getAsInt(),obj.get("file_name").getAsString(),HOST,PORT);
            System.out.println(obj.get("_id") + ": " + obj.get("title"));
            System.out.println(new Gson().toJson(obj));
        }
    }

    public static void getSong(int songId, String fileName, String host, int port) {
        System.out.println("Requesting file: " + fileName);
        File file = new File(fileName);
        try( Socket s = new Socket(host,port);
             PrintWriter out = new PrintWriter(s.getOutputStream());
             BufferedInputStream in = new BufferedInputStream(s.getInputStream());
             FileOutputStream fout = new FileOutputStream(file);) {
            out.println("getSong/"+songId);
            out.flush();
            byte buffer[] = new byte[JamsServer.BUFFER_SIZE];
            int read;
            while((read=in.read(buffer))>0) {
                //System.out.println("Read " + read + " bytes");
                fout.write(buffer,0,read);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Got: " + fileName);
    }

}
