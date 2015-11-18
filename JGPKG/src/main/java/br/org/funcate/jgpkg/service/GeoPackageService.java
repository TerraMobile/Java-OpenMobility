package br.org.funcate.jgpkg.service;


import android.content.Context;

import com.augtech.geoapi.geopackage.GeoPackage;
import com.augtech.geoapi.geopackage.GpkgField;
import com.augtech.geoapi.geopackage.GpkgTable;
import com.augtech.geoapi.geopackage.ICursor;
import com.augtech.geoapi.geopackage.ISQLDatabase;
import com.augtech.geoapi.geopackage.geometry.StandardGeometryDecoder;
import com.augtech.geoapi.geopackage.table.FeaturesTable;
import com.augtech.geoapi.geopackage.table.GpkgContents;
import com.augtech.geoapi.geopackage.table.GpkgTriggers;

import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.org.funcate.dynamicforms.FormUtilities;
import br.org.funcate.extended.model.TMConfigEditableLayer;
import br.org.funcate.geopackage.AndroidSQLDatabase;
import br.org.funcate.jgpkg.exception.QueryException;

public class GeoPackageService {

    static Logger log = Logger.getAnonymousLogger();

    private static GeoPackage connect(ISQLDatabase database, boolean overwrite) {

        log.log(Level.INFO, "Connecting to GeoPackage...");

        GeoPackage geoPackage = new GeoPackage(database, overwrite);

        // Quick test to get the current contents
        if (geoPackage != null) {
            int numExist = geoPackage.getUserTables(GpkgTable.TABLE_TYPE_FEATURES).size();
            log.log(Level.INFO, "" + numExist + " feature tables in the GeoPackage");

            numExist = geoPackage.getUserTables(GpkgTable.TABLE_TYPE_TILES).size();
            log.log(Level.INFO, "" + numExist + " tile tables in the GeoPackage");
        }
        return geoPackage;

    }





    public static GeoPackage readGPKG(Context context, String gpkgFilePath)
    {
        AndroidSQLDatabase gpkgDB = new AndroidSQLDatabase(context, new File(gpkgFilePath));

        GeoPackage geoPackage = connect(gpkgDB, false);

        return geoPackage;

    }


    public static List<SimpleFeature> getGeometries(GeoPackage gpkg, String tableName, BoundingBox boundingBox) throws Exception
    {
/*        if(!gpkg.isGPKGValid(false))
        {
            throw new Exception("Invalid GeoPackage file.");
        }*/
        List<SimpleFeature> features;
        if(boundingBox==null || boundingBox.isEmpty())
            features = gpkg.getFeatures(tableName);
        else
            features = gpkg.getFeatures(tableName, boundingBox);

        return features;
    }

    public static SimpleFeature getFeatureByID(GeoPackage gpkg, String tableName, long featureID) throws Exception
    {
/*        if(!gpkg.isGPKGValid(false))
        {
            throw new Exception("Invalid GeoPackage file.");
        }*/
        GpkgTable layerTable = gpkg.getUserTable(tableName, GpkgTable.TABLE_TYPE_FEATURES);
        String whereClause = layerTable.getPrimaryKey(gpkg) + "=" + featureID;

        List<SimpleFeature> features;
        features = gpkg.getFeatures(tableName, whereClause, new StandardGeometryDecoder());

        SimpleFeature feature = null;
        if(!features.isEmpty()) feature = features.get(0);

        return feature;
    }

    public static boolean deleteFeature(GeoPackage gpkg, String tableName, long featureID) throws Exception
    {
/*        if(!gpkg.isGPKGValid(false))
        {
            throw new Exception("Invalid GeoPackage file.");
        }*/
        return gpkg.deleteFeature(tableName, featureID);
    }

    public static List<SimpleFeature> getTiles(GeoPackage gpkg, String tableName) throws Exception
    {
/*        if(!gpkg.isGPKGValid(false))
        {
            throw new Exception("Invalid GeoPackage file.");
        }*/

        List<SimpleFeature> features = gpkg.getTiles(tableName, "");

        return features;
    }

    public static List<SimpleFeature> getTiles(GeoPackage gpkg, BoundingBox box, String tableName, int zoomLevel) throws Exception
    {
/*        if(!gpkg.isGPKGValid(false))
        {
            throw new Exception("Invalid GeoPackage file.");
        }*/

        List<SimpleFeature> features = gpkg.getTiles(tableName, box, zoomLevel);

        return features;
    }

    public static byte[] getTile(GeoPackage gpkg, String tableName, int col, int row, int zoomLevel) throws Exception
    {
      /*  if(!gpkg.isGPKGValid(false))
        {
            throw new Exception("Invalid GeoPackage file.");
        }*/

        byte[] tile = gpkg.getTile(tableName, col, row, zoomLevel);

        return tile;
    }

