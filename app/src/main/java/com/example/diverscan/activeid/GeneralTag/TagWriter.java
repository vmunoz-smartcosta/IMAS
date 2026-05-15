package com.example.diverscan.activeid.GeneralTag;

import android.content.Context;
 import android.content.pm.PackageInfo;
 import android.content.pm.PackageManager;
 import android.content.pm.ServiceInfo;
 import android.os.AsyncTask;
 import android.util.Log;

import com.example.diverscan.activeid.ConfiguracionesGeneral.SharedPreferencesGetSet;
import com.zebra.rfid.api3.ACCESS_OPERATION_CODE;
import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.DYNAMIC_POWER_OPTIMIZATION;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.MEMORY_BANK;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TagAccess;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class TagWriter implements Readers.RFIDReaderEventHandler{
    final static String TAG = "RFID_SAMPLE";
    Context context;
    private static Readers readers;
    private static ArrayList<ReaderDevice> availableRFIDReaderList;
    private static ReaderDevice readerDevice;
    private static RFIDReader reader;
    private EventHandler eventHandler;
    //*************************************************************
    // handheld que usan en Fidelitas.
    String readername = "MC3300x";
    //String readername = "RFD8500123";
    //String readername = "MC3300R";
    //*************************************************************
    private int MAX_POWER = 0;
    private String Power;
    private static final String _PASSWORD = "00";
    // Anti-crash: flag para serializar el procesamiento de batches de tags.
    // Evita que múltiples AsyncDataUpdate corran en paralelo cuando el SDK
    // dispara eventReadNotify decenas de veces por segundo con muchas etiquetas.
    private final AtomicBoolean isProcessingBatch = new AtomicBoolean(false);

    ResponseHandlerInterface responseHandlerInterface;

    //*******************************************************************************************
    private static TagWriter instance = null;

    private TagWriter() {
        // Private constructor to enforce Singleton pattern
    }

    public static synchronized TagWriter getInstance() {
        if (instance == null) {
            instance = new TagWriter();
        }
        return instance;
    }

    private boolean initialized = false;

    public boolean isInitialized() {
        return initialized;
    }
    public void setResponseHandler(ResponseHandlerInterface handler) {
        this.responseHandlerInterface = handler;
    }
    public void onCreate(ResponseHandlerInterface activity)
    {
        // FIX LIFECYCLE: actualizar responseHandlerInterface SIEMPRE, no solo en la primera
        // inicialización. Sin esto, al navegar entre Activities el singleton seguía
        // enviando eventos RFID (handleTagdata, handleTriggerPress, SetMessage) a la
        // Activity anterior — ya pausada o destruida — causando la apariencia de
        // "desconexión" al cambiar de pantalla.
        responseHandlerInterface = activity;
        context = activity.GetContext().getApplicationContext();
        
        if (!initialized) {
            try {
                Power = SharedPreferencesGetSet.leer_local("potenciaAntena", context);
                if (Power != null && !Power.isEmpty()) {
                    MAX_POWER = Integer.parseInt(Power);
                } else {
                    MAX_POWER = 270; // Valor por defecto si falla la lectura
                }
            } catch (Exception e) {
                MAX_POWER = 270;
            }
            InitSDK();
            initialized = true;
        } else {
            // Si ya está inicializado, intentar reconectar o verificar conexión
            if (readers != null) {
                 new ConnectionTask().execute();
            }
        }
    }

    public ArrayList<String> getAvailableReaderNames() {
        ArrayList<String> names = new ArrayList<>();
        try {
            if (readers != null) {
                availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                if (availableRFIDReaderList != null) {
                    for (ReaderDevice device : availableRFIDReaderList) {
                        names.add(device.getName());
                        Log.d("RFID_SDK", "[VALIDACION] Lector RFID detectado por el SDK: " + device.getName());
                    }
                    Log.d("RFID_SDK", "[VALIDACION] getAvailableReaderNames: " + names.size() + " lectores RFID encontrados.");
                } else {
                    Log.w("RFID_SDK", "[VALIDACION] getAvailableReaderNames: La lista de lectores RFID es nula.");
                }
            } else {
                Log.e("RFID_SDK", "[VALIDACION] getAvailableReaderNames: El objeto Readers no ha sido inicializado.");
            }
        } catch (InvalidUsageException e) {
            Log.e("RFID_SDK", "[VALIDACION] Error al obtener lista de lectores RFID: " + e.getMessage());
            e.printStackTrace();
        }
        return names;
    }

    public void setReaderName(String name) {
        Log.d("RFID_SDK", "Cambiando readerName a: " + name);
        this.readername = name;
        // Si el lector actual es diferente, desconectar para forzar nueva conexión al buscar
        if (reader != null && !reader.getHostName().equals(name)) {
            Log.d("RFID_SDK", "Desconectando lector anterior (" + reader.getHostName() + ") para cambiar a " + name);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    disconnect();
                    reader = null;
                    return null;
                }
            }.execute();
        }
    }

    //*******************************************************************************************

    public String Test1() {
        return "Antenna power Set to 220";
    }

    //*******************************************************************************************

    public String Test2() {
        return "Session set to S2";
    }

    //*******************************************************************************************

    // cambios realizados por andrey sanchez Zuñiga
    public String Defaults()
    {
        String value ="";
        // check reader connection
        if (!isReaderConnected())
        {
            value="No ha conectado";
            return value;
        }

        try
        {
            // Power to 270
            Antennas.AntennaRfConfig config = null;
            config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(MAX_POWER);
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);
            // singulation to S0
            Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
            s1_singulationControl.setSession(SESSION.SESSION_S0);
            s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
            s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
            reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
        }
        catch (InvalidUsageException e)
        {
            e.printStackTrace();
        }
        catch (OperationFailureException e)
        {
            e.printStackTrace();
            value ="No se ha conectado al lector o no hay lectores disponibles.";
            return  value ;
        }
        return value;
    }

    //*******************************************************************************************

    public boolean setTriggerMode(String Val)
    { //Se recomienda pasar a boolean para controlar la conexión correctamente
        try
        {
            if(Val.equals("RFID"))
            {
                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.BARCODE_MODE, false);
                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE,true);
                if(connect() != "conectado"){
                    return false;
                }
            }
            else if(Val.equals("BARCODE"))
            {

                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, false);
                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.BARCODE_MODE, true);
                if(disconnect() != "desconectado"){
                    return false;
                }
            }
        }
        catch(InvalidUsageException e)
        {
            e.printStackTrace();
            return false;
        }
        catch (OperationFailureException e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    //*******************************************************************************************

    public void EncenderRFID()
    {
        try{
            reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.BARCODE_MODE, false);
            reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE,true); //comentado
            connect();  //comentado
        }catch(InvalidUsageException e){
            e.printStackTrace();
        }
        catch (OperationFailureException e)
        {
            e.printStackTrace();
        }
    }

    //*******************************************************************************************

    public void ApagarRFID()
    {
        try
        {
            reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, false);   //comentado
            reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.BARCODE_MODE, true);
        }
        catch(InvalidUsageException e)
        {
            e.printStackTrace();
        }
        catch (OperationFailureException e)
        {
            e.printStackTrace();
        }
    }
    private boolean WriteTag(String sourceEPC, String Password, MEMORY_BANK memory_bank, String EPCToWrite, int offset) {
        Log.d(TAG, "WriteTag " + EPCToWrite);
        try {
            TagAccess tagAccess = new TagAccess();
            TagAccess.WriteAccessParams writeAccessParams = tagAccess.new WriteAccessParams();
            writeAccessParams.setAccessPassword(Long.parseLong(Password,16));
            writeAccessParams.setMemoryBank(memory_bank);
            writeAccessParams.setOffset(offset); // start writing from word offset 0
            writeAccessParams.setWriteData(EPCToWrite);
            // set retries in case of partial write happens
            writeAccessParams.setWriteRetries(5);
            // data length in words
            writeAccessParams.setWriteDataLength(EPCToWrite.length() / 4);
            // 5th parameter bPrefilter flag is true which means API will apply pre filter internally
            // 6th parameter should be true in case of changing EPC ID it self i.e. source and target both is EPC
            boolean useTIDfilter = memory_bank == MEMORY_BANK.MEMORY_BANK_EPC;
            reader.Actions.TagAccess.writeWait(sourceEPC, writeAccessParams, null, new TagData(), true, useTIDfilter);
        } catch (OperationFailureException | InvalidUsageException e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage() + " TESTING " + e.getStackTrace());
            return false;
        }
        return true;
    }

    //*******************************************************************************************

    public boolean WriteTag(String SourceEPC, String EPCToWrite)
    {
        try
        {
            setAccessOperationConfiguration();
            return WriteTag(SourceEPC, _PASSWORD, MEMORY_BANK.MEMORY_BANK_EPC, EPCToWrite, 2);
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    //*******************************************************************************************

    public void setAntennaPower(int power) {
        Log.d(TAG, "setAntennaPower " + power);
        // Guardia crítica: evita NullPointerException si el lector no está conectado.
        // El usuario puede mover el slider de potencia antes de que el SDK inicialice el lector.
        if (reader == null || !reader.isConnected()) {
            Log.w("RFID_SDK", "setAntennaPower ignorado: reader es null o no está conectado. Potencia solicitada: " + power);
            return;
        }
        try {
            // set antenna configurations
            Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(power);
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);
        } catch (InvalidUsageException e) {
            Log.e("RFID_SDK", "setAntennaPower: InvalidUsageException - " + e.getMessage());
            e.printStackTrace();
        } catch (OperationFailureException e) {
            Log.e("RFID_SDK", "setAntennaPower: OperationFailureException - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtiene la tabla de niveles de potencia soportados por el lector.
     * @return Arreglo de enteros con los valores de potencia (ej: 270 para 27.0 dBm)
     */
    public int[] getSupportedPowerLevels() {
        if (reader != null && reader.isConnected()) {
            try {
                return reader.ReaderCapabilities.getTransmitPowerLevelValues();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //*******************************************************************************************

    public void setDPO(boolean bEnable) {
        Log.d(TAG, "setDPO " + bEnable);
        if (reader == null || !reader.isConnected()) {
            Log.w("RFID_SDK", "setDPO ignorado: reader es null o no está conectado.");
            return;
        }
        try {
            // control the DPO
            reader.Config.setDPOState(bEnable ? DYNAMIC_POWER_OPTIMIZATION.ENABLE : DYNAMIC_POWER_OPTIMIZATION.DISABLE);
        } catch (InvalidUsageException e) {
            Log.e("RFID_SDK", "setDPO: InvalidUsageException - " + e.getMessage());
            e.printStackTrace();
        } catch (OperationFailureException e) {
            Log.e("RFID_SDK", "setDPO: OperationFailureException - " + e.getMessage());
            e.printStackTrace();
        }
    }

    //*******************************************************************************************

    public void setAccessOperationConfiguration() {
        if (reader == null || !reader.isConnected()) {
            Log.w("RFID_SDK", "setAccessOperationConfiguration ignorado: reader es null o no está conectado.");
            return;
        }
        // set required power and profile
        setAntennaPower(MAX_POWER);
        // in case of RFD8500 disable DPO
        if (reader.getHostName().contains("RFD8500"))
            setDPO(false);
        //
        try {
            // set access operation time out value to 1 second, so reader will tries for a second
            // to perform operation before timing out
            reader.Config.setAccessOperationWaitTimeout(7000);
        } catch (InvalidUsageException e) {
            Log.e("RFID_SDK", "setAccessOperationConfiguration: InvalidUsageException - " + e.getMessage());
            e.printStackTrace();
        } catch (OperationFailureException e) {
            Log.e("RFID_SDK", "setAccessOperationConfiguration: OperationFailureException - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isReaderConnected()
    {
        if(reader != null && reader.isConnected())
            return true;
        else
        {
            Log.d(TAG, "No se ha conectado al lector");
            return false;
        }
    }

    //*******************************************************************************************

    public String onResume() {
        if (reader != null && reader.isConnected()) {
            return "Conectado";
        }
        return connect();
    }

    public String onResume(ResponseHandlerInterface handler) {
        this.responseHandlerInterface = handler;
        return onResume();
    }

    //Adm*******************************************************************************************

    public void onPause() {
        // No desconectar al pausar la actividad para mantener persistencia entre vistas
        // disconnect(); 
    }

    public void closeConnection() {
        disconnect();
    }

    //*******************************************************************************************

    public void onDestroy() {
        //dispose();
    }

    //*******************************************************************************************

    private synchronized String connect() {
        if (reader != null) {
            Log.d(TAG, "conectar " + reader.getHostName());
            try {
                if (!reader.isConnected()) {
                    // Establish connection to the RFID Reader
                    reader.connect();
                    ConfigureReader();
                    responseHandlerInterface.SetMessage("Conectado");
                    return "Conectado";
                }
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException e) {
                e.printStackTrace();

                Log.d(TAG, "OperationFailureException " + e.getVendorMessage());
                String des = e.getResults().toString();
                return "Connection failed" + e.getVendorMessage() + " " + des;
            }
        }
        return "";
    }

    //*******************************************************************************************

    private synchronized String disconnect(){
        Log.d(TAG, "Desconectado" + reader);
        try{
            if (reader != null){
                reader.Events.removeEventsListener(eventHandler);
                reader.disconnect();
                //responseHandlerInterface.SetMessage("Desconectado " + reader);
            }
        }catch (InvalidUsageException ex){
            ex.printStackTrace();
            return ex.getMessage();
        }catch (OperationFailureException ex){
            ex.printStackTrace();
            return ex.getMessage();
        }catch (Exception ex){
            ex.printStackTrace();
            return ex.getMessage();
        }
        return "desconectado";
    }

    //*******************************************************************************************

//    private synchronized void dispose(){
//        try{
//            if (readers != null){
//                reader = null;
//                readers.Dispose();
//                readers = null;
//            }
//        }catch (Exception ex){
//            ex.printStackTrace();
//        }
//    }

    //*******************************************************************************************

    public void InitSDK()
    {
        Log.d(TAG, "InitSDK");
        checkZebraServiceAvailability();   // << DIAGNÓSTICO: verifica SDK antes de conectar
        if(readers == null){
            new CreateInstanceTask().execute();
        }else
            new ConnectionTask().execute();
    }

    /**
     * Diagnóstico completo del Zebra RFID Mobile SDK.
     * Verifica si el paquete del servicio está instalado, activo y accesible.
     * Todos los resultados se emiten con el tag RFID_SDK para filtrar fácilmente
     * en Logcat: adb logcat -s RFID_SDK
     */
    private void checkZebraServiceAvailability() {
        final String DIAG = "RFID_SDK";
        PackageManager pm = context.getPackageManager();

        // Paquetes obligatorios del flujo RFID.
        String[] zebraRfidPackages = {
            "com.zebra.rfid.rfidmanager",
            "com.zebra.rfid.service",
            "com.symbol.rfid.service"
        };

        // Paquetes Zebra opcionales para barcode/scanner. No son requeridos por esta vista.
        String[] zebraScannerPackages = {
            "com.zebra.scannercontrol",
            "com.symbol.datawedge"
        };

        Log.d(DIAG, "========== DIAGNÓSTICO ZEBRA RFID SDK ==========");
        Log.d(DIAG, "[VALIDACION] Esta validacion corresponde al flujo RFID. DataWedge y scannercontrol son opcionales en esta vista.");

        // 1. Verificar los paquetes requeridos del flujo RFID.
        Log.d(DIAG, "[VALIDACION] Verificando paquetes requeridos para RFID...");
        for (String pkg : zebraRfidPackages) {
            try {
                PackageInfo info = pm.getPackageInfo(pkg, PackageManager.GET_SERVICES);
                Log.d(DIAG, "[OK] Paquete ENCONTRADO: " + pkg
                        + " | versionName=" + info.versionName
                        + " | versionCode=" + info.getLongVersionCode());

                // Si tiene servicios, listarlos
                if (info.services != null && info.services.length > 0) {
                    for (ServiceInfo svc : info.services) {
                        Log.d(DIAG, "     Servicio: " + svc.name
                                + " | enabled=" + svc.isEnabled()
                                + " | exported=" + svc.exported);
                    }
                } else {
                    Log.w(DIAG, "     Sin servicios declarados en " + pkg);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(DIAG, "[FALTA] Paquete NO instalado: " + pkg);
            }
        }

        // 1.1 Verificar paquetes opcionales de scanner solo como referencia.
        Log.d(DIAG, "[VALIDACION] Verificando paquetes opcionales de scanner/barcode (no requeridos para RFID)...");
        for (String pkg : zebraScannerPackages) {
            try {
                PackageInfo info = pm.getPackageInfo(pkg, PackageManager.GET_SERVICES);
                Log.d(DIAG, "[OPCIONAL] Paquete de scanner encontrado: " + pkg
                        + " | versionName=" + info.versionName
                        + " | versionCode=" + info.getLongVersionCode());
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(DIAG, "[OPCIONAL] Paquete de scanner no instalado: " + pkg);
            }
        }

        // 2. Buscar TODOS los paquetes instalados que contengan 'zebra' o 'rfid' en su nombre
        Log.d(DIAG, "--- Escaneando paquetes Zebra/RFID instalados para soporte del flujo RFID ---");
        try {
            java.util.List<PackageInfo> allPackages = pm.getInstalledPackages(0);
            boolean found = false;
            for (PackageInfo pi : allPackages) {
                String name = pi.packageName.toLowerCase();
                if (name.contains("zebra") || name.contains("rfid") || name.contains("symbol")) {
                    Log.d(DIAG, "[SCAN] " + pi.packageName + " v" + pi.versionName);
                    found = true;
                }
            }
            if (!found) {
                Log.e(DIAG, "[SCAN] No se encontró NINGÚN paquete Zebra/RFID instalado en el dispositivo.");
                Log.e(DIAG, "       → El Zebra RFID Mobile SDK NO está instalado.");
                Log.e(DIAG, "       → Instala el APK del SDK desde: https://techdocs.zebra.com");
            }
        } catch (Exception e) {
            Log.e(DIAG, "Error al escanear paquetes instalados: " + e.getMessage());
        }

        // 3. Verificar el servicio específico que falla
        try {
            android.content.pm.ServiceInfo svcInfo = pm.getServiceInfo(
                new android.content.ComponentName(
                    "com.zebra.rfid.rfidmanager",
                    "com.zebra.rfid.rfidmanager.RFIDService"),
                0);
            Log.d(DIAG, "[OK] RFIDService accesible directamente:");
            Log.d(DIAG, "     name=" + svcInfo.name);
            Log.d(DIAG, "     enabled=" + svcInfo.isEnabled());
            Log.d(DIAG, "     exported=" + svcInfo.exported);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(DIAG, "[FALLA] RFIDService NO encontrado: com.zebra.rfid.rfidmanager/.RFIDService");
            Log.e(DIAG, "        Causa: " + e.getMessage());
            Log.e(DIAG, "        → Esto explica el error 'avc: denied { find }' de SELinux.");
        }

        Log.d(DIAG, "========== FIN DIAGNÓSTICO ==========");
    }

    //*******************************************************************************************

    private class CreateInstanceTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... voids){
            Log.d(TAG, "[VALIDACION] CreateInstanceTask: Iniciando configuracion de Readers para RFID.");
            InvalidUsageException invalidUsageException = null;
            try{
                Log.d(TAG, "[VALIDACION] Intentando inicializar RFID con ENUM_TRANSPORT.SERVICE_SERIAL");
                readers = new Readers(context, ENUM_TRANSPORT.SERVICE_SERIAL);
                availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                Log.d(TAG, "[VALIDACION] SERVICE_SERIAL: Lectores RFID encontrados: " + (availableRFIDReaderList != null ? availableRFIDReaderList.size() : 0));
            }catch (InvalidUsageException e){
                Log.e(TAG, "[VALIDACION] Error en SERVICE_SERIAL: " + e.getMessage());
                e.printStackTrace();
                invalidUsageException = e;
            }
            
            if (invalidUsageException != null){
                Log.w(TAG, "[VALIDACION] SERVICE_SERIAL fallo, reintentando con ENUM_TRANSPORT.BLUETOOTH");
                if (readers != null) {
                    readers.Dispose();
                    readers = null;
                }
                try {
                    readers = new Readers(context, ENUM_TRANSPORT.BLUETOOTH);
                    availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                    Log.d(TAG, "[VALIDACION] BLUETOOTH: Lectores RFID encontrados: " + (availableRFIDReaderList != null ? availableRFIDReaderList.size() : 0));
                } catch (Exception e) {
                    Log.e(TAG, "[VALIDACION] Error critico al inicializar Readers en ambos modos: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid){
            super.onPostExecute(aVoid);
            new ConnectionTask().execute();
        }
    }

    //*******************************************************************************************

    private class ConnectionTask extends AsyncTask<Void, Void, String>{
        @Override
        protected String doInBackground(Void... voids){
            Log.d(TAG, "[VALIDACION] ConnectionTask: iniciando validacion de lector RFID.");
            GetAvailableReader();
            if(reader != null){
                return connect();
            }
            Log.e(TAG, "[VALIDACION] ConnectionTask: no se encontro lector RFID para conectar.");
            return "No se pudo encontrar o conectar el lector";
        }

        @Override
        protected void onPostExecute(String result){super.onPostExecute(result);
        }
    }

    //*******************************************************************************************

    private synchronized void GetAvailableReader() {
        Log.d(TAG, "[VALIDACION] GetAvailableReader: buscando lectores RFID disponibles.");
        try {
            if (readers != null)
            {
                readers.attach( this);
                if (readers.GetAvailableRFIDReaderList() != null) {
                    availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                    if (availableRFIDReaderList.size() != 0) {
                        // if single reader is available then connect it
                        // get first reader from list
                        //readerDevice = availableRFIDReaderList.get(0);
                        //reader = readerDevice.getRFIDReader();
                        if (availableRFIDReaderList.size() == 1) {
                            readerDevice = availableRFIDReaderList.get(0);
                            reader = readerDevice.getRFIDReader();
                            Log.d(TAG, "[VALIDACION] GetAvailableReader: lector RFID unico seleccionado -> " + readerDevice.getName());
                        } else {
                            // search reader specified by name
                            for (ReaderDevice device : availableRFIDReaderList) {
                                Log.d(TAG, "[VALIDACION] GetAvailableReader: evaluando lector RFID -> " + device.getName());
                                if (device.getName().equals(readername))
                                {
                                    readerDevice = device;
                                    reader = readerDevice.getRFIDReader();
                                    Log.d(TAG, "[VALIDACION] GetAvailableReader: lector RFID seleccionado por nombre -> " + readername);
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "[VALIDACION] GetAvailableReader: el SDK RFID devolvio una lista vacia de lectores.");
                    }
                } else {
                    Log.w(TAG, "[VALIDACION] GetAvailableReader: GetAvailableRFIDReaderList devolvio null.");
                }
            } else {
                Log.e(TAG, "[VALIDACION] GetAvailableReader: Readers es null, no se puede validar RFID.");
            }
        }
        catch (InvalidUsageException e)
        {
            Log.e(TAG, "[VALIDACION] GetAvailableReader: InvalidUsageException -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    //*******************************************************************************************

    @Override
    public void RFIDReaderAppeared(ReaderDevice readerDevice) {
        Log.d(TAG, "RFIDReaderAppeared " + readerDevice.getName());
        new ConnectionTask().execute();
    }

    //*******************************************************************************************

    @Override
    public void RFIDReaderDisappeared(ReaderDevice readerDevice) {
        Log.d(TAG, "RFIDReaderDisappeared " + readerDevice.getName());
        if (readerDevice.getName().equals(reader.getHostName()))
            disconnect();
    }

    //*******************************************************************************************

    private void ConfigureReader() {
        Log.d(TAG,"ConfigureReader" + reader.getHostName());
        if (reader.isConnected()) {

            TriggerInfo triggerInfo = new TriggerInfo();
            triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
            triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);
            try{
                if (eventHandler == null)
                    eventHandler = new EventHandler();
                reader.Events.addEventsListener(eventHandler);
                reader.Events.setHandheldEvent(true);
                reader.Events.setTagReadEvent(true);
                reader.Events.setAttachTagDataWithReadEvent(false);
                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
                reader.Config.setStartTrigger(triggerInfo.StartTrigger);
                reader.Config.setStopTrigger(triggerInfo.StopTrigger);
                // Fix 2: proteger contra Power nulo/vacío/inválido.
                // Sin este guard, un NumberFormatException silencioso deja MAX_POWER en 0
                // y la antena no emite sin ningún error visible.
                try {
                    if (Power != null && !Power.trim().isEmpty()) {
                        MAX_POWER = Integer.parseInt(Power.trim());
                    } else {
                        // Intentar releer desde SharedPrefs antes de usar el fallback
                        String freshPower = SharedPreferencesGetSet.leer_local("potenciaAntena", context);
                        MAX_POWER = (freshPower != null && !freshPower.trim().isEmpty())
                                ? Integer.parseInt(freshPower.trim())
                                : 270;
                        Power = String.valueOf(MAX_POWER);
                        Log.w(TAG, "ConfigureReader: Power vacío, usando fallback MAX_POWER=" + MAX_POWER);
                    }
                } catch (NumberFormatException nfe) {
                    MAX_POWER = 270;
                    Log.e(TAG, "ConfigureReader: Power no es un entero válido ('" + Power + "'), usando 270");
                }
                Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
                config.setTransmitPowerIndex(MAX_POWER);
                config.setrfModeTableIndex(0);
                config.setTari(0);
                reader.Config.Antennas.setAntennaRfConfig(1, config);
                // Set the singulation control
                Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
                s1_singulationControl.setSession(SESSION.SESSION_S0);
                s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
                s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
                reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
                // delete any prefilters
                reader.Actions.PreFilters.deleteAll();

            } catch (InvalidUsageException | OperationFailureException e) {
                e.printStackTrace();
            }
        }
    }

    //*******************************************************************************************

    public synchronized void performInventory(){
        if(!isReaderConnected())
            return;
        try{
            reader.Actions.Inventory.perform();
        }catch (InvalidUsageException ex){
            ex.printStackTrace();
        }catch (OperationFailureException ex){
            ex.printStackTrace();
        }
    }

    //*******************************************************************************************

    public synchronized void stopInventory(){
        if (!isReaderConnected())
            return;
        try{
            reader.Actions.Inventory.stop();
        }catch (InvalidUsageException ex){
            ex.printStackTrace();
        }catch (OperationFailureException ex){
            ex.printStackTrace();
        }
    }

    //*******************************************************************************************

    public class EventHandler implements RfidEventsListener {
        // Read Event Notification
        public void eventReadNotify(RfidReadEvents e) {
            // FIX ANTI-CRASH: incrementar lote a 1000 tags para reducir la frecuencia
            // de disparos del evento con campos RFID densos.
            TagData[] myTags = reader.Actions.getReadTags(1000);
            if (myTags != null) {
                for (int index = 0; index < myTags.length; index++) {
                    Log.d(TAG, "Tag ID " + myTags[index].getTagID());
                    if (myTags[index].getOpCode() == ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ &&
                            myTags[index].getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS) {
                        if (myTags[index].getMemoryBankData().length() > 0) {
                            Log.d(TAG, " Mem Bank Data " + myTags[index].getMemoryBankData());
                        }
                    }
                    if (myTags[index].isContainsLocationInfo()) {
                        short dist = myTags[index].LocationInfo.getRelativeDistance();
                        Log.d(TAG, "Tag relative distance " + dist);
                    }
                }
                // FIX ANTI-CRASH: usar SERIAL_EXECUTOR en lugar de THREAD_POOL_EXECUTOR.
                // Con THREAD_POOL_EXECUTOR, cientos de tags generan cientos de AsyncTasks
                // simultáneos → RejectedExecutionException / OOM.
                // SERIAL_EXECUTOR garantiza procesamiento secuencial sin condiciones de
                // carrera sobre los HashMap de ChequearInventario.
                // El guard AtomicBoolean descarta eventos redundantes si el procesador
                // del batch anterior aún no terminó (throttle de seguridad).
                if (!isProcessingBatch.getAndSet(true)) {
                    new AsyncDataUpdate().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, myTags);
                } else {
                    Log.d(TAG, "eventReadNotify: batch anterior aún en proceso, descartando evento (" + myTags.length + " tags)");
                }
            }
        }
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
            Log.d(TAG, "Status Notification 1: " + rfidStatusEvents.StatusEventData.getStatusEventType());
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            responseHandlerInterface.handleTriggerPress(true);
                            return null;
                        }
                    }.execute();
                }
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            responseHandlerInterface.handleTriggerPress(false);
                            return null;
                        }
                    }.execute();
                }
            }
        }
    }

    //*******************************************************************************************

    private class AsyncDataUpdate extends AsyncTask<TagData[], Void, Void> {
        @Override
        protected Void doInBackground(TagData[]... params) {
            try {
                responseHandlerInterface.handleTagdata(params[0]);
            } finally {
                // FIX ANTI-CRASH: liberar el flag SIEMPRE, incluso si handleTagdata lanza excepción.
                // Sin este finally, un crash interno dejaría el flag en true y bloquearía
                // todas las lecturas posteriores de la sesión.
                isProcessingBatch.set(false);
            }
            return null;
        }
    }

    //*******************************************************************************************

    public void LocateTag(String tagId){
        try {
            if(isReaderConnected()){
                reader.Actions.TagLocationing.Perform(tagId, null, null);
            }else{
                responseHandlerInterface.SetMessage("No hay lectores disponibles.");
            }
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }

    //*******************************************************************************************

    public void StopLocateTag(){
        try {
            reader.Actions.TagLocationing.Stop();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }

    //*******************************************************************************************
}
