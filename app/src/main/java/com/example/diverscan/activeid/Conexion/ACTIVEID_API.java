package com.example.diverscan.activeid.Conexion;

import android.content.Context;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import cz.msebera.android.httpclient.entity.StringEntity;

public class ACTIVEID_API {
    private static final String BASE_URL = "http://18.234.199.225/WCF-IMAS/Service1.svc"; // --local WCF (emulador Android)
    // private static final String BASE_URL = "http://192.168.137.179:8080/Service1.svc"; // --local WCF (IP maquina local para dispositivos fisicos)
    // private static final String BASE_URL = "http://148.113.164.10/WCF_ACTIVE_ANDROID/Service1.svc"; // --prod
    private final AsyncHttpClient AsyncClient;

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public ACTIVEID_API() {
        this.AsyncClient = new AsyncHttpClient();
        this.AsyncClient.setTimeout(9000000);
        // En emulador Android, IIS Express suele validar Host como localhost:puerto.
        if (BASE_URL.contains("10.0.2.2")) {
            this.AsyncClient.addHeader("Host", "localhost:8091");
        }
    }

    public void get(String url, AsyncHttpResponseHandler responseHandler) {
        this.AsyncClient.get(getAbsoluteUrl(url), responseHandler);
    }

    public void post(Context context, String url, StringEntity entity, AsyncHttpResponseHandler responseHandler) {
        this.AsyncClient.post(context, getAbsoluteUrl(url), entity, "application/json; charset=utf-8", responseHandler);
    }

    public void cancel(Context context) {
        this.AsyncClient.cancelRequests(context, true);
    }

    private String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL.trim() + relativeUrl;
    }
}
