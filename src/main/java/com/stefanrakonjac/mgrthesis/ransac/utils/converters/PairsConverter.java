/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.utils.converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import messif.objects.impl.ObjectFeature;
import boofcv.struct.geo.AssociatedPair;

/**
 * @author Stefan.Rakonjac
 *
 */
public class PairsConverter {
	
	private static final Logger logger = LoggerFactory.getLogger(PairsConverter.class);

	/**
	 * BoofCV-friendly pairs converter
	 * 
	 * @param pairs
	 * @return
	 *		Array of {@code double} arrays
	 */
	public static double[][] getPairs (final List<AssociatedPair> pairs) {
		if(pairs == null) throw new IllegalArgumentException("pairs is null");
		
		final double[][] retval = new double[pairs.size()][4];
		
		for(int i = 0; i < pairs.size(); i++) {
			final AssociatedPair pair = pairs.get(i);
			retval[i][0] = pair.getP1().getX();
			retval[i][1] = pair.getP1().getY();
			retval[i][2] = pair.getP2().getX();
			retval[i][3] = pair.getP2().getY();
		}
		
		return retval;
	}
	

	/**
	 * MESSIF-friendly pairs converter
	 * 
	 * @param pairs
	 * @return
	 *		Array of {@code double} arrays
	 */
	public static double[][] getPairs (final Map<ObjectFeature, ObjectFeature> pairs) {
		if(pairs == null) throw new IllegalArgumentException("pairs is null");
		
		final double[][] retval = new double[pairs.size()][4];
		
		int i = 0;
		for(Entry<ObjectFeature, ObjectFeature> pair : pairs.entrySet()) {
			retval[i][0] = pair.getKey().getX();
			retval[i][1] = pair.getKey().getY();
			retval[i][2] = pair.getValue().getX();
			retval[i][3] = pair.getValue().getY();
			i++;
		}
		
		return retval;
	}

	/**
	 * BoofCV-friendly pairs converter
	 * @param pairs
	 * @return
	 */
	public static List<AssociatedPair> toAssociatedPairList(double[][] pairs) {
		if(pairs == null) throw new IllegalArgumentException("pairs is null");
		logger.debug("toAssociatedPairList(pairs[{}][4])", pairs.length);
		
		List<AssociatedPair> retval = new ArrayList<>(pairs.length);
		for(double[] pair : pairs) {
			retval.add(new AssociatedPair(pair[0], pair[1], pair[2], pair[3]));
		}
		return retval;
	}
}
