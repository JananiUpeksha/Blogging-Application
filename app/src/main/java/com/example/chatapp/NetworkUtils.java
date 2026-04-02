package com.example.chatapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

/**
 * Utility class to check network / online status.
 */
public class NetworkUtils {

    private NetworkUtils() {}

    /** Returns true if the device currently has an active internet connection. */
    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null &&
                    (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                  || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                  || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            // Legacy path (API < 23)
            android.net.NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }
}
