package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by david on 11/4/15.
 */
public class TestClient {
    public static void main(String args[]) throws IOException {
        Socket s = new Socket("localhost",8080);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        PrintWriter out = new PrintWriter(s.getOutputStream());
        out.println("getSongs");
        out.flush();
        System.out.println("Songs: "+in.readLine());
    }
}
