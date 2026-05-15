package com.example.diverscan.activeid.Inventory;

import java.io.Serializable;
import java.util.Date;

public class Inventories implements Serializable {

    String _id;
    String TakeName;
    String TakeDescription;
    String TakeDate;
    String fechaFinal;
    String estado;
    String idRazonSocial;
    String idEdificio;
    String idPiso;
    String idOficina;

    public Inventories(String id, String takeName, String takeDescription, String takeDate) {
        this._id = id;
        this.TakeName = takeName;
        this.TakeDescription = takeDescription;
        this.TakeDate = takeDate;
    }

    public Inventories() {}

    public String getid() {
        return _id;
    }
    public String getTakeName() {
        return TakeName;
    }
    public String getTakeDescription() {
        return TakeDescription;
    }
    public String getTakeDate() {
        return TakeDate;
    }
    public String getfechaFinal() {
        return fechaFinal;
    }
    public String getEstado (){ return estado; }
    public String getidRazonSocial() { return idRazonSocial; }
    public String getidEdificio() { return idEdificio; }
    public String getidPiso() { return idPiso; }
    public String getidOficina() { return idOficina; }

    public void set_id(String id) {
        this._id = id;
    }
    public void setTakeName(String takeName) {
        TakeName = takeName;
    }
    public void setTakeDescription(String takeDescription) {
        TakeDescription = takeDescription;
    }
    public void setTakeDate(String takeDate) {
        TakeDate = takeDate;
    }
    public void setfechaFinal(String FechaFinal) {
        fechaFinal = FechaFinal;
    }
    public void setEstado(String Estado) {
        estado = Estado;
    }
    public void setidRazonSocial(String idRazonSocial) { this.idRazonSocial = idRazonSocial; }
    public void setidEdificio(String idEdificio) { this.idEdificio = idEdificio; }
    public void setidPiso(String idPiso) { this.idPiso = idPiso; }
    public void setidOficina(String idOficina) { this.idOficina = idOficina; }
}
