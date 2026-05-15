package com.example.diverscan.activeid.Conexion;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;

public class NetworkConnection extends AsyncTask<Void, Void, Boolean>
{
    public interface EntoncesHacer
    {
        void cuandoHayInternet();
        void cuandoNOHayInternet();
    }

    // variables necesarias, el dialogo de progreso, el contexto de la actividad
    // y una instancia de EntoncesHacer, donde se ejecutan las acciones.
    private ProgressDialog dialog;
    private Context context;
    private EntoncesHacer accion;

    // Constructor, recibe el contexto de la actividad actual,
    // y la instancia de EntoncesHacer
    public NetworkConnection(Context context, EntoncesHacer accion)
    {
        this.context = context;
        this.accion = accion;
    }

    // corre en el Thread de la UI antes de empezar la tarea en segundo plano.
    // aquí aprovechamos y mostramos el progress...
    @Override
    protected void onPreExecute() {
        // preparamos el cuadro de dialogo
        dialog = new ProgressDialog(context);
        dialog.setMessage("Verificando conexión con el servidor");
        dialog.setCancelable(false);
        dialog.show();

        // llamamos al padre
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Void... arg0) {
        try {
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress("35.172.159.36", 80), 5000);
            socket.close();
            return true;
        } catch (java.io.IOException e) {
            android.util.Log.e("NetworkConnection", "Error connecting to 35.172.159.36:80", e);
            e.printStackTrace();
            return false;
        }
    }

    // aquí de acuerdo al resultado llamaremos a uno u otro método
    // de la interface EntoncesHacer
    @Override
    protected void onPostExecute(Boolean resultado) {
        // llamamos al padre
        super.onPostExecute(resultado);

        // cerramos el cuadro de progreso
        dialog.dismiss();

        // de acuerdo al resultado del ping, se ejecuta una acción o la otra
        if (resultado) {
            accion.cuandoHayInternet();
        } else {
            accion.cuandoNOHayInternet();
        }
    }
}