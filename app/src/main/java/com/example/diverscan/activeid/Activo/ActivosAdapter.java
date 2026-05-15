package com.example.diverscan.activeid.Activo;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.diverscan.activeid.R;

import java.util.ArrayList;

public class ActivosAdapter extends RecyclerView.Adapter<ActivosAdapter.ActivoViewHolder> {

    private ArrayList<EntidadActivos> listaActivos;

    public ActivosAdapter(ArrayList<EntidadActivos> listaActivos) {
        this.listaActivos = listaActivos;
    }

    @Override
    public ActivoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activo_sincronizado, parent, false);
        return new ActivoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ActivoViewHolder holder, int position) {
        EntidadActivos activo = listaActivos.get(position);
        holder.txtDescripcion.setText(activo.getDescripcion());
        holder.txtBarcode.setText("Placa: " + activo.getCodeBar());
        holder.txtSerie.setText("Serie: " + activo.getSerial());
        holder.txtMarcaModelo.setText(activo.getMarca() + " - " + activo.getModelo());
        
        String ubicacion = "";
        if (activo.getEdificio() != null) ubicacion += activo.getEdificio();
        if (activo.getPiso() != null) ubicacion += " - " + activo.getPiso();
        if (activo.getOficina() != null) ubicacion += " - " + activo.getOficina();
        holder.txtUbicacion.setText(ubicacion);
        
        holder.txtEncargado.setText("Encargado: " + activo.getAlias());
        holder.txtTag.setText("EPC: " + activo.getTag());
        holder.txtEstado.setText("Estado: " + activo.getEstadoDescripcion());
    }

    @Override
    public int getItemCount() {
        return listaActivos.size();
    }

    public static class ActivoViewHolder extends RecyclerView.ViewHolder {
        TextView txtDescripcion, txtBarcode, txtSerie, txtMarcaModelo, txtUbicacion, txtEncargado, txtTag, txtEstado;

        public ActivoViewHolder(View itemView) {
            super(itemView);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcion);
            txtBarcode = itemView.findViewById(R.id.txtBarcode);
            txtSerie = itemView.findViewById(R.id.txtSerie);
            txtMarcaModelo = itemView.findViewById(R.id.txtMarcaModelo);
            txtUbicacion = itemView.findViewById(R.id.txtUbicacion);
            txtEncargado = itemView.findViewById(R.id.txtEncargado);
            txtTag = itemView.findViewById(R.id.txtTag);
            txtEstado = itemView.findViewById(R.id.txtEstado);
        }
    }
}
