package com.passthejams.app;

import java.net.InetAddress;

/**
 * Created by david on 12/2/15.
 */
public class Device {
    public InetAddress host;
    public int          port;
    public String       name;
    public String       serviceName;

    @Override
    public String toString() {
        return name+" "+host+":"+port+" "+serviceName;
    }
}
