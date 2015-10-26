package com.passthejams.app;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import static com.passthejams.app.R.id.text_network_response;

public class NetworkTest extends Activity {
    int nExceptions = 0;
    public static final String TAG = "NetworkTest";
    NsdManager.RegistrationListener mRegistrationListener = null;
    String mServiceName = null;
    NsdManager mNsdManager = null;
    NsdManager.DiscoveryListener mDiscoveryListener = null;
    public static final String SERVICE_TYPE = "_http._tcp.";
    NsdManager.ResolveListener mResolveListener = null;

    NsdServiceInfo mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_test);

        Log.d(TAG, "Creating NetworkTest");
        //registerService(9000);
        initializeRegistrationListener();
        initializeDiscoveryListener();
        initializeResolveListener();
        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        mNsdManager.discoverServices(SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        //registerService(9000);
        Button button = (Button) findViewById(R.id.button_connect);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText edit_host = (EditText) findViewById(R.id.host);
                EditText edit_port = (EditText) findViewById(R.id.port);
                TextView response = (TextView) findViewById(R.id.text_network_response);
                response.setText(edit_host.getText() + "\n" + edit_port.getText());

                int port = 80;
                String host = edit_host.getText().toString();


                try {
                    port = Integer.parseInt(edit_port.getText().toString());
                } catch (NumberFormatException e) {
                }

                String line = "";
                try {
                    line = new getNetwork().execute(host, port).get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    line = e.getMessage();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    line = e.getMessage();
                }

                response.setText(line);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_network_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void registerService(int port) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();

        serviceInfo.setServiceName("PassTheJams");
        serviceInfo.setServiceType("_http._tcp.");
        serviceInfo.setPort(port);

        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

    }

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
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success " + service);
                if(!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG,"Unknown service type: " + service.getServiceType());
                } else if(service.getServiceName().equals(mServiceName)) {
                    Log.d(TAG,"Same machine as: " + mServiceName);
                } else if(service.getServiceName().contains("PassTheJams")) {
                    mNsdManager.resolveService(service,mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "Service lost: " + service);
            }

            @Override
            public void onDiscoveryStopped(String service) {

            }
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }


        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }
                mService = serviceInfo;
                int port = mService.getPort();
                InetAddress host = mService.getHost();
            }
        };
    }

    @Override
    protected void onPause() {
        if(mNsdManager != null) {
            mNsdManager.unregisterService(mRegistrationListener);
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG,"Resuming NetworkTest");
        super.onResume();
        if(mNsdManager != null) {
            registerService(9000);
            mNsdManager.discoverServices(SERVICE_TYPE,NsdManager.PROTOCOL_DNS_SD,mDiscoveryListener);
        }
    }

    @Override
    protected void onDestroy() {
        if(mNsdManager != null) {
            mNsdManager.unregisterService(mRegistrationListener);
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
        super.onDestroy();
    }

    private class getNetwork extends AsyncTask<Object,Void,String>
    {
        protected String doInBackground(Object... params) {
            String host = (String) params[0];
            int port = (Integer) params[1];
            String line = "";

            try {
                Socket sock = new Socket(host, port);
                PrintWriter out =
                        new PrintWriter(sock.getOutputStream(), true);
                BufferedReader in =
                        new BufferedReader(
                                new InputStreamReader(sock.getInputStream()));
                line = in.readLine();


                in.close();
                out.close();
                sock.close();

            } catch (UnknownHostException e) {
                line = ""+ ++nExceptions+e.getMessage();
                e.printStackTrace();
            } catch (IOException e) {
                line = ""+ ++nExceptions+e.getMessage();
                e.printStackTrace();
            }
            return line;
        }
    }
}
