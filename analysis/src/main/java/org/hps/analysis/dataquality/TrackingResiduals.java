package org.hps.analysis.dataquality;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;

/**
 * DQM driver for track residuals in position and time
 *
 * @author mgraham on June 5, 2014
 */
// TODO:  Add some quantities for DQM monitoring: 
public class TrackingResiduals extends DataQualityMonitor {

    // Collection Names
    String trackTimeDataCollectionName = "TrackTimeData";
    String trackResidualsCollectionName = "TrackResiduals";
    String gblStripClusterDataCollectionName = "GBLStripClusterData";

    int nEvents = 0;

    private String plotDir = "TrackResiduals/";
    String[] trackingQuantNames = {};
    int nmodules = 6;
    private String posresDir = "PostionResiduals/";
    private String uresDir = "UResiduals/";
    private String timeresDir = "TimeResiduals/";
    private Map<String, Double> xposTopMeanResidMap;
    private Map<String, Double> yposTopMeanResidMap;
    private Map<String, Double> xposBotMeanResidMap;
    private Map<String, Double> yposBotMeanResidMap;
    private Map<String, Double> timeMeanResidMap;
    private Map<String, Double> xposTopSigmaResidMap;
    private Map<String, Double> yposTopSigmaResidMap;
    private Map<String, Double> xposBotSigmaResidMap;
    private Map<String, Double> yposBotSigmaResidMap;
    private Map<String, Double> timeSigmaResidMap;

    @Override
    protected void detectorChanged(Detector detector) {

        aida.tree().cd("/");
        resetOccupancyMap();
        for (int i = 1; i <= nmodules; i++) {
            IHistogram1D xresid = aida.histogram1D(plotDir + posresDir + "Module " + i + " Top x Residual", 50, -getRange(i, true), getRange(i, true));
            IHistogram1D yresid = aida.histogram1D(plotDir + posresDir + "Module " + i + " Top y Residual", 50, -getRange(i, false), getRange(i, false));
            IHistogram1D xresidbot = aida.histogram1D(plotDir + posresDir + "Module " + i + " Bot x Residual", 50, -getRange(i, true), getRange(i, true));
            IHistogram1D yresidbot = aida.histogram1D(plotDir + posresDir + "Module " + i + " Bot y Residual", 50, -getRange(i, false), getRange(i, false));
        }

        for (int i = 1; i <= nmodules * 2; i++) {
            IHistogram1D tresid = aida.histogram1D(plotDir + timeresDir + "HalfModule " + i + " t Residual", 50, -20, 20);
            IHistogram1D utopresid = aida.histogram1D(plotDir + uresDir + "HalfModule " + i + " Top u Residual", 50, -getRange((i + 1) / 2, false), getRange((i + 1) / 2, false));
            IHistogram1D ubotresid = aida.histogram1D(plotDir + uresDir + "HalfModule " + i + " Bot u Residual", 50, -getRange((i + 1) / 2, false), getRange((i + 1) / 2, false));
        }
    }

    @Override
    public void process(EventHeader event) {
        aida.tree().cd("/");
        if (!event.hasCollection(GenericObject.class, trackTimeDataCollectionName))
            return;
        if (!event.hasCollection(GenericObject.class, trackResidualsCollectionName))
            return;
        nEvents++;
        List<GenericObject> trdList = event.get(GenericObject.class, trackResidualsCollectionName);
        for (GenericObject trd : trdList) {
            int nResid = trd.getNDouble();
            int isBot = trd.getIntVal(trd.getNInt() - 1);//last Int is the top/bottom flag
            for (int i = 1; i <= nResid; i++)

                if (isBot == 1) {
                    aida.histogram1D(plotDir + posresDir + "Module " + i + " Bot x Residual").fill(trd.getDoubleVal(i - 1));//x is the double value in the generic object
                    aida.histogram1D(plotDir + posresDir + "Module " + i + " Bot y Residual").fill(trd.getFloatVal(i - 1));//y is the float value in the generic object
                } else {
                    aida.histogram1D(plotDir + posresDir + "Module " + i + " Top x Residual").fill(trd.getDoubleVal(i - 1));//x is the double value in the generic object
                    aida.histogram1D(plotDir + posresDir + "Module " + i + " Top y Residual").fill(trd.getFloatVal(i - 1));//y is the float value in the generic object                    
                }
        }

        List<GenericObject> ttdList = event.get(GenericObject.class, trackTimeDataCollectionName);
        for (GenericObject ttd : ttdList) {
            int nResid = ttd.getNDouble();
            for (int i = 1; i <= nResid; i++)
                aida.histogram1D(plotDir + timeresDir + "HalfModule " + i + " t Residual").fill(ttd.getDoubleVal(i - 1));//x is the double value in the generic object               
        }
        if (!event.hasCollection(GenericObject.class, gblStripClusterDataCollectionName))
            return;
        List<GenericObject> gblSCDList = event.get(GenericObject.class, gblStripClusterDataCollectionName);
        for (GenericObject gblSCD : gblSCDList) {
            double umeas = gblSCD.getDoubleVal(15);//TODO:  implement generic methods into GBLStripClusterData so this isn't hard coded
            double utrk = gblSCD.getDoubleVal(16);//implement generic methods into GBLStripClusterData so this isn't hard coded
            double resid = umeas - utrk;
            double tanlambda = gblSCD.getDoubleVal(21);//use the slope as a proxy for the top/bottom half of tracker

            int i = gblSCD.getIntVal(0);//implement generic methods into GBLStripClusterData so this isn't hard coded
            if (tanlambda > 0)
                aida.histogram1D(plotDir + uresDir + "HalfModule " + i + " Top u Residual").fill(resid);//x is the double value in the generic object                 
            else
                aida.histogram1D(plotDir + uresDir + "HalfModule " + i + " Bot u Residual").fill(resid);//x is the double value in the generic object                 

        }
    }

