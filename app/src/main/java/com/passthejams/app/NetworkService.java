package com.passthejams.app;

import android.app.Service;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Service to handle all networking requests.
 *
 * Requests
 * getName          - returns the name of this device
 * getSongs         - returns a json array of all songs
 * getSong/#        - returns the song file
 * getCurrentSong   - returns json with the current song
 * suggestSong/#    - pass in json for song. User is notified.
 */
public class NetworkService extends Service implements Closeable{
    private static final String TAG = "NetworkService";

    private boolean quit = false;
    private ServerSocket serverSocket = null;

    //TODO: get settings storage
    public String name = "Test Phone";
    public static final String undefined = "This operation is undefined";
    public static final int BUFFER_SIZE = 1024;

    private int port = 0;
    private NsdHelper mNsdHelper = null;

    //nsd doesn't work if the socket is on an IPv6 address
    //This doesn't seem to help though.
    static {
        System.setProperty("java.net.preferIPv4Stack" , "true");
    }

    public NetworkService() {
    }

    //Methods for binding with service
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class NetworkBinder extends Binder {
        NetworkService getService() {
            return NetworkService.this;
        }
    }
    NetworkBinder mBinder = new NetworkBinder();

    /**
     * Called when this service is started.
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //set up server
        quit = false;
        MainLoop loop = new MainLoop(this);
        new Thread(loop).start();

        //set up nsd
        //moved to network thread
        return START_STICKY;
    }

    /**
     * Cleans up when this service is stopped.
     */
    @Override
    public void onDestroy() {
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used to stop this service. Closes the socket which will cause the network thread to stop.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        quit = true;
        serverSocket.close();
        serverSocket = null;
        if(mNsdHelper != null) {
            mNsdHelper.close();
        }
    }

    /**
     * This class is just a thread that waits for new connections. It should be blocked the majority
     * of the time.
     */
    private class MainLoop implements Runnable {
        NetworkService service = null;

        MainLoop(NetworkService service) {
            this.service = service;
        }

        @Override
        public void run() {
            //initialize socket and nsd
            //Log.v(TAG,getSongs());
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                e.printStackTrace();
                serverSocket = null;
                return;
            }
            port = serverSocket.getLocalPort();
            Log.d(TAG, "Started ServerSocket on "+serverSocket.getInetAddress()+":" + port);
            if(serverSocket.getInetAddress() instanceof Inet6Address) {
                Log.v(TAG,"sever binded to ipv6 address");
            }
            mNsdHelper = new NsdHelper("Passthejams",port,serverSocket.getInetAddress(),NetworkService.this);
            mNsdHelper.registerService();
            mNsdHelper.startDiscovery();

            //main network loop
            while(!service.quit) {
                try {
                    Socket socky = service.serverSocket.accept();
                    ConnectionHelper helper = new ConnectionHelper(service,socky);
                    new Thread(helper).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Returns a list of devices found on the network with nsd.
     * @return
     */
    public ArrayList<Device> getDevices() {
        ArrayList<Device> devices = new ArrayList<>();

        for(String deviceName:mNsdHelper.getNetworkDevices().keySet()) {
            devices.add(mNsdHelper.getNetworkDevices().get(deviceName));
        }
        return devices;
    }

    //==============================================================================================
    //methods for talking to other devices
    //==============================================================================================

    /**
     * Talks to the other device and gets its name.
     * @param device
     * @param callback
     */
    public void getDeviceName(final Device device, final GetDeviceNameCallback callback) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Socket s = null;
                BufferedReader in = null;
                PrintWriter out = null;
                try {
                    s = new Socket(device.host,device.port);
                    in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    out = new PrintWriter(s.getOutputStream());
                    out.println("getName");
                    out.flush();
                    String name = in.readLine();
                    if(callback != null) callback.onSuccess(device,name);
                } catch (IOException e) {
                    if(callback != null) callback.onError(device,e);
                } finally {
                    if(out != null)try{out.close();}catch (Exception e){}
                    out = null;
                    if(in != null)try{in.close();}catch (Exception e){}
                    in = null;
                    if(s != null)try{s.close();}catch (Exception e){}
                    s = null;
                }
            }
        };
        new Thread(r).start();
    }
    public interface GetDeviceNameCallback {
        void onSuccess(Device device, String name);
        void onError(Device device, Exception e);
    }

