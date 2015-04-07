package org.hps.conditions.cli;

import java.io.PrintStream;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.FieldValueMap;
import org.hps.conditions.database.DatabaseConditionsManager;

/**
 * This is a command for the conditions CLI that will add a conditions record, making a conditions set with a particular
 * collection ID available by run number via the {@link org.hps.conditions.database.DatabaseConditionsManager}.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class AddCommand extends AbstractCommand {

    /**
     * For printing out messages.
     */
    private final PrintStream ps = System.out;

    /**
     * Define command line options.
     */
    private static final Options OPTIONS = new Options();
    static {
        OPTIONS.addOption(new Option("h", false, "Show help for add command"));
        OPTIONS.addOption("r", true, "The starting run number (required)");
        OPTIONS.getOption("r").setRequired(true);
        OPTIONS.addOption("e", true, "The ending run number (default is starting run number)");
        OPTIONS.addOption("t", true, "The table name (required)");
        OPTIONS.getOption("t").setRequired(true);
        OPTIONS.addOption("c", true, "The collection ID (required)");
        OPTIONS.getOption("c").setRequired(true);
        OPTIONS.addOption("T", true, "A tag value (optional)");
        OPTIONS.addOption("u", true, "Your user name (optional)");
        OPTIONS.addOption("m", true, "The notes about this conditions set (optional)");
    }

    /**
     * Class constructor.
     */
    AddCommand() {
        super("add", "Add a conditions record to associate a collection to a run range", OPTIONS);
    }

    /**
     * Execute the command with the given arguments.
     *
     * @param arguments the command line arguments
     */
    final void execute(final String[] arguments) {

        final CommandLine commandLine = parse(arguments);

        // This command has 3 required options.
        if (commandLine.getOptions().length == 0) {
            this.printUsage();
            System.exit(1);
        }

        // Run start (required).
        final int runStart;
        if (commandLine.hasOption("r")) {
            runStart = Integer.parseInt(commandLine.getOptionValue("r"));
        } else {
            throw new RuntimeException("Missing required -r option with run number.");
        }

        // Run end.
        int runEnd = runStart;
        if (commandLine.hasOption("e")) {
            runEnd = Integer.parseInt(commandLine.getOptionValue("e"));
        }

        // Name of table (required).
        String tableName;
        if (commandLine.hasOption("t")) {
            tableName = commandLine.getOptionValue("t");
        } else {
            throw new RuntimeException("Missing required -t argument with table name");
        }
        final String name = tableName;

        // Collection ID (required).
        int collectionId;
        if (commandLine.hasOption("c")) {
            collectionId = Integer.parseInt(commandLine.getOptionValue("c"));
        } else {
            throw new RuntimeException("Missing required -c argument with collection ID");
        }

        // User name.
        String createdBy = System.getProperty("user.name");
        if (commandLine.hasOption("u")) {
            createdBy = commandLine.getOptionValue("u");
        }

        // Tag to assign (optional).
        String tag = null;
        if (commandLine.hasOption("T")) {
            tag = commandLine.getOptionValue("T");
        }

        // Notes (optional).
        String notes = null;
        if (commandLine.hasOption("m")) {
            notes = commandLine.getOptionValue("m");
        }

        // Create the conditions record to insert.
        final ConditionsRecord conditionsRecord = createConditionsRecord(runStart, runEnd, tableName, name,
                collectionId, createdBy, tag, notes);
        try {
            boolean createdConnection = false;
            final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
            if (!DatabaseConditionsManager.getInstance().isConnected()) {
                createdConnection = manager.openConnection();
            }
            conditionsRecord.insert();
            manager.closeConnection(createdConnection);
        } catch (ConditionsObjectException e) {
            throw new RuntimeException("An error occurred while adding a conditions record.", e);
        }
        ps.println("successfully added conditions record ...");
        ps.println(conditionsRecord);
    }

    /**
     * Create a conditions record.
     *
     * @param runStart the run start
     * @param runEnd the run end
     * @param tableName the table name
     * @param name the key name
     * @param collectionId the collection ID
     * @param createdBy the user name
     * @param tag the conditions tag
     * @param notes the text notes about the collection
     * @return the new conditions record
     */
    // FIXME: Too many method parameters (max 7 is recommended).
    private ConditionsRecord createConditionsRecord(final int runStart, final int runEnd, final String tableName,
            final String name, final int collectionId, final String createdBy, final String tag, final String notes) {
        final ConditionsRecord conditionsRecord = new ConditionsRecord();
        final FieldValueMap fieldValues = new FieldValueMap();
        fieldValues.put("run_start", runStart);
        fieldValues.put("run_end", runEnd);
        fieldValues.put("table_name", tableName);
        fieldValues.put("name", name);
        fieldValues.put("collection_id", collectionId);
        fieldValues.put("created_by", createdBy);
        if (tag != null) {
            fieldValues.put("tag", tag);
        }
        if (notes != null) {
            fieldValues.put("notes", notes);
        }
        conditionsRecord.setFieldValues(fieldValues);
        fieldValues.put("created", new Date());
        return conditionsRecord;
    }
}
