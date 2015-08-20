package org.hps.record.run;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Implementation of database operations for {@link RunSummary} objects in the run database.
 *
 * @author Jeremy McCormick, SLAC
 */
public class RunSummaryDaoImpl implements RunSummaryDao {

    /**
     * SQL query strings.
     */
    private static final class RunSummaryQuery {

        /**
         * Delete by run number.
         */
        private static final String DELETE_RUN = "DELETE FROM runs WHERE run = ?";
        /**
         * Insert a record for a run.
         */
        private static final String INSERT = "INSERT INTO runs (run, start_time_utc, end_time_utc, nevents, nfiles, end_ok, created) VALUES(?, ?, ?, ?, ?, ?, NOW())";
        /**
         * Select all records.
         */
        private static final String SELECT_ALL = "SELECT * from runs";
        /**
         * Select record by run number.
         */
        private static final String SELECT_RUN = "SELECT run, start_time_utc, end_time_utc, nevents, nfiles, end_ok, run_ok, updated, created FROM runs WHERE run = ?";
        /**
         * Update information for a run.
         */
        private static final String UPDATE_RUN = "UPDATE runs SET start_time_utc, end_time_utc, nevents, nfiles, end_ok, run_ok WHERE run = ?";
    }

    /**
     * Setup class logging.
     */
    private static final Logger LOGGER = LogUtil.create(RunSummaryDaoImpl.class, new DefaultLogFormatter(), Level.ALL);

    /**
     * The database connection.
     */
    private final Connection connection;

    /**
     * The database API for EPICS data.
     */
    private EpicsDataDao epicsDataDao = null;

    /**
     * The database API for EVIO file information.
     */
    private EvioFilesDao evioFilesDao = null;

    /**
     * The database API for scaler data.
     */
    private ScalerDataDao scalerDataDao = null;

    /**
     * Create a new DAO object for run summary information.
     *
     * @param connection the database connection
     */
    public RunSummaryDaoImpl(final Connection connection) {
        // Set the connection.
        if (connection == null) {
            throw new IllegalArgumentException("The connection is null.");
        }
        this.connection = connection;

        // Setup DAO API objects for managing complex object state.
        epicsDataDao = new EpicsDataDaoImpl(this.connection);
        scalerDataDao = new ScalerDataDaoImpl(this.connection);
        evioFilesDao = new EvioFilesDaoImpl(this.connection);
    }

    /**
     * Delete a run summary from the database including its referenced objects such as EPICS data.
     *
     * @param runSummary the run summary to delete
     */
    @Override
    public void deleteFullRunSummary(final RunSummary runSummary) {
        // Delete EPICS log.
        epicsDataDao.deleteEpicsData(runSummary.getRun());

        // Delete scaler data.
        scalerDataDao.deleteScalerData(runSummary.getRun());

        // Delete file list.
        evioFilesDao.deleteEvioFiles(runSummary.getRun());

        // Finally delete the run summary information.
        this.deleteRunSummary(runSummary.getRun());
    }

