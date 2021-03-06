package org.hps.conditions;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.util.Driver;

/**
 * This {@link org.lcsim.util.Driver} can be used to customize the behavior of the {@link DatabaseConditionsManager}. It
 * allows the setting of a detector name and run number, as well as other parameters, if the user wishes to override the
 * default behavior of the conditions system, which is generally activated from LCSim events. It is not necessary to run
 * this Driver in order to activate the default database conditions system. Only one instance of this Driver should ever
 * be included in a steering file.
 * <p>
 * This is an example of using the Driver in an XML steering file:
 *
 * <pre>
 * {@code
 * <driver name="ConditionsDriver" type="org.hps.conditions.ConditionsDriver">
 *     <detectorName>HPS-TestRun-v5</detectorName>
 *     <runNumber>1351</runNumber>
 *     <tag>test_run</tag>
 *     <xmlConfigResource>/org/hps/conditions/config/conditions_database_testrun_2012.xml</xmlConfigResource>
 *     <freeze>true</freeze>
 * </driver>
 * }
 * </pre>
 * <p>
 * This is a "special" Driver which must have its initialization occur at the right time. It has a custom initialization
 * method {@link #initialize()} which should be called after all Driver setup has occurred, but before the job actually
 * begins. This is so the conditions system functions properly, including the activation of registered listeners. The
 * setup is performed by the <code>JobManager</code>, which is used in the default command line front end of the 
 * hps-distribution. If that class is not being used, then the method must be executed manually at the right
 * time to achieve the proper behavior.
 *
 * @author Jeremy McCormick, SLAC
 * 
 * @deprecated Use built-in options of job manager.
 */
@Deprecated
public class ConditionsDriver extends Driver {

    /** The name of the detector model. */
    private String detectorName;

    /**
     * True to freeze the conditions system after activation (requires valid detector name and run number).
     */
    private boolean freeze;

    /** The user run number. */
    private int runNumber = 0;

    /** The conditions system tag. */
    private String tag;

    /** The XML config resource. */
    private String xmlConfigResource;

    /**
     * Default constructor.
     */
    public ConditionsDriver() {
    }

    /**
     * Setup the conditions system based on the Driver parameters.
     *
     * @throws RuntimeException if there is a problem setting up the conditions system
     */
    public final void initialize() {

        final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();

        if (this.xmlConfigResource != null) {
            // Set a custom XML configuration resource.
            conditionsManager.setXmlConfig(this.xmlConfigResource);
        }

        if (this.tag != null) {
            // Set a tag for filtering ConditionsRecord objects.
            conditionsManager.addTag(this.tag);
        }
        if (this.detectorName != null) {
            // The manager can only be initialized here if there is a user supplied detector name.
            try {
                // Initialize the conditions manager.
                conditionsManager.setDetector(this.detectorName, this.runNumber);
                if (this.freeze) {
                    // User configured to freeze conditions for the job.
                    conditionsManager.freeze();
                }
            } catch (final ConditionsNotFoundException e) {
                throw new RuntimeException("Error initializing conditions from ConditionsDriver.", e);
            }
        }
    }

    /**
     * Set the name of the detector to use.
     *
     * @param detectorName the name of the detector
     */
    public final void setDetectorName(final String detectorName) {
        this.detectorName = detectorName;
    }

    /**
     * Set whether or not the conditions system should be "frozen" after the detector name and run number are set. When
     * frozen, the conditions system will ignore subsequent calls to
     * {@link org.lcsim.conditions.ConditionsManager#setDetector(String, int)} and instead use the user supplied
     * detector and run for the whole job.
     *
     * @param freeze <code>true</code> to freeze the conditions system after it is setup
     */
    public final void setFreeze(final boolean freeze) {
        this.freeze = freeze;
    }

    /**
     * Set a custom run number to setup the conditions system. In the case where the actual event stream has run numbers
     * that differ from this one, most likely the Driver should be configured to be frozen after setup using
     * {@link #setFreeze(boolean)}. The method {@link #setDetectorName(String)} needs to be called before this one or an
     * exception will be thrown.
     *
     * @param runNumber the user supplied run number for the job
     */
    public final void setRunNumber(final int runNumber) {
        this.runNumber = runNumber;
    }

    /**
     * Set a tag used to filter ConditionsRecords.
     *
     * @param tag the tag value e.g. "pass0"
     */
    public final void setTag(final String tag) {
        this.tag = tag;
    }

    /**
     * Set an XML configuration resource.
     *
     * @param xmlConfigResource the XML configuration resource
     */
    public final void setXmlConfigResource(final String xmlConfigResource) {
        this.xmlConfigResource = xmlConfigResource;
    }

    public int getRunNumber() {
        return this.runNumber;
    }

    public String getDetectorName() {
        return this.detectorName;
    }
}
