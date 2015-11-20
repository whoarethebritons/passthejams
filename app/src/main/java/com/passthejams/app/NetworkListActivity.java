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
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class NetworkListActivity extends Activity {
    private static final String TAG = "NetworkListActivity";
    private NetworkService mService = null;

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

        if(mService != null) {
            for(String key:mService.networkDevices.keySet()) {
                String item = key+" ";
                NsdServiceInfo info = mService.networkDevices.get(key);
                item += info.getHost() + ":" + info.getPort();
                devices.add(item);
                Log.d(TAG,item);
            }
        }

        ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,devices);
        listView.setAdapter(adapter);
    }
}
