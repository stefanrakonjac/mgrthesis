/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils;

import org.apache.commons.math3.linear.RealMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stefan.Rakonjac
 *
 */
public class MatrixUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(MatrixUtils.class);
	
	public static double[] getMainDiagonal(RealMatrix matrix) {

		if(matrix == null || !matrix.isSquare()) 
			throw new IllegalArgumentException("matrix");
		
		logger.trace("getMainDiagonal({})", matrix);

		final int matrixDimension = matrix.getColumnDimension();
		final double[] retval = new double[matrixDimension];	
		
		for(int i = 0; i < matrixDimension; i++) {
			retval[i] = matrix.getEntry(i, i);
		}
		
		return retval;
	}

	public static double[][] transpose(final double[][] matrix) {
		
		if(matrix == null) 
			throw new IllegalArgumentException("matrix is null");
		
		logger.trace("transpose(double[][])", matrix.length, matrix.length != 0 ? matrix[0].length : 0);
		
		if(matrix.length == 0)
			return new double[0][0];
		
		final int matrixRows = matrix.length;
		final int matrixColumns = matrix[0].length;
		
		final double[][] retval = new double[matrixColumns][matrixRows];
		
		for(int i = 0; i < matrixRows; i++) {
			for(int j = 0; j < matrixColumns; j++) {
				
				retval[j][i] = matrix[i][j];
			}
		}
		
		return retval;
	}
}
