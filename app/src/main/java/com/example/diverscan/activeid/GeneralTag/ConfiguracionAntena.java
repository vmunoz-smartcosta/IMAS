package com.example.diverscan.activeid.GeneralTag;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.diverscan.activeid.ConfiguracionesGeneral.SharedPreferencesGetSet;
import com.example.diverscan.activeid.R;
import com.example.diverscan.activeid.login.LoginActivity;
import com.zebra.rfid.api3.TagData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConfiguracionAntena extends AppCompatActivity implements ResponseHandlerInterface{
    private static final String UI_TAG = "RFID_UI";
    private static final SimpleDateFormat LOG_TIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private static final int MAX_REINTENTOS_LECTORES = 4;
    private static final long RETARDO_REINTENTO_LECTORES_MS = 1200L;
    private TextView txtPotencia;
    private TextView txtCnfActual;
    private SeekBar skPotencia;
    private ProgressBar pgPotencia;
    private TextView txtPorcentaje;
    private View mConfigurarAntena;
    private String Power;
    public String potenciaAntena;
    private int potenciaInicial;
    private Spinner spReaders;
    Context _context;
    Activity _activity;

    // ── Prueba de Lectura ──────────────────────────────
    private Button btnLecturaSencilla;
    private Button btnLecturaSostenida;
    private TextView txtResultadoLectura;
    private TextView txtContadorTags;
    private boolean lecturaSostenidaActiva = false;
    private int contadorTags = 0;
    private final Handler handlerLectura = new Handler();
    // ──────────────────────────────────────────────────

    private long startTime=1*60*15000;
    private final long interval = 1*1000;
    CountDownTimer sessionActivate;
    TagWriter rfidHandler;
    private int[] supportedPowerLevels;

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actvity_configurar_antena);
        logAperturaPantalla("onCreate");
        Log.d(UI_TAG, "[VIEW] Abriendo ConfiguracionAntena para validacion de SDK RFID y lectura RFID.");
        _context = this;
        _activity = this;
        controles();
        eventos();
        rfidHandler = TagWriter.getInstance();
        Log.d(UI_TAG, "[VIEW] Instancia TagWriter obtenida. Iniciando flujo de validacion RFID.");
        rfidHandler.onCreate(this);

        try{
            Power = SharedPreferencesGetSet.leer_local("potenciaAntena", this);
            txtCnfActual.setText("Potencia actual: " + Power);
            Log.d(UI_TAG, "[VIEW] Potencia RFID inicial leida desde preferencias: " + Power);
            
            // Inicialización básica (se refinará cuando el lector conecte)
            skPotencia.setMax(300);
            int potenciaInicial = 0;
            try { potenciaInicial = Integer.parseInt(Power); } catch (NumberFormatException ignored) {}
            skPotencia.setProgress(Math.max(potenciaInicial, 30));
        }catch(Exception e){
            e.printStackTrace();
        }

        try {
            txtPotencia.setText("Versión. " + _context.getPackageManager().getPackageInfo(
                    getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        sessionActivate = new CountDownTimer(startTime, interval){

            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {

                Intent intent = new Intent(ConfiguracionAntena.this, LoginActivity.class);
                startActivity(intent);
            }
        }.start();

        cargarLectoresRfid(1);
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(UI_TAG, "[VIEW] onPause en ConfiguracionAntena.");
        if (rfidHandler != null) {
            rfidHandler.onPause();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        logAperturaPantalla("onPostResume");
        // FIX ANR: rfidHandler.onResume() llama internamente a connect() — operación
        // bloqueante de Bluetooth. Ejecutarla en el UI thread causa ANR.
        // Se mueve a hilo background (igual que Lectura_Inventario y ElegirUbicacion_TomaFisica).
        if (rfidHandler != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(UI_TAG, "[VIEW] onPostResume: validando reconexion del lector RFID.");
                    final String status = rfidHandler.onResume(ConfiguracionAntena.this);
                    Log.d(UI_TAG, "[VIEW] onPostResume: resultado de reconexion RFID = " + status);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing() && !isDestroyed()) {
                                Toast.makeText(getApplicationContext(), status, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }).start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(UI_TAG, "[VIEW] onDestroy en ConfiguracionAntena.");
        // rfidHandler.onDestroy(); // Comentado para mantener conexión
    }
    @Override
    public void SetMessage(String msg) {
        Log.d(UI_TAG, "[VIEW] Mensaje recibido desde SDK RFID: " + msg);
        runOnUiThread(() -> {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            if (msg.equalsIgnoreCase("Conectado")) {
                actualizarCapacidadesLector();
                cargarLectoresRfid(1);
            }
        });
    }

    private void actualizarCapacidadesLector() {
        new Thread(() -> {
            supportedPowerLevels = rfidHandler.getSupportedPowerLevels();
            if (supportedPowerLevels != null && supportedPowerLevels.length > 0) {
                Log.d(UI_TAG, "[VIEW] Capacidades RFID cargadas. Niveles de potencia disponibles: " + supportedPowerLevels.length);
                runOnUiThread(() -> {
                    skPotencia.setMax(supportedPowerLevels.length - 1);
                    
                    // Buscar el índice que corresponde a la potencia guardada
                    String savedPower = SharedPreferencesGetSet.leer_local("potenciaAntena", _context);
                    int savedValue = 270;
                    try { savedValue = Integer.parseInt(savedPower); } catch (Exception ignored) {}
                    
                    int closestIndex = 0;
                    int minDiff = Integer.MAX_VALUE;
                    for (int i = 0; i < supportedPowerLevels.length; i++) {
                        int diff = Math.abs(supportedPowerLevels[i] - savedValue);
                        if (diff < minDiff) {
                            minDiff = diff;
                            closestIndex = i;
                        }
                    }
                    skPotencia.setProgress(closestIndex);
                    txtPorcentaje.setText(String.valueOf(supportedPowerLevels[closestIndex]));
                    txtCnfActual.setText("Potencia actual RFID (Indice " + closestIndex + "): " + supportedPowerLevels[closestIndex]);
                });
            } else {
                Log.w(UI_TAG, "[VIEW] El lector RFID no devolvio niveles de potencia soportados.");
            }
        }).start();
    }

    @Override
    public void handleTriggerPress(boolean pressed) {
        // Fix 6: performInventory/stopInventory son operaciones SDK que pueden bloquearse.
        // No deben ejecutarse en el UI thread — se delegan a un hilo background.
        final boolean isPressed = pressed;
        Log.d(UI_TAG, "[VIEW] Evento de gatillo RFID recibido. pressed=" + pressed);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isPressed) {
                    rfidHandler.performInventory();
                } else {
                    rfidHandler.stopInventory();
                }
            }
        }).start();
    }

    @Override
    public void handleTagdata(TagData[] tagData) {
        if (tagData == null) return;
        Log.d(UI_TAG, "[VIEW] Batch de lectura RFID recibido. Cantidad de tags: " + tagData.length);
        StringBuilder sb = new StringBuilder();
        for (TagData tag : tagData) {
            String epc = tag.getTagID();
            sb.append(epc).append("\n");
            contadorTags++;
            Log.d("RFID_SDK", "[TAG] EPC: " + epc);
        }
        final String nuevosEPCs = sb.toString();
        final int total = contadorTags;
        runOnUiThread(() -> {
            String actual = txtResultadoLectura.getText().toString();
            if (actual.equals("(sin lecturas RFID)")) actual = "";
            txtResultadoLectura.setText(actual + nuevosEPCs);
            txtContadorTags.setText("Tags RFID leidos: " + total);
        });
    }

    @Override
    public Context GetContext() {
        return this;
    }
    @Override
    public void onUserInteraction(){
        super.onUserInteraction();
        sessionActivate.cancel();
        sessionActivate.start();
    }

    private void logAperturaPantalla(String origen) {
        String fechaHora = LOG_TIME_FORMAT.format(new Date());
        Log.i(UI_TAG, "[APERTURA] ConfiguracionAntena abierta desde " + origen
                + " | fechaHora=" + fechaHora
                + " | epochMs=" + System.currentTimeMillis());
    }

    private void conectarLector() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                Log.d(UI_TAG, "[VIEW] Iniciando intento de conexion RFID desde la vista.");
                return rfidHandler.onResume(); // Llama connect()
            }

            @Override
            protected void onPostExecute(String result) {
                Log.d(UI_TAG, "[VIEW] Resultado de conexion RFID: " + (result.isEmpty() ? "Lector RFID ya conectado" : result));
                Toast.makeText(_context,
                        result.isEmpty() ? "Lector RFID ya conectado" : result,
                        Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void cargarLectoresRfid(int intento) {
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... voids) {
                Log.d(UI_TAG, "[VIEW] Solicitando al SDK la lista de lectores RFID disponibles. intento=" + intento);
                return rfidHandler.getAvailableReaderNames();
            }

            @Override
            protected void onPostExecute(List<String> readerNames) {
                if (readerNames != null && !readerNames.isEmpty()) {
                    actualizarSpinnerLectores(readerNames);
                    return;
                }

                boolean sdkReady = rfidHandler != null && rfidHandler.isSdkReady();
                Log.w(UI_TAG, "[VIEW] No se obtuvieron lectores RFID. intento=" + intento + ", sdkReady=" + sdkReady);

                if (intento < MAX_REINTENTOS_LECTORES) {
                    handlerLectura.postDelayed(() -> cargarLectoresRfid(intento + 1), RETARDO_REINTENTO_LECTORES_MS);
                } else {
                    Log.e(UI_TAG, "[VIEW] El SDK RFID no devolvio lectores para la vista de configuracion tras "
                            + MAX_REINTENTOS_LECTORES + " intentos.");
                    Toast.makeText(_context, "No hay lectores RFID disponibles", Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    private void actualizarSpinnerLectores(List<String> readerNames) {
        Log.d(UI_TAG, "[VIEW] Lectores RFID encontrados para la vista: " + readerNames.size() + " -> " + readerNames);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(_context,
                android.R.layout.simple_spinner_item, readerNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spReaders.setAdapter(adapter);
        spReaders.setSelection(0);
        spReaders.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String lectorSeleccionado = readerNames.get(position);
                Log.d(UI_TAG, "[VIEW] Lector RFID seleccionado en vista: " + lectorSeleccionado);
                rfidHandler.setReaderName(lectorSeleccionado);
                conectarLector();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public void controles(){
         mConfigurarAntena     = findViewById(R.id.FConfigurarAntena);
         txtPotencia           = findViewById(R.id.txtPotencia);
         skPotencia            = findViewById(R.id.skPotencia);
         pgPotencia            = findViewById(R.id.progressBar);
         txtPorcentaje         = findViewById(R.id.txtPorcentaje);
         txtCnfActual          = findViewById(R.id.txtUltimaConfiguracion);
         spReaders             = findViewById(R.id.spinnerLectores);
         // Prueba de lectura
         btnLecturaSencilla    = findViewById(R.id.btnLecturaSencilla);
         btnLecturaSostenida   = findViewById(R.id.btnLecturaSostenida);
         txtResultadoLectura   = findViewById(R.id.txtResultadoLectura);
         txtContadorTags       = findViewById(R.id.txtContadorTags);
    }

    public void eventos(){
        skPotencia.setOnSeekBarChangeListener(OnSeekPotencia);

        // ── Lectura Sencilla ──────────────────────────────────────────────
        // Dispara un inventario puntual y lo detiene automáticamente a los 1.5 s.
        btnLecturaSencilla.setOnClickListener(v -> {
            if (!rfidHandler.isReaderConnected()) {
                Log.w(UI_TAG, "[VIEW] Lectura RFID simple cancelada: no hay lector conectado.");
                Toast.makeText(_context, "Sin lector RFID conectado", Toast.LENGTH_SHORT).show();
                return;
            }
            limpiarResultados();
            Log.d(UI_TAG, "[VIEW] Iniciando prueba de lectura RFID simple.");
            new Thread(() -> {
                rfidHandler.performInventory();
                // Detener tras 1.5 s
                handlerLectura.postDelayed(() -> new Thread(() -> rfidHandler.stopInventory()).start(), 1500);
            }).start();
        });

        // ── Lectura Sostenida ─────────────────────────────────────────────
        // Toggle: inicia inventario continuo o lo detiene según el estado actual.
        btnLecturaSostenida.setOnClickListener(v -> {
            if (!rfidHandler.isReaderConnected()) {
                Log.w(UI_TAG, "[VIEW] Lectura RFID continua cancelada: no hay lector conectado.");
                Toast.makeText(_context, "Sin lector RFID conectado", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!lecturaSostenidaActiva) {
                lecturaSostenidaActiva = true;
                limpiarResultados();
                btnLecturaSostenida.setText("Detener RFID continuo");
                btnLecturaSostenida.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F")));
                Log.d(UI_TAG, "[VIEW] Iniciando prueba de lectura RFID continua.");
                new Thread(() -> rfidHandler.performInventory()).start();
            } else {
                lecturaSostenidaActiva = false;
                btnLecturaSostenida.setText("Iniciar RFID continuo");
                btnLecturaSostenida.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#388E3C")));
                Log.d(UI_TAG, "[VIEW] Deteniendo prueba de lectura RFID continua.");
                new Thread(() -> rfidHandler.stopInventory()).start();
            }
        });
    }

    /** Limpia el área de resultados y reinicia el contador. */
    private void limpiarResultados() {
        contadorTags = 0;
        Log.d(UI_TAG, "[VIEW] Reiniciando resultados de lectura RFID en pantalla.");
        runOnUiThread(() -> {
            txtResultadoLectura.setText("");
            txtContadorTags.setText("Tags RFID leidos: 0");
        });
    }

    public SeekBar.OnSeekBarChangeListener OnSeekPotencia = new SeekBar.OnSeekBarChangeListener(){
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
            if (supportedPowerLevels != null && progress < supportedPowerLevels.length) {
                final int valorReal = supportedPowerLevels[progress];
                potenciaAntena = String.valueOf(valorReal);
                txtPorcentaje.setText(potenciaAntena);
                if (fromUser) {
                    Log.d(UI_TAG, "[VIEW] Usuario ajusto potencia RFID. indice=" + progress + ", valor=" + valorReal);
                    SharedPreferencesGetSet.guardar_local("potenciaAntena", potenciaAntena, getApplicationContext());
                    // Aplicar al lector inmediatamente si está conectado
                    if (rfidHandler != null) {
                        new Thread(() -> {
                            rfidHandler.setAntennaPower(valorReal);
                        }).start();
                    }
                }
            } else {
                // Fallback si no hay capacidades cargadas aún
                potenciaAntena = String.valueOf(progress);
                txtPorcentaje.setText(potenciaAntena);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            final int progress = seekBar.getProgress();
            if (supportedPowerLevels != null) {
                final int valorReal = supportedPowerLevels[progress];
                txtCnfActual.setText("Potencia actual RFID: " + valorReal);
                if (rfidHandler != null) {
                    Log.d(UI_TAG, "[VIEW] Aplicando potencia RFID seleccionada. indice=" + progress + ", valor=" + valorReal);
                    new Thread(() -> rfidHandler.setAntennaPower(valorReal)).start();
                }
            } else if (rfidHandler != null) {
                Log.d(UI_TAG, "[VIEW] Aplicando potencia RFID sin capacidades cargadas. valor=" + progress);
                new Thread(() -> rfidHandler.setAntennaPower(progress)).start();
            }
        }

    };





}
