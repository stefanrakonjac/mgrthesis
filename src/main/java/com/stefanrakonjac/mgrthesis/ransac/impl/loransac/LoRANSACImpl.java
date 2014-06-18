/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.impl.loransac;

import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.FTools.allOrientationValid;
import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.FTools.exFDs;
import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.FTools.fds;
import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.FTools.lineariseFundamentalMatrix;
import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.FTools.realRoots;
import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.FTools.slcm;
import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.FTools.u2f;
import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.FTools.u2fw;
import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.RTools.inlierIndices;
import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.RTools.rSampleT;
import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.RTools.randomSubset;
import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.RTools.scoreLess;
import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.UTools.nullSpace;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stefanrakonjac.mgrthesis.ransac.impl.AbstRANSAC;
import com.stefanrakonjac.mgrthesis.ransac.impl.boofcv.BoofCVRANSACImpl;
import com.stefanrakonjac.mgrthesis.ransac.utils.ArraysUtils;

/**
 * This implementation heavily uses BoofCV library as well as {@link BoofCVRANSACImpl} implementation does, but it provides its own
 * RANSAC algorithm implementation, specified by LO-RANSAC implementation provided by Karel Lebeda in his master thesis work
 * 
 * @author Stefan.Rakonjac
 *
 */
public class LoRANSACImpl extends AbstRANSAC {
	
	private static final Logger logger = LoggerFactory.getLogger(LoRANSACImpl.class);

	/**
	 * Defines what is the minimal number of samples to draw before running local optimization
	 */
	private static final int MIN_SAMPLES_DRAWN_TO_LO = 50;
	
	/**
	 * TODO: define meaning
	 */
	private static final int TC = 4;
	
	private static final int ILSQ_ITERS = 4;

	/**
	 * TODO: define meaning
	 */
	private static final int RANSAC_REPEAT = 10;

	private int length;
	private boolean[] inliersMap;
	private int inliersCount = 0;
	private double[] serializedTentativePairs;
	private double[] serializedModel;

	/* -------------------------------------------------------------------------
	 * Actual RANSAC implementation
	 * ------------------------------------------------------------------------- */

