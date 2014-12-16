package org.hps.recon.ecal.cluster;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.awt.Point;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.recon.ecal.HPSEcalClusterIC;
import org.lcsim.detector.IGeometryInfo;
import org.lcsim.detector.solids.Trd;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;


/**
 * This Driver creates clusters from the CalorimeterHits of an
 * {@link org.lcsim.geometry.subdetectur.HPSEcal3} detector.
 * 
 * This clustering logic is based on that from the CLAS-Note-2005-001. 
 * This sorts hits from highest to lowest energy and build clusters around 
 * each local maximum/seed hit. Common hits are distributed between clusters 
 * when minimum between two clusters. There is a threshold cut for minimum
 * hit energy, minimum cluster energy, and minimum seed hit energy. There is 
 * also a timing threshold with respect to the seed hit. All of these parameters
 * are tunable and should be refined with more analysis. 
 *
 *
 * @author Holly Szumila-Vance <hvanc001@odu.edu>
 * @author Kyle McCarty <mccaky@gmail.com>
 *
 */
public class EcalClusterIC extends Driver {
	// File writer to output cluster results.
    FileWriter writeHits = null;
    // LCIO collection name for calorimeter hits.
    String ecalCollectionName;
    // Name of the calorimeter detector object.
    String ecalName = "Ecal";
    // LCIO cluster collection name to which to write.
    String clusterCollectionName = "EcalClustersIC";
    // Collection name for rejected hits
    String rejectedHitName = "RejectedHits";
    // File path to which to write event display output.
    String outfile = null;
    // Map of crystals to their neighbors.
    NeighborMap neighborMap = null;
    // Minimum energy threshold for hits; lower energy hits will be
    // excluded from clustering. Units in GeV.
    double hitEnergyThreshold = 0.0075;
    // Minimum energy threshold for seed hits; if seed hit is below
    // cluster is excluded from output. Units in GeV.
    double seedEnergyThreshold = 0.1;
    // Minimum energy threshold for cluster hits; if total cluster
    // energy is below, the cluster is excluded. Units in GeV.
    double clusterEnergyThreshold = 0.3;  
    // A Comparator that sorts CalorimeterHit objects from highest to
    // lowest energy.
    private static final EnergyComparator ENERGY_COMP = new EnergyComparator();
    // Track the event number for the purpose of outputting to event
    // display format.
    private int eventNum = -1;
    // Apply time cut to hits
    boolean timeCut = false;
    // Minimum time cut window range. Units in ns.
    double minTime = 0.0;
    // Maximum time cut window range. Units in ns.
    double timeWindow = 20.0;
    // Variables for electron energy corrections
    static final double ELECTRON_ENERGY_A = -0.0027;
    static final double ELECTRON_ENERGY_B = -0.06;
    static final double ELECTRON_ENERGY_C = 0.95;
    // Variables for positron energy corrections
    static final double POSITRON_ENERGY_A = -0.0096;
    static final double POSITRON_ENERGY_B = -0.042;
    static final double POSITRON_ENERGY_C = 0.94;
    // Variables for photon energy corrections
    static final double PHOTON_ENERGY_A = 0.0015;
    static final double PHOTON_ENERGY_B = -0.047;
    static final double PHOTON_ENERGY_C = 0.94;
    // Variables for electron position corrections
    static final double ELECTRON_POS_A = 0.0066;
	static final double ELECTRON_POS_B = -0.03;
	static final double ELECTRON_POS_C = 0.028;
	static final double ELECTRON_POS_D = -0.45;
	static final double ELECTRON_POS_E = 0.465;
    // Variables for positron position corrections
	static final double POSITRON_POS_A = 0.0072;
	static final double POSITRON_POS_B = -0.031;
	static final double POSITRON_POS_C = 0.007;
	static final double POSITRON_POS_D = 0.342;
	static final double POSITRON_POS_E = 0.108;
    // Variables for photon position corrections
	static final double PHOTON_POS_A = 0.005;
	static final double PHOTON_POS_B = -0.032;
	static final double PHOTON_POS_C = 0.011;
	static final double PHOTON_POS_D = -0.037;
	static final double PHOTON_POS_E = 0.294;
	
    
       
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }
    
    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }

    /**
     * Output file name for event display output.
     * @param outfile
     */
    public void setOutfile(String outfile) {
        this.outfile = outfile;
    }
    
    public void setRejectedHitName(String rejectedHitName){
    	this.rejectedHitName = rejectedHitName;
    }
    
    /**
     * Minimum energy for a hit to be used in a cluster. Default of 0.0075 GeV
     *
     * @param hitEnergyThreshold
     */
    public void sethitEnergyThreshold(double hitEnergyThreshold) {
        this.hitEnergyThreshold = hitEnergyThreshold;
    }

    /**
     * Minimum energy for a seed hit. Default of 0.1 GeV
     *
     * @param seedEnergyThreshold
     */
    public void setseedEnergyThreshold(double seedEnergyThreshold) {
        this.seedEnergyThreshold = seedEnergyThreshold;
    }
    
    /**
     * Minimum energy for a cluster. Default of 0.3 GeV
     *
     * @param clusterEnergyThreshold
     */
    public void setclusterEnergyThreshold(double clusterEnergyThreshold) {
        this.clusterEnergyThreshold = clusterEnergyThreshold;
    }
    
    /**
     * Apply time cuts to hits. Defaults to false.
     *
     * @param timeCut
     */
    public void setTimeCut(boolean timeCut) {
        this.timeCut = timeCut;
    }

    /**
     * Minimum hit time, if timeCut is true. Default of 0 ns.
     *
     * @param minTime
     */
    public void setMinTime(double minTime) {
        this.minTime = minTime;
    }

    /**
     * Width of time window, if timeCut is true. Default of 20 ns.
     *
     * @param timeWindow
     */
    public void setTimeWindow(double timeWindow) {
        this.timeWindow = timeWindow;
    }
    
    // Make a map for quick calculation of the x-y position of crystal face
    public Map<Point, double[]> correctedPositionMap = new HashMap<Point, double[]>();
    
    public void startOfData() {
    	// Make sure that the calorimeter hit collection name is defined.
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
        
        // Make sure the name of calorimeter detector is defined.
        if (ecalName == null) {
            throw new RuntimeException("The parameter ecalName was not set!");
        }
        
        if (outfile!=null) {
            // Create a file writer and clear the output file, if it exists.
            try {
                writeHits = new FileWriter(outfile);
                writeHits.write("");
            } catch (IOException e) {
            }
        }
    }

    public void detectorChanged(Detector detector) {
        // Get the calorimeter.
    	HPSEcal3 ecal = (HPSEcal3) detector.getSubdetector(ecalName);
    	
        // Store the map of neighbor crystals for the current calorimeter set-up.
        neighborMap = ecal.getNeighborMap();
    }

    public void process(EventHeader event) {
    	// Make sure the current event contains calorimeter hits.
        if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
        	
            // Generate clusters from the calorimeter hits.
            //List<HPSEcalClusterIC> clusterList = null;
            try { createClusters(event); }
            catch(IOException e) { }
        }
    }

    public void createClusters(EventHeader event) throws IOException {
    	
    	// Create a list to store the event hits in.
        List<CalorimeterHit> hitList = new ArrayList<CalorimeterHit>();
        List<CalorimeterHit> baseList = event.get(CalorimeterHit.class, ecalCollectionName);
        for(CalorimeterHit r : baseList) {
        	hitList.add(r);
        }
        
        //Create a list to store the newly created clusters in.
        ArrayList<HPSEcalClusterIC> clusterList = new ArrayList<HPSEcalClusterIC>();
        
        //Create a list to store the rejected hits in.
        ArrayList<CalorimeterHit> rejectedHitList = new ArrayList<CalorimeterHit>();
        
        // Sort the list of hits by energy.
        Collections.sort(hitList, ENERGY_COMP);
        
        // Filter the hit list of any hits that fail to pass the
        // designated threshold.
        filterLoop:
        for(int index = hitList.size() - 1; index >= 0; index--) {
        	// If the hit is below threshold or outside of time window, kill it.
        	if((hitList.get(index).getCorrectedEnergy() < hitEnergyThreshold)||
        			(timeCut && (hitList.get(index).getTime() < minTime || hitList.get(index).getTime() > (minTime + timeWindow)))) {
        		rejectedHitList.add(hitList.get(index));
        		hitList.remove(index);
        	}
        	
        	// Since the hits are sorted by energy from highest to
        	// lowest, any hit that is above threshold means that all
        	// subsequent hits will also be above threshold. Continue through
        	// list to check in time window. 
        	else { continue; }
        }
        
    	//Create a map to connect the cell ID of a calorimeter crystal to the hit which occurred in that crystal.
    	HashMap<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>();
        for (CalorimeterHit hit : hitList) { hitMap.put(hit.getCellID(), hit); }
        
        //Map a crystal to a list of all clusters in which it is a member.
        Map<CalorimeterHit, List<CalorimeterHit>> commonHits = new HashMap<CalorimeterHit, List<CalorimeterHit>>();

        //Map a crystal to the seed of the cluster of which it is a member.
        HashMap<CalorimeterHit, CalorimeterHit> hitSeedMap = new HashMap<CalorimeterHit, CalorimeterHit>();
        
        // Loop through all calorimeter hits to locate seeds and perform
        // first pass calculations for component and common hits.
        for (int ii = 0; ii <= hitList.size() - 1; ii ++){
        	CalorimeterHit hit = hitList.get(ii);
        	// Get the set of all neighboring crystals to the current hit.
            Set<Long> neighbors = neighborMap.get(hit.getCellID());

            // Generate a list to store any neighboring hits in.
            ArrayList<CalorimeterHit> neighborHits = new ArrayList<CalorimeterHit>();
            
            // Sort through the set of neighbors and, if a hit exists
            // which corresponds to a neighbor, add it to the list of
            // neighboring hits.
            for (Long neighbor : neighbors) {
            	// Get the neighboring hit.
            	CalorimeterHit neighborHit = hitMap.get(neighbor);
                        	
            	// If it exists, add it to the list.
            	if(neighborHit != null) { neighborHits.add(neighborHit); }
            }
            
            // Track whether the current hit is a seed hit or not.
            boolean isSeed = true;
            
            // Loops through all the neighboring hits to determine if
            // the current hit is the local maximum within its set of
            // neighboring hits.
            	seedHitLoop:
            		for(CalorimeterHit neighbor : neighborHits) {
            			if(!equalEnergies(hit, neighbor)) {
            				isSeed = false;
            				break seedHitLoop;
            			}
            		}
            // If this hit is a seed hit, just map it to itself.
            if (isSeed && hit.getCorrectedEnergy() >= seedEnergyThreshold) { hitSeedMap.put(hit, hit); }
            
            // If this hit is a local maximum but does not pass seed threshold, 
            // remove from hit list and do not cluster. 
            else if (isSeed  && hit.getCorrectedEnergy() < seedEnergyThreshold){           	
            	hitList.remove(ii);
            	rejectedHitList.add(hit); 
            	ii --;
        	}
            
            // If this hit is not a seed hit, see if it should be
            // attached to any neighboring seed hits.
            else {
                // Sort through the list of neighboring hits.
                for (CalorimeterHit neighborHit : neighborHits) {
                	// Check whether the neighboring hit is a seed.
                	if(hitSeedMap.get(neighborHit) == neighborHit) {
                        // If the neighboring hit is a seed hit and the
                        // current hit has been associated with a cluster,
                        // then it is a common hit between its previous
                        // seed and the neighboring seed.
                        if (hitSeedMap.containsKey(hit)) {
                        	// Check and see if a list of common seeds
                        	// for this hit already exists or not.
                        	List<CalorimeterHit> commonHitList = commonHits.get(hit);
                        	
                        	// If it does not, make a new one.
                        	if(commonHitList == null) { commonHitList = new ArrayList<CalorimeterHit>(); }
                        	
                        	// Add the neighbors to the seeds to set of
                        	// common seeds.
                            commonHitList.add(neighborHit);
                            commonHitList.add(hitSeedMap.get(hit));

                            
                            // Put the common seed list back into the set.
                            commonHits.put(hit, commonHitList);
                        }
                        
                        // If the neighboring hit is a seed hit and the
                    	// current hit has not been added to a cluster yet
                    	// associate it with the neighboring seed and note
                        // that it has been clustered.
                        else {
                          	hitSeedMap.put(hit, neighborHit);
                        }
                	}
                }
            }
        } // End primary seed loop.
        
        // Performs second pass calculations for component hits.
        secondaryHitsLoop:
        for (CalorimeterHit secondaryHit : hitList) {
        	// Look for hits that already have an associated seed/clustering.
        	if(!hitSeedMap.containsKey(secondaryHit)) { continue secondaryHitsLoop; }
        	
        	// Get the secondary hit's neighboring crystals.
            Set<Long> secondaryNeighbors = neighborMap.get(secondaryHit.getCellID());
            
            // Make a list to store the hits associated with the
            // neighboring crystals.
            List<CalorimeterHit> secondaryNeighborHits = new ArrayList<CalorimeterHit>();
            
            // Loop through the neighboring crystals.
            for (Long secondaryNeighbor : secondaryNeighbors) {
            	// Get the hit associated with the neighboring crystal.
            	CalorimeterHit secondaryNeighborHit = hitMap.get(secondaryNeighbor);
            	
            	// If the neighboring crystal exists and is not already
            	// in a cluster, add it to the list of neighboring hits.
                if (secondaryNeighborHit != null && !hitSeedMap.containsKey(secondaryNeighborHit)) {
                	secondaryNeighborHits.add(secondaryNeighborHit);
                }
            }
            
            // Loop over the secondary neighbor hits.
            for (CalorimeterHit secondaryNeighborHit : secondaryNeighborHits) {
            	// If the neighboring hit is of lower energy than the
            	// current secondary hit, then associate the neighboring
            	// hit with the current secondary hit's seed.
            	if(!equalEnergies(secondaryNeighborHit, secondaryHit)){
            		hitSeedMap.put(secondaryNeighborHit, hitSeedMap.get(secondaryHit));}
            	else {continue;}
            }
        } // End component hits loop.

      
        // Performs second pass calculations for common hits.
        commonHitsLoop:
        for (CalorimeterHit clusteredHit : hitSeedMap.keySet()) {
        	        	
        	// Seed hits are never common hits and can be skipped.
        	if(hitSeedMap.get(clusteredHit) == clusteredHit) { continue commonHitsLoop; }
        	
    		// Get the current clustered hit's neighboring crystals.
            Set<Long> clusteredNeighbors = neighborMap.get(clusteredHit.getCellID());
            
            // Store a list of all the clustered hits neighboring
            // crystals which themselves contain hits.
            List<CalorimeterHit> clusteredNeighborHits = new ArrayList<CalorimeterHit>();
            
            // Loop through the neighbors and see if they have hits.
            for (Long neighbor : clusteredNeighbors) {
            	// Get the hit associated with the neighbor.
            	CalorimeterHit clusteredNeighborHit = hitMap.get(neighbor);
            	
            	// If it exists, add it to the neighboring hit list.

                if (clusteredNeighborHit != null && hitSeedMap.get(clusteredNeighborHit) != null) {         	
                	clusteredNeighborHits.add(clusteredNeighborHit);
                }
            }
            
            // Get the seed hit associated with this clustered hit.
            CalorimeterHit clusteredHitSeed = hitSeedMap.get(clusteredHit);

            
            // Loop over the clustered neighbor hits.
            for (CalorimeterHit clusteredNeighborHit : clusteredNeighborHits) {
            	// Check to make sure that the clustered neighbor hit
            	// is not already associated with the current clustered
            	// hit's seed.                    	
            	
                if ((hitSeedMap.get(clusteredNeighborHit) != clusteredHitSeed)){
                	// Check for lowest energy hit and that comparison hit is not already common. 
                	// If already common, this boundary is already accounted for. 
                	if(!equalEnergies(clusteredHit, clusteredNeighborHit)
                			&& !commonHits.containsKey(clusteredNeighborHit)){
                		                		     		
                			// Check and see if a list of common seeds
                			// for this hit already exists or not.
                			List<CalorimeterHit> commonHitList = commonHits.get(clusteredHit);
                    	
                			// If it does not, make a new one.                 	
                			if(commonHitList == null) { commonHitList = new ArrayList<CalorimeterHit>();}
                    	
                			// Add the neighbors to the seeds to set of
                			// common seeds.
                			commonHitList.add(clusteredHitSeed);                			
                			commonHitList.add(hitSeedMap.get(clusteredNeighborHit));
                        
                			// Put the common seed list back into the set.
                			commonHits.put(clusteredHit, commonHitList); 
                			
                	}
                }             
            }
        } // End common hits loop.

        
        // Remove any common hits from the clustered hits collection.
        for(CalorimeterHit commonHit : commonHits.keySet()) {
        	hitSeedMap.remove(commonHit);
        }
        
        
        /*
         * All hits are sorted from above. The next part of the code is for calculating energies and positions.
         */
                
        //Create map to contain the total energy of each cluster
        Map<CalorimeterHit, Double> seedEnergy = new HashMap<CalorimeterHit, Double>();
        
        // Get energy of each cluster, excluding common hits
        for (CalorimeterHit iSeed : hitList) {
            if(hitSeedMap.get(iSeed) == iSeed) {
            	seedEnergy.put(iSeed, 0.0);
            }
        }
        
        //Putting total cluster energies excluding common hit energies into map with seed keys    
        for (Map.Entry<CalorimeterHit, CalorimeterHit> entry : hitSeedMap.entrySet()) {
            CalorimeterHit eSeed = entry.getValue();
            double eEnergy = seedEnergy.get(eSeed);
            eEnergy += entry.getKey().getCorrectedEnergy();
            seedEnergy.put(eSeed, eEnergy);
        }

        // Create a map to contain final uncorrected cluster energies including common hit distributions.
        Map<CalorimeterHit, Double> seedEnergyTot = seedEnergy;
        
        //Distribute common hit energies with clusters
        for (Map.Entry<CalorimeterHit, List<CalorimeterHit>> entry1 : commonHits.entrySet()) {
        	CalorimeterHit commonCell = entry1.getKey();
        	CalorimeterHit seedA = entry1.getValue().get(0);
        	CalorimeterHit seedB = entry1.getValue().get(1);    	
        	double eFractionA = (seedEnergy.get(seedA))/((seedEnergy.get(seedA)+seedEnergy.get(seedB)));
        	double eFractionB = (seedEnergy.get(seedB))/((seedEnergy.get(seedA)+seedEnergy.get(seedB)));
        	double currEnergyA = seedEnergyTot.get(seedA);
        	double currEnergyB = seedEnergyTot.get(seedB);
        	currEnergyA += eFractionA * commonCell.getCorrectedEnergy();
        	currEnergyB += eFractionB * commonCell.getCorrectedEnergy();

        	seedEnergyTot.put(seedA, currEnergyA);
        	seedEnergyTot.put(seedB, currEnergyB);       	
        }
        
        
        // Make mapping to contain clusters with corrected energy.
        Map<CalorimeterHit, Double> seedEnergyCorr = new HashMap<CalorimeterHit, Double>();
        
        // Energy Corrections as per HPS Note 2014-001
            // Iterate through known clusters with energies and apply correction.
            for (Map.Entry<CalorimeterHit, Double> entryC : seedEnergyTot.entrySet()) {
                double rawEnergy = entryC.getValue();
                
                // Energy correction for initial guess of electron:
                int pdg = 11;
                double corrEnergy = enCorrection(pdg, rawEnergy);

                seedEnergyCorr.put(entryC.getKey(), corrEnergy);    
            }// end of energy corrections
        
                
        // Cluster Position as per HPS Note 2014-001
        // Create map with seed as key to position/centroid value
        Map<CalorimeterHit, double[]> rawSeedPosition = new HashMap<CalorimeterHit, double[]>();
        Map<CalorimeterHit, double[]> corrSeedPosition = new HashMap<CalorimeterHit, double[]>();
        
        // top level iterates through seeds
        for (Map.Entry<CalorimeterHit, Double> entryS : seedEnergyTot.entrySet()) {
        	//get the seed for this iteration
           	CalorimeterHit seedP = entryS.getKey();
           	
           	double xCl = 0.0; // calculated cluster x position, prior to correction
            double yCl = 0.0; // calculated cluster y position
            double eNumX = 0.0; 
            double eNumY = 0.0;
            double eDen = 0.0;
            double w0 = 3.1;

        	// iterate through hits corresponding to seed
        	for (Map.Entry<CalorimeterHit, CalorimeterHit> entryP : hitSeedMap.entrySet()) {
        		if(entryP.getValue() == seedP){
        			///////////////////////////////
        			// This block fills a map with crystal to center of face of crystal 
        			// Get the hit indices as a Point.
        			int ix = entryP.getKey().getIdentifierFieldValue("ix");
        			int iy = entryP.getKey().getIdentifierFieldValue("iy");
        			Point hitIndex = new Point(ix, iy);

        			// Get the corrected position for this index pair.
        			double[] position = correctedPositionMap.get(hitIndex);

        			// If the result is null, it hasn't been calculated yet.
        			if(position == null) {
        				// Calculate the corrected position.
        				IGeometryInfo geom = entryP.getKey().getDetectorElement().getGeometry();
        				double[] pos = geom.transformLocalToGlobal(VecOp.add(geom.transformGlobalToLocal(geom.getPosition()),(Hep3Vector)new BasicHep3Vector(0,0,-1*((Trd)geom.getLogicalVolume().getSolid()).getZHalfLength()))).v();
      
        				// Convert the result to  a Double[] array.
        				position = new double[3];
        				position[0] = pos[0];
        				position[1] = pos[1];
        				position[2] = pos[2];
      
        				// Store the result in the map.
        				correctedPositionMap.put(hitIndex, position);
        			}
        			///////////////////////////////
        	
        			//Use Method 3 weighting scheme described in note:
        			eNumX += Math.max(0.0,(w0+Math.log(entryP.getKey().getCorrectedEnergy()
        					/seedEnergyTot.get(seedP))))*(correctedPositionMap.get(hitIndex)[0]/10.0);    		
        			eNumY += Math.max(0.0,(w0+Math.log(entryP.getKey().getCorrectedEnergy()
        					/seedEnergyTot.get(seedP))))*(correctedPositionMap.get(hitIndex)[1]/10.0);
        			eDen += Math.max(0.0, (w0+Math.log(entryP.getKey().getCorrectedEnergy()/
        					seedEnergyTot.get(seedP))));        	        	
        		}
        		
        	}
        	
        	xCl = eNumX/eDen;
            yCl = eNumY/eDen;
            
            double[] rawPosition = new double[3];
            rawPosition[0] = xCl*10.0;//mm
            rawPosition[1] = yCl*10.0;//mm
            int ix = seedP.getIdentifierFieldValue("ix");
			int iy = seedP.getIdentifierFieldValue("iy");
			Point hitIndex = new Point(ix, iy);
            rawPosition[2] = correctedPositionMap.get(hitIndex)[2];
            
            
            
            // Apply position correction factors:
            // Position correction for electron:
            int pdg = 11;
            double xCorr = posCorrection(pdg, xCl*10.0, seedEnergyTot.get(seedP));
           
            double[] corrPosition = new double[3];
            corrPosition[0] = xCorr*10.0;//mm
            corrPosition[1] = yCl*10.0;//mm
            corrPosition[2] = correctedPositionMap.get(hitIndex)[2];
                        
            corrSeedPosition.put(seedP, corrPosition);
            rawSeedPosition.put(seedP, rawPosition);

        	
        }// end of cluster position calculation

                
        /*
         * Outputs results to cluster collection. 
         */
        // Only write output if something actually exists.
        if (hitMap.size() != 0) {
            // Loop over seeds
            for (Map.Entry<CalorimeterHit, CalorimeterHit> entry2 : hitSeedMap.entrySet()) {
                if (entry2.getKey() == entry2.getValue()){
                	if(seedEnergyCorr.get(entry2.getKey())<clusterEnergyThreshold) 
                	{	
                		//Not clustered for not passing cuts
                		rejectedHitList.add(entry2.getKey()); 
                	}
                	
                	else{
                		// New cluster
                		HPSEcalClusterIC cluster = new HPSEcalClusterIC(entry2.getKey());
                		clusterList.add(cluster);
                		// Loop over hits belonging to seeds
                		for (Map.Entry<CalorimeterHit, CalorimeterHit> entry3 : hitSeedMap.entrySet()) {
                			if (entry3.getValue() == entry2.getValue()) {
                				if(rejectedHitList.contains(entry2.getValue())){
                					rejectedHitList.add(entry3.getKey());
                				}
                				else{
                					// Add hit to cluster
                					cluster.addHit(entry3.getKey());
                				}
                			}
                		}
                		
                    for (Map.Entry<CalorimeterHit, List<CalorimeterHit>> entry4 : commonHits.entrySet()) {
                        if (entry4.getValue().contains(entry2.getKey())) {                       	
                        	// Add shared hits for energy distribution between clusters
                            cluster.addSharedHit(entry4.getKey()); 
                        }
                    }
                                        
                    //Input both raw and corrected cluster energies
            		if (seedEnergyCorr.values().size() > 0){
            			cluster.setEnergy(seedEnergyCorr.get(entry2.getKey()));
            			cluster.setRawEnergy(seedEnergyTot.get(entry2.getKey()));
            			}

            		//Input both uncorrected and corrected cluster positions. 
            		cluster.setCorrPosition(corrSeedPosition.get(entry2.getKey()));
            		cluster.setRawPosition(rawSeedPosition.get(entry2.getKey()));
                  
                    
                	}// End checking thresholds and write out.
                }                            
            } //End cluster loop
//            System.out.println("Number of clusters: "+clusterList.size());    

            
        } //End event output loop.
        int flag = 1 << LCIOConstants.CLBIT_HITS;
        event.put(clusterCollectionName, clusterList, HPSEcalClusterIC.class, flag);
        event.put(rejectedHitName, rejectedHitList, CalorimeterHit.class, flag);
        
        
    }
    
    public void endOfData() {
        if (writeHits != null) {
            // Close the event display output writer.
            try {
                writeHits.close();
            } catch (IOException e) {
            }
        }
    }
    
 
    private static class EnergyComparator implements Comparator<CalorimeterHit> {
    	/**
    	 * Compares the first hit with respect to the second. This
    	 * method will compare hits first by energy, and then spatially.
    	 * In the case of equal energy hits, the hit closest to the
    	 * beam gap and closest to the positron side of the detector
    	 * will be selected. If all of these conditions are true, the
    	 * hit with the positive y-index will be selected. Hits with
    	 * all four conditions matching are the same hit.
    	 * @param hit1 The hit to compare.
    	 * @param hit2 The hit with respect to which the first should
    	 * be compared.
    	 */
    public int compare(CalorimeterHit hit1, CalorimeterHit hit2) {
    	// Hits are sorted on a hierarchy by three conditions. First,
    	// the hits with the highest energy come first. Next, they
    	// are ranked by vertical proximity to the beam gap, and
    	// lastly, they are sorted by horizontal proximity to the
    	// positron side of the detector.
    	
    	// Get the hit energies.
    	double[] e = { hit1.getCorrectedEnergy(), hit2.getCorrectedEnergy() };
    	
    	// Perform the energy comparison. The higher energy hit
    	// will be ordered first.
    	if(e[0] < e[1]) { return 1; }
    	else if(e[0] > e[1]) { return -1; }
    	
    	// If the hits are the same energy, we must perform the
    	// spatial comparisons.
    	else {
    		// Get the position with respect to the beam gap.
    		int[] iy = { Math.abs(hit1.getIdentifierFieldValue("iy")), Math.abs(hit2.getIdentifierFieldValue("iy")) };
    		
    		// The closest hit is first.
    		if(iy[0] > iy[1]) { return -1; }
    		else if(iy[0] < iy[1]) { return 1; }
    		
    		// Hits that are identical in vertical distance from
    		// beam gap and energy are differentiated with distance
    		// horizontally from the positron side of the detector.
    		else {
        		// Get the position from the positron side.
        		int[] ix = { hit1.getIdentifierFieldValue("ix"), hit2.getIdentifierFieldValue("ix") };
        		
        		// The closest hit is first.
        		if(ix[0] > ix[1]) { return 1; }
        		else if(ix[0] < ix[1]) { return -1; }
    			
        		// If all of these checks are the same, compare
        		// the raw value for iy. If these are identical,
        		// then the two hits are the same. Otherwise, sort
        		// the numerical value of iy. (This removes the
        		// issue where hits (x, y) and (x, -y) can have
        		// the same energy and be otherwise seen as the
        		// same hit from the above checks.
        		else { return Integer.compare(hit1.getIdentifierFieldValue("iy"), hit2.getIdentifierFieldValue("iy")); }
    		}
    	}
    }
}
     
    

    /**
     * Handles pathological case where multiple neighboring crystals have EXACTLY the same energy.
     * @param hit
     * @param neighbor Neighbor to hit
     * @return boolean value of if the hit is a seed
     */
    private boolean equalEnergies(CalorimeterHit hit, CalorimeterHit neighbor){
    	boolean isSeed = true;
    	
    	int hix = hit.getIdentifierFieldValue("ix");
    	int hiy = Math.abs(hit.getIdentifierFieldValue("iy"));
    	int nix = neighbor.getIdentifierFieldValue("ix");
    	int niy = Math.abs(neighbor.getIdentifierFieldValue("iy"));
    	double hE = hit.getCorrectedEnergy();
    	double nE = neighbor.getCorrectedEnergy();
    	if(hE < nE) {
    		isSeed = false;
    	}
    	else if((hE == nE) && (hiy > niy)) {
    		isSeed = false;
    	}
    	else if((hE == nE) && (hiy == niy) && (hix > nix)) {
    		isSeed = false;
    	}
    	return isSeed;	
    }
    /**
     * Calculates energy correction based on cluster raw energy and particle type as per 
     *<a href="https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014">HPS Note 2014-001</a>
     * @param pdg Particle id as per PDG
     * @param rawEnergy Raw Energy of the cluster (sum of hits with shared hit distribution)
     * @return Corrected Energy
     */    
    public double enCorrection(int pdg, double rawEnergy){
  	   if (pdg == 11) { // Particle is electron  		   
  		   return energyCorrection(rawEnergy, ELECTRON_ENERGY_A, ELECTRON_ENERGY_B, ELECTRON_ENERGY_C);   
  	   }
  	   else if (pdg == -11) { //Particle is positron
		   return energyCorrection(rawEnergy, POSITRON_ENERGY_A, POSITRON_ENERGY_B, POSITRON_ENERGY_C);   
  	   }
  	   else if (pdg == 22) { //Particle is photon
		   return energyCorrection(rawEnergy, PHOTON_ENERGY_A, PHOTON_ENERGY_B, PHOTON_ENERGY_C);   
  	   }
  	   else { //Unknown 
  		   double corrEnergy = rawEnergy;
  		   return corrEnergy;}
  	   
     }   
    
    /**
     * Calculates the energy correction to a cluster given the variables from the fit as per
     * <a href="https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014">HPS Note 2014-001</a>
     * @param rawEnergy Raw energy of the cluster
     * @param A,B,C from fitting in note
     * @return Corrected Energy
     */   
    public double energyCorrection(double rawEnergy, double varA, double varB, double varC){
    	double corrEnergy = rawEnergy / (varA * rawEnergy + varB / (Math.sqrt(rawEnergy)) + varC);
    	return corrEnergy;
    }
       
    
    /**
     * Calculates position correction based on cluster raw energy, x calculated position, 
     * and particle type as per 
     * <a href="https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014">HPS Note 2014-001</a>
     * @param pdg Particle id as per PDG
     * @param xCl Calculated x centroid position of the cluster, uncorrected, at face
     * @param rawEnergy Raw energy of the cluster (sum of hits with shared hit distribution)
     * @return Corrected x position
     */
    public double posCorrection(int pdg, double xPos, double rawEnergy){
    	double xCl = xPos/10.0;//convert to mm
    	if (pdg == 11) { //Particle is electron    	
    		double xCorr = positionCorrection(xCl, rawEnergy, ELECTRON_POS_A, ELECTRON_POS_B, ELECTRON_POS_C, ELECTRON_POS_D, ELECTRON_POS_E);
    		return xCorr*10.0;
    	}
    	else if (pdg == -11) {// Particle is positron   	
    		double xCorr = positionCorrection(xCl, rawEnergy, POSITRON_POS_A, POSITRON_POS_B, POSITRON_POS_C, POSITRON_POS_D, POSITRON_POS_E);
    		return xCorr*10.0;
    	}
    	else if (pdg == 22) {// Particle is photon  	
    		double xCorr = positionCorrection(xCl, rawEnergy, PHOTON_POS_A, PHOTON_POS_B, PHOTON_POS_C, PHOTON_POS_D, PHOTON_POS_E);
    		return xCorr*10.0;
    	}
    	else { //Unknown 
    		double xCorr = xCl;
    		return xCorr*10.0;}
    	}
    
    
   /**
    * Calculates the position correction in cm using the raw energy and variables associated with the fit
    * of the particle as described in  
    * <a href="https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014">HPS Note 2014-001</a>
    * @param xCl
    * @param rawEnergy
    * @param varA
    * @param varB
    * @param varC
    * @param varD
    * @param varE
    * @return
    */    
    public double positionCorrection(double xCl, double rawEnergy, double varA, double varB, double varC, double varD, double varE){
    	double xCorr = xCl-(varA/Math.sqrt(rawEnergy) + varB )*xCl-
				(varC*rawEnergy + varD/Math.sqrt(rawEnergy) + varE);
    	return xCorr;
    }
   
    	
 }    