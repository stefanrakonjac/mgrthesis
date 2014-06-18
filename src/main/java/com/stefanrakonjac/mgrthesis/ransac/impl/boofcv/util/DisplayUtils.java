/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.impl.boofcv.util;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.stefanrakonjac.mgrthesis.ransac.impl.struct.PrecisionRecall;

import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.geo.AssociatedPair;

/**
 * @author Stefan.Rakonjac
 *
 */
public class DisplayUtils {

	public static void showPairs(String leftImageURL, String rightImageURL, List<AssociatedPair> associatedPairList) {
		
		BufferedImage imageA = UtilImageIO.loadImage(leftImageURL);
		BufferedImage imageB = UtilImageIO.loadImage(rightImageURL);
 
		// display the inlier matches found using BoofCV Ransac
		AssociationPanel panel = new AssociationPanel(20);
		panel.setAssociation(associatedPairList);
		panel.setImages(imageA,imageB);

		ShowImages.showWindow(panel, "Inlier Pairs");
	}
	
	public static void showScatterPlot(final String plotName, final String[] imageNames, final PointType[] pointTypes, final double[] matchesCount) {
		
		XYSeriesCollection seriesCollection = new XYSeriesCollection();
	    
	    final Map<PointType, XYSeries> seriesByPointTypes = new HashMap<>();
	    
	    final XYSeries seriesPositive = new XYSeries("Positive");
	    seriesCollection.addSeries(seriesPositive);
	    seriesByPointTypes.put(PointType.POSITIVE, seriesPositive);
	    
	    final XYSeries seriesNegative = new XYSeries("Negative");
	    seriesCollection.addSeries(seriesNegative);
	    seriesByPointTypes.put(PointType.NEGATIVE, seriesNegative);
	    
	    final XYSeries seriesNeutral = new XYSeries("Neutral");
	    seriesCollection.addSeries(seriesNeutral);
	    seriesByPointTypes.put(PointType.NEUTRAL, seriesNeutral);
	    
	    for(int i = 0; i < imageNames.length; i++) {
	    	XYDataItem dataItem = new XYDataItem((double) i+1, matchesCount[i]);
	    	seriesByPointTypes.get(pointTypes[i]).add(dataItem);
	    }
	    SymbolAxis axis = new SymbolAxis("image names", imageNames);
	    axis.setVerticalTickLabels(true);
	    
		JFreeChart scatterPlot = ChartFactory.createScatterPlot(plotName, "image names", "image scores", seriesCollection, PlotOrientation.VERTICAL, true, true, true);
		scatterPlot.getXYPlot().setDomainAxis(axis);
		
        ChartFrame frame = new ChartFrame(plotName, scatterPlot);
        frame.pack();
        frame.setVisible(true);
	}

	public static void showPrecisionRecallCurves(final String plotName, final List<PrecisionRecall> precisionRecallsA, List<PrecisionRecall> precisionRecallsB) {

		XYSeriesCollection seriesCollection = new XYSeriesCollection();

	    final XYSeries firstSeries = new XYSeries("First curve");
	    seriesCollection.addSeries(firstSeries);
	    
	    for(PrecisionRecall precisionRecall : precisionRecallsA) {
	    	XYDataItem dataItem = new XYDataItem(precisionRecall.getPrecision(), precisionRecall.getRecall());
	    	firstSeries.add(dataItem);
	    }

	    final XYSeries secondSeries = new XYSeries("Second curve");
	    seriesCollection.addSeries(secondSeries);
	    
	    for(PrecisionRecall precisionRecall : precisionRecallsB) {
	    	XYDataItem dataItem = new XYDataItem(precisionRecall.getPrecision(), precisionRecall.getRecall());
	    	secondSeries.add(dataItem);
	    }
	    
		JFreeChart chart = ChartFactory.createXYLineChart(plotName, "recall", "precision", seriesCollection, PlotOrientation.VERTICAL, true, true, true);
		
        ChartFrame frame = new ChartFrame(plotName, chart);
        frame.pack();
        frame.setVisible(true);
	}
	
	public static enum PointType {
		POSITIVE, NEUTRAL, NEGATIVE;
	}
}
