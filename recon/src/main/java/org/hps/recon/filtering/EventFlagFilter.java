package org.hps.recon.filtering;

import org.lcsim.event.EventHeader;

/**
 * Accept only events where all of the specified flags exist and have a value of
 * 1.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class EventFlagFilter extends EventReconFilter {

    String[] flagNames = {"svt_bias_good", "svt_position_good", "svt_burstmode_noise_good"};

    public void setFlagNames(String[] flagNames) {
        this.flagNames = flagNames;
    }

    @Override
    public void process(EventHeader event) {
        incrementEventProcessed();
        if (flagNames != null) {
            for (int i = 0; i < flagNames.length; i++) {
                int[] flag = event.getIntegerParameters().get(flagNames[i]);
                if (flag == null || flag[0] == 0) {
                    skipEvent();
                }
            }
        }
        incrementEventPassed();
    }
}