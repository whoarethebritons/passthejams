package com.passthejams.app;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Created by david on 12/2/15.
 */
public class NsdHelper implements Closeable {
    public static final String TAG = "NsdHelper";
    public static final String SERVICE_TYPE = "_http._tcp.";

    private String mServiceName = null;
    private String mRegisteredServiceName = null;
    private NetworkService mNetworkService = null;
    private int mPort;
    private InetAddress mAddress;
    private HashMap<String,Device> mNetworkDevices;

    NsdManager mNsdManager = null;
    NsdManager.RegistrationListener mRegistrationListener = null;
    NsdManager.DiscoveryListener mDiscoveryListener = null;
    NsdManager.ResolveListener mResolveListener = null;

    NsdServiceInfo mService = null;

    /**
     * Creates a new object. Registering and discovery must be manually called.
     * @param serviceName
     * @param port
     * @param address
     * @param networkService
     */
    public NsdHelper(String serviceName, int port, InetAddress address, NetworkService networkService) {
        mServiceName = serviceName;
        mPort = port;
        initializeDiscoveryListener();
        initializeRegistrationListener();
        mNsdManager = (NsdManager) networkService.getSystemService(Context.NSD_SERVICE);
        mNetworkService = networkService;
        mAddress = address;
    }

    /**
     * Registers this service with nsd. The actuall work is done by another thread, so returning
     * from this function does not mean success.
     */
    public void registerService() {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();

        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(mPort);

        Log.v(TAG, "Registering up service " + serviceInfo);
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    /**
     * Unregisters this service with nsd. The actuall work is done by another thread, so returning
     * from this function does not mean success.
     */
    public void unregisterService() {
        mNsdManager.unregisterService(mRegistrationListener);
    }


    /**
     * Starts looking for services with the same type. The actuall work is done by another thread, so returning
     * from this function does not mean success.
     */
    public void startDiscovery() {
        mNsdManager.discoverServices(SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }


    /**
     * Stops looking for services. The actuall work is done by another thread, so returning
     * from this function does not mean success.
     */
    public void stopDiscovery() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    /**
     * Creates a new callback object for rigistering with nsd.
     */
    private void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                mService = nsdServiceInfo;
                mServiceName = nsdServiceInfo.getServiceName();
                Log.v(TAG,"Registered nsd service "+nsdServiceInfo);
            }
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                //error
                //display error to screen
                Log.e(TAG,"Failed to register nsd "+errorCode+" "+serviceInfo);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {

            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG,"Failed to unregister nsd "+errorCode+" "+serviceInfo);
            }
        };
    }

    /**
     * Creates a new callback object for listening from nsd.
     */
    private void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started.");
                mNetworkDevices = new HashMap<>();
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                if(service.getServiceType().equals(SERVICE_TYPE)) {
                    if(service.getServiceName().contains("PassTheJams") &&
                            (!service.getServiceName().equals(mServiceName)||true)) {
                        Log.v(TAG, "Service discovery success " + service);
                        mNsdManager.resolveService(service, newResolveListener());
                        //mNetworkDevices.put(service.getServiceName(), service);
                    }
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                if(service.getServiceType().equals(SERVICE_TYPE)) {
                    if(service.getServiceName().contains("PassTheJams") &&
                            !service.getServiceName().equals(mServiceName)) {
                        Log.e(TAG, "Service lost: " + service);
                        mNetworkDevices.remove(service.getServiceName());
                    }
                }
            }

            @Override
            public void onDiscoveryStopped(String service) {
                mNetworkDevices = new HashMap<>();
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

    /**
     * Creates a new callback object for resolving a service with nsd.
     */
    private NsdManager.ResolveListener newResolveListener() {
        return new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Device d = new Device();
                d.host = serviceInfo.getHost();
                d.port = serviceInfo.getPort();
                d.serviceName = serviceInfo.getServiceName();
                mNetworkService.getDeviceName(d, new NetworkService.GetDeviceNameCallback() {
                    @Override
                    public void onSuccess(Device device, String name) {
                        device.name = name;
                        mNetworkDevices.put(device.serviceName, device);
                        Log.v(TAG, "Connected to " + device);
                    }

                    @Override
                    public void onError(Device device, Exception e) {
                        Log.e(TAG, "Unable to get device name from "+device.host+":"+device.port, e);
                    }
                });
                //mNetworkDevices.put(serviceInfo.getServiceName(), d);
            }
        };
    }

    @Override
    public void close() throws IOException {
        if(mNsdManager != null) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            mNsdManager.unregisterService(mRegistrationListener);
            mNsdManager = null;
        }
    }

    /**
     * Returns a list of all devices that have been discovered and successfully talked to.
     */
    public HashMap<String,Device> getNetworkDevices() {
        return mNetworkDevices;
    }
}