    public static Map<String, Integer> getTilesBounds(GeoPackage gpkg, String tableName, String tableType, Integer zoomLevel) throws Exception {
/*        if (!gpkg.isGPKGValid(true)) {
            throw new Exception("Invalid GeoPackage file.");
        }*/

        ICursor icursor = gpkg.getUserTable(tableName, tableType).query(gpkg, new String[]{"max(tile_row), min(tile_row), max(tile_column), min(tile_column)"}, "zoom_level="+zoomLevel.intValue());

        Map<String, Integer> ranges = new HashMap<String, Integer>();

        if (icursor.moveToNext()) {
/*
            int minZoomLevel = icursor.getInt(icursor.getColumnIndex("min(zoom_level)"));
            int maxZoomLevel = icursor.getInt(icursor.getColumnIndex("max(zoom_level)"));
*/
            int minTileRow = icursor.getInt(icursor.getColumnIndex("min(tile_row)"));
            int maxTileRow = icursor.getInt(icursor.getColumnIndex("max(tile_row)"));
            int minTileColumn = icursor.getInt(icursor.getColumnIndex("min(tile_column)"));
            int maxTileColumn = icursor.getInt(icursor.getColumnIndex("max(tile_column)"));
/*
            ranges.put("minZoomLevel", minZoomLevel);
            ranges.put("maxZoomLevel", maxZoomLevel);
*/
            ranges.put("minTileRow", minTileRow);
            ranges.put("maxTileRow", maxTileRow);
            ranges.put("minTileColumn", minTileColumn);
            ranges.put("maxTileColumn", maxTileColumn);
        }


        return ranges;
    }

    private static GpkgTable getGpkgTable(GeoPackage gpkg, String gpkgTableName) {
        return gpkg.getSystemTable(gpkgTableName);
    }

    private static GpkgContents getGpkgContents(GeoPackage gpkg) {

        String gpkgTableName = "gpkg_contents";

        return (GpkgContents) getGpkgTable(gpkg,gpkgTableName);
    }

    /**
     * Load the configuration to create form to editable layer.
     * @param gpkg, the GeoPackage reference
     * @return The id of the editable layer and the JSON configuration into TMConfigEditableLayer object
     * @throws Exception
     */
    public static TMConfigEditableLayer getTMConfigEditableLayer(GeoPackage gpkg) throws QueryException {

        String tableName = "tm_layer_form";
        String[] columns = new String[3];
        columns[0] = "gpkg_layer_identify";
        columns[1] = "tm_form";
        columns[2] = "tm_media_table";

        TMConfigEditableLayer tmConfigEditableLayer=new TMConfigEditableLayer();
        ICursor c=null;
        try {
            c = gpkg.getDatabase().doQuery(tableName, columns, null);
            while (c.moveToNext()) {
                String id = c.getString(0);
                String json = c.getString(1);
                String mediaTable = c.getString(2);
                boolean isValid = GeoPackageService.validateEditableLayerConfiguration(gpkg, id, json, mediaTable);
                if(isValid) {
                    json = removeUnprintableCharacters(json);
                    tmConfigEditableLayer.addConfig(id, json, mediaTable);
                    GeoPackageService.removeTriggersOfTable(gpkg, id);
                }
            }
            c.close();
        }catch (Exception e) {
            if(c!=null && c instanceof ICursor) c.close();
            throw new QueryException(e.getMessage());
        }

        return tmConfigEditableLayer;
    }

    public static ArrayList<GpkgField> getLayerFields(GeoPackage gpkg, String gpkgTableName){

        ArrayList<GpkgField> fields = new ArrayList<GpkgField>();

        FeaturesTable userTable = (FeaturesTable) gpkg.getUserTable(gpkgTableName, GpkgTable.TABLE_TYPE_FEATURES);

        Collection<GpkgField> cFields = userTable.getFields();

        Iterator<GpkgField> it = cFields.iterator();

        while (it.hasNext()) {
            fields.add(it.next());
        }

        return fields;
    }

    public static SimpleFeatureType getLayerFeatureType(GeoPackage gpkg, String gpkgTableName) throws QueryException {

        FeaturesTable userTable = (FeaturesTable) gpkg.getUserTable(gpkgTableName, GpkgTable.TABLE_TYPE_FEATURES);
        SimpleFeatureType featureType=null;
        try {
            featureType = userTable.getSchema();
        }catch (Exception e){
            throw new QueryException(e.getMessage());
        }

        return featureType;
    }