    @Override
    public void calculateEndOfRunQuantities() {
        IAnalysisFactory analysisFactory = IAnalysisFactory.create();
        IFitFactory fitFactory = analysisFactory.createFitFactory();
        IFitter fitter = fitFactory.createFitter("chi2");
        IPlotter plotterXTop = analysisFactory.createPlotterFactory().create("x-residual Top");
        IPlotter plotterXBottom = analysisFactory.createPlotterFactory().create("x-residual Bottom");
        IPlotter plotterYTop = analysisFactory.createPlotterFactory().create("y-residual Top");
        IPlotter plotterYBottom = analysisFactory.createPlotterFactory().create("y-residual Bottom");

        IPlotter plotterTime = analysisFactory.createPlotterFactory().create("Track Time");

        plotterXTop.createRegions(2, 3);
        IPlotterStyle pstyle = plotterXTop.style();
        pstyle.legendBoxStyle().setVisible(false);
        plotterXBottom.createRegions(2, 3);
        IPlotterStyle pstyle2 = plotterXBottom.style();
        pstyle2.legendBoxStyle().setVisible(false);

        plotterYTop.createRegions(2, 3);
        IPlotterStyle pstyle3 = plotterYTop.style();
        pstyle3.legendBoxStyle().setVisible(false);
        plotterYBottom.createRegions(2, 3);
        IPlotterStyle pstyle4 = plotterYBottom.style();
        pstyle4.legendBoxStyle().setVisible(false);

        plotterTime.createRegions(3, 4);
        IPlotterStyle pstyle5 = plotterTime.style();
        pstyle5.legendBoxStyle().setVisible(false);

        int irXTop = 0;
        int irXBot = 0;
        int irYTop = 0;
        int irYBot = 0;
        for (int i = 1; i <= nmodules; i++) {
            IHistogram1D xresidTop = aida.histogram1D(plotDir + posresDir + "Module " + i + " Top x Residual");
            IHistogram1D yresidTop = aida.histogram1D(plotDir + posresDir + "Module " + i + " Top y Residual");
            IHistogram1D xresidBot = aida.histogram1D(plotDir + posresDir + "Module " + i + " Bot x Residual");
            IHistogram1D yresidBot = aida.histogram1D(plotDir + posresDir + "Module " + i + " Bot y Residual");
            IFitResult xresultTop = fitGaussian(xresidTop, fitter, "range=\"(-1.0,1.0)\"");
            IFitResult yresultTop = fitGaussian(yresidTop, fitter, "range=\"(-0.5,0.5)\"");
            IFitResult xresultBot = fitGaussian(xresidBot, fitter, "range=\"(-1.0,1.0)\"");
            IFitResult yresultBot = fitGaussian(yresidBot, fitter, "range=\"(-8.0,8.0)\"");
            double[] parsXTop = xresultTop.fittedParameters();
            double[] parsYTop = yresultTop.fittedParameters();
            double[] parsXBot = xresultBot.fittedParameters();
            double[] parsYBot = yresultBot.fittedParameters();

            plotterXTop.region(irXTop).plot(xresidTop);
            plotterXTop.region(irXTop).plot(xresultTop.fittedFunction());
            irXTop++;
            plotterXBottom.region(irXBot).plot(xresidBot);
            plotterXBottom.region(irXBot).plot(xresultBot.fittedFunction());
            irXBot++;

            plotterYTop.region(irYTop).plot(yresidTop);
            plotterYTop.region(irYTop).plot(yresultTop.fittedFunction());
            irYTop++;
            plotterYBottom.region(irYBot).plot(yresidBot);
            plotterYBottom.region(irYBot).plot(yresultBot.fittedFunction());
            irYBot++;

            xposTopMeanResidMap.put(getQuantityName(0, 0, 1, i) + "_x", parsXTop[1]);
            xposTopSigmaResidMap.put(getQuantityName(0, 1, 1, i) + "_x", parsXTop[2]);
            yposTopMeanResidMap.put(getQuantityName(0, 0, 1, i) + "_y", parsYTop[1]);
            yposTopSigmaResidMap.put(getQuantityName(0, 1, 1, i) + "_y", parsYTop[2]);
            xposBotMeanResidMap.put(getQuantityName(0, 0, 0, i) + "_x", parsXBot[1]);
            xposBotSigmaResidMap.put(getQuantityName(0, 1, 0, i) + "_x", parsXBot[2]);
            yposBotMeanResidMap.put(getQuantityName(0, 0, 0, i) + "_y", parsYBot[1]);
            yposBotSigmaResidMap.put(getQuantityName(0, 1, 0, i) + "_y", parsYBot[2]);

        }
        int iTime = 0;
        for (int i = 1; i <= nmodules * 2; i++) {
            IHistogram1D tresid = aida.histogram1D(plotDir + timeresDir + "HalfModule " + i + " t Residual");
            IFitResult tresult = fitGaussian(tresid, fitter, "range=\"(-15.0,15.0)\"");
            double[] parsTime = tresult.fittedParameters();
            plotterTime.region(iTime).plot(tresid);
            plotterTime.region(iTime).plot(tresult.fittedFunction());
            iTime++;

            timeMeanResidMap.put(getQuantityName(1, 0, 2, i) + "_dt", parsTime[1]);
            timeSigmaResidMap.put(getQuantityName(1, 1, 2, i) + "_dt", parsTime[2]);

        }

        if (outputPlots) {
            try {
                plotterXTop.writeToFile(outputPlotDir + "X-Residuals-Top.png");
            } catch (IOException ex) {
                Logger.getLogger(SvtMonitoring.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                plotterYTop.writeToFile(outputPlotDir + "Y-Residuals-Top.png");
            } catch (IOException ex) {
                Logger.getLogger(SvtMonitoring.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                plotterXBottom.writeToFile(outputPlotDir + "X-Residuals-Bottom.png");
            } catch (IOException ex) {
                Logger.getLogger(SvtMonitoring.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                plotterYBottom.writeToFile(outputPlotDir + "Y-Residuals-Bottom.png");
            } catch (IOException ex) {
                Logger.getLogger(SvtMonitoring.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                plotterTime.writeToFile(outputPlotDir + "Time-Residuals.png");
            } catch (IOException ex) {
                Logger.getLogger(SvtMonitoring.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private String getQuantityName(int itype, int iquant, int top, int nlayer) {
        String typeString = "position_resid";
        String quantString = "mean_";
        if (itype == 1)
            typeString = "time_resid";
        if (iquant == 1)
            quantString = "sigma_";

        String botString = "bot_";
        if (top == 1)
            botString = "top_";
        if (top == 2)
            botString = "";

        String layerString = "module" + nlayer;
        if (itype == 1)
            layerString = "halfmodule" + nlayer;

        return typeString + quantString + botString + layerString;
    }

    @Override
    public void printDQMData() {
        System.out.println("TrackingResiduals::printDQMData");
        for (Map.Entry<String, Double> entry : xposTopMeanResidMap.entrySet())
            System.out.println(entry.getKey() + " = " + entry.getValue());
        for (Map.Entry<String, Double> entry : xposBotMeanResidMap.entrySet())
            System.out.println(entry.getKey() + " = " + entry.getValue());
        for (Map.Entry<String, Double> entry : xposTopSigmaResidMap.entrySet())
            System.out.println(entry.getKey() + " = " + entry.getValue());
        for (Map.Entry<String, Double> entry : xposBotSigmaResidMap.entrySet())
            System.out.println(entry.getKey() + " = " + entry.getValue());
        for (Map.Entry<String, Double> entry : yposTopMeanResidMap.entrySet())
            System.out.println(entry.getKey() + " = " + entry.getValue());
        for (Map.Entry<String, Double> entry : yposBotMeanResidMap.entrySet())
            System.out.println(entry.getKey() + " = " + entry.getValue());
        for (Map.Entry<String, Double> entry : yposTopSigmaResidMap.entrySet())
            System.out.println(entry.getKey() + " = " + entry.getValue());
        for (Map.Entry<String, Double> entry : yposBotSigmaResidMap.entrySet())
            System.out.println(entry.getKey() + " = " + entry.getValue());
        for (Map.Entry<String, Double> entry : timeMeanResidMap.entrySet())
            System.out.println(entry.getKey() + " = " + entry.getValue());
        for (Map.Entry<String, Double> entry : timeSigmaResidMap.entrySet())
            System.out.println(entry.getKey() + " = " + entry.getValue());
        System.out.println("*******************************");
    }

    @Override
    public void printDQMStrings() {
        for (Map.Entry<String, Double> entry : xposTopMeanResidMap.entrySet())
            System.out.println("ALTER TABLE dqm ADD " + entry.getKey() + " double;");
        for (Map.Entry<String, Double> entry : xposBotMeanResidMap.entrySet())
            System.out.println("ALTER TABLE dqm ADD " + entry.getKey() + " double;");
        for (Map.Entry<String, Double> entry : xposTopSigmaResidMap.entrySet())
            System.out.println("ALTER TABLE dqm ADD " + entry.getKey() + " double;");
        for (Map.Entry<String, Double> entry : xposBotSigmaResidMap.entrySet())
            System.out.println("ALTER TABLE dqm ADD " + entry.getKey() + " double;");
        for (Map.Entry<String, Double> entry : yposTopMeanResidMap.entrySet())
            System.out.println("ALTER TABLE dqm ADD " + entry.getKey() + " double;");
        for (Map.Entry<String, Double> entry : yposBotMeanResidMap.entrySet())
            System.out.println("ALTER TABLE dqm ADD " + entry.getKey() + " double;");
        for (Map.Entry<String, Double> entry : yposTopSigmaResidMap.entrySet())
            System.out.println("ALTER TABLE dqm ADD " + entry.getKey() + " double;");
        for (Map.Entry<String, Double> entry : yposBotSigmaResidMap.entrySet())
            System.out.println("ALTER TABLE dqm ADD " + entry.getKey() + " double;");
        for (Map.Entry<String, Double> entry : timeMeanResidMap.entrySet())
            System.out.println("ALTER TABLE dqm ADD " + entry.getKey() + " double;");
        for (Map.Entry<String, Double> entry : timeSigmaResidMap.entrySet())
            System.out.println("ALTER TABLE dqm ADD " + entry.getKey() + " double;");
    }

    private void resetOccupancyMap() {
        xposBotMeanResidMap = new HashMap<>();
        xposBotSigmaResidMap = new HashMap<>();
        yposBotMeanResidMap = new HashMap<>();
        yposBotSigmaResidMap = new HashMap<>();
        xposTopMeanResidMap = new HashMap<>();
        xposTopSigmaResidMap = new HashMap<>();
        yposTopMeanResidMap = new HashMap<>();
        yposTopSigmaResidMap = new HashMap<>();
        timeMeanResidMap = new HashMap<>();
        timeSigmaResidMap = new HashMap<>();
        for (int i = 0; i < nmodules; i++) {
            xposTopMeanResidMap.put(getQuantityName(0, 0, 1, i) + "_x", -999.);
            yposTopMeanResidMap.put(getQuantityName(0, 0, 1, i) + "_y", -999.);
            xposTopSigmaResidMap.put(getQuantityName(0, 1, 1, i) + "_x", -999.);
            yposTopSigmaResidMap.put(getQuantityName(0, 1, 1, i) + "_y", -999.);
            xposBotMeanResidMap.put(getQuantityName(0, 0, 0, i) + "_x", -999.);
            yposBotMeanResidMap.put(getQuantityName(0, 0, 0, i) + "_y", -999.);
            xposBotSigmaResidMap.put(getQuantityName(0, 1, 0, i) + "_x", -999.);
            yposBotSigmaResidMap.put(getQuantityName(0, 1, 0, i) + "_y", -999.);
        }
        for (int i = 0; i < nmodules * 2; i++) {
            timeMeanResidMap.put(getQuantityName(1, 0, 2, i) + "_dt", -999.);
            timeSigmaResidMap.put(getQuantityName(1, 1, 2, i) + "_dt", -999.);
        }

    }

    IFitResult fitGaussian(IHistogram1D h1d, IFitter fitter, String range) {
        double[] init = {20.0, 0.0, 0.2};
        return fitter.fit(h1d, "g", init, range);
//        double[] init = {20.0, 0.0, 1.0, 20, -1};
//        return fitter.fit(h1d, "g+p1", init, range);
    }

    private double getRange(int layer, boolean isX) {
        double range = 2.5;
        if (isX) {
            if (layer == 1)
                return 0.5;
            if (layer == 2)
                return 0.5;
            if (layer == 3)
                return 0.5;
            if (layer == 4)
                return 1.0;
            if (layer == 5)
                return 1.0;
            if (layer == 6)
                return 1.0;
        } else {
            if (layer == 1)
                return 0.005;
            if (layer == 2)
                return 0.5;
            if (layer == 3)
                return 0.5;
            if (layer == 4)
                return 1.0;
            if (layer == 5)
                return 1.0;
            if (layer == 6)
                return 1.5;
        }
        return range;

    }

}