	@Override
	public void run() {
		
		prepareLoRANSACData();
		
		if(inliersLimit == 0) { /* in the case of unlimited least squares */
			inliersLimit = Integer.MAX_VALUE;
		}
		
		boolean loIteration = false;
		
		// keep track of the scores accomplished
		Score maxScore = new Score(), maxScoreSingle = new Score();
		
		// initialize the pool
		final int[] pool = new int[length];
		for (int i = 0; i < length; i++) {
			pool[i] = i;
		}
		
		double[] f1 = new double[81]; // 9x9
		double[] f2; // f1[9] inclusive ... f1[18] exclusive

		final double[] poly = new double[4];
		final double[] roots = new double[3]; // 1 or 3 roots of poly
		final double[] a = new double[9*9];
		final double[] z = new double[9*length];
		
		int[] nullspaceBuffer = new int[2*9];
		
		lineariseFundamentalMatrix(serializedTentativePairs, z, pool, length);
		
		final double[] buffer = new double[9*length];
		final double[] f = new double[9];
		
		double[] error0 = new double[length];
		double[] error1 = new double[length];
		double[] error2 = new double[length];
		double[] error3 = new double[length];
		
		double[][] errors = new double[5][];
		errors[0] = error0;
		errors[1] = error1;
		errors[2] = error2;
		errors[3] = error3;
		errors[4] = error3;
		
		final int[] inliers = new int[length];
		
		boolean newMaximum;
		boolean doIterate;
		
		// main LoRANSAC loop
		int totalIterations = maxIterations;
		for(int iteration = 0; iteration < totalIterations; iteration++) {
			
			newMaximum = false;
			doIterate = false;
			
			// random minimal sample
			rSampleT(z, 9, pool, 7, length, a);
			
			for (int i = 7*9; i < 9*9; i++) { 
				// fill with zeros to square
				a[i] = 0d;
			}
			
			if (nullSpace(a, f1, 9, nullspaceBuffer) != 2) {
				continue;
			}
			
			// in C version, f2 is a reference to f1 + 9 and following slcm method expects two 3x3 matrices (f1 and f2)
			f2 = ArrayUtils.subarray(f1, 9, 18);
			
			slcm(f1, f2, poly);
			
			// slcm changes f2 so this change needs to be pushed back to f1
			ArraysUtils.copySubArray(f1, f2, 9);
			
			int numberOfSolutions = realRoots(poly, roots);
			
			for (int i = 0; i < numberOfSolutions; i++) { // 1 or 3 hypotheses per sample
				
				for (int j = 0; j < 9; j++) {
					f[j] = f1[j] * roots[i] + f2[j] * (1 - roots[i]);
				}

				// orientation constraint
				if (!allOrientationValid(f, serializedTentativePairs, ArrayUtils.subarray(pool, pool.length-7, pool.length), 7)) {
					continue;
				}

				/* consensus */
				final double[] d = errors[i];
				fds(serializedTentativePairs, f, d, length);
				
				final Score score = inlierIndices(d, length, threshold, inliers);

				if(scoreLess(maxScore, score)) { /* so-far-the-best */
					maxScore = score.clone();
					errors[i] = errors[3];
					errors[3] = d;
					serializedModel = f.clone();
					newMaximum = true;
				}
				
				if(scoreLess(maxScoreSingle, score)) { /* so-far-the-best from sample */
					maxScoreSingle = score.clone();
					doIterate = iteration > MIN_SAMPLES_DRAWN_TO_LO;
					errors[4] = d;
				}
			}
			
			if(iteration >= MIN_SAMPLES_DRAWN_TO_LO && !loIteration && maxScoreSingle.getInliers() > 7) {
				doIterate = true;
			}
			
			if(doIterate && doOptimizations) {
				loIteration = true;
				
				double[] d = errors[0];
				
				Score score = inlierIndices(errors[4], length, TC*threshold, inliers);
				
				u2f(serializedTentativePairs, inliers, score.getInliers(), f, buffer);
				fds(serializedTentativePairs, f, d, length);
				
				score = inlierIndices(d, length, threshold, inliers);
				score = inFrani(serializedTentativePairs, length, inliers, score.getInliers(), threshold, errors, buffer, f, inliersLimit);

				if(scoreLess(maxScore, score)) {
					maxScore = score.clone();
					d = errors[0];
					errors[0] = errors[3];
					errors[3] = d;
					serializedModel = f.clone();
					newMaximum = true;
				}
			}
			
			if (newMaximum) { 
				// updating number of samples needed
				
				final int newTotalIterations = recalculateMaxIterations(maxScore.getInliers(), bestSolutionProbability, 7, length);
				if (newTotalIterations < totalIterations) {
					logger.debug("Downsizing total number of iterations from {} to {} (inliers found: {}; best solution prob: {}; minimal inliers: {}, tentative pairs count: {}", 
								totalIterations, newTotalIterations, maxScore.getInliers(), bestSolutionProbability, 7, length);
					
					totalIterations = newTotalIterations;
				}
			}
		}
		
		// If there were no LOs, do at least one now
		if (doOptimizations && !loIteration && maxScoreSingle.getInliers() > 7) {
			
			double[] d = errors[0];
			
			Score score = inlierIndices(errors[4], length, TC*threshold, inliers);
			u2f(serializedTentativePairs, inliers, score.getInliers(), f, buffer);
			fds(serializedTentativePairs, f, d, length);
			
			score = inlierIndices(d, length, threshold, inliers);
			score = inFrani (serializedTentativePairs, length, inliers, score.getInliers(), threshold, errors, buffer, f, inliersLimit);

			if(scoreLess(maxScore, score)) {
				maxScore = score.clone();
				d = errors[0];
				errors[0] = errors[3];
				errors[3] = d;
				serializedModel = f.clone();
			}
		}
		
		for(int i = 0; i < length; i++) {
			if(inliersMap[i] = errors[3][i] <= threshold) inliersCount ++;
		}
		
		writeLoRANSACData();
	}


