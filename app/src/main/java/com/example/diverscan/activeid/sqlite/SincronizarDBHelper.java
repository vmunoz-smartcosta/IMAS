package com.example.diverscan.activeid.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.diverscan.activeid.Activo.EntidadActivos;
import com.example.diverscan.activeid.Activo.EntidadCategoriaActivos;
import com.example.diverscan.activeid.AssetStatus.EntidadAssetStatus;
import com.example.diverscan.activeid.Employees.EntidadEmployees;
import com.example.diverscan.activeid.Inventory.EntidadEdificios;
import com.example.diverscan.activeid.Inventory.EntidadOficina2;
import com.example.diverscan.activeid.Inventory.EntidadPisos;
import com.example.diverscan.activeid.Inventory.EntidadRazonSocial;
import com.example.diverscan.activeid.Inventory.EntidadTiposInventarios;
import com.example.diverscan.activeid.Inventory.EntidadUsuarios;
import com.example.diverscan.activeid.Inventory.Entidad_TomaDetalle;
import com.example.diverscan.activeid.Inventory.Entidad_TomaFisica;
import com.example.diverscan.activeid.Roles.EntidadDatosRol;
import com.example.diverscan.activeid.Tags.EntidadTags;
import com.example.diverscan.activeid.Tags.EntidadTiposTags;

import java.util.ArrayList;
import java.util.List;

