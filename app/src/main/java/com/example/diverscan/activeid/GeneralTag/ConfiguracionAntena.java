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

import java.util.List;

public class ConfiguracionAntena extends AppCompatActivity implements ResponseHandlerInterface{
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
        _context = this;
        _activity = this;
        controles();
        eventos();
        rfidHandler = TagWriter.getInstance();
        rfidHandler.onCreate(this);

        try{
            Power = SharedPreferencesGetSet.leer_local("potenciaAntena", this);
            txtCnfActual.setText("Potencia actual: " + Power);
            
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


        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... voids) {
                Log.d("RFID_SDK", "Listando lectores disponibles...");
                return rfidHandler.getAvailableReaderNames();
            }

            @Override
            protected void onPostExecute(List<String> readerNames) {
                if (readerNames != null && !readerNames.isEmpty()) {
                    Log.d("RFID_SDK", "Lectores encontrados: " + readerNames.size());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(_context,
                            android.R.layout.simple_spinner_item, readerNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spReaders.setAdapter(adapter);

                    // Seleccionar el primero o el actual
                    spReaders.setSelection(0);

                    // Escuchar cambios de selección
                    spReaders.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String lectorSeleccionado = readerNames.get(position);
                            Log.d("RFID_SDK", "Lector seleccionado en UI: " + lectorSeleccionado);
                            rfidHandler.setReaderName(lectorSeleccionado);
                            conectarLector();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                } else {
                    Log.e("RFID_SDK", "No se encontraron lectores en la búsqueda inicial.");
                    Toast.makeText(_context, "No hay lectores disponibles", Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (rfidHandler != null) {
            rfidHandler.onPause();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        // FIX ANR: rfidHandler.onResume() llama internamente a connect() — operación
        // bloqueante de Bluetooth. Ejecutarla en el UI thread causa ANR.
        // Se mueve a hilo background (igual que Lectura_Inventario y ElegirUbicacion_TomaFisica).
        if (rfidHandler != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String status = rfidHandler.onResume(ConfiguracionAntena.this);
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
        // rfidHandler.onDestroy(); // Comentado para mantener conexión
    }
    @Override
    public void SetMessage(String msg) {
        runOnUiThread(() -> {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            if (msg.equalsIgnoreCase("Conectado")) {
                actualizarCapacidadesLector();
            }
        });
    }

    private void actualizarCapacidadesLector() {
        new Thread(() -> {
            supportedPowerLevels = rfidHandler.getSupportedPowerLevels();
            if (supportedPowerLevels != null && supportedPowerLevels.length > 0) {
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
                    txtCnfActual.setText("Potencia actual (Índice " + closestIndex + "): " + supportedPowerLevels[closestIndex]);
                });
            }
        }).start();
    }

    @Override
    public void handleTriggerPress(boolean pressed) {
        // Fix 6: performInventory/stopInventory son operaciones SDK que pueden bloquearse.
        // No deben ejecutarse en el UI thread — se delegan a un hilo background.
        final boolean isPressed = pressed;
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
            if (actual.equals("(sin lecturas)")) actual = "";
            txtResultadoLectura.setText(actual + nuevosEPCs);
            txtContadorTags.setText("Tags leídos: " + total);
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
    private void conectarLector() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                Log.d("RFID_SDK", "Iniciando intento de conexión...");
                return rfidHandler.onResume(); // Llama connect()
            }

            @Override
            protected void onPostExecute(String result) {
                Log.d("RFID_SDK", "Resultado de conexión: " + (result.isEmpty() ? "Ya conectado" : result));
                Toast.makeText(_context,
                        result.isEmpty() ? "Lector ya conectado" : result,
                        Toast.LENGTH_SHORT).show();
            }
        }.execute();
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
                Toast.makeText(_context, "Sin lector conectado", Toast.LENGTH_SHORT).show();
                return;
            }
            limpiarResultados();
            Log.d("RFID_SDK", "[TEST] Lectura sencilla iniciada");
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
                Toast.makeText(_context, "Sin lector conectado", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!lecturaSostenidaActiva) {
                lecturaSostenidaActiva = true;
                limpiarResultados();
                btnLecturaSostenida.setText("⏹ Detener Continua");
                btnLecturaSostenida.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F")));
                Log.d("RFID_SDK", "[TEST] Lectura sostenida iniciada");
                new Thread(() -> rfidHandler.performInventory()).start();
            } else {
                lecturaSostenidaActiva = false;
                btnLecturaSostenida.setText("▶ Iniciar Continua");
                btnLecturaSostenida.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#388E3C")));
                Log.d("RFID_SDK", "[TEST] Lectura sostenida detenida");
                new Thread(() -> rfidHandler.stopInventory()).start();
            }
        });
    }

    /** Limpia el área de resultados y reinicia el contador. */
    private void limpiarResultados() {
        contadorTags = 0;
        runOnUiThread(() -> {
            txtResultadoLectura.setText("");
            txtContadorTags.setText("Tags leídos: 0");
        });
    }

    public SeekBar.OnSeekBarChangeListener OnSeekPotencia = new SeekBar.OnSeekBarChangeListener(){

        // Fix 5: potencia mínima funcional para el lector Zebra
        private final int MIN_POWER = 30;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
            if (supportedPowerLevels != null && progress < supportedPowerLevels.length) {
                int valorReal = supportedPowerLevels[progress];
                potenciaAntena = String.valueOf(valorReal);
                txtPorcentaje.setText(potenciaAntena);
                if (fromUser) {
                    SharedPreferencesGetSet.guardar_local("potenciaAntena", potenciaAntena, getApplicationContext());
                    // Aplicar al lector inmediatamente si está conectado
                    if (rfidHandler != null) {
                        new Thread(() -> {
                            rfidHandler.setAntennaPower(progress);
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
                txtCnfActual.setText("Potencia actual: " + supportedPowerLevels[progress]);
            }

            if (rfidHandler != null) {
                new Thread(() -> {
                    // El SDK de Zebra usa el ÍNDICE para configurar la potencia
                    rfidHandler.setAntennaPower(progress);
                }).start();
            }
        }

    };





}