    /**
     * Write a Feature and your related medias on database.
     * @param gpkg, The Database representing the GeoPackage.
     * @param mediaTable, The name of the media table.
     * @param feature, The SimpleFeature instance.
     * @param databaseImages, The media's list to be kept on database. If no medias to keep, use null.
     * @param insertImages, The media's list to be inserted. If no medias to insert, use null.
     * @throws QueryException
     */
    public static void writeLayerFeature(GeoPackage gpkg, String mediaTable, SimpleFeature feature, ArrayList<String> databaseImages, ArrayList<Object> insertImages) throws QueryException {

        try {
            long insertedRows=0;
            int removedRows = 0;
            if(feature.getID()!=null) {

                String strFeatureID = feature.getID().replaceAll(feature.getFeatureType().getTypeName(),"");
                long featureID = new Long(strFeatureID).longValue();

                if(gpkg.updateFeature(feature)) {

                    if (mediaTable != null && !mediaTable.isEmpty()) {
                        removedRows = gpkg.removeMedias(mediaTable, databaseImages, featureID);// TODO: write the number of removed medias on log

                        if (insertImages!=null && !insertImages.isEmpty()) {
                            insertedRows = gpkg.insertMedias(mediaTable, featureID, insertImages);// TODO: write the number of inserted medias on log
                        }
                    }
                }
            }else {
                long featureID = gpkg.insertFeature(feature);
                // if featureID == -1 then there is insertion process failure.
                if(featureID >=0) {
                    if(mediaTable!=null && !mediaTable.isEmpty() && insertImages!=null && !insertImages.isEmpty())
                        insertedRows = gpkg.insertMedias(mediaTable, featureID, insertImages);// TODO: write the number of inserted medias on log
                }
            }
        }catch (Exception e){
            throw new QueryException(e.getMessage());
        }
    }

    public static Map<String, Object> getMedias(GeoPackage gpkg, String mediaTable, long featureID) throws QueryException {
        Map<String, Object> medias = null;
        try {
            if(featureID>=0) {

                if(mediaTable!=null && !mediaTable.isEmpty()) {
                    medias = gpkg.getMedias(mediaTable, featureID);
                }
            }

        }catch (Exception e){
            throw new QueryException(e.getMessage());
        }
        return medias;
    }

    public static ArrayList<ArrayList<GpkgField>> getGpkgFieldsContents(GeoPackage gpkg, String[] columns, String whereClause) throws QueryException {


        GpkgContents contents = getGpkgContents(gpkg);

        if(columns==null)
        {
            columns=new String[1];
            columns[0]="*";
        }
        if(whereClause==null)
        {
            whereClause="";
        }

        ICursor c = contents.query(gpkg, columns, whereClause);
        ArrayList<ArrayList<GpkgField>> records=new ArrayList<ArrayList<GpkgField>>();
        while (c.moveToNext()){
            ArrayList<GpkgField> aRecord=new ArrayList<GpkgField>(c.getColumnCount());
            for (int i = 0; i < c.getColumnCount(); i++) {
                GpkgField field = (contents.getField(c.getColumnName(i))).clone();
                field.setValue(getCursorValue(c, i, field.getFieldType()));
                aRecord.add(field);
            }
            records.add(aRecord);
        }
        return records;
    }

    /**
     * Get column value by field type, test if type is valid
     * @param cursor Open query cursor
     * @param position position on the column
     * @param fieldType Type of the column (DOUBLE, TEXT, INTEGER, BOOLEAN and BYTE)
     * @return A generic object of the field type
     */
    private static Object getCursorValue(ICursor cursor, int position, String fieldType) throws QueryException {
        Object value=null;

        try
        {
            if("DOUBLE".equalsIgnoreCase(fieldType))
            {
                value = (Double)cursor.getDouble(position);
            } else if("TEXT".equalsIgnoreCase(fieldType))
            {
                value = (String)cursor.getString(position);
            } else if("INTEGER".equalsIgnoreCase(fieldType))
            {
                value = (Integer)cursor.getInt(position);
            } else if("BOOLEAN".equalsIgnoreCase(fieldType))
            {
                value = (Boolean) cursor.getBoolean(position);
            } else if("BYTE".equalsIgnoreCase(fieldType))
            {
                value = (byte[])cursor.getBlob(position);
            } else if("DATETIME".equalsIgnoreCase(fieldType))
            {
                //TODO: CAST TO DATE USING SIMPLEDATEFORMAT
                value = (String)cursor.getString(position);
            }
            return value;
        }
        catch(ClassCastException e)
        {
            e.printStackTrace();
            throw new QueryException("Invalid type cast while getting cursor values.");
        }


    }

    public static boolean updateFeature(GeoPackage gpkg, SimpleFeature feature) throws Exception {
        boolean exec=false;
        if(feature.getID()!=null) {
            exec = gpkg.updateFeature(feature);
        }
        return exec;
    }

