package com.example.diverscan.activeid.Activo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.diverscan.activeid.R;
import com.example.diverscan.activeid.sqlite.AssetsDBHelper;

import java.util.ArrayList;

public class VerActivosActivity extends AppCompatActivity {

    private RecyclerView recyclerActivos;
    private ActivosAdapter adapter;
    private AssetsDBHelper assetsDBHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_activos);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Activos Sincronizados");
        }

        recyclerActivos = (RecyclerView) findViewById(R.id.recyclerActivos);
        recyclerActivos.setLayoutManager(new LinearLayoutManager(this));

        assetsDBHelper = new AssetsDBHelper(this);
        cargarActivos();
    }

    private void cargarActivos() {
        try {
            // Consulta para obtener todos los activos
            String query = "SELECT * FROM Activos";
            ArrayList<EntidadActivos> listaActivos = assetsDBHelper.CargarActivos(query);

            if (listaActivos != null && !listaActivos.isEmpty()) {
                adapter = new ActivosAdapter(listaActivos);
                recyclerActivos.setAdapter(adapter);
            } else {
                Toast.makeText(this, "No se encontraron activos sincronizados.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al cargar activos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