	/**
	 * <p> Does all the necessary data transformations so it can be used by LoRANSAC algorithm: <p>
	 * 
	 * <ul>
	 * 	<li> Serializes 2D array of tentative pairs (field: {@code tentativePairs} ) into 1D array having the length 
	 *  	 6 times the original pairs length (new field: {@code serializedTentativePairs})  </li>
	 *  <li> Sets the {@code length} field to the number of tentative pairs (value of: {@code tentativePairs.length}) </li>
	 *  <li> Assigns to {@code inliers} field a new array of double having the length equal to {@code length} field </li>
	 *  <li> Assigns to {@code serializedModel} field a new array of double having the length 9 </li>
	 * </ul>
	 * 
	 * <p> Sets {@code tentativePairs} field to null when done. </p>
	 * 
	 */
	private void prepareLoRANSACData() {
		
		// prepare LoRANSAC pairs data format
		
		length = tentativePairs.length;
		inliersMap = new boolean[length];
		serializedTentativePairs = new double[length*6];
		serializedModel = new double[9];
		
		double[] pair;
		for(int i = 0; i < tentativePairs.length; i++) {
			 pair = tentativePairs[i];
			 serializedTentativePairs[6*i] = pair[0];
			 serializedTentativePairs[6*i + 1] = pair[1];
			 serializedTentativePairs[6*i + 2] = 1; // we are dealing with 2D data, 3rd dimension is always 1
			 serializedTentativePairs[6*i + 3] = pair[2];
			 serializedTentativePairs[6*i + 4] = pair[3];
			 serializedTentativePairs[6*i + 5] = 1; // ditto
		}
		
		tentativePairs = null; // let garbage collector do its job
	}
	
