package com.passthejams.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.app.Activity;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class NetworkListActivity extends Activity {
    private static final String TAG = "NetworkListActivity";
    private NetworkService mService = null;
    private ArrayList<Device> deviceList;
    private ArrayList<TrackInfo> songList = null;
    private Device device = null;

    private ServiceConnection conn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            NetworkService.NetworkBinder binder = (NetworkService.NetworkBinder) service;
            mService = binder.getService();
            Log.d(TAG,"Bound to service");
            updateList();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_list);

        updateList();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this,NetworkService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected  void onStop() {
        super.onStop();
        if(mService!=null) {
            unbindService(conn);
            mService = null;
        }
    }

    private void updateList() {
        ListView listView = (ListView) findViewById(R.id.listView2);

        ArrayList<String> devices = new ArrayList<>();
        deviceList = null;

        if(mService != null) {
            deviceList = mService.getDevices();
            for(Device d:deviceList) {
                String item = d.name+" ";
                item += d.host + ":" + d.port;
                item += " "+d.serviceName;
                devices.add(item);
                Log.d(TAG,item);
            }
        }

        //Yes, all the nested callbacks
        ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,devices);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Device d = deviceList.get(position);
                device = d;
                Log.d(TAG,"Selected " + d.name+" "+d.host+":"+d.port);
                ArrayList<String> list = new ArrayList<String>();
                list.add("Loading");
                ArrayAdapter adapter = new ArrayAdapter(NetworkListActivity.this,android.R.layout.simple_list_item_1,list);
                ListView listView = (ListView) findViewById(R.id.listView2);
                listView.setAdapter(adapter);

                mService.getSongs(d, new NetworkService.Callback() {
                    @Override
                    public void stringCallback(final NetworkService service, final Device device, String response) {
                        Log.d(TAG,"Response from "+device.host+":"+device.port+" = "+response);
                        JsonArray a = new JsonParser().parse(response).getAsJsonArray();
                        ArrayList<String> songs = new ArrayList<String>();
                        songList = new ArrayList<TrackInfo>();
                        for(JsonElement o:a) {
                            JsonObject obj = o.getAsJsonObject();
                            String line = obj.getAsJsonPrimitive("title").getAsString();
                            songs.add(line);
                            songList.add(new TrackInfo(obj));
                        }
                        ArrayAdapter adapter = new ArrayAdapter(NetworkListActivity.this,android.R.layout.simple_list_item_1,songs);
                        ListView listView = (ListView) findViewById(R.id.listView2);
                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                TrackInfo song = songList.get(i);
                                Log.v(TAG,"Copying " +song.title+", "+song.file_name+" to id "+song._id);
                                service.getSong(song, device, new NetworkService.GetSongCallback() {
                                    @Override
                                    public void onSuccess(NetworkService service, Device device, TrackInfo song) {
                                        Log.v(TAG,"Copied " +song.title+", "+song.file_name+" to id "+song._id);
                                    }

                                    @Override
                                    public void onError(NetworkService service, Device device, TrackInfo song, Exception error) {
                                        Log.v(TAG,"Failed to copy " +song.title+", "+song.file_name+" to id "+song._id,error);
                                    }
                                });
                            }
                        });
                        //listView.setAdapter(adapter);
                        UpdateList uiUpdate = new UpdateList();
                        uiUpdate.listView = listView;
                        uiUpdate.adapter = adapter;
                        runOnUiThread(uiUpdate);
                    }
                });
            }
        });
    }

    private class UpdateList implements Runnable {
        public ListView listView;
        public ArrayAdapter adapter;
        @Override
        public void run() {
            listView.setAdapter(adapter);
        }
    }
}
