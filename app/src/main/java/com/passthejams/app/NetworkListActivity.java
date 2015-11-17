package com.passthejams.app;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.app.Activity;
import android.os.IBinder;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class NetworkListActivity extends Activity {
    private NetworkService service = null;

    private ServiceConnection conn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_list);

        ListView listView = (ListView) findViewById(R.id.listView2);

        ArrayList<String> devices = new ArrayList<>();
        devices.add("Device one");
        devices.add("Device devicy");

        ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,devices);
        listView.setAdapter(adapter);
    }

}
