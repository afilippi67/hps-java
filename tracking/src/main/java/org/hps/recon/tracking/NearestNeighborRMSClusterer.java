package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.special.Gamma;

import org.hps.readout.svt.HPSSVTConstants;
//===> import org.hps.conditions.deprecated.HPSSVTCalibrationConstants;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
//===> import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.event.RawTrackerHit;

/**
 *
 * @author Matt Graham
 */
// TODO: Add class documentation.
public class NearestNeighborRMSClusterer implements ClusteringAlgorithm {

    private static String _NAME = "NearestNeighborRMS";
    private double _seed_threshold;
    private double _neighbor_threshold;
    private double _cluster_threshold;
    private double _meanTime = 24;
    private double _timeWindow = 48;
    private double _neighborDeltaT = Double.POSITIVE_INFINITY;
    private final double _minChiProb = Gamma.regularizedGammaQ(4, 20);

    /**
     * Instantiate NearestNeighborRMS with specified thresholds. Seed threshold
     * is the minimum charge to initiate a cluster. Neighbor threshold is the
     * minimum charge to add a neighboring cell to a cluster. Cluster threshold
     * is minimum charge of the entire cluster. All thresholds are in units of
     * RMS noise of the channel(s).
     *
     * @param seed_threshold seed threshold
     * @param neighbor_threshold neighbor threshold
     * @param cluster_threshold cluster threshold
     */
    public NearestNeighborRMSClusterer(double seed_threshold, double neighbor_threshold, double cluster_threshold) {
        _seed_threshold = seed_threshold;
        _neighbor_threshold = neighbor_threshold;
        _cluster_threshold = cluster_threshold;
    }

    public void setMeanTime(double _meanTime) {
        this._meanTime = _meanTime;
    }

    public void setTimeWindow(double _timeWindow) {
        this._timeWindow = _timeWindow;
    }

    public void setNeighborDeltaT(double _neighborDeltaT) {
        this._neighborDeltaT = _neighborDeltaT;
    }

    /**
     * Instantiate NearestNeighborRMS with default thresholds:
     *
     * seed_threshold = 4*RMS noise neighbor_threshold = 3*RMS noise
     * cluster_threshold = 4*RMS noise
     */
    public NearestNeighborRMSClusterer() {
        this(4.0, 3.0, 4.0);
    }

    /**
     * Set the seed threshold. Units are RMS noise.
     *
     * @param seed_threshold seed threshold
     */
    public void setSeedThreshold(double seed_threshold) {
        _seed_threshold = seed_threshold;
    }

    /**
     * Set the neighbor threshold. Units are RMS noise.
     *
     * @param neighbor_threshold neighbor threshold
     */
    public void setNeighborThreshold(double neighbor_threshold) {
        _neighbor_threshold = neighbor_threshold;
    }

    /**
     * Set the cluster threshold. Units are RMS noise.
     *
     * @param cluster_threshold cluster threshold
     */
    public void setClusterThreshold(double cluster_threshold) {
        _cluster_threshold = cluster_threshold;
    }

