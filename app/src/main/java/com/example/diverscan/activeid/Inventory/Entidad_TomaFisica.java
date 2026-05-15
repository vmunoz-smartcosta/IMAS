package com.example.diverscan.activeid.Inventory;

import java.io.Serializable;

public class Entidad_TomaFisica implements Serializable {
     String IdToma;
     String TakeDate;
     String TakeDescription;
     String TakeName;
     String TakeStatus;
     String idRazonSocial;
     String idEdificio;
     String idPiso;
     String idOficina;

     public Entidad_TomaFisica(String idToma, String takeDate, String takeDescription, String takeName, String takeStatus,
                               String idRazonSocial, String idEdificio, String idPiso, String idOficina){
          this.IdToma = idToma;
          this.TakeDate= takeDate;
          this.TakeDescription = takeDescription;
          this.TakeName = takeName;
          this.TakeStatus = takeStatus;
          this.idRazonSocial = idRazonSocial;
          this.idEdificio = idEdificio;
          this.idPiso = idPiso;
          this.idOficina = idOficina;
     }

    public String getIdToma() {
        return IdToma;
    }

    public String getTakeDate() {
        return TakeDate;
    }

    public String getTakeDescription() {
        return TakeDescription;
    }

    public String getTakeName() {
        return TakeName;
    }

    public String getTakeStatus() {
        return TakeStatus;
    }

    public void setIdToma(String idToma) {
        IdToma = idToma;
    }

    public void setTakeDate(String takeDate) {
        TakeDate = takeDate;
    }

    public void setTakeDescription(String takeDescription) {
        TakeDescription = takeDescription;
    }

    public void setTakeName(String takeName) {
        TakeName = takeName;
    }

    public void setTakeStatus(String takeStatus) {
        TakeStatus = takeStatus;
    }

    public String getIdRazonSocial() {
        return idRazonSocial;
    }

    public void setIdRazonSocial(String idRazonSocial) {
        this.idRazonSocial = idRazonSocial;
    }

    public String getIdEdificio() {
        return idEdificio;
    }

    public void setIdEdificio(String idEdificio) {
        this.idEdificio = idEdificio;
    }

    public String getIdPiso() {
        return idPiso;
    }

    public void setIdPiso(String idPiso) {
        this.idPiso = idPiso;
    }

    public String getIdOficina() {
        return idOficina;
    }

    public void setIdOficina(String idOficina) {
        this.idOficina = idOficina;
    }
}
