package com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils;

import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;

import com.stefanrakonjac.mgrthesis.ransac.impl.loransac.LoRANSACImpl.Score;

public class RTools {

	/**
	 * <b> double checked </b>
	 * 
	 * <p> Indices of inliers with error lower than given threshold. </p>
	 * 
	 * {@code rtools.h : Score inlidxs (const double * err, int len, double th, int * inl) }
	 * 
	 * @param error
	 * 
	 * @param length
	 * 		number of correspondences  
	 * @param threshold
	 * 
	 * @param inliers
	 * 
	 * @return
	 * 		Returns RANSAC score
	 */
	public static Score inlierIndices(final double[] error, final int length, final double threshold, final int[] inliers) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */

		if(length < 0) 
			throw new IllegalArgumentException("length: " + length);
		if(error == null || error.length != length) 
			throw new IllegalArgumentException("error: " + ArrayUtils.toString(error));
		if(threshold < 0) 
			throw new IllegalArgumentException("threshold: " + threshold);
		if(inliers == null || inliers.length != length) 
			throw new IllegalArgumentException("inliers: " + ArrayUtils.toString(inliers));
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */
		
		final Score retval = new Score();
		
		for(int i = 0; i < length; i++) {
			
			retval.addError(truncatedQuadraticError(error[i], threshold));
			if(error[i] <= threshold) {
				inliers[retval.increasInliers()] = i;
			}
		}
		
		return retval;
	}
	
	/**
	 * <b> double checked </b>
	 * 
	 * <p> desc </p>
	 * 
	 * {@code rtools.h : double truncQuad(double epsilon, double thr) }
	 * 
	 * @param epsilon
	 * 
	 * @param thershold
	 * 
	 * @return
	 * 
	 */
	public static double truncatedQuadraticError(final double epsilon, final double threshold) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */
		
		if(threshold < 0) 
			throw new IllegalArgumentException("threshold: " + threshold);
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */
		
		if (threshold == 0) {
			return 0;
		}
		
		if (epsilon >= threshold*9d/4) {
			return 0;
		}
		
		return 1 - (epsilon/(threshold*9d/4));
	}
	
	/**
	 * <b> double checked </b>
	 * 
	 * <p> def </p>
	 * 
	 * {@code void rtools.h : rsampleT (double *data, int dat_siz, int *pool, int size, int max_sz, double *dst) }
	 * 
	 * @param data
	 * 
	 * @param dataSize
	 * 		
	 * @param pool
	 * 		
	 * @param size
	 * 		number of samples to drawn (i.e. 7)
	 * @param maxSize
	 * 		length of the pool
	 * @param destination
	 * 		9 x 9 matrix represented by an array of double
	 */
	public static void rSampleT(double[] data, int dataSize, int[] pool, int size, int maxSize, double[] destination) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */

		if(dataSize < 0) 
			throw new IllegalArgumentException("dataSize: " + dataSize);
		if(size < 0) 
			throw new IllegalArgumentException("size: " + size);
		if(maxSize < 0) 
			throw new IllegalArgumentException("maxSize: " + maxSize);
		if(data == null || data.length != 9*maxSize) 
			throw new IllegalArgumentException("data: " + ArrayUtils.toString(data));
		if(pool == null || pool.length != maxSize) 
			throw new IllegalArgumentException("inliers: " + ArrayUtils.toString(pool));
		if(destination == null || destination.length != 81) 
			throw new IllegalArgumentException("destination: " + ArrayUtils.toString(destination));
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */
		
		int src;
		int p = 0; // destination related
		
		for(int i = 0; i < size; ++i) {
			
			int q = sample(pool, maxSize, i);
			
			src = q; // data related
			
			for(int j = 0; j < dataSize; j++) {
				
				destination[p] = data[src];
				p++;
				src += maxSize;
				
			}
		}
	}

	/**
	 * <b> checked </b>
	 * 
	 * {@code rtools.h : int sample (int *pool, int max_sz, int i) }
	 * 
	 * @param pool
	 * 
	 * @param maxSize
	 * 
	 * @param i
	 * 
	 * @return
	 * 
	 */
	public static int sample(int[] pool, int maxSize, int i) {
		
		int s = new Random().nextInt(Integer.MAX_VALUE) % (maxSize - i);
		int j = maxSize - i - 1;
		int q = pool[s];
		pool[s] = pool[j];
		pool[j] = q;
		return q;
	}
	
	/**
	 * <p> Score comparator </p>
	 * 
	 * {@code rtools.h : int scoreLess(const Score s1, const Score s2) }
	 * 
	 * @param s1
	 * 
	 * @param s2
	 * 
	 * @return
	 * 		Returns {@code true} if score {@code s1} is evaluated as less than score {@code s2}, otherwise returns {@code false}
	 */
	public static boolean scoreLess(final Score s1, final Score s2) {
		
		switch(s1.getType()) {
		case SC_M: 
			return s1.getError() < s2.getError();
		case SC_H: 
			if(s1.getInliers() == s2.getInliers()) return s1.getError() < s2.getError();
		case SC_R: 
			return s1.getInliers() < s2.getInliers();
		default:
			throw new IllegalArgumentException("Unknown score type: " + s1);
		}
	}
	
	/**
	 * <b> checked </b>
	 * 
	 * <p> def </p>
	 * 
	 * {@code rtools.h : int *randsubset (int * pool, int max_sz, int siz) }
	 * 
	 * @param pool
	 * 
	 * @param maxSize
	 * 
	 * @param size
	 * 
	 * @return
	 * 
	 */
	public static int[] randomSubset(int[] pool, int maxSize, int size) {
		
		   int j,q,s;
		   
		   final Random random = new Random();

		   for(int i = 0; i < size; i++) {
		      s = random.nextInt(Integer.MAX_VALUE) % (maxSize - i);
		      j = maxSize - i - 1; 
		      q = pool[s];
		      pool[s] = pool[j];
		      pool[j] = q;
		   }

		   return ArrayUtils.subarray(pool, maxSize-size, maxSize); 
	}
}