    /**
     * Delete a run summary by run number.
     *
     * @param run the run number
     */
    @Override
    public void deleteRunSummary(final int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(RunSummaryQuery.DELETE_RUN);
            preparedStatement.setInt(1, run);
            preparedStatement.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Delete a run summary but not its objects.
     *
     * @param runSummary the run summary object
     */
    @Override
    public void deleteRunSummary(final RunSummary runSummary) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(RunSummaryQuery.DELETE_RUN);
            preparedStatement.setInt(1, runSummary.getRun());
            preparedStatement.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get the list of run numbers.
     *
     * @return the list of run numbers
     */
    @Override
    public List<Integer> getRuns() {
        final List<Integer> runs = new ArrayList<Integer>();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = this.connection.prepareStatement("SELECT distinct(run) FROM runs ORDER BY run");
            final ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                final Integer run = resultSet.getInt(1);
                runs.add(run);
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return runs;
    }

    /**
     * Get a list of run summaries without loading their objects such as EPICS data.
     *
     * @return the list of run summaries
     */
    @Override
    public List<RunSummary> getRunSummaries() {
        PreparedStatement statement = null;
        final List<RunSummary> runSummaries = new ArrayList<RunSummary>();
        try {
            statement = this.connection.prepareStatement(RunSummaryQuery.SELECT_ALL);
            final ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                final RunSummary runSummary = new RunSummary(resultSet.getInt("run"));
                runSummary.setStartTimeUtc(resultSet.getLong("start_time_utc"));
                runSummary.setEndTimeUtc(resultSet.getLong("end_time_utc"));
                runSummary.setTotalEvents(resultSet.getInt("nevents"));
                runSummary.setTotalFiles(resultSet.getInt("nfiles"));
                runSummary.setEndOkay(resultSet.getBoolean("end_ok"));
                runSummary.setRunOkay(resultSet.getBoolean("run_ok"));
                runSummary.setUpdated(resultSet.getTimestamp("updated"));
                runSummary.setCreated(resultSet.getTimestamp("created"));
                runSummaries.add(runSummary);
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return runSummaries;
    }

    /**
     * Get a run summary by run number without loading object state.
     *
     * @param run the run number
     * @return the run summary object
     */
    @Override
    public RunSummary getRunSummary(final int run) {
        PreparedStatement statement = null;
        RunSummary runSummary = null;
        try {
            statement = this.connection.prepareStatement(RunSummaryQuery.SELECT_RUN);
            statement.setInt(1, run);
            final ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                throw new IllegalArgumentException("No record exists for run " + run + " in database.");
            }

            runSummary = new RunSummary(run);
            runSummary.setStartTimeUtc(resultSet.getLong("start_time_utc"));
            runSummary.setEndTimeUtc(resultSet.getLong("end_time_utc"));
            runSummary.setTotalEvents(resultSet.getInt("nevents"));
            runSummary.setTotalFiles(resultSet.getInt("nfiles"));
            runSummary.setEndOkay(resultSet.getBoolean("end_ok"));
            runSummary.setRunOkay(resultSet.getBoolean("run_ok"));
            runSummary.setUpdated(resultSet.getTimestamp("updated"));
            runSummary.setCreated(resultSet.getTimestamp("created"));
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return runSummary;
    }

    /**
     * Insert a list of run summaries along with their complex state such as referenced scaler and EPICS data.
     *
     * @param runSummaryList the list of run summaries
     * @param deleteExisting <code>true</code> to allow deletion and replacement of existing run summaries
     */
    @Override
    public void insertFullRunSummaries(final List<RunSummary> runSummaryList, final boolean deleteExisting) {

        if (runSummaryList == null) {
            throw new IllegalArgumentException("The run summary list is null.");
        }
        if (runSummaryList.isEmpty()) {
            throw new IllegalArgumentException("The run summary list is empty.");
        }

        LOGGER.info("inserting " + runSummaryList.size() + " run summaries into database");

        // Turn off auto commit.
        try {
            LOGGER.info("turning off auto commit");
            this.connection.setAutoCommit(false);
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        // Loop over all runs found while crawling.
        for (final RunSummary runSummary : runSummaryList) {

            final int run = runSummary.getRun();

            LOGGER.info("inserting run summary for run " + run + " into database");

            // Does the run exist in the database already?
            if (this.runSummaryExists(run)) {
                // Is deleting existing rows allowed?
                if (deleteExisting) {
                    LOGGER.info("deleting existing run summary");
                    // Delete the existing rows.
                    this.deleteFullRunSummary(runSummary);
                } else {
                    // Rows exist but updating is disallowed which is a fatal error.
                    throw new IllegalStateException("Run " + runSummary.getRun()
                            + " already exists and updates are disallowed.");
                }
            }

            // Insert full run summary information including sub-objects.
            LOGGER.info("inserting run summary");
            this.insertFullRunSummary(runSummary);
            LOGGER.info("run summary for " + run + " inserted successfully");

            try {
                // Commit the transaction for the run.
                LOGGER.info("committing transaction");
                this.connection.commit();
            } catch (final SQLException e1) {
                try {
                    LOGGER.severe("rolling back transaction");
                    // Rollback the transaction if there was an error.
                    this.connection.rollback();
                } catch (final SQLException e2) {
                    throw new RuntimeException(e2);
                }
            }

            LOGGER.info("done inserting run summary " + run);

            LOGGER.getHandlers()[0].flush();
        }

        try {
            LOGGER.info("turning auto commit on");
            // Turn auto commit back on.
            this.connection.setAutoCommit(true);
        } catch (final SQLException e) {
            e.printStackTrace();
        }

        LOGGER.info("done inserting run summaries");
    }

    /**
     * Insert a run summary including all its objects.
     *
     * @param runSummary the run summary object to insert
     */
    @Override
    public void insertFullRunSummary(final RunSummary runSummary) {

        // Insert basic run log info.
        this.insertRunSummary(runSummary);

        // Insert list of files.
        LOGGER.info("inserting EVIO " + runSummary.getEvioFileList().size() + " files");
        evioFilesDao.insertEvioFiles(runSummary.getEvioFileList(), runSummary.getRun());

        // Insert EPICS data.
        LOGGER.info("inserting " + runSummary.getEpicsDataSet().size() + " EPICS records");
        epicsDataDao.insertEpicsData(runSummary.getEpicsDataSet());

        // Insert scaler data.
        LOGGER.info("inserting " + runSummary.getScalerData().size() + " scaler data records");
        scalerDataDao.insertScalerData(runSummary.getScalerData(), runSummary.getRun());
    }

    /**
     * Insert a run summary but not its objects.
     *
     * @param runSummary the run summary object
     */
    @Override
    public void insertRunSummary(final RunSummary runSummary) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(RunSummaryQuery.INSERT);
            preparedStatement.setInt(1, runSummary.getRun());
            preparedStatement.setLong(2, runSummary.getStartTimeUtc());
            preparedStatement.setLong(3, runSummary.getEndTimeUtc());
            preparedStatement.setInt(4, runSummary.getTotalEvents());
            preparedStatement.setInt(5, runSummary.getEvioFileList().size());
            preparedStatement.setBoolean(6, runSummary.getEndOkay());
            preparedStatement.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Read a run summary and its objects such as scaler data.
     *
     * @param run the run number
     * @return the full run summary
     */
    @Override
    public RunSummary readFullRunSummary(final int run) {

        // Read main run summary but not referenced objects.
        final RunSummary runSummary = this.getRunSummary(run);

        // Read EPICS data and set on RunSummary.
        runSummary.setEpicsData(epicsDataDao.getEpicsData(run));

        // Read scaler data and set on RunSummary.
        runSummary.setScalerData(scalerDataDao.getScalerData(run));

        // Read EVIO file list and set on RunSummary.
        runSummary.setEvioFileList(evioFilesDao.getEvioFiles(run));

        return runSummary;
    }

    /**
     * Return <code>true</code> if a run summary exists in the database for the run number.
     *
     * @param run the run number
     * @return <code>true</code> if run exists in the database
     */
    @Override
    public boolean runSummaryExists(final int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement("SELECT run FROM runs where run = ?");
            preparedStatement.setInt(1, run);
            final ResultSet rs = preparedStatement.executeQuery();
            return rs.first();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Update a run summary but not its complex state.
     *
     * @param runSummary the run summary to update
     */
    @Override
    public void updateRunSummary(final RunSummary runSummary) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(RunSummaryQuery.UPDATE_RUN);
            preparedStatement.setLong(1, runSummary.getStartTimeUtc());
            preparedStatement.setLong(2, runSummary.getEndTimeUtc());
            preparedStatement.setInt(3, runSummary.getTotalEvents());
            preparedStatement.setInt(4, runSummary.getEvioFileList().size());
            preparedStatement.setBoolean(5, runSummary.getEndOkay());
            preparedStatement.setBoolean(6, runSummary.getRunOkay());
            preparedStatement.setInt(7, runSummary.getRun());
            preparedStatement.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}