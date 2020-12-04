package com.github.kaiwinter.myatmo.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NetworkUtil {

    /**
     * Checks if the netatmo API can be reached.
     *
     * @return true if the netatmo API cannot be reached
     */
    public static boolean isOffline() {
        try (Socket sock = new Socket()) {
            sock.connect(new InetSocketAddress("api.netatmo.net", 443), 1500);
            return false;
        } catch (IOException e) {
            return true;
        }
    }
}
