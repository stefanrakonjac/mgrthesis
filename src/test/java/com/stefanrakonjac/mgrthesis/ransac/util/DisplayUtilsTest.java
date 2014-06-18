/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.util;

import org.testng.annotations.Test;

import com.stefanrakonjac.mgrthesis.ransac.impl.boofcv.util.DisplayUtils;
import com.stefanrakonjac.mgrthesis.ransac.impl.boofcv.util.DisplayUtils.PointType;

/**
 * @author Stefan.Rakonjac
 *
 */
public class DisplayUtilsTest {
	
	@Test(enabled=false)
	public void showScatterPlotTest() {
		
		final String[] imageNames = new String[] { "booksh", "box", "castle", "corr", "graff", "head", "kampa", "Kyoto", "leafs", "plant", "rotunda", "shout", "valbonne", "wall", "wash", "zoom" };
		final PointType[] pointTypes = new PointType[] { PointType.POSITIVE, PointType.NEGATIVE, PointType.POSITIVE, PointType.POSITIVE, PointType.NEUTRAL, PointType.POSITIVE, PointType.NEGATIVE, PointType.POSITIVE, PointType.POSITIVE, PointType.NEUTRAL, PointType.POSITIVE, PointType.NEGATIVE, PointType.POSITIVE, PointType.POSITIVE, PointType.NEUTRAL, PointType.POSITIVE };
		final double[] matchesCount = new double[] { 132, 44, 43, 39, 37, 38, 44, 32, 12, 19, 32, 31, 31, 41, 45, 78 };
		
		DisplayUtils.showScatterPlot("booksh", imageNames, pointTypes, matchesCount);
	}

}