	/**
	 * <p> Makes opposite actions to {@link #prepareLoRANSACData()} </p>
	 * 
	 * TODO: define contract
	 * 
	 */
	private void writeLoRANSACData() {
		
		inliers = new double[inliersCount][4];
		outliers = new double[serializedTentativePairs.length/6 - inliersCount][4];
		
		// sort tentative pairs to inliers or outliers
		
		for(int i = 0, j = 0, k = 0; i < length; i++) {
			if (inliersMap[i]) {
				inliers[j][0] = serializedTentativePairs[i*6];
				inliers[j][1] = serializedTentativePairs[i*6 + 1];
				inliers[j][2] = serializedTentativePairs[i*6 + 3];
				inliers[j][3] = serializedTentativePairs[i*6 + 4];
				j++;
			} else {
				outliers[k][0] = serializedTentativePairs[i*6];
				outliers[k][1] = serializedTentativePairs[i*6 + 1];
				outliers[k][2] = serializedTentativePairs[i*6 + 3];
				outliers[k][3] = serializedTentativePairs[i*6 + 4];
				k++;
			}
		}
		
		// de-serialize estimated model
		
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				
				geometryEstimation[i][j] = serializedModel[i*3+j];
			}
		}
		
	}


	/* -------------------------------------------------------------------------
	 * Helper methods
	 * ------------------------------------------------------------------------- */
	
	private Score inFrani (double[] u, int length, int[] inliers, int numberOfInliers, double threshold, double[][] errors, double[] buffer, double[] serializedModel, int inlLimit) {

		Score score, maxScore = new Score();

		if (numberOfInliers < 16) {
			return maxScore;
		}
		
		int sampleSize = numberOfInliers/2 <= 14 ? numberOfInliers/2 : 14;
		double[] f = new double[9];
		int[] intbuff = new int[length];

		double[] d = errors[2];
		errors[2] = errors[0];
		errors[0] = d;

		for (int i = 0; i < RANSAC_REPEAT; ++i) {
			
			int[] sample = randomSubset(inliers, numberOfInliers, sampleSize);
			u2f(u, sample, sampleSize, f, buffer);
			fds (u, f, errors[0], length);
			errors[4] = errors[0];

			score = iterF(u, length, intbuff, threshold, TC*threshold, f, errors, buffer, inlLimit);

			if (scoreLess(maxScore, score)) {
				maxScore = score;
				d = errors[2];
				errors[2] = errors[0];
				errors[0] = d;
				serializedModel = f.clone();
			}
		}
		
		d = errors[2];
		errors[2] = errors[0];
		errors[0] = d;

		return maxScore;
	}

	/**
	 * <p> def </p>
	 * 
	 * {@ code ranF.c : Score iterF (double *u, int len, int *inliers, double th, double ths, double *F, double **errs, double *buffer, unsigned inlLimit) }
	 * 
	 * @param u
	 * 
	 * @param length
	 * 
	 * @param inliers
	 * 
	 * @param threshold
	 * 
	 * @param d
	 * 
	 * @param f
	 * 
	 * @param errors
	 * 
	 * @param buffer
	 * 
	 * @param inlLimit
	 * 
	 * @return
	 * 
	 */
	private Score iterF(double[] u, int length, int[] inliers, double threshold, double thresholds, double[] serializedModel, double[][] errors, double[] buffer, int inlLimit) {
		
		double[] d = errors[1];
		double[] f = new double[9]; 
		double dth = (thresholds - threshold) / ILSQ_ITERS;
		
		int[] inlSubset;
		
		Score score = new Score(), singleScore, maxScore;

		double[] w = new double[length];
		

		// F from the sample inliers by threshold
		maxScore = inlierIndices(errors[4], length, threshold, inliers);
		
		if (maxScore.getInliers() < 8) return score;
		
		if (maxScore.getInliers() <= inlLimit) { 
			
			// if we are under the limit, just use what we have without shuffling
			u2f(u, inliers, maxScore.getInliers(), f, buffer);
		
		} else {
			
			inlSubset = randomSubset (inliers, maxScore.getInliers(), inlLimit);
			u2f(u, inlSubset, inlLimit, f, buffer);
		}

		// iterate
		
		for (int it = 0; it < ILSQ_ITERS; it++) {
			
			exFDs (u, f, d, w, length);
			score = inlierIndices(d, length, threshold, inliers);
			singleScore = inlierIndices(d, length, thresholds, inliers);

			if (scoreLess(maxScore, score)) {
				maxScore = score;
				errors[1] = errors[0];
				errors[0] = d;
				d = errors[1];
				serializedModel = f.clone();
			}
			
			if (singleScore.getInliers() < 8) {
				return maxScore;
			}

			if (singleScore.getInliers() <= inlLimit) { 
				
				// if we are under the limit, just use what we have without shuffling
				u2fw(u, inliers, w, singleScore.getInliers(), f, buffer);
				
			} else {
				
				inlSubset = randomSubset (inliers, singleScore.getInliers(), inlLimit);
				u2fw(u, inlSubset, w, inlLimit, f, buffer);
				
			}

			thresholds -= dth;
		}

		fds(u, f, d, length);
		score = inlierIndices(d, length, threshold, inliers);
		
		if (scoreLess(maxScore, score)) {
			maxScore = score;
			errors[1] = errors[0];
			errors[0] = d;
			serializedModel = f.clone();
		}
		
		return maxScore;
	}

	/**
	 * A simple structure for holding relative results of single algorithm runs 
	 * 
	 * @author Stefan.Rakonjac
	 *
	 */
	public static class Score {

		public static enum TYPE {
			SC_M, SC_H, SC_R;
		}
		
		/**
		 * Scoring, truncated quadratic gain function
		 */
		private double error = 0;
		
		/**
		 * Inliers count, rectangular gain function
		 */
		private int inliers = 0;
		
		private TYPE type = TYPE.SC_R;

		public double getError() {
			return error;
		}

		public Score setError(double error) {
			this.error = error;
			return this;
		}

		public int getInliers() {
			return inliers;
		}

		public Score setInliers(int inliers) {
			this.inliers = inliers;
			return this;
		}
		
		public TYPE getType() {
			return type;
		}

		public Score setType(TYPE type) {
			this.type = type;
			return this;
		}

		public int increasInliers() {
			return inliers++;
		}

		public void addError(double error) {
			this.error += error;
		}
		
		@Override
		public String toString() {
			return "Score [error=" + error + ", inliers=" + inliers + ", type=" + type + "]";
		}
		
		@Override
		public Score clone() {
			return new Score().setType(type).setError(error).setInliers(inliers);
		}
	}
	
	/* 
	 * {@inheritDoc} 
	 */
	@Override
	public String name() {
		return LoRANSACImpl.class.getSimpleName();
	}
}
