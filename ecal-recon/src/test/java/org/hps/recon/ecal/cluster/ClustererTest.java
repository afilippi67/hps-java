package org.hps.recon.ecal.cluster;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.job.EventMarkerDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * This test does basic sanity checks on the output from the Clusterer algorithms,
 * and it creates an AIDA file with useful plots.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ClustererTest extends TestCase {
    
    static final String fileLocation = "http://www.lcsim.org/test/hps-java/MockDataReconTest.slcio";
    File inputFile;
    File testOutputDir;
    static int nEvents = 50; 
    
    public void setUp() {
        // Cache the input file.
        try {
            inputFile = new FileCache().getCachedFile(new URL(fileLocation));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        // Create test output dir.
        testOutputDir = new TestOutputFile(getClass().getSimpleName());
        testOutputDir.mkdir();        
    }
   
    public void testClasInnerCalClusterer() {
        // This is just replicating the default cuts.
        runClustererTest("ClasInnerCalClusterer", new double[] { 0.0075, 0.1, 0.3, 0.0, 20.0, 0. }, true);;
    }
    
    public void testSimplClasInnerCalClusterer() {
        runClustererTest("SimpleClasInnerCalClusterer", new double[] { 0.0, 0.0, 9999.0, 0. }, true);
    }
    
    public void testNearestNeighborClusterer() {
        runClustererTest("NearestNeighborClusterer", new double[] { 0.0, 2.0 }, true);
    }
    
    public void testLegacyClusterer() {
        runClustererTest("LegacyClusterer", new double[] { 0.0, 0.0 }, true);
    }
    
    /**
     * Run the standard test for a Clusterer.
     * @param clustererName The name of the Clusterer.
     * @param cuts The cut values.
     * @param writeLcioFile Whether or not to write an LCIO output file.
     */
    private void runClustererTest(String clustererName, double[] cuts, boolean writeLcioFile) {
        
        // Initialize conditions system.
        new DatabaseConditionsManager();
        
        // Config loop.
        LCSimLoop loop = new LCSimLoop();       
        try {
            loop.setLCIORecordSource(inputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        loop.add(new EventMarkerDriver());
        
        // Configure the ClusterDriver and add it to the loop.
        String clusterCollectionName = clustererName + "Clusters";
        ClusterDriver clusterDriver = new ClusterDriver();
        clusterDriver.setClustererName(clustererName);
        clusterDriver.setCuts(cuts);
        clusterDriver.getLogger().setLevel(Level.ALL);
        clusterDriver.setInputHitCollectionName("EcalHits");       
        clusterDriver.setOutputClusterCollectionName(clusterCollectionName);
        clusterDriver.setRaiseErrorNoHitCollection(true);
        clusterDriver.getLogger().setLevel(Level.INFO);
        loop.add(clusterDriver);                         
        
        loop.add(new ClusterCheckDriver(clusterCollectionName));
        
        if (writeLcioFile) {
            loop.add(new LCIODriver(testOutputDir.getPath() + File.separator + clustererName));
        }
        
        // Run the job.
        try {
            loop.loop(nEvents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        loop.dispose();
    }
    
    /**
     * This Driver will check some of the basic Cluster values (currently just size and energy).
     * It also produces some QA plots for each Clusterer in its own output AIDA dir within the plot tree.
     */
    static class ClusterCheckDriver extends Driver {                
        
        AIDA aida = AIDA.defaultInstance();
        IHistogram1D energyH1D;
        IHistogram1D uncorrectedEnergyH1D;
        IHistogram1D countH1D;
        IHistogram1D sizeH1D;    
        IHistogram1D highestHitEnergyH1D;
        IHistogram1D hitEnergyH1D;
        IHistogram2D rawVsCorrectedH2D;
        IHistogram1D positionXH1D;
        IHistogram1D positionYH1D;
        String clusterCollectionName;
        String clustererName;        
        
        ClusterCheckDriver(String clusterCollectionName) {
            this.clusterCollectionName = clusterCollectionName;
        }        
        
        public void startOfData() {
            energyH1D = aida.histogram1D(clusterCollectionName + "/Cluster Energy", 300, 0.0, 3.0);
            uncorrectedEnergyH1D = aida.histogram1D(clusterCollectionName + "/Uncorrected Cluster Energy", 200, 0.0, 2.0);
            countH1D = aida.histogram1D(clusterCollectionName + "/Cluster Count", 10, -0.5, 9.5);
            sizeH1D = aida.histogram1D(clusterCollectionName + "/Cluster Size", 30, 0.5, 30.5);
            highestHitEnergyH1D = aida.histogram1D(clusterCollectionName + "/Highest Hit Energy", 300, 0.0, 1.5);
            hitEnergyH1D = aida.histogram1D(clusterCollectionName + "/Hit Energy", 300, 0.0, 1.5);
            rawVsCorrectedH2D = aida.histogram2D(clusterCollectionName + "/Raw vs Corrected Energy", 100, 0.0, 2.0, 100, 0.0, 2.0);
            positionXH1D = aida.histogram1D(clusterCollectionName + "/Position X", 300, 0., 1500.0);
            positionYH1D = aida.histogram1D(clusterCollectionName + "/Position Y", 500, 0., 1000.0);
        }
        
        public void process(EventHeader event) {
            List<Cluster> clusters = event.get(Cluster.class, this.clusterCollectionName);
            for (Cluster cluster : clusters) {
                assertTrue("The cluster energy is invalid.", cluster.getEnergy() > 0.);
                assertTrue("The cluster has no hits.", !cluster.getCalorimeterHits().isEmpty());                
                energyH1D.fill(cluster.getEnergy());
                double rawEnergy = ClusterUtilities.computeRawEnergy(cluster);
                uncorrectedEnergyH1D.fill(rawEnergy);
                sizeH1D.fill(cluster.getCalorimeterHits().size());
                rawVsCorrectedH2D.fill(rawEnergy, cluster.getEnergy());
                for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                    hitEnergyH1D.fill(hit.getCorrectedEnergy());
                }                
                highestHitEnergyH1D.fill(ClusterUtilities.getHighestEnergyHit(cluster).getCorrectedEnergy());
                positionXH1D.fill(Math.abs(cluster.getPosition()[0]));
                positionYH1D.fill(Math.abs(cluster.getPosition()[1]));
            }            
            countH1D.fill(clusters.size());
        }              
    }
    
    /**
     * This method writes the AIDA file to disk from all the tests.
     */
    public void tearDown() {
        try {
            AIDA.defaultInstance().saveAs(testOutputDir.getPath() + File.separator + this.getClass().getSimpleName() + ".aida");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }  
}