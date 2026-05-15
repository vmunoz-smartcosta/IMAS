package com.example.diverscan.activeid.Inventory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.example.diverscan.activeid.Activo.ActivoInventario;
import com.example.diverscan.activeid.Activo.AjustarActivoUbicacion;
import com.example.diverscan.activeid.Activo.EntidadActivos;
import com.example.diverscan.activeid.AssetStatus.EntidadAssetStatus;
import com.example.diverscan.activeid.ConfiguracionesGeneral.SharedPreferencesGetSet;
import com.example.diverscan.activeid.GeneralTag.ResponseHandlerInterface;
import com.example.diverscan.activeid.GeneralTag.TagWriter;
import com.example.diverscan.activeid.R;
import com.example.diverscan.activeid.login.LoginActivity;
import com.example.diverscan.activeid.sqlite.AssetsDBHelper;
import com.example.diverscan.activeid.sqlite.InventoryDBHelper;
import com.example.diverscan.activeid.sqlite.OfficesDBHelper;
import com.zebra.rfid.api3.TagData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class Lectura_Inventario extends AppCompatActivity implements ResponseHandlerInterface, IChequearInventario {
    private static final String RFID_FLOW_TAG = "RFID_FLOW";
    AssetsDBHelper AssetsDBHelper;
    com.example.diverscan.activeid.sqlite.SincronizarDBHelper sincronizarDBHelper;
    private TextView txt_TagsLeidos;
    private String Faltante = "Faltante", NoPertenece = "No Pertenece", Encontrado = "Encontrado",
            idTake, takeName, takeDescription, takeDate, idOficina, idTypeInventory;
    private static final String ESTADO_TODOS_GUID_CERO = "00000000-0000-0000-0000-000000000000";
    private static final String ESTADO_TODOS_GUID_UNO = "00000000-0000-0000-0000-000000000001";
    private static final String LABEL_ESTADO_TODOS = "Todos";
    private String estadoActivoTipo = "";
    private String ultimoEstadoFiltroAplicado = "";
    int contadorActivosFaltantes = 0, contActivosEncontrados = 0, contadorActivosSobrantes = 0;
    private RecyclerView ListaLectura;
    private EditText txtEncontrados, txtSobrantes, txtFaltantes, txtLeidas, txtBarcode, txtUbicacion;
    public int CantidadEPCLeida, EPCEncontrados, EPCSobrantes, EPCActivosUbicacion, EPCFaltantes;
    OfficesDBHelper OfficesDBHelper;
    private Button GuardarRessultado;
    AdaptadorLecturas itemAdapterAssets;
    private View LecturaInventarioView;
    private ArrayList<String> _activosEncontrados, _activosFaltantes, _activosActivoNoPertenece = new ArrayList<String>();
    private ArrayList<EntidadDetalleInventario> _detalleInventario = new ArrayList<EntidadDetalleInventario>();
    private ArrayList<EntidadInventario> _inventario = new ArrayList<EntidadInventario>();
    private ArrayList<ActivoInventario> activoInventarios = new ArrayList<ActivoInventario>();
    ArrayList<InventarioVisual> inventarioVisuals = new ArrayList<InventarioVisual>();

    // FIX ANTI-CRASH: HashSet paralelo para lookup O(1) de assetSysId.
    // El ArrayList original usaba contains() por referencia de objeto (bug) y era O(n).
    // Con cientos de tags la búsqueda O(n²) sumada a los runOnUiThread sucesivos
    // inundaba el Looper de la UI y generaba ANR.
    private final HashSet<String> _assetSysIdsEnLista = new HashSet<>();

    // FIX ANTI-CRASH: buffer de actualizaciones visuales pendientes + Handler para
    // agrupar múltiples RetornarActivo() en un solo bloque de 300ms.
    // Sin esto, 100 tags = 100 runOnUiThread() individuales = ANR garantizado.
    private final List<InventarioVisual> _pendingVisualUpdates =
            Collections.synchronizedList(new ArrayList<InventarioVisual>());
    private final android.os.Handler _uiUpdateHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private volatile boolean _flushScheduled = false;
    private static final long FLUSH_INTERVAL_MS = 300;

    private ArrayList<String> _activosSobrantes = new ArrayList<String>();
    private ArrayList<String> _activosEncontrado = new ArrayList<String>();
    private ArrayList<String> _activosNoExiste = new ArrayList<String>();
    InventoryDBHelper inventoryDBHelper;
    TagWriter rfidHandler;
    private String _lastTag = "";
    private boolean triggerPressed = false, scannerActivate = false;
    ChequearInventario _chequearInventario;
    Activity _activity;
    Context _context;
    AlertDialog alertDialog;
    private long startTime = 1 * 60 * 15000;
    private final long interval = 1 * 1000;
    CountDownTimer sessionActivate;

    private ProgressDialog dialog;
    ConstraintLayout rlsnackbar;
    Snackbar _snackbar;
    private Switch OnRfid;
    private Spinner spEstadoActivo;
    private TextView lblEstadoActivo;
    public EntidadActivos entidadActivos;
    //*************************************************************************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lectura_inventario);
        getSupportActionBar().hide();
        _activity = this;
        _context = this;
        controles();
        eventos();
        prepararSesionRfid(false);
        hideSoftKeyboard();
        RecibirTakesInfo();
        sessionActivate = new CountDownTimer(startTime, interval) {

            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {

                Intent intent = new Intent(Lectura_Inventario.this, LoginActivity.class);
                startActivity(intent);
            }
        }.start();
    }

    private void inicializarRFID() {
        rfidHandler = TagWriter.getInstance();
        if (rfidHandler == null) {
            return;
        }
        rfidHandler.onCreate(this);
        rfidHandler.setResponseHandler(this);
        Log.d(RFID_FLOW_TAG, "[Lectura_Inventario] Sesion RFID enlazada a la pantalla. initialized="
                + rfidHandler.isInitialized() + ", reconnect=" + false);
    }

    private void prepararSesionRfid(boolean reconnectIfNeeded) {
        inicializarRFID();
        if (!reconnectIfNeeded || rfidHandler == null) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(RFID_FLOW_TAG, "[Lectura_Inventario] Validando/reutilizando conexion RFID al volver a la pantalla.");
                final String status = rfidHandler.onResume(Lectura_Inventario.this);
                Log.d(RFID_FLOW_TAG, "[Lectura_Inventario] Resultado de validacion RFID: " + status);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing() && !isDestroyed() && status != null && !status.isEmpty()) {
                            Toast.makeText(_context, status, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }

    //*************************************************************************************************************

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        sessionActivate.cancel();
        sessionActivate.start();
    }

    //*************************************************************************************************************

    public void controles() {

        LecturaInventarioView = findViewById(R.id.LecturaInventario);
        txtEncontrados = findViewById(R.id.txtEncontrados);
        txtFaltantes = findViewById(R.id.txtFaltantes);
        txtSobrantes = findViewById(R.id.txtSobrantes);
        txtLeidas = findViewById(R.id.txtLeidos);
        txtUbicacion = findViewById(R.id.txtUbicacion);
        GuardarRessultado = findViewById(R.id.btn_GuardarToma);
        txtBarcode = findViewById(R.id.txt_barcode);
        inventoryDBHelper = new InventoryDBHelper(LecturaInventarioView.getContext());
        AssetsDBHelper = new AssetsDBHelper(_context);
        OfficesDBHelper = new OfficesDBHelper(_context);
        sincronizarDBHelper = new com.example.diverscan.activeid.sqlite.SincronizarDBHelper(_context);
        ListaLectura = findViewById(R.id.listView);
        itemAdapterAssets = new AdaptadorLecturas(inventarioVisuals);
        rlsnackbar = findViewById(R.id.clLecturaInventario);
        OnRfid = findViewById(R.id.swRfid);
        spEstadoActivo = findViewById(R.id.sp_estado_activo);
        lblEstadoActivo = findViewById(R.id.lbl_estado_activo);

        // FIX ANTI-CRASH: inicializar RecyclerView UNA sola vez aquí.
        // El código original llamaba setLayoutManager()+setAdapter() en cada
        // inserción (fillRecyclerView / CargaInicialRecyclerView), lo que
        // recrea el estado interno del RV en cada tag nuevo → crash por inconsistencia.
        ListaLectura.setLayoutManager(new LinearLayoutManager(_context));
        ListaLectura.setAdapter(itemAdapterAssets);
    }


    //*************************************************************************************************************

    public void eventos() {
        GuardarRessultado.setOnClickListener(OnClickListenerGuardarResultado);
        txtFaltantes.setText(String.valueOf(contadorActivosFaltantes));
        txtSobrantes.setText(String.valueOf(contadorActivosSobrantes));
        txtEncontrados.setText(String.valueOf(contActivosEncontrados));
        OnRfid.setOnCheckedChangeListener(AccionRFID);

        txtBarcode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String texto = s.toString();
                // BUG-FIX: Se elimina el disparador automático por longitud (length >= 6) 
                // para permitir el ingreso de placas cortas y evitar cortes en EPCs largos.
                // Ahora se procesa solo cuando hay un terminador explícito (\n, \t, \r)
                // que suelen enviar los lectores de barcode configurados como teclado.
                if (texto.endsWith("\n") || texto.endsWith("\t") || texto.endsWith("\r")) {
                    String placa = texto.trim();
                    if (placa.length() >= 2) {
                        AgregarActivoPlaca(placa);
                    }
                    txtBarcode.setText("");
                    txtBarcode.requestFocus();
                }
            }
        });

        // Permite procesar al presionar "Enter" / "Listo" desde el teclado virtual
        txtBarcode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String placa = txtBarcode.getText().toString().trim();
                // Se permite procesar placas cortas (mínimo 2 caracteres) al presionar Enter
                if (placa.length() >= 2) {
                    AgregarActivoPlaca(placa);
                    txtBarcode.setText("");
                    txtBarcode.requestFocus();
                    return true;
                }
                return false;
            }
        });

    }

    private void AgregarActivoPlaca(String placa) {
        _chequearInventario.AgregarActivosBarcode(placa, this);
    }
    //*************************************************************************************************************

    public void hideSoftKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    //*************************************************************************************************************
    Switch.OnCheckedChangeListener AccionRFID = new Switch.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
            if (isChecked) {
                MostrarProgressDialog("Encendiendo RFID");
            } else {
                MostrarProgressDialog("Apagando RFID");
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                        if (isChecked) {
                            iniciarRFID();
                        } else {
                            desconectarRFID();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    txtBarcode.requestFocus();
                                }
                            });
                        }
                    } catch (InterruptedException e) {
                        // Fix 6: restaurar el flag de interrupción del hilo
                        Thread.currentThread().interrupt();
                    } finally {
                        // Fix 1: dialog.dismiss() siempre en UI thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (dialog != null && dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                            }
                        });
                    }
                }
            }).start();
        }
    };

    private void MostrarProgressDialog(String mensaje) {
        dialog = new ProgressDialog(_context);
        dialog.setMessage(mensaje);
        dialog.setCancelable(false);
        dialog.show();
    }

    private void iniciarRFID() {
        try {
            scannerActivate = false;
            // setTriggerMode ya puede lanzar excepciones de SDK; el dialog.dismiss()
            // se gestiona en el finally del hilo llamante (AccionRFID) para garantizar
            // que siempre se ejecute en el UI thread.
            if (!rfidHandler.setTriggerMode("RFID")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MostrarSnackBar(false, "No se ha podido iniciar el RFID");
                    }
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void desconectarRFID() {
        try {
            scannerActivate = true;
            if (!rfidHandler.setTriggerMode("BARCODE")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MostrarSnackBar(false, "No se ha podido apagar el RFID");
                    }
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //*************************************************************************************************************

    public void RecibirTakesInfo() {

        idTake = getIntent().getExtras().getString("IdTake");
        takeName = getIntent().getExtras().getString("takeName");
        takeDescription = getIntent().getExtras().getString("takeDescription");
        takeDate = getIntent().getExtras().getString("takeDate");
        idOficina = getIntent().getExtras().getString("idOficina");
        idTypeInventory = getIntent().getExtras().getString("tipoInventario");

        estadoActivoTipo = sincronizarDBHelper.ObtenerEstadoActivoPorTipo(idTypeInventory);
        activoInventarios = OfficesDBHelper.ActivosUbicacion(idOficina, estadoActivoTipo);

        _chequearInventario = new ChequearInventario(activoInventarios, idTake, this, this);
        configurarFiltroEstadoSiAplica(estadoActivoTipo);

    }

    private boolean esEstadoTodos(String estadoActivo) {
        return estadoActivo == null
                || estadoActivo.trim().isEmpty()
                || ESTADO_TODOS_GUID_CERO.equalsIgnoreCase(estadoActivo)
                || ESTADO_TODOS_GUID_UNO.equalsIgnoreCase(estadoActivo);
    }

    private void configurarFiltroEstadoSiAplica(String estadoActivo) {
        if (!esEstadoTodos(estadoActivo)) {
            spEstadoActivo.setVisibility(View.GONE);
            lblEstadoActivo.setVisibility(View.GONE);
            return;
        }

        List<EntidadAssetStatus> catalogoEstados = sincronizarDBHelper.ObtenerAssetStatusCatalogo();
        final ArrayList<String> labels = new ArrayList<>();
        final HashMap<String, String> labelToId = new HashMap<>();
        labels.add(LABEL_ESTADO_TODOS);
        labelToId.put(LABEL_ESTADO_TODOS, ESTADO_TODOS_GUID_CERO);

        for (EntidadAssetStatus estado : catalogoEstados) {
            String nombre = estado.getName();
            if (nombre != null && !nombre.trim().isEmpty() && !labelToId.containsKey(nombre)) {
                labels.add(nombre);
                labelToId.put(nombre, estado.getId());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEstadoActivo.setAdapter(adapter);
        spEstadoActivo.setVisibility(View.VISIBLE);
        lblEstadoActivo.setVisibility(View.VISIBLE);
        Log.i("Lectura_Inventario", "Filtro por estado habilitado. Estados disponibles: " + labels.size());

        spEstadoActivo.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String label = labels.get(position);
                String estadoSeleccionado = labelToId.get(label);
                if (estadoSeleccionado == null) {
                    estadoSeleccionado = ESTADO_TODOS_GUID_CERO;
                }
                aplicarFiltroEstadoActivos(label, estadoSeleccionado);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void aplicarFiltroEstadoActivos(String label, String estadoSeleccionado) {
        if (estadoSeleccionado.equals(ultimoEstadoFiltroAplicado)) {
            return;
        }
        if (contActivosEncontrados > 0 || contadorActivosSobrantes > 0) {
            Toast.makeText(this, "No se puede cambiar el filtro con lecturas ya realizadas", Toast.LENGTH_LONG).show();
            return;
        }

        ultimoEstadoFiltroAplicado = estadoSeleccionado;
        activoInventarios = OfficesDBHelper.ActivosUbicacion(idOficina, estadoSeleccionado);

        inventarioVisuals.clear();
        _assetSysIdsEnLista.clear(); // FIX: sincronizar HashSet con el reset de la lista visual
        _pendingVisualUpdates.clear(); // FIX: descartar actualizaciones pendientes del filtro anterior
        itemAdapterAssets.notifyDataSetChanged();
        contadorActivosFaltantes = 0;
        contActivosEncontrados = 0;
        contadorActivosSobrantes = 0;
        txtFaltantes.setText("0");
        txtEncontrados.setText("0");
        txtSobrantes.setText("0");

        _chequearInventario = new ChequearInventario(activoInventarios, idTake, this, this);
        Log.i("Lectura_Inventario", "Filtro estado seleccionado: " + label + " (" + estadoSeleccionado + "), activos: " + activoInventarios.size());
    }

    //*************************************************************************************************************
    private boolean guardadoOk = false;
    /*Comienza lectura de inventario*/
    private void GuardarResultados() {
        try {
            if (inventarioVisuals == null || inventarioVisuals.isEmpty()) {
                MostrarSnackBar(false, "No hay lecturas para guardar.");
                return;
            }

            // Limpia contenedores para evitar duplicados si el usuario guarda varias veces
            _detalleInventario.clear();
            _inventario.clear();

            // IDs coherentes
            final String idInventory = UUID.randomUUID().toString();
            final String idTomasDelInventario = UUID.randomUUID().toString();

            // Usuario actual
            final String idUsuario = SharedPreferencesGetSet.leer_local("_userId", getApplicationContext());
            String idTipoInventarioSubtoma = idTake;
            if (idTipoInventarioSubtoma == null || idTipoInventarioSubtoma.trim().isEmpty()) {
                idTipoInventarioSubtoma = idTypeInventory;
            }
            final String idTipoInventarioFinal = idTipoInventarioSubtoma;
            Log.i("Lectura_Inventario", "GuardarResultados idTake=" + idTake + ", tipoInventarioUI=" + idTypeInventory + ", idTipoInventarioSubtoma=" + idTipoInventarioFinal);

            // Fecha en ISO (recomendado para el WCF)
            final String fechaIso = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                    .format(new java.util.Date());

            // Cabecera: TomasDelInventario
            final EntidadTomasInventario entidadTomasInventario =
                    new EntidadTomasInventario(
                            idTomasDelInventario,
                            fechaIso,
                            idOficina,
                            idUsuario,
                            idTipoInventarioFinal
                    );

            // DetalleInventario (uno por cada item visual leído)
            for (InventarioVisual item : inventarioVisuals) {
                _detalleInventario.add(
                        new EntidadDetalleInventario(
                                UUID.randomUUID().toString(), // IdDetalleInventario
                                idInventory,                   // FK_idInventory
                                item.getNumero(),
                                item.getDescripcion(),
                                item.getEPC(),
                                item.getStatus(),
                                "0"                            // Excluido
                        )
                );
            }

            // Totales
            int activosLeidos    = contadorActivosSobrantes + contActivosEncontrados;
            int activosUbicacion = contActivosEncontrados + contadorActivosFaltantes;

            // Cabecera Inventario
            _inventario.add(
                    new EntidadInventario(
                            idInventory,
                            idTomasDelInventario,
                            "2",
                            String.valueOf(activosLeidos),
                            String.valueOf(activosUbicacion),
                            String.valueOf(contActivosEncontrados),
                            String.valueOf(contadorActivosFaltantes),
                            String.valueOf(contadorActivosSobrantes),
                            fechaIso
                    )
            );

            // Copias inmutables para el hilo background
            final ArrayList<EntidadDetalleInventario> detalleFinal = new ArrayList<>(_detalleInventario);
            final ArrayList<EntidadInventario> inventarioFinal    = new ArrayList<>(_inventario);

            // Fix 2: ejecutar persistencia en hilo background para no bloquear el UI thread
            MostrarProgressDialog("Guardando resultados...");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final boolean ok1 = inventoryDBHelper.InsertOrReplaceTomaInventario(entidadTomasInventario);
                    final boolean ok2 = inventoryDBHelper.InsertOrReplaceDetalleInventario(detalleFinal);
                    final boolean ok3 = inventoryDBHelper.InsertOrReplaceInventario(inventarioFinal);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dialog != null && dialog.isShowing()) {
                                dialog.dismiss();
                            }
                            if (!ok1 || !ok2 || !ok3) {
                                MostrarSnackBar(false, "No se han podido guardar los resultados del inventario, intente nuevamente.");
                            } else {
                                guardadoOk = true;
                                MostrarSnackBar(true, "Resultados guardados. Puedes sincronizar ahora.");
                            }
                        }
                    });
                }
            }).start();

        } catch (Exception ex) {
            ex.printStackTrace();
            MostrarSnackBar(false, "Error guardando: " + ex.getMessage());
        }
    }




    //*************************************************************************************************************
    private Button.OnClickListener OnClickListenerGuardarResultado = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            GuardarResultados();
        }
    };

    //*************************************************************************************************************
    //region Lecturas
    // FIX ANTI-CRASH: CargaInicialRecyclerView ya no llama setLayoutManager/setAdapter
    // (movidos a controles()). Encola el item en el buffer para ser procesado en el
    // flush de 300ms junto con los demás items del mismo batch de lectura.
    private void CargaInicialRecyclerView(InventarioVisual response) {
        _assetSysIdsEnLista.add(response.getAssetSysId());
        inventarioVisuals.add(response);
        contadorActivosFaltantes++;
        // Notificar al adapter solo con insert puntual; el flush acumula más cambios
        itemAdapterAssets.notifyItemInserted(inventarioVisuals.size() - 1);
        txtFaltantes.setText(String.valueOf(contadorActivosFaltantes));
    }

    // FIX ANTI-CRASH: fillRecyclerView ahora delega a _pendingVisualUpdates + flush.
    // El patron anterior hacía un runOnUiThread por tag y lanzaba
    // setLayoutManager+setAdapter en cada nuevo sobrante → ANR + IndexOutOfBounds.
    public void fillRecyclerView(InventarioVisual response) {
        _pendingVisualUpdates.add(response);
        scheduleFlush();
    }

    // Programa un único flush en 300ms; descarta programaciones duplicadas.
    private void scheduleFlush() {
        if (!_flushScheduled) {
            _flushScheduled = true;
            _uiUpdateHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    flushPendingUpdates();
                }
            }, FLUSH_INTERVAL_MS);
        }
    }

    // Procesa en el UI thread todos los InventarioVisual acumulados en el buffer.
    // Un solo notifyDataSetChanged() al final en lugar de N llamadas individuales.
    private void flushPendingUpdates() {
        _flushScheduled = false;
        List<InventarioVisual> batch;
        synchronized (_pendingVisualUpdates) {
            batch = new ArrayList<>(_pendingVisualUpdates);
            _pendingVisualUpdates.clear();
        }
        if (batch.isEmpty()) return;

        for (InventarioVisual item : batch) {
            applyVisualUpdate(item);
        }
        // Un solo refresh del adapter para todo el batch
        itemAdapterAssets.notifyDataSetChanged();
        // Actualizar contadores una sola vez
        txtFaltantes.setText(String.valueOf(contadorActivosFaltantes));
        txtEncontrados.setText(String.valueOf(contActivosEncontrados));
        txtSobrantes.setText(String.valueOf(contadorActivosSobrantes));
    }

    // Lógica de negocio extraida de fillRecyclerView: clasifica el item y actualiza
    // los contadores internos. NO toca la UI directamente (lo hace flushPendingUpdates).
    private void applyVisualUpdate(InventarioVisual response) {
        try {
            int tamanoLista = inventarioVisuals.size();
            if (tamanoLista > 0) {
                // Buscar si ya existe por assetSysId y cambiaró de estado
                for (int i = 0; i < inventarioVisuals.size(); i++) {
                    if (inventarioVisuals.get(i).getAssetSysId().equals(response.getAssetSysId()) &&
                            !inventarioVisuals.get(i).getStatus().equals(response.getStatus())) {
                        inventarioVisuals.get(i).setStatus(response.getStatus());
                        contActivosEncontrados++;
                        contadorActivosFaltantes--;
                        return;
                    }
                }
                // FIX ANTI-CRASH: usar HashSet O(1) en lugar de contains() por referencia O(n).
                // El código original llamaba inventarioVisuals.contains(response.getAssetSysId())
                // que compara String con InventarioVisual → siempre false → duplicados infinitos.
                if (!_assetSysIdsEnLista.contains(response.getAssetSysId())) {
                    _assetSysIdsEnLista.add(response.getAssetSysId());
                    inventarioVisuals.add(response);
                    contadorActivosSobrantes++;
                    return;
                }
                // Ya está en lista con el mismo estado: ignorar
                return;
            }
            // Lista estaba vacía: primer item
            _assetSysIdsEnLista.add(response.getAssetSysId());
            inventarioVisuals.add(response);
            contadorActivosSobrantes++;
        } catch (Exception ex) {
            Log.e("Lectura_Inventario", "applyVisualUpdate error: ", ex);
        }
    }
    //endregion

    //*************************************************************************************************************

    /*Terminan metodos de lectura*/
    ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

    /*Acción de lectura de tags con la HH*/
    public void Message() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 24);
    }

    //*************************************************************************************************************

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(RFID_FLOW_TAG, "[Lectura_Inventario] onPause: se conserva la sesion RFID para otras pantallas.");
        rfidHandler.onPause();
    }

    //*************************************************************************************************************

    @Override
    protected void onPostResume() {
        super.onPostResume();
        prepararSesionRfid(true);
    }

    //*************************************************************************************************************

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // rfidHandler.onDestroy();
    }

    //*************************************************************************************************************

    @Override
    public void handleTagdata(TagData[] tagData) {
        if (!scannerActivate) {
            if (_chequearInventario.CheckTagsInventario(tagData, this)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Message();
                    }
                });
            }
        }
    }

    /*public void ProbarEPCManual(String epc) {
        ArrayList<String> tagsSimulados = new ArrayList<>();
        tagsSimulados.add(epc);

        boolean encontrado = _chequearInventario.CheckTagsInventario(tagsSimulados, this);
        if (encontrado) {
            runOnUiThread(() -> Message()); // beep si fue válido
        } else {
            runOnUiThread(() -> Toast.makeText(this, "No se encontró el EPC", Toast.LENGTH_SHORT).show());
        }
    }*/

    //*************************************************************************************************************

    @Override
    public void handleTriggerPress(boolean pressed) {
        try{
            triggerPressed = pressed;
            if (pressed) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
                rfidHandler.performInventory();
            } else{
                rfidHandler.stopInventory();
                //ProbarEPCManual("800474453240000000031607");
                //handler.postDelayed(r, 1000);
            }
        }catch (Exception ex){
            Log.d(ex.getMessage(), ex.getStackTrace().toString());
            Toast.makeText(_context, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    //*************************************************************************************************************

    @Override
    public Context GetContext() {
        return this;
    }

    //*************************************************************************************************************

    @Override
    public void SetMessage(String Text) {
        final String text = Text;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(_context, text, Toast.LENGTH_LONG).show();

            }
        });
    }

    //*************************************************************************************************************

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Message();
            AlertasError("ATENCION", "Si continua podría perder su progreso."+ "\n"+ "¿Realmente desea salir?");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //*************************************************************************************************************

    @Override
    public boolean onSupportNavigateUp() {
        Message();
        AlertasError("ATENCION", "Si continua podría perder su progreso."+ "\n"+ "¿Realmente desea salir?");
        return false;
    }

    //*************************************************************************************************************

    public boolean AlertasError(String titulo, String Mensaje){
        final boolean respuesta = false;
        LayoutInflater inflater = _activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_notificaciones_error, null);
        TextView txvTitulo = view.findViewById(R.id.txvTitleMessageDialog);
        TextView txvMessage = view.findViewById(R.id.txvMessageDialog);
        txvTitulo.setText(titulo);
        txvMessage.setText(Mensaje);
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(_activity);
        builder.setView(view);
        builder.setPositiveButton(Html.fromHtml("<font color='#D81622' background-color'#555555'>Aceptar</font>"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                onBackPressed();
            }
        });
        builder.setNegativeButton(Html.fromHtml("<font color='#D81622' background-color'#555555'>Cancelar</font>"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {


            }
        });
        builder.setIcon(R.drawable.alertaicono);
        alertDialog = builder.show();
        return  true;
    }

    //region Recepcion Activos
    @Override
    public void RetornarActivo(final InventarioVisual eInventarioVisual) {

        if(eInventarioVisual == null){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fillRecyclerView(eInventarioVisual);
            }
        });
    }

    @Override
    public void RetornarCargaInicial(final InventarioVisual eInventarioVisual) {
        if(eInventarioVisual == null){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CargaInicialRecyclerView(eInventarioVisual);
            }
        });
    }

    private void MostrarSnackBar(boolean tipoSnack, String mensaje) {
        if (tipoSnack) {
            _snackbar = Snackbar.make(rlsnackbar, mensaje, 3000);
            _snackbar.setActionTextColor(Color.rgb(179, 179, 179));
            View snackBarView = _snackbar.getView();
            snackBarView.setBackgroundColor(Color.rgb(4, 165, 77));
        } else {
            _snackbar = Snackbar.make(rlsnackbar, mensaje, 4000);
            _snackbar.setActionTextColor(Color.rgb(179, 179, 179));
            View snackBarView = _snackbar.getView();
            snackBarView.setBackgroundColor(Color.rgb(242, 59, 59));
        }
        _snackbar.show();
    }
    //endregion

    //*************************************************************************************************************

}
