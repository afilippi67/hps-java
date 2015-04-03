package org.hps.conditions.database;

import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Database utility methods.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
// TODO: Merge this single method into the manager class or a connection utilities class.
public final class DatabaseUtilities {

    /**
     * Do not allow instantiation.
     */
    private DatabaseUtilities() {
    }

    /**
     * Cleanup a JDBC <code>ResultSet</code> by closing it and its <code>Statement</code>
     *
     * @param resultSet The database ResultSet.
     */
    static void cleanup(final ResultSet resultSet) {
        Statement statement = null;
        try {
            statement = resultSet.getStatement();
        } catch (Exception e) {
        }
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
