package com.example.diverscan.activeid.Inventory;

import android.support.annotation.NonNull;

public class EntidadTiposInventarios {

    String idTipoToma;
    String nombreTipoToma;
    String descripcionTipoToma;
    String fechaInicio;
    String fechaFinal;
    String estado;
    String estadoActivo;
    String idRazonSocial;
    String idEdificio;
    String idPiso;
    String idOficina;

    public EntidadTiposInventarios (String IdTipoToma, String NombreTipoToma, String DescripcionTipoToma){

        this.idTipoToma = IdTipoToma;
        this.nombreTipoToma = NombreTipoToma;
        this.descripcionTipoToma = DescripcionTipoToma;
    }

    public EntidadTiposInventarios() {
        // Constructor vacío, se pueden inicializar valores por defecto aquí si es necesario
    }

    public String getidTipoToma(){
        return idTipoToma;
    }
    public String getnombreTipoToma (){ return nombreTipoToma; }
    public String getdescripcionTipoToma (){ return descripcionTipoToma; }
    public String getfechaInicio (){ return fechaInicio; }
    public String getfechaFinal (){ return fechaFinal; }
    public String getestado (){ return estado; }
    public String getestadoActivo (){ return estadoActivo; }
    public String getidRazonSocial() { return idRazonSocial; }
    public String getidEdificio() { return idEdificio; }
    public String getidPiso() { return idPiso; }
    public String getidOficina() { return idOficina; }

    public void setidTipoToma(String IdTipoToma){
        idTipoToma = IdTipoToma;
    }
    public void setnombreTipoToma (String NombreTipoToma){
        nombreTipoToma = NombreTipoToma;
    }
    public void setdescripcionTipoToma (String DescripcionTipoToma){ descripcionTipoToma = DescripcionTipoToma ;}
    public void setfechaInicio (String FechaInicio){
        fechaInicio = FechaInicio;
    }
    public void setfechaFinal (String FechaFinal){
        fechaFinal = FechaFinal;
    }
    public void setestado (String Estado){
        estado = Estado;
    }
    public void setestadoActivo (String EstadoActivo){
        estadoActivo = EstadoActivo;
    }
    public void setidRazonSocial(String IdRazonSocial) { idRazonSocial = IdRazonSocial; }
    public void setidEdificio(String IdEdificio) { idEdificio = IdEdificio; }
    public void setidPiso(String IdPiso) { idPiso = IdPiso; }
    public void setidOficina(String IdOficina) { idOficina = IdOficina; }

    @Override
    public String toString() {
        return nombreTipoToma;
    }
}