public class SincronizarDBHelper  extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "Test_ActiveId_v1.db";
    private static final int DATABASE_VERSION = 4;

    public SincronizarDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {}

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i2) {}

    //****************************************************************************************************
    //Tabla TipoInventario
    public boolean InsertOrReplaceTipoInventario(ArrayList<EntidadTiposInventarios> tiposInventarios) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            Log.i("SincronizarDBHelper", "InsertOrReplaceTipoInventario inicio. total=" + (tiposInventarios != null ? tiposInventarios.size() : 0));
            // Verificar si las columnas 'fechaInicio', 'fechaFinal' y 'estado' existen
            Cursor cursor = db.rawQuery("PRAGMA table_info(TipoTomaInventario);", null);
            boolean fechaInicioExists = false;
            boolean fechaFinalExists = false;
            boolean estadoExists = false;
            boolean estadoActivoExists = false;
            boolean idRazonSocialExists = false;
            boolean idEdificioExists = false;
            boolean idPisoExists = false;
            boolean idOficinaExists = false;

            while (cursor.moveToNext()) {
                String columnName = cursor.getString(cursor.getColumnIndex("name"));
                if (columnName.equals("fechaInicio")) {
                    fechaInicioExists = true;
                }
                if (columnName.equals("estado")) {
                    estadoExists = true;
                }
                if (columnName.equals("fechaFinal")) {
                    fechaFinalExists = true;
                }
                if (columnName.equals("estadoActivo")) {
                    estadoActivoExists = true;
                }
                if (columnName.equals("idRazonSocial")) {
                    idRazonSocialExists = true;
                }
                if (columnName.equals("idEdificio")) {
                    idEdificioExists = true;
                }
                if (columnName.equals("idPiso")) {
                    idPisoExists = true;
                }
                if (columnName.equals("idOficina")) {
                    idOficinaExists = true;
                }
            }
            cursor.close();

            // Si las columnas no existen, agregarlas
            if (!fechaInicioExists) {
                db.execSQL("ALTER TABLE TipoTomaInventario ADD COLUMN fechaInicio TEXT;");
            }
            if (!fechaFinalExists) {
                db.execSQL("ALTER TABLE TipoTomaInventario ADD COLUMN fechaFinal TEXT;");
            }
            if (!estadoExists) {
                db.execSQL("ALTER TABLE TipoTomaInventario ADD COLUMN estado TEXT;");
            }
            if (!estadoActivoExists) {
                db.execSQL("ALTER TABLE TipoTomaInventario ADD COLUMN estadoActivo TEXT;");
            }
            if (!idRazonSocialExists) {
                db.execSQL("ALTER TABLE TipoTomaInventario ADD COLUMN idRazonSocial TEXT;");
            }
            if (!idEdificioExists) {
                db.execSQL("ALTER TABLE TipoTomaInventario ADD COLUMN idEdificio TEXT;");
            }
            if (!idPisoExists) {
                db.execSQL("ALTER TABLE TipoTomaInventario ADD COLUMN idPiso TEXT;");
            }
            if (!idOficinaExists) {
                db.execSQL("ALTER TABLE TipoTomaInventario ADD COLUMN idOficina TEXT;");
            }

            // Inserta o reemplaza los datos en la tabla
            for (EntidadTiposInventarios item : tiposInventarios) {
                Log.i("SincronizarDBHelper", "TipoSQLite IN{"
                        + "idTipoToma=" + item.getidTipoToma()
                        + ", nombre=" + item.getnombreTipoToma()
                        + ", estado=" + item.getestado()
                        + ", idRazonSocial=" + item.getidRazonSocial()
                        + ", idEdificio=" + item.getidEdificio()
                        + ", idPiso=" + item.getidPiso()
                        + ", idOficina=" + item.getidOficina()
                        + "}");
                String query = "INSERT OR REPLACE INTO TipoTomaInventario(_id, Nombre, Descripcion, fechaInicio, fechaFinal, estado, estadoActivo, idRazonSocial, idEdificio, idPiso, idOficina) " +
                        "VALUES('" + item.getidTipoToma() + "','" + item.getnombreTipoToma() + "','" + item.getdescripcionTipoToma() + "'," +
                        "'" + item.getfechaInicio() + "','" + item.getfechaFinal() + "','" + item.getestado() + "','" + item.getestadoActivo() + "'," +
                        "'" + item.getidRazonSocial() + "','" + item.getidEdificio() + "','" + item.getidPiso() + "','" + item.getidOficina() + "')";
                db.execSQL(query);
            }

            db.setTransactionSuccessful();
        } catch (Exception ex) {
            Log.w("myApp", "Error 22 " + ex.toString() + " " + ex.getStackTrace());
            return false;
        } finally {
            db.endTransaction();
            return true;
        }
    }

    //****************************************************************************************************

    //****************************************************************************************************

    public String ObtenerEstadoActivoPorTipo(String idTipo) {
        String estadoActivo = "";
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            String query = "SELECT estadoActivo FROM TipoTomaInventario WHERE _id = '" + idTipo + "'";
            Cursor cursor = db.rawQuery(query, null);
            if (cursor != null && cursor.moveToFirst()) {
                estadoActivo = cursor.getString(0);
                cursor.close();
            }
        } catch (Exception ex) {
            Log.e("SincronizarDBHelper", "Error en ObtenerEstadoActivoPorTipo: " + ex.getMessage());
        }
        return estadoActivo;
    }

    public List<EntidadAssetStatus> ObtenerAssetStatusCatalogo() {
        List<EntidadAssetStatus> estados = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            String query = "SELECT _id, Name, Description FROM AssetStatus ORDER BY Name";
            Cursor cursor = db.rawQuery(query, null);
            while (cursor != null && cursor.moveToNext()) {
                estados.add(new EntidadAssetStatus(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2)
                ));
            }
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        } catch (Exception ex) {
            Log.e("SincronizarDBHelper", "Error en ObtenerAssetStatusCatalogo: " + ex.getMessage());
        }
        return estados;
    }

    public boolean InsertOrReplaceTomaDetalle(ArrayList<Entidad_TomaDetalle> tomaDetalles){
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try{
            for (Entidad_TomaDetalle item: tomaDetalles){
                String query="INSERT OR REPLACE INTO TomaFisicaDetalle(idTakeDetail, FK_TomaFisica,EPC,DateRead) " +
                        "VALUES('"+item.getIdTakeDetail()+"','"+item.getFk_TomaFisica()+"','"+item.getepc()+"','"+item.getDateRead()+"')";
                db.execSQL(query);
            }
            db.setTransactionSuccessful();
        }catch (Exception ex){
            Log.w("myApp", "Error 22" + ex.toString()+" "+ex.getStackTrace());
            return false;
        }finally {
            db.endTransaction();
            return true;
        }
    }

    //****************************************************************************************************

    public boolean InsertOrReplaceRazones(ArrayList<EntidadRazonSocial> razon){

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {

            for (EntidadRazonSocial item: razon) {

                String query="INSERT OR REPLACE INTO RazonSocial(_id, Nombre) " +
                        "VALUES('"+item.getIdRazon()+"','"+item.getNombreRazon()+"')";
                db.execSQL(query);
            }

            db.setTransactionSuccessful();

        }catch (Exception ex){

            Log.w("myApp", "Error 22 " +ex.toString()+ " "+ex.getStackTrace());
            return false;
        } finally {
            db.endTransaction();
            return true;
        }
    }

    //****************************************************************************************************

    public boolean InsertOrReplaceEdificios(ArrayList<EntidadEdificios> edificio){

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try{
            for (EntidadEdificios item: edificio) {

                String query="INSERT OR REPLACE INTO Edificios(_id,Nombre, idRazonSocial, RazonSocial) " +
                        "VALUES('"+item.getIdEdificio()+"','"+item.getNombreEdificio()+"','"+item.getIdRazonSocial()+"','"
                +item.getRazonSocial()+"')";
                db.execSQL(query);
            }

            db.setTransactionSuccessful();

        }catch (Exception ex){

            Log.w("myApp","Error 22" +ex.toString()+ " " +ex.getStackTrace());
            return false;
        }finally{
            db.endTransaction();
            return true;
        }
    }

    //****************************************************************************************************

     public boolean InsertOrReplacePisos (ArrayList<EntidadPisos> piso){

         SQLiteDatabase db = this.getWritableDatabase();
         db.beginTransaction();

         try{
             for (EntidadPisos item: piso){

                 String query = "INSERT OR REPLACE INTO Pisos (_id, Nombre, idEdificio, Edificio) " +
                         "VALUES ('"+item.getIdPiso()+"','"+item.getNombrePiso()+"','"+item.getIdEdificio()+"','"
                 +item.getEdificio()+"')";
                 db.execSQL(query);
             }

             db.setTransactionSuccessful();

         }catch (Exception ex){
             Log.w("myApp","Error 22" +ex.toString()+ " " +ex.getStackTrace());
             return false;

         }finally {
             db.endTransaction();
             return true;
         }
     }

    //****************************************************************************************************

    public boolean InsertOrReplaceOficinas (ArrayList<EntidadOficina2> oficina){

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try{
            for (EntidadOficina2 item: oficina){

                String query = "INSERT OR REPLACE INTO Oficina (_id, Nombre, idPiso, Piso, Tag) " +
                        "VALUES ('"+item.getIdOficina()+"','"+item.getNombreOficina()+"','"+item.getIdPiso()+"','"
                        +item.getPiso()+"','"+item.getIdTag()+"')";
                db.execSQL(query);
            }

            db.setTransactionSuccessful();

        }catch (Exception ex){
            Log.w("myApp","Error 22" +ex.toString()+ " " +ex.getStackTrace());
            return false;

        }finally {
            db.endTransaction();
            return true;
        }
    }

    //****************************************************************************************************

    public boolean InsertOrReplaceTags(ArrayList<EntidadTags> tags){
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try{
            for(EntidadTags item: tags){
                String query = "INSERT OR REPLACE INTO Tags (_id, EPC, IdTipoTag)" +
                        "Values ('"+item.getTagSysId()+"','"+item.getTagID()+"','"+item.getTagTypeSysId()+"')";
                db.execSQL(query);
            }
            db.setTransactionSuccessful();
        }catch (Exception ex){
            Log.w("myApp", "Error 22" + ex.toString()+" "+ ex.getStackTrace());
            return false;
        }finally {
            db.endTransaction();
            return true;

        }
    }

    //****************************************************************************************************

    public boolean InsertOrReplaceTipoTags(ArrayList<EntidadTiposTags> tags){
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try{
            for(EntidadTiposTags item: tags){
                String query = "INSERT OR REPLACE INTO tipoTags (IdTipoTag, code,  name, description, category)" +
                        "Values ('"+item.getTagTypeSysId()+"','"+item.getCode()+"','"+item.getName()+"','"+item.getDescription()+"','"+item.getCategory()+"')";
                db.execSQL(query);
            }
            db.setTransactionSuccessful();
        }catch (Exception ex){
            Log.w("myApp", "Error 22" + ex.toString()+" "+ ex.getStackTrace());
            return false;
        }finally {
            db.endTransaction();
            return true;

        }
    }

    //****************************************************************************************************

    public boolean InsertOrReplaceTomaFisica(ArrayList<Entidad_TomaFisica> tomafisicas){
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try{
            Log.i("SincronizarDBHelper", "InsertOrReplaceTomaFisica inicio. total=" + (tomafisicas != null ? tomafisicas.size() : 0));
            String sql = "INSERT OR REPLACE INTO Tomas(_id, TakeName, TakeDescription, TakeDate, TakeStatus) VALUES(?, ?, ?, ?, ?)";
            android.database.sqlite.SQLiteStatement statement = db.compileStatement(sql);
            for(Entidad_TomaFisica item: tomafisicas){
                Log.i("SincronizarDBHelper", "TomaSQLite IN{"
                        + "IdToma=" + item.getIdToma()
                        + ", TakeName=" + item.getTakeName()
                        + ", TakeStatus=" + item.getTakeStatus()
                        + ", idRazonSocial=" + item.getIdRazonSocial()
                        + ", idEdificio=" + item.getIdEdificio()
                        + ", idPiso=" + item.getIdPiso()
                        + ", idOficina=" + item.getIdOficina()
                        + "}");
                statement.clearBindings();
                statement.bindString(1, item.getIdToma() == null ? "" : item.getIdToma());
                statement.bindString(2, item.getTakeName() == null ? "" : item.getTakeName());
                statement.bindString(3, item.getTakeDescription() == null ? "" : item.getTakeDescription());
                statement.bindString(4, item.getTakeDate() == null ? "" : item.getTakeDate());
                statement.bindString(5, item.getTakeStatus() == null ? "" : item.getTakeStatus());
                statement.executeInsert();
            }

            db.setTransactionSuccessful();
            return true;
        }
        catch(Exception ex){
            Log.w("myApp", "Error 22" + ex.toString()+" "+ ex.getStackTrace());
            return false;
        }
        finally {
            db.endTransaction();
        }
    }

    //****************************************************************************************************

    public Boolean doesRecordExist() {
        String q = "Select * FROM  Tomas";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(q, null);
        if (cursor.moveToFirst()) {
            return true;
        } else
            {
                return false;
            }
    }

    //****************************************************************************************************

    public boolean InsertOrReplaceActivos (ArrayList<EntidadActivos> activo){

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try{
            String sql = "INSERT OR REPLACE INTO Activos (_id, Alias, Descripcion, Departamento," +
                    " Oficina, piso, edificio, Compania, Tag, Numero, CodeBar, IdOficina, IdEstante," +
                    " IdCategoria, IdPiso, IdEdificio, IdCompania, Marca, Modelo, Serial, parentAssetSysId,EmployeeRelatedSysId,AssetStatusSysId, AnoFabricacion, Capacidad, EstadoDescripcion, EstadoConservacion) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            android.database.sqlite.SQLiteStatement statement = db.compileStatement(sql);

            for (EntidadActivos item: activo){
                statement.clearBindings();
                statement.bindString(1, item.getIdActivo());
                statement.bindString(2, item.getAlias() != null ? item.getAlias() : "");
                statement.bindString(3, item.getDescripcion() != null ? item.getDescripcion() : "");
                statement.bindString(4, item.getDepartamento() != null ? item.getDepartamento() : "");
                statement.bindString(5, item.getOficina() != null ? item.getOficina() : "");
                statement.bindString(6, item.getPiso() != null ? item.getPiso() : "");
                statement.bindString(7, item.getEdificio() != null ? item.getEdificio() : "");
                statement.bindString(8, item.getCompania() != null ? item.getCompania() : "");
                statement.bindString(9, item.getTag() != null ? item.getTag() : "");
                statement.bindString(10, item.getNumero() != null ? item.getNumero() : "");
                statement.bindString(11, item.getCodeBar() != null ? item.getCodeBar() : "");
                statement.bindString(12, item.getIdOficina() != null ? item.getIdOficina() : "");
                statement.bindString(13, item.getIdEstante() != null ? item.getIdEstante() : "");
                statement.bindString(14, item.getIdCategoria() != null ? item.getIdCategoria() : "");
                statement.bindString(15, item.getIdPiso() != null ? item.getIdPiso() : "");
                statement.bindString(16, item.getIdEdificio() != null ? item.getIdEdificio() : "");
                statement.bindString(17, item.getIdCompania() != null ? item.getIdCompania() : "");
                statement.bindString(18, item.getMarca() != null ? item.getMarca() : "");
                statement.bindString(19, item.getModelo() != null ? item.getModelo() : "");
                statement.bindString(20, item.getSerial() != null ? item.getSerial() : "");
                statement.bindString(21, item.getParentAssetSysId() != null ? item.getParentAssetSysId() : "");
                statement.bindString(22, item.getEmployeeRelatedSysId() != null ? item.getEmployeeRelatedSysId() : "");
                statement.bindString(23, item.getAssetStatusSysId() != null ? item.getAssetStatusSysId() : "");
                statement.bindString(24, item.getAnoFabricacion() != null ? item.getAnoFabricacion() : "");
                statement.bindString(25, item.getCapacidad() != null ? item.getCapacidad() : "");
                statement.bindString(26, item.getEstadoDescripcion() != null ? item.getEstadoDescripcion() : "");
                statement.bindString(27, item.getEstadoConservacion() != null ? item.getEstadoConservacion() : "");
                
                statement.execute();
            }

            db.setTransactionSuccessful();

        }catch (Exception ex){
            Log.w("myApp","Error 22" +ex.toString()+ " " +ex.getStackTrace());
            return false;

        }finally {
            db.endTransaction();
            return true;
        }
    }

    //****************************************************************************************************

    public boolean InsertOrReplaceCategoriaActivo (ArrayList<EntidadCategoriaActivos> categoriaActivos){

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try{
            for (EntidadCategoriaActivos item: categoriaActivos){

                String query = "INSERT OR REPLACE INTO categoriaActivos (assetCategorySysId, description, name)" +
                        "VALUES ('"+item.getAssetCategorySysId()+"','"+item.getDescription()+"','"+item.getName()+"')";
                db.execSQL(query);
            }

            db.setTransactionSuccessful();

        }catch (Exception ex){
            Log.w("myApp","Error 22" +ex.toString()+ " " +ex.getStackTrace());
            return false;

        }finally {
            db.endTransaction();
            return true;
        }
    }

    //****************************************************************************************************

    public boolean InsertOrReplaceUsuarios (ArrayList<EntidadUsuarios> usuario){

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try{
            for (EntidadUsuarios item: usuario){

                String query = "INSERT OR REPLACE INTO Users (_id, username, pass, email, bloqueado ," +
                        " aprobado , sesionActiva , contrasenaFallida , UltimaActividad , UltimoInicio ," +
                        " FechaBloqueo) " +
                        "VALUES ('"+item.getUserSysId()+"','"+item.getUserName()+"','"+item.getPassword()+"','"
                        +item.getEmail()+"','"+item.getBloqueado()+"','"+item.getAprobado()+"','"
                        +item.getSesionActiva()+"','"+item.getContrasenaFallida()+"','"+item.getUltimaActividad()+"','"
                        +item.getUltimoInicio()+"','"+item.getFechaBloqueo()+"')";
                db.execSQL(query);
            }

            db.setTransactionSuccessful();

        }catch (Exception ex){
            Log.w("myApp","Error 22" +ex.toString()+ " " +ex.getStackTrace());
            return false;

        }finally {
            db.endTransaction();
            return true;
        }
    }

    //****************************************************************************************************

    public boolean InsertOrReplaceRolHH (ArrayList<EntidadDatosRol> Rol){

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        int i = 1;
        try{
            for (EntidadDatosRol item: Rol){

                String query = "INSERT OR REPLACE INTO RolHH (_idRol, page, username,description,UserSysId, Esta_Bloqueado) " +
                        "VALUES ('"+item.get_idRol()+"','"+item.getPage()+"','"+item.getUsername()+"','"
                        +item.getDescription()+"','"+item.getUserSysId()+"','"+item.getEstaBloqueado()+"')";
                db.execSQL(query);
                i++;
            }

            db.setTransactionSuccessful();

        }catch (Exception ex){
            Log.w("myApp","Error 22" +ex.toString()+ " " +ex.getStackTrace());
            return false;

        }finally {
            db.endTransaction();
            return true;
        }
    }

    //****************************************************************************************************

    public boolean InsertOrReplaceAssetStatus (ArrayList<EntidadAssetStatus> assetStatusList){

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        int i = 1;
        try{
            for (EntidadAssetStatus item: assetStatusList){

                String query = "INSERT OR REPLACE INTO AssetStatus (_id, Name,Description) " +
                        "VALUES ('"+item.getId()
                        +"','"+item.getName().replaceAll("'", "''").replace("\"","''")
                        +"','"+item.getDescription().replaceAll("'", "''").replace("\"","''")+"')";
                db.execSQL(query);
                i++;
            }

            db.setTransactionSuccessful();

        }
        catch (Exception ex)
        {
            Log.w("myApp","Error 22" +ex.toString()+ " " +ex.getStackTrace());
            return false;

        }
        finally
        {
            db.endTransaction();
            return true;
        }
    }

    //****************************************************************************************************

    public boolean InsertOrReplaceEmployees (ArrayList<EntidadEmployees> employeesList){

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        int i = 1;
        try{
            for (EntidadEmployees item: employeesList){

                String query = "INSERT OR REPLACE INTO Employees (EmployeeSysId, Name,LastName,Id,CompanyIdExtern) " +
                        "VALUES ('"+item.getEmployeeSysId()
                        +"','"+item.getName().replaceAll("'", "''").replace("\"","''")
                        +"','"+item.getLastName().replaceAll("'", "''").replace("\"","''")
                        +"','"+item.getId()
                        +"','"+item.getCompanyIdExtern()+"')";
                db.execSQL(query);
                i++;
            }

            db.setTransactionSuccessful();

        }catch (Exception ex){
            Log.w("myApp","Error 22" +ex.toString()+ " " +ex.getStackTrace());
            return false;

        }finally {
            db.endTransaction();
            return true;
        }
    }
}
