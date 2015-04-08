package org.hps.recon.tracking;

import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.recon.tracking.seedtracker.TrackCheck;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class HitTimeTrackCheck implements TrackCheck {

    private final double rmsTimeCut;
//    private final int minTrackHits = 10;
    private final int minTrackHits = 6;
    private int seedsChecked = 0;
    private int seedsPassed = 0;
    private int tracksChecked = 0;
    private int tracksPassed = 0;
    private boolean debug = false;

    public HitTimeTrackCheck(double rmsTimeCut) {
        this.rmsTimeCut = rmsTimeCut;
    }

    @Override
    public boolean checkSeed(SeedCandidate candidate) {
//        System.out.format("seed with %d hits\n", candidate.getHits().size());
        int nStrips = 0;
        double meanTime = 0;
        for (HelicalTrackHit hth : candidate.getHits()) {
            for (HelicalTrackStrip hts : ((HelicalTrackCross) hth).getStrips()) {
                nStrips++;
                meanTime += hts.time();
            }
        }
        meanTime /= nStrips;
        double rmsTime = 0;
        for (HelicalTrackHit hth : candidate.getHits()) {
            for (HelicalTrackStrip hts : ((HelicalTrackCross) hth).getStrips()) {
                rmsTime += Math.pow(hts.time() - meanTime, 2);
//                rmsTime += hts.time() * hts.time();
//                rmsTime += Math.abs(hts.time());
            }
        }
//        if (nStrips<6) return true;
        seedsChecked++;
//        rmsTime = Math.sqrt(rmsTime / nStrips);
//        System.out.format("seed RMS %f on %d hits\n",rmsTime,nStrips);
        boolean passCheck = (rmsTime < minTrackHits * rmsTimeCut * rmsTimeCut);
//        boolean passCheck = (rmsTime < minTrackHits * rmsTimeCut);
        if (passCheck) {
            seedsPassed++;
        }
        if (debug && seedsChecked % 10000 == 0) {
            System.out.format("Checked %d seeds, %d passed (%d failed)\n", seedsChecked, seedsPassed, seedsChecked - seedsPassed);
        }
        return passCheck;

    }

    @Override
    public boolean checkTrack(SeedTrack track) {
//        System.out.format("track with %d hits\n", track.getTrackerHits().size());
        tracksChecked++;
        int nStrips = 0;
        double meanTime = 0;
        for (TrackerHit hit : track.getTrackerHits()) {
            for (HelicalTrackStrip hts : ((HelicalTrackCross) hit).getStrips()) {
                nStrips++;
                meanTime += hts.time();
            }
        }
        meanTime /= nStrips;
        double rmsTime = 0;
        for (TrackerHit hit : track.getTrackerHits()) {
            for (HelicalTrackStrip hts : ((HelicalTrackCross) hit).getStrips()) {
                rmsTime += Math.pow(hts.time() - meanTime, 2);
//                rmsTime += hts.time() * hts.time();
//                rmsTime += Math.abs(hts.time());
            }
        }
        rmsTime = Math.sqrt(rmsTime / nStrips);
//        rmsTime = rmsTime / nStrips;
//        System.out.format("track RMS %f on %d hits\n", rmsTime, nStrips);
        boolean passCheck = (rmsTime < rmsTimeCut);
        if (passCheck) {
            tracksPassed++;
        }
        if (debug && tracksChecked % 100 == 0) {
            System.out.format("Checked %d tracks, %d passed (%d failed)\n", tracksChecked, tracksPassed, tracksChecked - tracksPassed);
        }
        return passCheck;
    }
}
