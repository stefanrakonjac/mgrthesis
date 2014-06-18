package com.stefanrakonjac.mgrthesis.ransac;

/**
 * @author Stefan.Rakonjac
 *
 */
public interface RANSAC extends Runnable {
	
	/** Default Threshold (in pixels) */
	public static final double D_TRESHOLD = 0.5d;
	
	/** Default Maximal Number of Iterations */
	public static final int D_MAX_ITERATIONS = 1000000;
	
	/** Default Best Solution Probability [0-1]: 0.95 */
	public static final double D_BEST_SOLUTION_PROBABILITY = 0.95d;
	
	/**
	 * @return The name of the RANSAC implementation
	 */
	String name();
	
	/**
	 * 
	 * @return
	 */
	double[][] getGeometryEstimation();


	/**
	 * 
	 * @return
	 */
	double[][] getInliers();

	/**
	 * 
	 * @return
	 */
	double[][] getOutliers();
	
	
	/* --------------- RANSAC data setters --------------- */

	/** 
	 * Pairs setter 
	 * 
	 * @return
	 * 		{@code this}
	 */
	RANSAC setTentativePairs(double[][] pairs);

	/**
	 * Threshold setter
	 * 
	 * @param threshold
	 * @return
	 * 		{@code this}
	 */
	RANSAC setThreshold(double threshold);
	
	/**
	 * Max iterations setter
	 * 
	 * @param maxIterations
	 * @return
	 * 		{@code this}
	 */
	RANSAC setMaxIterations(int maxIterations);
	
	/**
	 * Best solution probability setter
	 * 
	 * @param bestSolutionProbability
	 * @return
	 * 		{@code this}
	 */
	RANSAC setBestSolutionProbability(double bestSolutionProbability);
	
	/**
	 * Maximal number of inliers for least squares setter
	 * 
	 * @param inliersLimit
	 * @return
	 * 		{@code this}
	 */
	RANSAC setInliersLimit(int inliersLimit);

	/**
	 * Do algorithm-specific optimizations setter
	 * 
	 * @param doOptimizations
	 * @return
	 */
	RANSAC doOptimizations(boolean doOptimizations);
}
