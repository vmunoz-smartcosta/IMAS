package com.example.diverscan.activeid.Barcodes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;
import java.util.Set;

public class LectorCodigoBarras  {

    private static final String TAG = "LectorCodigoBarras";
    Intent i = new Intent();

    String scannerInputPlugin = "com.symbol.datawedge.api.ACTION";
    String extraData = "com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN";

    public void onResume(Context context){
        Log.d(TAG, "onResume: Solicitando desactivar plugin de DataWedge");
        i.setAction(scannerInputPlugin);
        i.putExtra(extraData, "DISABLE:PLUGIN");
        context.sendBroadcast(i);
    }

    public void onReceive(Context context, Intent intent){
          Log.d(TAG, "onReceive: Recibido broadcast de DataWedge");
          String command = intent.getStringExtra("COMMAND");
          String commandidentifier = intent.getStringExtra("COMMAND_IDENTIFIER");
          String result = intent.getStringExtra("RESULT");

          Bundle bundle = new Bundle();
          String resultInfo = "";
          if(intent.hasExtra("RESULT_INFO")){
              bundle = intent.getBundleExtra("RESULT_INFO");
              Set<String> keys = bundle.keySet();
              for(String key : keys){
                  resultInfo += key + ": "+ bundle.getString(key) + "\n";
              }
          }

          String text = "Command: "+command+"\n" +
                  "Result: " +result+"\n" +
                  "Result Info: " +resultInfo + "\n" +
                  "CID:"+commandidentifier;

          Log.i(TAG, "DataWedge Result: " + text);
          Toast.makeText(context, text, Toast.LENGTH_LONG).show();
      }
}
