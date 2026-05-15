package com.example.diverscan.activeid.Conexion;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;

public class NetworkUsages {
    private static final String DEBUG_TAG = "NetworkStatusExample";
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean isNetDisponible(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public String NameNetWork(Context context){
        WifiManager _wifi = (WifiManager) context.getSystemService (Context.WIFI_SERVICE);
        String _nombre= "";
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiInfo wifiInfo = _wifi.getConnectionInfo();
        _nombre = wifiInfo.getSSID();
        return _nombre;
    }

    public boolean isOnline(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}
