package org.hps.detector;

import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtAlignmentConstant;
import org.hps.conditions.svt.SvtAlignmentConstant.SvtAlignmentConstantCollection;

/**
 * Test loading SVT alignment constants into the Java detector model.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class SvtAlignmentTest extends TestCase {

    private static final String DETECTOR_NAME = "HPS-EngRun2015-Nominal-v1";
    
    private static final int[] RUNS = {
        /* 4mm */
        4847,
        /* 3mm */
        5037,
        /* 2mm */
        5066,
        /* 1.5mm */
        5259,
        /* open */
        5222,
        /* 0.5mm a.k.a. nominal */
        5632
        };
        
    private static double ANGLES[][] = {
        /* 4mm */
        {0.0107, -0.0116},
        /* 3mm */
        {0.0077, -0.0086},
        /* 2mm */
        {0.0046, -0.005},
        /* 1.5mm */
        {0.0031, -0.0033},
        /* open */
        {0.0216, -0.0217},
        /* 0.5mm a.k.a. nominal */
        {0, 0}
    };

    public void testSvtAlignment() throws Exception {
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.setLogLevel(Level.WARNING);
        int runIndex = 0;
        for (int run : RUNS) {       
            
            System.out.println();            
            System.out.println("loading run " + run + " SVT alignments for detector " + DETECTOR_NAME + " ...");
            
            manager.setDetector(DETECTOR_NAME, run);
            
            SvtAlignmentConstantCollection collection = 
                    manager.getCachedConditions(SvtAlignmentConstantCollection.class, "svt_alignments").getCachedData();
            System.out.println("got collection " + collection.getCollectionId() + " of size " + collection.size());
            
            SvtAlignmentConstant top = collection.find(13100);
            SvtAlignmentConstant bottom = collection.find(23100);
            
            System.out.println("param " + top.getParameter() + " = " + top.getValue());
            System.out.println("param " + bottom.getParameter() + " = " + bottom.getValue());
            
            assertEquals("Top angle wrong.", ANGLES[runIndex][0], top.getValue());
            assertEquals("Bottom angle wrong.", ANGLES[runIndex][1], bottom.getValue());
            ++runIndex;
        }
    }
}