    /**
     * Talks to the other device, retries a json string representing all of its songs.
     * @param device
     * @param callback
     */
    public void getSongs(final Device device, final Callback callback) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Socket s = null;
                BufferedReader in = null;
                PrintWriter out = null;
                try {
                    s = new Socket(device.host,device.port);
                    in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    out = new PrintWriter(s.getOutputStream());
                    out.println("getSongs");
                    out.flush();
                    String songs = in.readLine();
                    callback.stringCallback(NetworkService.this, device, songs);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if(out != null)try{out.close();}catch (Exception e){}
                    out = null;
                    if(in != null)try{in.close();}catch (Exception e){}
                    in = null;
                    if(s != null)try{s.close();}catch (Exception e){}
                    s = null;
                }
            }
        };
        new Thread(r).start();
    }
    public interface Callback {
        void stringCallback(NetworkService service, Device device, String response);
    }

    /**
     * Talks to the other device, and retrieves a song from it. TrackInfo song must have a song id
     * from the other device. The song is copied and added to the mediastore.
     * @param song
     * @param device
     * @param callback
     */
    public void getSong(final TrackInfo song, final Device device, final GetSongCallback callback) {
        Runnable r = new Runnable() {
            @Override
            public void run() {

                Socket sock = null;
                PrintWriter writer = null;
                try {
                    sock = new Socket(device.host,device.port);
                    writer = new PrintWriter(sock.getOutputStream());
                    writer.println("getSong/"+song._id);
                    writer.flush();

                    TrackInfo song2 = recievePermanent(song, sock.getInputStream());
                    if(callback != null) {
                        callback.onSuccess(NetworkService.this,device,song2);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if(callback != null) {
                        callback.onError(NetworkService.this,device,song,e);
                    }
                } finally {
                    if(writer != null) try{writer.close();} catch (Exception e) {};
                    writer = null;
                    if(sock != null) try{sock.close();} catch (IOException e) {};
                    sock = null;
                }
            }
        };
        new Thread(r).start();
    }

    public interface GetSongCallback {
        void onSuccess(NetworkService service, Device device, TrackInfo song);
        void onError(NetworkService service, Device device, TrackInfo song, Exception error);
    }

    //==============================================================================================
    //Dealing with incoming requests
    //==============================================================================================

    /**
     * This runnable is called to handle all incoming connections.
     */
    private class ConnectionHelper implements Runnable {
        NetworkService service = null;
        Socket socket = null;

        ConnectionHelper(NetworkService service, Socket socket) {
            this.service = service;
            this.socket = socket;
        }

        boolean compare(String input, String command) {
            return input.substring(0,command.length()).equals(command);
        }

        @Override
        public void run() {
            BufferedReader in = null;
            PrintWriter out = null;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line = in.readLine();
                if(compare(line,"getName")) {
                    out = new PrintWriter(socket.getOutputStream());
                    out.println(service.name);
                    out.flush();
                } else if(compare(line,"getSongs")) {
                    out = new PrintWriter(socket.getOutputStream());
                    out.println(service.getSongs());
                    out.flush();
                } else if(compare(line,"getSong/")) {
                    String argument = line.substring("getSong/".length());
                    int id = Integer.parseInt(argument);
                    service.sendSong(id,socket.getOutputStream());
                } else if(compare(line,"getCurrentSong")) {
                    out = new PrintWriter(socket.getOutputStream());
                    out.println(service.getCurrentSong());
                    out.flush();
                } else if(compare(line,"suggestSong/")) {
                    String argument = line.substring("suggestSong/".length());
                    JsonObject json = new JsonParser().parse(argument).getAsJsonObject();
                    service.suggestSong(json);
                } else {
                    out = new PrintWriter(socket.getOutputStream());
                    out.println(undefined);
                    out.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try{if(in!=null)in.close();in=null;}catch(Exception e){}
                try{if(out!=null)out.close();out=null;}catch(Exception e){}
                try{if(socket!=null)socket.close();socket=null;}catch(Exception e){}
            }
        }
    }

    /**
     * Returns a json array of all songs on this device.
     * @return json array of all songs
     */
    private String getSongs() {
        Context context = getApplicationContext();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 ";
        Cursor cursor = context.getContentResolver().query(uri, Shared.PROJECTION_SONG, selection, null, null);
        JsonArray array = new JsonArray();
        while(cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

            JsonObject obj = new JsonObject();
            obj.addProperty("title", cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
            obj.addProperty("artist", cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
            obj.addProperty("album",cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
            obj.addProperty("_id", cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
            obj.addProperty("album_id", cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
            Uri uri2 = ContentUris.withAppendedId(Shared.libraryUri, id);
            File f = new File(uri2.getPath());
            String fileName = f.getName();
            Cursor cursor1 =  getContentResolver().query(uri2, new String[]{MediaStore.Audio.AudioColumns.DISPLAY_NAME}, null, null, null);
            if(cursor1 != null && cursor1.moveToFirst()) {
                fileName = cursor1.getString(0);
            }
            if(cursor1!=null)cursor1.close();
            obj.addProperty("file_name", fileName);
            array.add(obj);
        }
        cursor.close();
        return new Gson().toJson(array);
    }

    /**
     * Copies the song with given id to out.
     * @param id
     * @param out
     */
    private void sendSong(int id, OutputStream out) {
        Uri uri = ContentUris.withAppendedId(Shared.libraryUri, id);
        Log.v(TAG, "Sending uri " + uri);
        Cursor cursor = getContentResolver().query(uri,new String[]{MediaStore.Audio.AudioColumns.DATA},null,null,null);
        File f = null;
        if(cursor != null) {
            if(cursor.moveToFirst()) {
                f = new File(cursor.getString(0));
            }
            cursor.close();;
        }
        if(f==null) {
            f = new File(uri.getPath());
        }
        sendFile(f, out);
    }

    /**
     * Unimplemented. Should return json of TrackInfo of current song.
     * @return
     */
    private  String getCurrentSong() {
        return "";
    }

    /**
     * Retrieves json of a suggested song and notifies the user. Not fully implemented.
     * @param json
     */
    private void suggestSong(JsonObject json) {
        Context context = getApplicationContext();
        String song = json.getAsJsonPrimitive("title").getAsString();
        Toast toast = Toast.makeText(context,song,Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Recieves the song file over the input stream, saving to a temporary file.
     * Not currently used or tested.
     * @param json
     * @param in
     */
    private void recieveTemporary(JsonObject json, InputStream in) {
        Context context = getApplicationContext();
        File dir = context.getCacheDir();
        File songDir = new File(dir,"songs");
        songDir.mkdir();
        String fileName = json.getAsJsonPrimitive("file_name").getAsString();
        File songFile = new File(songDir,fileName);
        recieveFile(songFile, in);
    }

    /**
     * Recieves the song file over the input stream, permanetly adding to the media store.
     * @param song
     * @param in
     */
    private TrackInfo recievePermanent(TrackInfo song, InputStream in) {
        Context context = getApplicationContext();
        //TODO: make a setting to say where to store new songs
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        //only newer versions support looking at sd cards
//        if (Build.VERSION.SDK_INT >= 19) {
//            if (!Environment.isExternalStorageRemovable()) {
//                //this device defaults to internal storage
//                //for now, get the first true external storage
//                for (File f : context.getExternalFilesDirs(Environment.DIRECTORY_MUSIC)) {
//                    if(Build.VERSION.SDK_INT >= 21) {
//                        if (Environment.isExternalStorageRemovable(f)) {
//                            dir = f;
//                            break;
//                        }
//                    } else {
//                        if(!f.equals(dir)) {
//                            dir = f;
//                            break;
//                        }
//                    }
//                }
//            }
//        }
        File songFile = new File(dir,song.file_name);
        recieveFile(songFile, in);


        Log.v(TAG, "Adding file to mediastore " + songFile.getAbsolutePath() + " mime " + song.getMimeType());

        //Calling ScanMedia didn't add the file as a song
        //Instead, we must add it manually
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, songFile.getAbsolutePath());
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, songFile.getName());
        values.put(MediaStore.MediaColumns.TITLE, song.title);
        values.put(MediaStore.MediaColumns.MIME_TYPE, song.getMimeType());
        values.put(MediaStore.Audio.Media.ARTIST, song.artist);
        values.put(MediaStore.Audio.Media.ALBUM, song.album);
        values.put(MediaStore.Audio.Media.IS_MUSIC, true);

        Uri uri = MediaStore.Audio.Media.getContentUriForPath(songFile.getAbsolutePath());
        Log.v(TAG,uri.toString());
        Uri uri2 = this.getContentResolver().insert(uri, values);
        Log.v(TAG,"Uri of new song: "+uri2);
        TrackInfo song2 = new TrackInfo(uri2,getContentResolver());

        return song2;
    }

    /**
     * Copies the given file to an output stream.
     * @param f
     * @param out
     */
    private void sendFile(File f, OutputStream out) {
        byte buffer[] = new byte[BUFFER_SIZE];
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            int read = 0;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(in != null) try{in.close();}catch (IOException e){};
            in = null;
        }
    }

    //Recieves a given file from the input stream.
    private void recieveFile(File f, InputStream in) {
        Log.v(TAG,"Recieving file "+f.getAbsolutePath());
        byte buffer[] = new byte[BUFFER_SIZE];
        int read;
        long total = 0;
        FileOutputStream fout = null;
        try{
            fout = new FileOutputStream(f);
            while ((read = in.read(buffer)) > 0) {
                fout.write(buffer, 0, read);
                total += read;
            }
            fout.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(fout != null) try{fout.close();}catch (IOException e){};
            fout = null;
        }
        if(!f.exists()) {
            Log.e(TAG,"Written file "+f.getAbsolutePath()+" doesn't exist");
        }
        Log.v(TAG,"Wrote "+total+" bytes");
    }
}