    public static boolean removeTriggersOfTable(GeoPackage gpkg, String tableName) throws Exception {
        boolean exec=false;
        if(!gpkg.getDatabase().hasRTreeEnabled()) {

            String stmt = MessageFormat.format(GpkgTriggers.SELECT_ALL_SPATIAL_TRIGGERS_TO_ONE_TABLE, tableName);

            ICursor c = gpkg.getDatabase().doRawQuery(stmt);

            while (c.moveToNext()) {
                String triggerName = c.getString(0);
                if(!triggerName.isEmpty()) {
                    String dropTrigger = MessageFormat.format(GpkgTriggers.DROP_SPATIAL_TRIGGERS, triggerName);
                    gpkg.getDatabase().execSQL(dropTrigger);
                    exec=true;
                }
            }
            c.close();
        }

        return exec;
    }

    private static boolean validateEditableLayerConfiguration(GeoPackage gpkg, String layerName, String jsonForm, String mediaTable) throws Exception {
        boolean isValid=false;

        if(layerName==null || layerName.isEmpty() || jsonForm==null || jsonForm.isEmpty()) return false;

        String VERIFY_STATEMENT = "SELECT name FROM sqlite_master WHERE type = ''{0}'' AND tbl_name = ''{1}''";
        String stmt = MessageFormat.format(VERIFY_STATEMENT, "table", layerName);

        // validate if layer table exists on geopackage
        ICursor c = gpkg.getDatabase().doRawQuery(stmt);
        if (c.moveToFirst()) {
            String tableName = c.getString(0);
            c.close();
            if(tableName==null || tableName.isEmpty()) {
                return false;
            }else if(tableName.equals(layerName)) {
                isValid=true;
            }
        }else {
            c.close();
            return false;
        }

        // validate if picture field exists on json
        CharSequence cs = FormUtilities.TYPE_PICTURES;
        if(jsonForm.contains(cs)) {

            // validate if registered name of the media table is valid
            if (mediaTable == null || mediaTable.isEmpty()) {

                mediaTable = layerName + "_picture_data";
                GeoPackageService.createMediaTable(gpkg, layerName, mediaTable);
                isValid = true;

            } else {

                stmt = MessageFormat.format(VERIFY_STATEMENT, "table", mediaTable);
                // validate if media table exists on geopackage
                c = gpkg.getDatabase().doRawQuery(stmt);
                if (c.moveToFirst()) {
                    String tableName = c.getString(0);
                    c.close();
                    if(tableName==null || tableName.isEmpty() || !tableName.equals(mediaTable)) {
                        GeoPackageService.createMediaTable(gpkg, layerName, mediaTable);
                        isValid = true;
                    }
                } else {
                    c.close();
                }
            }
        }

        return isValid;
    }

    private static String removeUnprintableCharacters(String str) {

        StringBuffer buf = new StringBuffer();
        Matcher m = Pattern.compile("\\\\u([0-9A-Fa-f]{4})").matcher(str);
        while (m.find()) {
            try {
                int cp = Integer.parseInt(m.group(1), 16);
                String rep = "";
                // Replace invisible control characters and unused code points
                switch (Character.getType(cp))
                {
                    case Character.CONTROL:     // \p{Cc}
                    case Character.FORMAT:      // \p{Cf}
                    case Character.PRIVATE_USE: // \p{Co}
                    case Character.SURROGATE:   // \p{Cs}
                    case Character.UNASSIGNED:  // \p{Cn}
                        m.appendReplacement(buf, rep);
                        break;
                    default:
                        char[] chars = Character.toChars(cp);
                        rep = new String(chars);
                        m.appendReplacement(buf, rep);
                        break;
                }

            } catch (NumberFormatException e) {
                System.err.println("Confused: " + e);
            }
        }
        m.appendTail(buf);
        str = buf.toString();
        return str;
    }

    private static void createMediaTable(GeoPackage gpkg, String layerName, String mediaTable) throws Exception {

        FeaturesTable userTable = (FeaturesTable) gpkg.getUserTable(layerName, GpkgTable.TABLE_TYPE_FEATURES);
        String layerPK = userTable.getPrimaryKey(gpkg);

        String mediaTableStmt = "CREATE TABLE IF NOT EXISTS '"+mediaTable+"' ("+
                " PK_UID INTEGER PRIMARY KEY AUTOINCREMENT,"+
                " feature_id INTEGER NOT NULL,"+
                " picture BLOB,"+
                " picture_mime_type TEXT,"+
                " CONSTRAINT fk_feature_id FOREIGN KEY (feature_id) REFERENCES "+layerName+"("+layerPK+") ON DELETE CASCADE);";

        gpkg.getDatabase().execSQL(mediaTableStmt);

        String updateTmLayerFormConfigStmt = "UPDATE tm_layer_form SET tm_media_table='"+mediaTable+"' WHERE gpkg_layer_identify='"+layerName+"';";

        gpkg.getDatabase().execSQL(updateTmLayerFormConfigStmt);
    }
}