    /**
     * Find clusters using the nearest neighbor algorithm.
     *
     * @param base_hits List of RawTrackerHits to be clustered
     * @return list of clusters, with a cluster being a list of RawTrackerHits
     */
    @Override
    public List<List<FittedRawTrackerHit>> findClusters(List<FittedRawTrackerHit> base_hits) {

        // Check that the seed threshold is at least as large as the neighbor threshold
        if (_seed_threshold < _neighbor_threshold) {
            throw new RuntimeException("Tracker hit clustering error: seed threshold below neighbor threshold");
        }

        // Create maps that show the channel status and relate the channel number to the raw hit
        // and vice versa
        int mapsize = 2 * base_hits.size();
//        Map<Integer, Boolean> clusterable = new HashMap<Integer, Boolean>(mapsize);
        Set<Integer> clusterableSet = new HashSet<Integer>(mapsize);

//        Map<FittedRawTrackerHit, Integer> hit_to_channel = new HashMap<FittedRawTrackerHit, Integer>(mapsize);
        Map<Integer, FittedRawTrackerHit> channel_to_hit = new HashMap<Integer, FittedRawTrackerHit>(mapsize);

        // Create list of channel numbers to be used as cluster seeds
        List<Integer> cluster_seeds = new ArrayList<Integer>();

        // Loop over the raw hits and construct the maps used to relate cells and hits, initialize
        // the
        // clustering status map, and create a list of possible cluster seeds
        for (FittedRawTrackerHit base_hit : base_hits) {

            RawTrackerHit rth = base_hit.getRawTrackerHit();
            // get the channel number for this hit
            SiTrackerIdentifierHelper sid_helper = (SiTrackerIdentifierHelper) rth.getIdentifierHelper();
            IIdentifier id = rth.getIdentifier();
            int channel_number = sid_helper.getElectrodeValue(id);

//            // Check for duplicate RawTrackerHit
//            if (hit_to_channel.containsKey(base_hit)) {
//                throw new RuntimeException("Duplicate hit: " + id.toString());
//            }
            // Check for duplicate RawTrackerHits or channel numbers
            if (channel_to_hit.containsKey(channel_number)) {
                // throw new RuntimeException("Duplicate channel number: "+channel_number);
//                System.out.println("Duplicate channel number: " + channel_number);
                //if the hit currently in the map has smaller time, use it and discard the new hit
                //TODO: be smarter about this
                if (Math.abs(((FittedRawTrackerHit) channel_to_hit.get(channel_number)).getT0()) < Math.abs(base_hit.getT0())) {
                    continue;
                }
            }

            // Add this hit to the maps that relate channels and hits
//            hit_to_channel.put(base_hit, channel_number);
            channel_to_hit.put(channel_number, base_hit);

            // Get the signal from the readout chip
            double signal = base_hit.getAmp();
            double noiseRMS = 0; 
            for(int sampleN = 0; sampleN < HPSSVTConstants.TOTAL_NUMBER_OF_SAMPLES; sampleN++){
                noiseRMS += ((HpsSiSensor) rth.getDetectorElement()).getNoise(channel_number, sampleN);
            }
            noiseRMS = noiseRMS/HPSSVTConstants.TOTAL_NUMBER_OF_SAMPLES;
            
            //===> double noiseRMS = HPSSVTCalibrationConstants.getNoise((SiSensor) rth.getDetectorElement(), channel_number);
            
            
            // Mark this hit as available for clustering if it is above the neighbor threshold
            if (signal / noiseRMS >= _neighbor_threshold && passChisqCut(base_hit)) {
                clusterableSet.add(channel_number);
            }

            // Add this hit to the list of seeds if it is above the seed threshold
            if (signal / noiseRMS >= _seed_threshold && passTimingCut(base_hit) && passChisqCut(base_hit)) {
                cluster_seeds.add(channel_number);
            }
        }

//        System.out.println("Hits to be clustered:");
//        for (int i = 0; i < 639; i++) {
//            FittedRawTrackerHit base_hit = channel_to_hit.get(i);
//            if (base_hit != null) {
//                System.out.format("channel %d\tt0 %f\t amp %f\n", i, base_hit.getT0(), base_hit.getAmp());
//            }
//        }

        // Create a list of clusters
        List<List<FittedRawTrackerHit>> cluster_list = new ArrayList<List<FittedRawTrackerHit>>();

        // Now loop over the cluster seeds to form clusters
        for (int seed_channel : cluster_seeds) {

            // First check if this hit is still available for clustering
//            if (!clusterable.get(seed_channel)) {
//                continue;
//            }
            if (!clusterableSet.contains(seed_channel)) {
                continue;
            }

            // Create a new cluster
            List<FittedRawTrackerHit> cluster = new ArrayList<FittedRawTrackerHit>();
            double cluster_signal = 0.;
            double cluster_noise_squared = 0.;
            double cluster_weighted_time = 0.;

            // Create a queue to hold channels whose neighbors need to be checked for inclusion
            LinkedList<Integer> unchecked = new LinkedList<Integer>();

            // Add the seed channel to the unchecked list and mark it as unavailable for clustering
            unchecked.addLast(seed_channel);
//            clusterable.put(seed_channel, false);
            clusterableSet.remove(seed_channel);

            // Check the neighbors of channels added to the cluster
            while (unchecked.size() > 0) {

                // Pull the next channel off the queue and add it's hit to the cluster
                int clustered_cell = unchecked.removeFirst();
                cluster.add(channel_to_hit.get(clustered_cell));
                FittedRawTrackerHit hit = channel_to_hit.get(clustered_cell);
                cluster_signal += hit.getAmp();
                double strip_noise = 0; 
                for(int sampleN = 0; sampleN < HPSSVTConstants.TOTAL_NUMBER_OF_SAMPLES; sampleN++){
                    strip_noise += ((HpsSiSensor) hit.getRawTrackerHit().getDetectorElement()).getNoise(clustered_cell, sampleN);
                }
                strip_noise = strip_noise/HPSSVTConstants.TOTAL_NUMBER_OF_SAMPLES;
                cluster_noise_squared += Math.pow(strip_noise, 2); 
                //===> cluster_noise_squared += Math.pow(HPSSVTCalibrationConstants.getNoise((SiSensor) hit.getRawTrackerHit().getDetectorElement(), clustered_cell), 2);
                cluster_weighted_time += hit.getT0() * hit.getAmp();
                // cluster_noise_squared +=0; //need to get the noise from the calib. const. class
                // Get the neigbor channels
                // Set<Integer> neighbor_channels =
                // electrodes.getNearestNeighborCells(clustered_cell);
                Collection<Integer> neighbor_channels = getNearestNeighborCells(clustered_cell);

                // Now loop over the neighbors and see if we can add them to the cluster
                for (int channel : neighbor_channels) {

                    // Check if this neighbor channel is still available for clustering
                    if (!clusterableSet.contains(channel)) {
                        continue;
                    }

                    FittedRawTrackerHit neighbor_hit = channel_to_hit.get(channel);
                    if (Math.abs(neighbor_hit.getT0() - cluster_weighted_time / cluster_signal) > _neighborDeltaT) {
//                        System.out.format("new hit t0 %f, cluster t0 %f\n", neighbor_hit.getT0(), cluster_weighted_time / cluster_signal);
                        continue;
                    }

                    // Add channel to the list of unchecked clustered channels
                    // and mark it unavailable for clustering
                    unchecked.addLast(channel);
                    clusterableSet.remove(channel);

                } // end of loop over neighbor cells
            } // end of loop over unchecked cells

            // Finished with this cluster, check cluster threshold and add it to the list of
            // clusters
            if (cluster.size() > 0 && cluster_signal / Math.sqrt(cluster_noise_squared) > _cluster_threshold) {
                cluster_list.add(cluster);
            }

        } // End of loop over seeds

//        for (List<FittedRawTrackerHit> cluster : cluster_list) {
//            System.out.format("cluster with %d hits\n",cluster.size());
//            for (FittedRawTrackerHit base_hit : cluster) {
//                System.out.format("channel %d\tt0 %f\t amp %f\n", base_hit.getRawTrackerHit().getIdentifierFieldValue("strip"), base_hit.getT0(), base_hit.getAmp());
//            }
//        }
        // Finished finding clusters
        return cluster_list;
    }

    private boolean passTimingCut(FittedRawTrackerHit hit) {
        double time = hit.getT0();
        return (Math.abs(time - _meanTime) < _timeWindow);
    }

    private boolean passChisqCut(FittedRawTrackerHit hit) {
        return hit.getShapeFitParameters().getChiProb() > _minChiProb;
    }

//    public int getNeighborCell(int cell, int ncells_0, int ncells_1) {
//        int neighbor_cell = cell + ncells_0;
//        if (isValidCell(neighbor_cell)) {
//            return neighbor_cell;
//        } else {
//            return -1;
//        }
//    }
    public Collection<Integer> getNearestNeighborCells(int cell) {
        Collection<Integer> neighbors = new ArrayList<Integer>(2);
        for (int ineigh = -1; ineigh <= 1; ineigh = ineigh + 2) {
            int neighbor_cell = cell + ineigh;
            if (isValidCell(neighbor_cell)) {
                neighbors.add(neighbor_cell);
            }
        }
        return neighbors;
    }

    public boolean isValidCell(int cell) {
        return (cell >= 0 && cell < HPSSVTConstants.TOTAL_STRIPS_PER_SENSOR);
    }
}
