package com.example.diverscan.activeid.Inventory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.diverscan.activeid.Inventory.EUbicacionActivo;
import com.example.diverscan.activeid.Edificio.EdificioDBHelper;
import com.example.diverscan.activeid.Edificio.EdificioNuevo;
import com.example.diverscan.activeid.GeneralTag.ResponseHandlerInterface;
import com.example.diverscan.activeid.GeneralTag.TagWriter;
import com.example.diverscan.activeid.Oficina.OficinaDBHelper;
import com.example.diverscan.activeid.Oficina.oficinaNuevo;
import com.example.diverscan.activeid.Piso.PisoDBHelper;
import com.example.diverscan.activeid.Piso.PisoNuevo;
import com.example.diverscan.activeid.R;
import com.example.diverscan.activeid.RazonSocial.RazonNuevo;
import com.example.diverscan.activeid.RazonSocial.RazonSocialDBHelper;
import com.example.diverscan.activeid.Utilities.AlertasPersonalizadas;
import com.example.diverscan.activeid.sqlite.InventoryDBHelper;
import com.example.diverscan.activeid.sqlite.OfficesDBHelper;
import com.zebra.rfid.api3.TagData;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ElegirUbicacion_TomaFisica extends AppCompatActivity implements ResponseHandlerInterface {

    private static final String TAG = "ElegirUbicacion_TomaFisica";
    private static final String RFID_FLOW_TAG = "RFID_FLOW";
    private View ElegirUbicacionView;
    InventoryDBHelper InventoryDBHelper;
    OfficesDBHelper OfficesDBHelper;
    List<String> listSpinner;
    Context _context;
    Activity _activity;
    EUbicacionActivo eUbicacionActivo = new EUbicacionActivo();
    private Button btn_continuar;
    private Spinner CompaniaView, EdificioView, PisoView, OficinaView, TipoInventarioView;

    private Map<Integer, RazonNuevo> _mapRazonSociales = new HashMap<Integer, RazonNuevo>();
    private Map<Integer, EdificioNuevo> _mapEdificios = new HashMap<Integer, EdificioNuevo>();
    private Map<Integer, PisoNuevo> _mapPisos = new HashMap<Integer, PisoNuevo>();
    private Map<Integer, oficinaNuevo> _mapOficinas = new HashMap<Integer, oficinaNuevo>();
    private Map<Integer, EntidadTiposInventarios> _mapTipoInventarios = new HashMap<Integer, EntidadTiposInventarios>();

    private boolean _itemSelectedUserCompania, _itemSelectedUserEdificio, _itemSelectedUserPiso = true;
    EditText txtAjusteOficina;
    public String idTake, takeName, takeDescription, takeDate, idOficina, idedificioActivo, idpisoActivo, idCompania;
    String idTipoInventario = "";
    // Filtros bloqueados
    String lockedIdRazonSocial, lockedIdEdificio, lockedIdPiso, lockedIdOficina;
    ConstraintLayout clsnackbar;
    Snackbar _snackbar;
    TagWriter rfidHandler;
    private String _lastTag = "";
    private boolean triggerPressed = false;

    public ElegirUbicacion_TomaFisica() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elegir_ubicacion__toma_fisica);
        _context = this;
        _activity = this;
        controles();
        eventos();
        RecibirTakesInfo(); // Llamar antes de cargar para tener los filtros
        cargarTiposInventarios();
        cargarRazonesSociales();
        cargarUbicaciones();
        prepararSesionRfid(false);
    }

    private void prepararSesionRfid(boolean reconnectIfNeeded) {
        rfidHandler = TagWriter.getInstance();
        if (rfidHandler == null) {
            return;
        }
        rfidHandler.onCreate(this);
        rfidHandler.setResponseHandler(this);
        Log.d(RFID_FLOW_TAG, "[ElegirUbicacion_TomaFisica] Sesion RFID enlazada a la pantalla. initialized="
                + rfidHandler.isInitialized() + ", reconnect=" + reconnectIfNeeded);
        if (!reconnectIfNeeded) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(RFID_FLOW_TAG, "[ElegirUbicacion_TomaFisica] Validando/reutilizando conexion RFID al volver a la pantalla.");
                rfidHandler.onResume(ElegirUbicacion_TomaFisica.this);
            }
        }).start();
    }

    public void controles() {
        ElegirUbicacionView = findViewById(R.id.elegirubicacion);
        InventoryDBHelper = new InventoryDBHelper(ElegirUbicacionView.getContext());
        btn_continuar = findViewById(R.id.btn_continua);

        CompaniaView = findViewById(R.id.compania_toma);
        EdificioView = findViewById(R.id.edificio_toma);
        PisoView = findViewById(R.id.piso_toma);
        OficinaView = findViewById(R.id.oficina_toma);
        TipoInventarioView = findViewById(R.id.sp_tipoInventario);

        OfficesDBHelper = new OfficesDBHelper(ElegirUbicacionView.getContext());
        txtAjusteOficina = findViewById(R.id.txtSectorBusquedaAS);
        clsnackbar = findViewById(R.id.clActivosToma);
    }

    public void eventos() {
        CompaniaView.setOnItemSelectedListener(onItemSpinnerListenerCompania);
        EdificioView.setOnItemSelectedListener(onItemSpinnerListenerEdificio);
        PisoView.setOnItemSelectedListener(onItemSpinnerListenerPiso);
        btn_continuar.setOnClickListener(OnClickListenerIrToma);
        txtAjusteOficina.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (txtAjusteOficina.getText().length() > 3) {
                    buscarOficinaPorIdPisoNombre();
                } else if (txtAjusteOficina.getText().length() == 0) {
                    PisoNuevo pisoRecord = (PisoNuevo) PisoView.getSelectedItem();
                    if (pisoRecord != null) {
                        cargarOficinas(pisoRecord.getIdPiso());
                    }
                }
            }
        });
    }

    private void cargarUbicaciones() {
        RazonNuevo razonSocialRecord = (RazonNuevo) CompaniaView.getSelectedItem();
        if (razonSocialRecord == null) {
            Log.e("ElegirUbicacion_TomaFisica", "No hay razón social seleccionada");
            Toast.makeText(getApplicationContext(), "No hay razones sociales sincronizadas", Toast.LENGTH_LONG).show();
            return;
        }
        idCompania = razonSocialRecord.getIdRazon();
        cargarEdificios(idCompania);

        EdificioNuevo edificioRecord = (EdificioNuevo) EdificioView.getSelectedItem();
        if (edificioRecord == null) {
            Log.e("ElegirUbicacion_TomaFisica", "No hay edificio seleccionado para compania: " + idCompania);
            Toast.makeText(getApplicationContext(), "No hay edificios para la compañía seleccionada", Toast.LENGTH_LONG).show();
            return;
        }
        idedificioActivo = edificioRecord.getIdEdificio();
        cargarPisos(idedificioActivo);

        PisoNuevo pisoRecord = (PisoNuevo) PisoView.getSelectedItem();
        if (pisoRecord == null) {
            Log.e("ElegirUbicacion_TomaFisica", "No hay piso seleccionado para edificio: " + idedificioActivo);
            Toast.makeText(getApplicationContext(), "No hay pisos para el edificio seleccionado", Toast.LENGTH_LONG).show();
            return;
        }
        idpisoActivo = pisoRecord.getIdPiso();
        cargarOficinas(idpisoActivo);
    }

    public void RecibirTakesInfo() {
        if (getIntent().getExtras() != null) {
            idTake = getIntent().getExtras().getString("Take_ID");
            takeName = getIntent().getExtras().getString("Take_Name");
            takeDescription = getIntent().getExtras().getString("Take_Description");
            takeDate = getIntent().getExtras().getString("Take_Date");

            // Recibir filtros bloqueados
            lockedIdRazonSocial = normalizeLockedFilter(getIntent().getExtras().getString("Take_idRazonSocial"));
            lockedIdEdificio = normalizeLockedFilter(getIntent().getExtras().getString("Take_idEdificio"));
            lockedIdPiso = normalizeLockedFilter(getIntent().getExtras().getString("Take_idPiso"));
            lockedIdOficina = normalizeLockedFilter(getIntent().getExtras().getString("Take_idOficina"));

            Log.i(TAG, "RecibirToma{ID=" + idTake
                    + ", Nombre=" + takeName
                    + ", Filtros={Razon=" + lockedIdRazonSocial + ", Edif=" + lockedIdEdificio + ", Piso=" + lockedIdPiso + ", Ofi=" + lockedIdOficina + "}}");
        }
    }

    private String normalizeLockedFilter(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        String value = rawValue.trim();
        if (value.isEmpty()) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if ("todos".equals(lower) || "todo".equals(lower) || "all".equals(lower)
                || "sin asignar".equals(lower) || "null".equals(lower) || "undefined".equals(lower)
                || "(null)".equals(lower) || "0".equals(lower) || "*".equals(lower)
                || "00000000-0000-0000-0000-000000000000".equals(lower)
                || "00000000-0000-0000-0000-000000000001".equals(lower)) {
            return "";
        }
        return value;
    }

    private String normalizeIdValue(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        String normalized = rawValue.trim();
        if (normalized.startsWith("{") && normalized.endsWith("}") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean idsMatch(String left, String right) {
        String l = normalizeIdValue(left);
        String r = normalizeIdValue(right);
        if (TextUtils.isEmpty(l) || TextUtils.isEmpty(r)) {
            return false;
        }
        return l.equalsIgnoreCase(r);
    }


    //region Se rellenan los spinners

    private void buscarOficinaPorIdPisoNombre() {
        if (_itemSelectedUserPiso) {
            PisoNuevo pisoRecord = (PisoNuevo) PisoView.getSelectedItem();
            if (pisoRecord == null) {
                return;
            }
            String nombreBusqueda = txtAjusteOficina.getText().toString();
            cargarOficinasPorPisoNombre(pisoRecord.getIdPiso(), nombreBusqueda);
        }
        _itemSelectedUserPiso = true;
    }

    private void cargarOficinasPorPisoNombre(String idPiso, String nombre) {

        _mapOficinas = (Map<Integer, oficinaNuevo>) OfficesDBHelper.ObtenerOficinaPorPisoDescripcion3(idPiso, nombre);
        oficinaNuevo[] oficinas = _mapOficinas.values().toArray(new oficinaNuevo[0]);
        fillSpinnerOficina(oficinas);
        Log.i(TAG, "cargarOficinasPorPisoNombre idPiso=" + idPiso + " nombre=" + nombre + " total=" + oficinas.length);
    }
    private void cargarTiposInventarios() {
        InventoryDBHelper inventoryDBHelper = new InventoryDBHelper(ElegirUbicacionView.getContext());
        _mapTipoInventarios = (Map<Integer, EntidadTiposInventarios>) inventoryDBHelper.ObtenerTiposInventario();
        EntidadTiposInventarios[] tiposInventarios = _mapTipoInventarios.values().toArray(new EntidadTiposInventarios[0]);
        fillTiposInventarios(tiposInventarios);
        Log.i(TAG, "cargarTiposInventarios total=" + tiposInventarios.length);
    }

    private void cargarRazonesSociales() {
        RazonSocialDBHelper razonSocialDBHelper = new RazonSocialDBHelper(ElegirUbicacionView.getContext());
        _mapRazonSociales = (Map<Integer, RazonNuevo>) razonSocialDBHelper.ObtenerRazon();
        if (_mapRazonSociales == null || _mapRazonSociales.size() == 0) {
            _mapRazonSociales = (Map<Integer, RazonNuevo>) razonSocialDBHelper.ObtenerRazonDesdeEdificiosFallback();
            Log.w(TAG, "cargarRazonesSociales fallback desde Edificios total=" + (_mapRazonSociales != null ? _mapRazonSociales.size() : 0));
        }
        RazonNuevo[] razones = _mapRazonSociales.values().toArray(new RazonNuevo[0]);
        fillSpinnerRazon(razones);
        Log.i(TAG, "cargarRazonesSociales total=" + razones.length);
    }

    private void cargarEdificios(String idCompania) {
        EdificioDBHelper edificioDBHelper = new EdificioDBHelper(ElegirUbicacionView.getContext());
        _mapEdificios = (Map<Integer, EdificioNuevo>) edificioDBHelper.ObtenerEdificio(idCompania);
        if (_mapEdificios == null || _mapEdificios.size() == 0) {
            _mapEdificios = (Map<Integer, EdificioNuevo>) edificioDBHelper.ObtenerTodosEdificios();
            Log.w(TAG, "cargarEdificios fallback general total=" + (_mapEdificios != null ? _mapEdificios.size() : 0));
        }
        EdificioNuevo[] edificios = _mapEdificios.values().toArray(new EdificioNuevo[0]);
        fillSpinnerEdificio(edificios);
        Log.i(TAG, "cargarEdificios compania=" + idCompania + " total=" + edificios.length);
    }

    private void cargarPisos(String idEdificio) {
        PisoDBHelper pisoDBHelper = new PisoDBHelper(ElegirUbicacionView.getContext());
        _mapPisos = (Map<Integer, PisoNuevo>) pisoDBHelper.ObtenerPiso(idEdificio);
        if (_mapPisos == null || _mapPisos.size() == 0) {
            _mapPisos = (Map<Integer, PisoNuevo>) pisoDBHelper.ObtenerTodosPisos();
            Log.w(TAG, "cargarPisos fallback general total=" + (_mapPisos != null ? _mapPisos.size() : 0));
        }
        PisoNuevo[] pisos = _mapPisos.values().toArray(new PisoNuevo[0]);
        fillSpinnerPiso(pisos);
        Log.i(TAG, "cargarPisos edificio=" + idEdificio + " total=" + pisos.length);
    }

    private void cargarOficinas(String idPiso) {
        OficinaDBHelper oficinaDBHelper = new OficinaDBHelper(ElegirUbicacionView.getContext());
        _mapOficinas = (Map<Integer, oficinaNuevo>) oficinaDBHelper.ObtenerOficina(idPiso);
        if (_mapOficinas == null || _mapOficinas.size() == 0) {
            _mapOficinas = (Map<Integer, oficinaNuevo>) oficinaDBHelper.ObtenerTodasOficinas();
            Log.w(TAG, "cargarOficinas fallback general total=" + (_mapOficinas != null ? _mapOficinas.size() : 0));
        }
        oficinaNuevo[] oficinas = _mapOficinas.values().toArray(new oficinaNuevo[0]);
        fillSpinnerOficina(oficinas);
        Log.i(TAG, "cargarOficinas piso=" + idPiso + " total=" + oficinas.length);
    }

    private AdapterView.OnItemSelectedListener onItemSpinnerListenerCompania = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (_itemSelectedUserCompania) {
                RazonNuevo razonSocialRecord = (RazonNuevo) CompaniaView.getSelectedItem();
                if (razonSocialRecord != null) {
                    cargarEdificios(razonSocialRecord.getIdRazon());
                }
            }
            _itemSelectedUserCompania = true;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private AdapterView.OnItemSelectedListener onItemSpinnerListenerEdificio = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (_itemSelectedUserEdificio) {
                EdificioNuevo edificioRecord = (EdificioNuevo) EdificioView.getSelectedItem();
                if (edificioRecord != null) {
                    cargarPisos(edificioRecord.getIdEdificio());
                }
            }
            _itemSelectedUserEdificio = true;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private AdapterView.OnItemSelectedListener onItemSpinnerListenerPiso = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (_itemSelectedUserPiso) {
                PisoNuevo pisoRecord = (PisoNuevo) PisoView.getSelectedItem();
                if (pisoRecord != null) {
                    cargarOficinas(pisoRecord.getIdPiso());
                }
            }
            _itemSelectedUserPiso = true;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private void fillTiposInventarios(EntidadTiposInventarios[] tiposInventarios) {
        TipoInventarioView.setAdapter(new ArrayAdapter<>(ElegirUbicacionView.getContext().getApplicationContext()
                , R.layout.spinner_layaout, tiposInventarios));

    }

    private void fillSpinnerRazon(RazonNuevo[] razonSocialRecords) {
        CompaniaView = findViewById(R.id.compania_toma);
        CompaniaView.setAdapter(new ArrayAdapter<>(ElegirUbicacionView.getContext().getApplicationContext()
                , R.layout.spinner_layaout, razonSocialRecords));

        if (TextUtils.isEmpty(lockedIdRazonSocial)) {
            CompaniaView.setEnabled(true);
            CompaniaView.setClickable(true);
            return;
        }

        boolean matched = false;
        for (int i = 0; i < razonSocialRecords.length; i++) {
            if (idsMatch(razonSocialRecords[i].getIdRazon(), lockedIdRazonSocial)) {
                _itemSelectedUserCompania = false; // Bloquear trigger de usuario
                CompaniaView.setSelection(i);
                CompaniaView.setEnabled(false);
                CompaniaView.setClickable(false);
                // Disparar carga de edificio manualmente ya que está bloqueado
                cargarEdificios(lockedIdRazonSocial);
                matched = true;
                break;
            }
        }
        if (!matched) {
            Log.w(TAG, "No se encontro compania bloqueada en catalogo local: " + lockedIdRazonSocial);
            CompaniaView.setEnabled(true);
            CompaniaView.setClickable(true);
        }
        Log.i(TAG, "fillSpinnerRazon locked=" + lockedIdRazonSocial + " matched=" + matched + " total=" + razonSocialRecords.length);
    }

    private void fillSpinnerEdificio(EdificioNuevo[] edificioRecords) {
        EdificioView = findViewById(R.id.edificio_toma);
        EdificioView.setAdapter(new ArrayAdapter<>(ElegirUbicacionView.getContext().getApplicationContext()
                , R.layout.spinner_layaout, edificioRecords));

        if (TextUtils.isEmpty(lockedIdEdificio)) {
            EdificioView.setEnabled(true);
            EdificioView.setClickable(true);
            return;
        }

        boolean matched = false;
        for (int i = 0; i < edificioRecords.length; i++) {
            if (idsMatch(edificioRecords[i].getIdEdificio(), lockedIdEdificio)) {
                _itemSelectedUserEdificio = false;
                EdificioView.setSelection(i);
                EdificioView.setEnabled(false);
                EdificioView.setClickable(false);
                cargarPisos(lockedIdEdificio);
                matched = true;
                break;
            }
        }
        if (!matched) {
            Log.w(TAG, "No se encontro edificio bloqueado en catalogo local: " + lockedIdEdificio);
            EdificioView.setEnabled(true);
            EdificioView.setClickable(true);
        }
        Log.i(TAG, "fillSpinnerEdificio locked=" + lockedIdEdificio + " matched=" + matched + " total=" + edificioRecords.length);
    }

    private void fillSpinnerPiso(PisoNuevo[] pisoRecords) {
        PisoView = findViewById(R.id.piso_toma);
        PisoView.setAdapter(new ArrayAdapter<>(ElegirUbicacionView.getContext().getApplicationContext()
                , R.layout.spinner_layaout, pisoRecords));

        if (TextUtils.isEmpty(lockedIdPiso)) {
            PisoView.setEnabled(true);
            PisoView.setClickable(true);
            return;
        }

        boolean matched = false;
        for (int i = 0; i < pisoRecords.length; i++) {
            if (idsMatch(pisoRecords[i].getIdPiso(), lockedIdPiso)) {
                _itemSelectedUserPiso = false;
                PisoView.setSelection(i);
                PisoView.setEnabled(false);
                PisoView.setClickable(false);
                cargarOficinas(lockedIdPiso);
                matched = true;
                break;
            }
        }
        if (!matched) {
            Log.w(TAG, "No se encontro piso bloqueado en catalogo local: " + lockedIdPiso);
            PisoView.setEnabled(true);
            PisoView.setClickable(true);
        }
        Log.i(TAG, "fillSpinnerPiso locked=" + lockedIdPiso + " matched=" + matched + " total=" + pisoRecords.length);
    }

    private void fillSpinnerOficina(oficinaNuevo[] oficinaRecords) {
        OficinaView = findViewById(R.id.oficina_toma);
        OficinaView.setAdapter(new ArrayAdapter<>(ElegirUbicacionView.getContext().getApplicationContext()
                , R.layout.spinner_layaout, oficinaRecords));

        if (TextUtils.isEmpty(lockedIdOficina)) {
            OficinaView.setEnabled(true);
            OficinaView.setClickable(true);
            txtAjusteOficina.setEnabled(true);
            return;
        }

        boolean matched = false;
        for (int i = 0; i < oficinaRecords.length; i++) {
            if (idsMatch(oficinaRecords[i].getIdOficina(), lockedIdOficina)) {
                OficinaView.setSelection(i);
                OficinaView.setEnabled(false);
                OficinaView.setClickable(false);
                txtAjusteOficina.setEnabled(false);
                matched = true;
                break;
            }
        }
        if (!matched) {
            Log.w(TAG, "No se encontro oficina bloqueada en catalogo local: " + lockedIdOficina);
            OficinaView.setEnabled(true);
            OficinaView.setClickable(true);
            txtAjusteOficina.setEnabled(true);
        }
        Log.i(TAG, "fillSpinnerOficina locked=" + lockedIdOficina + " matched=" + matched + " total=" + oficinaRecords.length);
    }
    //endregion

    public void LeerInventarioManual() {
        try {
            RazonNuevo razonSel = (RazonNuevo) CompaniaView.getSelectedItem();
            EdificioNuevo edifSel = (EdificioNuevo) EdificioView.getSelectedItem();
            PisoNuevo pisoSel = (PisoNuevo) PisoView.getSelectedItem();
            oficinaNuevo ofiSelBefore = (oficinaNuevo) OficinaView.getSelectedItem();
            Log.i(TAG, "LeerInventarioManual context{"
                    + "TakeID=" + idTake
                    + ", lockedRazon=" + lockedIdRazonSocial
                    + ", lockedEdificio=" + lockedIdEdificio
                    + ", lockedPiso=" + lockedIdPiso
                    + ", lockedOficina=" + lockedIdOficina
                    + ", selRazon=" + (razonSel != null ? razonSel.getIdRazon() : "null")
                    + ", selEdificio=" + (edifSel != null ? edifSel.getIdEdificio() : "null")
                    + ", selPiso=" + (pisoSel != null ? pisoSel.getIdPiso() : "null")
                    + ", selOficina=" + (ofiSelBefore != null ? ofiSelBefore.getIdOficina() : "null")
                    + "}");
            Object selectedOficina = OficinaView.getSelectedItem();
            if (selectedOficina == null) {
                Log.e(TAG, "LeerInventarioManual sin oficina seleccionada. adapters: companias=" + CompaniaView.getCount()
                        + ", edificios=" + EdificioView.getCount()
                        + ", pisos=" + PisoView.getCount()
                        + ", oficinas=" + OficinaView.getCount()
                        + ", tiposInventario=" + TipoInventarioView.getCount());
                Toast.makeText(getApplicationContext(), "No hay oficinas disponibles para continuar", Toast.LENGTH_LONG).show();
                return;
            }
            if (TextUtils.isEmpty(selectedOficina.toString())) {
                Toast.makeText(getApplicationContext(), "Seleccione una ubicación", Toast.LENGTH_LONG).show();
                return;
            }

            EntidadTiposInventarios entidadTiposInventarios = (EntidadTiposInventarios) TipoInventarioView.getSelectedItem();
            if (entidadTiposInventarios == null) {
                Log.e(TAG, "LeerInventarioManual sin tipo de inventario seleccionado");
                Toast.makeText(getApplicationContext(), "No hay tipo de inventario disponible", Toast.LENGTH_LONG).show();
                return;
            }
            idTipoInventario = entidadTiposInventarios.getidTipoToma();

            oficinaNuevo oficinaRecord = (oficinaNuevo) selectedOficina;
            idOficina = oficinaRecord.getIdOficina();
            Log.i(TAG, "LeerInventarioManual seleccionado oficina=" + idOficina + " tipoInventario=" + idTipoInventario);

            boolean respuesta = OfficesDBHelper.ActivosEnUbicacion(idOficina);
            if (!respuesta) {
                AlertasPersonalizadas.showAlertDialogAsk(_activity, "ALERTA", "Este sector " +
                        "no posee activos.", IrInventario, RespuestaNegativa);
                return;
            }

            Intent intent = new Intent(ElegirUbicacion_TomaFisica.this, Lectura_Inventario.class);
            intent.putExtra("IdTake", idTake);
            intent.putExtra("takeName", takeName);
            intent.putExtra("takeDescription", takeDescription);
            intent.putExtra("takeDate", takeDate);
            intent.putExtra("idOficina", idOficina);
            intent.putExtra("tipoInventario", idTipoInventario);
            startActivity(intent);

        } catch (Exception e) {
            Log.e("Activos Por Sector", e.getMessage());
            MostrarSnackBar(false, "Ha ocurrido un error, intente nuevamente");
        }
    }

    private void cargarUbicacionPorEPC(String EPC) {
        eUbicacionActivo = OfficesDBHelper.VerSectorEPC2(EPC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (Objects.isNull(eUbicacionActivo)) {
                AlertasPersonalizadas.showAlertDialog(_activity, "Atención",
                        "El tag leído no le pertenece a una oficina!" + "\n" + "" +
                                "Seleccione la ubicación manualmente o lea nuevamente el tag.");
                return;
            }
        }
        for (Map.Entry<Integer, RazonNuevo> item : _mapRazonSociales.entrySet()) {
            if (item.getValue().getIdRazon().equals(eUbicacionActivo.getIdRazonSocial())) {
                CompaniaView.setSelection(item.getKey());
            }
        }

        for (Map.Entry<Integer, EdificioNuevo> item : _mapEdificios.entrySet()) {
            if (item.getValue().getIdEdificio().equals(eUbicacionActivo.getIdEdificio())) {
                EdificioView.setSelection(item.getKey());
            }
        }

        for (Map.Entry<Integer, PisoNuevo> item : _mapPisos.entrySet()) {
            if (item.getValue().getIdPiso().equals(eUbicacionActivo.getIdPiso())) {
                PisoView.setSelection(item.getKey());
            }
        }

        for (Map.Entry<Integer, oficinaNuevo> item : _mapOficinas.entrySet()) {
            if (item.getValue().getIdOficina().equals(eUbicacionActivo.getIdOficina())) {
                OficinaView.setSelection(item.getKey());
            }
        }
    }

    //region Acción de lectura de tags con la HH
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(RFID_FLOW_TAG, "[ElegirUbicacion_TomaFisica] onPause: se conserva la sesion RFID para la siguiente pantalla.");
        rfidHandler.onPause();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        prepararSesionRfid(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // rfidHandler.onDestroy();
    }

    @Override
    public void handleTagdata(TagData[] tagData) {
        for (int index = 0; index < tagData.length; index++) {
            _lastTag = tagData[index].getTagID();
        }
    }

    public static void Message() {
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 24);
    }

    @Override
    public void handleTriggerPress(boolean pressed) {
        triggerPressed = pressed;
        if (pressed) {
            rfidHandler.performInventory();
        } else {
            rfidHandler.stopInventory();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cargarUbicacionPorEPC(_lastTag);
                    Message();
                }
            });
        }
    }

    @Override
    public Context GetContext() {
        return this;
    }

    @Override
    public void SetMessage(String Text) {
    }
    //endregion

    private Button.OnClickListener OnClickListenerIrToma = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            LeerInventarioManual();
        }
    };

    private void MostrarSnackBar(boolean tipoSnack, String mensaje) {
        if (tipoSnack) {
            _snackbar = Snackbar.make(clsnackbar, mensaje, 3000);
            _snackbar.setActionTextColor(Color.rgb(179, 179, 179));
            View snackBarView = _snackbar.getView();
            snackBarView.setBackgroundColor(Color.rgb(4, 165, 77));
        } else {
            _snackbar = Snackbar.make(clsnackbar, mensaje, 4000);
            _snackbar.setActionTextColor(Color.rgb(179, 179, 179));
            View snackBarView = _snackbar.getView();
            snackBarView.setBackgroundColor(Color.rgb(242, 59, 59));
        }
        _snackbar.show();
    }

    //region Respuestas Runnable
    final Runnable RespuestaNegativa = new Runnable() {
        @Override
        public void run() {
        }
    };

    final Runnable CerrarVentana = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };

    final Runnable IrInventario = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(ElegirUbicacion_TomaFisica.this, Lectura_Inventario.class);
            intent.putExtra("IdTake", idTake);
            intent.putExtra("takeName", takeName);
            intent.putExtra("takeDescription", takeDescription);
            intent.putExtra("takeDate", takeDate);
            intent.putExtra("idOficina", idOficina);
            intent.putExtra("tipoInventario", idTipoInventario);
            startActivity(intent);
        }
    };
    //endregion
}
