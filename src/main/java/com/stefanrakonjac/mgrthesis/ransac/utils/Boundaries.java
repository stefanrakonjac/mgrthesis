/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stefan.Rakonjac
 *
 */
public final class Boundaries {
	
	private static final Logger logger = LoggerFactory.getLogger(Boundaries.class);

	private final double x1;
	private final double y1;
	private final double x2;
	private final double y2;
	
	public Boundaries(String x1, String y1, String x2, String y2) {
		this(Double.parseDouble(x1), Double.parseDouble(y1), Double.parseDouble(x2), Double.parseDouble(y2));
	}
	
	public Boundaries(double x1, double y1, double x2, double y2) {
		super();
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}

	public boolean liesWithin(double x, double y) {
		return x1 <= x && x <= x2 && y1 <= y && y <= y2;
	}
	
	public double[][] limit(double[][] pairs) {
		
		if(pairs == null)
			throw new IllegalArgumentException("pairs");

		final Set<double[]> retval = new HashSet<>();
		
		for(double[] pair : pairs) {
			if(liesWithin(pair[0], pair[1])) {
				retval.add(pair);
			}
		}
		
		return ArraysUtils.to2Ddouble(retval);
	}

	public Map<String, double[][]> limitAll(Map<String, double[][]> mappedPairs) {
		
		if(mappedPairs == null)
			throw new IllegalArgumentException("mappedPairs");
		
		Map<String, double[][]> retval = new HashMap<>();
		
		for(String key : mappedPairs.keySet()) {
			
			// limit the array of points by this boundaries
			final double[][] limittedMappedPairs = limit(mappedPairs.get(key));
			
			if(limittedMappedPairs.length == 0) {
				logger.trace("Filtering visual word '{}' as non of the points mapped by this word lies within this boundaries", key);
			} else {
				retval.put(key, limittedMappedPairs);
			}
		}
		
		return retval;
	}
	
	public double getX1() {
		return x1;
	}

	public double getY1() {
		return y1;
	}
	
	public double[] getXY1() {
		return new double[] { x1, y1 };
	}

	public double getX2() {
		return x2;
	}

	public double getY2() {
		return y2;
	}
	
	public double[] getXY2() {
		return new double[] { x2, y2 };
	}

	@Override
	public String toString() {
		return "Boundries [x1=" + x1 + ", y1=" + y1 + ", x2=" + x2 + ", y2=" + y2 + "]";
	}
}
