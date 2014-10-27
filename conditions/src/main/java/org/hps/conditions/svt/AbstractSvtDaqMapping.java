package org.hps.conditions.svt;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;
import org.hps.util.Pair;


/**
 * This abstract class provides some of the basic functionality used to
 * access SVT DAQ map variables. 
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public abstract class AbstractSvtDaqMapping extends AbstractConditionsObject {
        
	public static abstract class AbstractSvtDaqMappingCollection<T extends AbstractSvtDaqMapping>
	        extends ConditionsObjectCollection<T> {
   
	    /**
		 * Flag values for top or bottom half.
		 */
	    public static final String TOP_HALF    = "T";
	    public static final String BOTTOM_HALF = "B";

        
	    /**
	     *  Flag values for axial or stereo sensors 
         */
	    public static final String AXIAL  = "A";
	    public static final String STEREO = "S";

	    /**
	     * Get a DAQ pair for the given {@link HpsSiSensor}
	     * 
	     * @param sensor A sensor of type {@link HpsSiSensor}
	     * @return The DAQ pair associated with the sensor
	     */
	    public abstract Pair<Integer, Integer> getDaqPair(HpsSiSensor sensor);
	    
	    
        /**
         * Get the orientation of a sensor.
         * 
         * @param daqPair for a given sensor
         * @return If a daqPair is found, return an "A" if the sensor 
         * 		   orientation is Axial, an "S" if the orientation is Stereo or
         * 		   null if the daqPair doesn't exist.
         */
        public abstract String getOrientation(Pair<Integer, Integer> daqPair);
	
	}
	
    public String getSvtHalf() {
        return getFieldValue("svt_half");
    }
    
    public int getLayerNumber() {
        return getFieldValue("layer");
    }
    
    public String getOrientation() { 
    	return getFieldValue("orientation");
    }
}
