package org.hps.conditions.svt;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.AbstractConditionsDatabaseObject.FieldValueMap;
import org.hps.conditions.ConditionsObject;
import org.hps.conditions.ConditionsObject.ConditionsObjectException;
import org.hps.conditions.ConditionsRecord;
import org.hps.conditions.ConditionsTableMetaData;
import org.hps.conditions.ConnectionManager;
import org.hps.conditions.DatabaseConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class creates a {@link SvtGainCollection} from the conditions database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtGainConverter extends DatabaseConditionsConverter<SvtGainCollection> {
    
    /**
     * Class constructor.
     */
    public SvtGainConverter() {
    }

    /**
     * Get the SVT channel constants for this run by named set.
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     * @return The channel constants data.
     */
    public SvtGainCollection getData(ConditionsManager manager, String name) {
        
        // Get the ConditionsRecord with the meta-data, which will use the current run number from the manager.
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);
               
        // Get the table name, field name, and field value defining the applicable conditions.
        String tableName = record.getTableName();
        String fieldName = record.getFieldName();
        int collectionId = record.getFieldValue();
                
        // Objects for building the return value.
        //SvtGainCollection collection = new SvtGainCollection();
        ConditionsTableMetaData tableMetaData = _objectFactory.getTableRegistry().getTableMetaData(tableName);
        SvtGainCollection collection = 
                new SvtGainCollection(tableMetaData, collectionId, true); 
        
        // Get the connection manager.
        ConnectionManager connectionManager = ConnectionManager.getConnectionManager();
                                                                                            
        // Construct the query to find matching calibration records using the ID field.
        String query = "SELECT svt_channel_id, gain, offset FROM "
                + tableName + " WHERE " + fieldName + " = " + collectionId
                + " ORDER BY svt_channel_id ASC";
            
        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);
               
        try {
            // Loop over the gain records.            
            while(resultSet.next()) {         
                
                // Setup conditions object.
                FieldValueMap fieldValues = new FieldValueMap();
                fieldValues.put("svt_channel_id", resultSet.getInt(1));
                fieldValues.put("gain", resultSet.getDouble(2));
                fieldValues.put("offset", resultSet.getDouble(3));
                ConditionsObject newObject = _objectFactory.createObject(
                        SvtGain.class, tableName, -1, fieldValues);
                
                // Add to collection.
                collection.add(newObject);
            }            
        } catch (SQLException x1) {
            throw new RuntimeException("Database error.", x1);
        } catch (ConditionsObjectException x2) {
            throw new RuntimeException("Error converting to SvtGain object.", x2);
        }
        
        // Return collection of gain objects to caller.
        return collection;
    }

    /**
     * Get the type handled by this converter.     
     * @return The type handled by this converter.
     */
    public Class<SvtGainCollection> getType() {
        return SvtGainCollection.class;
    }        
}