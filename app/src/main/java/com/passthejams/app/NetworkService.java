package com.passthejams.app;

import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public String name = "Name Me";
    public static final String undefined = "This operation is undefined";
    public static final int BUFFER_SIZE = 1024;

    private int port = 0;
    String mServiceName = null;
    NsdManager.RegistrationListener mRegistrationListener = null;
    NsdManager mNsdManager = null;
    NsdManager.DiscoveryListener mDiscoveryListener = null;
    public static final String SERVICE_TYPE = "_http._tcp.";

    NsdServiceInfo mService = null;

    HashMap<String,NsdServiceInfo> networkDevices = new HashMap<>();

    public NetworkService() {
    }

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

    @Override
    public void close() throws IOException {
        quit = true;
        serverSocket.close();
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
            try {
                service.serverSocket = new ServerSocket(service.port);
            } catch (IOException e) {
                e.printStackTrace();
                service.serverSocket = null;
                return;
            }
            service.port = service.serverSocket.getLocalPort();
            Log.d(TAG, "Started ServerSocket on "+serverSocket.getInetAddress()+":" + port);

            NsdServiceInfo serviceInfo = new NsdServiceInfo();

            serviceInfo.setServiceName("PassTheJams");
            serviceInfo.setServiceType(SERVICE_TYPE);
            serviceInfo.setPort(service.port);

            service.initializeRegistrationListener();
            service.initializeDiscoveryListener();

            service.mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
            service.mNsdManager.registerService(
                    serviceInfo, NsdManager.PROTOCOL_DNS_SD, service.mRegistrationListener);
            service.mNsdManager.discoverServices(SERVICE_TYPE,
                    NsdManager.PROTOCOL_DNS_SD, service.mDiscoveryListener);
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

    public ArrayList<Device> getDevices() {
        ArrayList<Device> devices = new ArrayList<>();
        for(String deviceName:networkDevices.keySet()) {
            NsdServiceInfo info = networkDevices.get(deviceName);
            Device d = new Device();
            d.host = info.getHost();
            d.port = info.getPort();
            d.name = deviceName;
            devices.add(d);
        }
        return devices;
    }

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
                    callback.stringCallback(NetworkService.this,device,songs);
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
                    out.println(service.undefined);
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
        Cursor cursor = context.getContentResolver().query(uri,Shared.PROJECTION_SONG,selection,null,null);
        JsonArray array = new JsonArray();
        while(cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

            JsonObject obj = new JsonObject();
            obj.addProperty("title",cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
            obj.addProperty("artist",cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
            obj.addProperty("album",cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
            obj.addProperty("_id",cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
            obj.addProperty("album_id",cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
            Uri uri2 = ContentUris.withAppendedId(Shared.libraryUri, id);
            File f = new File(uri2.getPath());
            obj.addProperty("file_name",f.getName());
            array.add(obj);
        }
        cursor.close();
        return new Gson().toJson(array);
    }

    private void sendSong(int id, OutputStream out) {
        Uri uri = ContentUris.withAppendedId(Shared.libraryUri, id);
        File f = new File(uri.getPath());
        sendFile(f, out);
    }

    private  String getCurrentSong() {
        return "";
    }

    private void suggestSong(JsonObject json) {
        Context context = getApplicationContext();
        String song = json.getAsJsonPrimitive("title").getAsString();
        Toast toast = Toast.makeText(context,song,Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Recieves the song file over the input stream, saving to a temporary file.
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
     * @param json
     * @param in
     */
    private void recievePermanent(JsonObject json, InputStream in) {
        Context context = getApplicationContext();
        //TODO: make a setting to say where to store new songs
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        //only newer versions support looking at sd cards
        if (Build.VERSION.SDK_INT >= 19) {
            if (!Environment.isExternalStorageRemovable()) {
                //this device defaults to internal storage
                //for now, get the first true external storage
                for (File f : context.getExternalFilesDirs(Environment.DIRECTORY_MUSIC)) {
                    if(Build.VERSION.SDK_INT >= 21) {
                        if (Environment.isExternalStorageRemovable(f)) {
                            dir = f;
                            break;
                        }
                    } else {
                        if(!f.equals(dir)) {
                            dir = f;
                            break;
                        }
                    }
                }
            }
        }
        String fileName = json.getAsJsonPrimitive("file_name").getAsString();
        File songFile = new File(dir,fileName);
        recieveFile(songFile,in);
        MediaScannerConnection.scanFile(context, new String[]{songFile.getAbsolutePath()}, null, null);
    }

    private void sendFile(File f, OutputStream out) {
        byte buffer[] = new byte[BUFFER_SIZE];
        try(FileInputStream in = new FileInputStream(f)) {
            int read = 0;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void recieveFile(File f, InputStream in) {
        byte buffer[] = new byte[BUFFER_SIZE];
        int read;
        try(FileOutputStream fout = new FileOutputStream(f)) {
            while ((read = in.read(buffer)) > 0) {
                fout.write(buffer, 0, read);
            }
            fout.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //nsd stuff
    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                mServiceName = nsdServiceInfo.getServiceName();
            }
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                //error
                //display error to screen
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {

            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {

            }
        };
    }

    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started.");
                networkDevices = new HashMap<>();
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                if(service.getServiceType().equals(SERVICE_TYPE)) {
                    if(service.getServiceName().contains("PassTheJams") &&
                            (!service.getServiceName().equals(mServiceName)||true)) {
                        Log.v(TAG, "Service discovery success " + service);
                        mNsdManager.resolveService(service, newResolveListener());
                        //networkDevices.put(service.getServiceName(), service);
                    }
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                if(service.getServiceType().equals(SERVICE_TYPE)) {
                    if(service.getServiceName().contains("PassTheJams") &&
                            !service.getServiceName().equals(mServiceName)) {
                        Log.e(TAG, "Service lost: " + service);
                        networkDevices.remove(service.getServiceName());
                    }
                }
            }

            @Override
            public void onDiscoveryStopped(String service) {
                networkDevices = new HashMap<>();
            }
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                try {
                    //mNsdManager.stopServiceDiscovery(this);
                }catch(IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                //mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public NsdManager.ResolveListener newResolveListener() {
        return new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                networkDevices.put(serviceInfo.getServiceName(),serviceInfo);
                Log.v(TAG,"Connected to " + serviceInfo);
            }
        };
    }

    public static class Device implements Serializable {
        private static final long serialVersionUID = 0L;
        public InetAddress  host;
        public int          port;
        public String       name;
    }

}
