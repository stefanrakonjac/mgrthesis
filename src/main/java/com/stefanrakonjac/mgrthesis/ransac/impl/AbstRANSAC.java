package com.stefanrakonjac.mgrthesis.ransac.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stefanrakonjac.mgrthesis.ransac.RANSAC;

/**
 * @author Stefan.Rakonjac
 *
 */
public abstract class AbstRANSAC implements RANSAC {
	
	private final static Logger logger = LoggerFactory.getLogger(AbstRANSAC.class);
	
	/* double error */
	public static double EPSILON = 2.2204e-16;
	
	/* RANSAC data */
	protected double[][] tentativePairs;
	protected double threshold = D_TRESHOLD;
	protected int maxIterations = D_MAX_ITERATIONS;
	protected double bestSolutionProbability = D_BEST_SOLUTION_PROBABILITY;
	protected int inliersLimit;
	protected boolean doOptimizations = true;
	
	/* computed data */
	protected double[][] inliers = new double[0][4];
	protected double[][] outliers = inliers;
	protected double[][] geometryEstimation = new double[3][3];
	
	protected AbstRANSAC() {
		// no-op
	}

	@Override
	public final RANSAC setTentativePairs(final double[][] tentativePairs) {
		if(tentativePairs == null) throw new IllegalArgumentException("tentativePairs is null");
		logger.debug("setTentativePairs(double[{}][4])", tentativePairs.length);
		
		this.tentativePairs = new double[tentativePairs.length][4];
		for(int i = 0; i < tentativePairs.length; i++) { 
			addTentativePair(i, tentativePairs[i]);
		}
		
		return this;
	}
	
	@Override
	public final RANSAC setThreshold(final double threshold) {
		logger.debug("setThreshold({})", threshold);
		this.threshold = threshold;
		
		return this;
	}
	
	@Override
	public final RANSAC setMaxIterations(int maxIterations) {
		logger.debug("setMaxIterations({})", maxIterations);
		this.maxIterations = maxIterations;
		
		return this;
	}
	
	@Override
	public final RANSAC setBestSolutionProbability(double bestSolutionProbability) {
		logger.debug("setBestSolutionProbability({})", bestSolutionProbability);
		this.bestSolutionProbability = bestSolutionProbability;
		
		return this;
	}

	@Override
	public RANSAC setInliersLimit(int inliersLimit) {
		logger.debug("setInliersLimit({})", inliersLimit);
		
		this.inliersLimit = inliersLimit;
		return this;
	}
	
	@Override
	public RANSAC doOptimizations(boolean doOptimizations) {
		logger.debug("doOptimizations({})", doOptimizations);
		
		this.doOptimizations = doOptimizations;
		return this;
	}

	private void addTentativePair(final int position, final double[] pair) {
		if(pair.length != 4) throw new IllegalArgumentException("pair does not have exact 4 coordinates ((x1, y1) -> (x2, y2))");
		
		this.tentativePairs[position][0] = pair[0];
		this.tentativePairs[position][1] = pair[1];
		this.tentativePairs[position][2] = pair[2];
		this.tentativePairs[position][3] = pair[3];
	}
	
	@Override
	public double[][] getInliers() {
		return inliers;
	}
	
	@Override
	public double[][] getOutliers() {
		return outliers;
	}

	@Override
	public double[][] getGeometryEstimation() {
		return geometryEstimation;
	}
	

	/**
	 * 
	 * @param inliersCount
	 * @param confidence
	 * @param minimalInliers
	 * @param totalPoints
	 * @return
	 */
	public static int recalculateMaxIterations(final int inliersCount, final double confidence, final int minimalInliers, final int totalPoints) {
		logger.debug("recalculateMaxIterations({}, {}, {}, {})", inliersCount, confidence, minimalInliers, totalPoints);
		int retval = D_MAX_ITERATIONS;
		
		double a = 1, b = 1;
		for(int i = 0; i < minimalInliers; i++) {
			a *= inliersCount - i;
			b *= totalPoints - i;
		}
		
		final double fraction = a/b;
		
		if(fraction < EPSILON) {
			retval = D_MAX_ITERATIONS;
		} else if(1d - fraction < EPSILON ) {
			retval = 1;
		} else {
			retval = (int) Math.round( Math.min((double) D_MAX_ITERATIONS, Math.log(1f - confidence) / Math.log(1f - fraction) ) );
		}
		
		return retval;
	}
}
