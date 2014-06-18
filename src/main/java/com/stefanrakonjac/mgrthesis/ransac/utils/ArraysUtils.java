/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.utils;

import java.util.Collection;

/**
 * @author Stefan.Rakonjac
 *
 */
public class ArraysUtils {

	public static void copyArray(double[] targetArray, double[] sourceArray) {
		
		if(targetArray == null)
			throw new IllegalArgumentException("targetArray");
		if(sourceArray == null)
			throw new IllegalArgumentException("sourceArray");
		if(targetArray.length != sourceArray.length) 
			throw new IllegalArgumentException("targetArray.length (" + targetArray.length + ") != sourceArray.length (" + sourceArray.length + ")");
		
		for(int i = 0; i < targetArray.length; i++) {
			targetArray[i] = sourceArray[i];
		}
	}

	public static void copySubArray(double[] targetArray, double[] sourceArray, int arrayStart) {

		if(targetArray == null)
			throw new IllegalArgumentException("targetArray");
		if(sourceArray == null || sourceArray.length > targetArray.length - arrayStart)
			throw new IllegalArgumentException("sourceArray");
		if(arrayStart < 0)
			throw new IllegalArgumentException("arrayStart");
		
		for(int i = 0; i < sourceArray.length; i++) {
			targetArray[arrayStart + i] = sourceArray[i];
		}
		
	}
	
	public static double[][] to2Ddouble(Collection<double[]> list) {
		
		if(list == null)
			throw new IllegalArgumentException("list");
		
		final double[][] retval = new double[list.size()][0];
		
		int elementPosition = 0;
		for(double[] element : list) {
			retval[elementPosition++] = element;
		}
		
		return retval;
	}

}
