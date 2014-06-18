/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.utils.converters;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.ejml.data.DenseMatrix64F;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stefan.Rakonjac
 *
 */
public class MatrixConverter {
	
	private static final Logger logger = LoggerFactory.getLogger(MatrixConverter.class);
	
	/**
	 * EJML friendly converter
	 * 
	 * @param matrix
	 * 
	 * @return
	 * 
	 */
	public static DenseMatrix64F toDenseMatrix64F(double[][] matrix) {
		if(matrix == null) throw new IllegalArgumentException("matrix is null");
		logger.debug("toDenseMatrix64F(matrix[{}][{}])", matrix.length, matrix.length > 0 ? matrix[0].length : 0);
		
		return new DenseMatrix64F(matrix);
	}

	public static double[][] toMatrix(DenseMatrix64F matrix) {
		if(matrix == null) throw new IllegalArgumentException("matrix is null");
		final int numRows = matrix.getNumRows();
		final int numColumns = matrix.getNumCols();
		logger.debug("toMatrix(DenseMatrix64F[{}][{}])", numRows, numColumns);

		final double[][] retval = new double[numRows][numColumns];
		for(int i = 0; i < numRows; i++) {
			for(int j = 0; j < numColumns; j++) {
				retval[i][j] = matrix.unsafe_get(i, j);
			}
		}
		
		return retval;
	}
	
	/**
	 * Apache-Commons-Math friendly converter
	 * 
	 * @param matrix
	 * 
	 * @param rowDimension
	 * 
	 * @param columnDimension
	 * 
	 * @return
	 * 
	 */
	public static RealMatrix toRealMatrix(final double[] matrix, final int rowDimension, final int columnDimension) {

		if(rowDimension < 0) 
			throw new IllegalArgumentException("rowDimension");
		if(columnDimension < 0) 
			throw new IllegalArgumentException("columnDimension");
		if(matrix == null || matrix.length != rowDimension*columnDimension) 
			throw new IllegalArgumentException("matrix");
		
		final double[][] data = new double[rowDimension][columnDimension];
		for(int i = 0; i < rowDimension; i++) {
			for(int j = 0; j < columnDimension; j++) {
				
				data[i][j] = matrix[i*columnDimension + j];
			}
		}
		
		return new Array2DRowRealMatrix(data);
	}
	
	/**
	 * Apache-Commons-Math friendly converter
	 * 
	 * @param matrix
	 * 
	 * @param targetMatrix 
	 * 
	 */
	public static double[] toMatrix(RealMatrix matrix) {
		
		if(matrix == null)
			throw new IllegalArgumentException("matrix");
		
		final int rowDimension = matrix.getRowDimension();
		final int columnDimension = matrix.getColumnDimension();
		final double[] retval = new double[rowDimension * columnDimension];
		
		for(int i = 0; i < rowDimension; i++) {
			for(int j = 0; j < columnDimension; j++) {
				
				retval[i*columnDimension + j] = matrix.getEntry(i, j);
			}
		}
		
		return retval;
	}
}
