/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stefanrakonjac.mgrthesis.ransac.utils.ArraysUtils;
import com.stefanrakonjac.mgrthesis.ransac.utils.converters.MatrixConverter;


/**
 * @author Stefan.Rakonjac
 *
 */
public class LapWrapper {
	
	private static final Logger logger = LoggerFactory.getLogger(LapWrapper.class);
	
	/**
	 * <p> Eigen-decomposition: Compute the eigenvalues and eigen-vectors of a real symmetric matrix {@code a}.</p>
	 * <p> The input and output matrices are related by: </p>
	 * <p> {@code A = E*D*E~}</p>
	 * <p> where D is the diagonal matrix of eigenvalues D[i,j] = ev[i] if i=j and 0 otherwise. The columns of E are the eigen-vectors.</p>
	 * 
	 * {@code lapwrap.h : int lap_eig(double *a, double *ev, lapack_int n) }
	 * 
	 * @param a
	 * 		Array to store for symmetric n by n input matrix {@code a}. 
	 * 		The computation overloads this with an orthogonal matrix of eigen-vectors E.
	 * @param ev
	 * 		Array of the output eigenvalues
	 * @param n
	 * 		Dimension parameter (dim(a)= n*n, dim(ev)= n)
	 * @return
	 * 		Returns {@core true} on success, {@code false} if failed to converge
	 * 
	 */
	public static boolean lapackEigen(final double[] a, final double[] ev, final int n) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */

		if(n < 0) 
			throw new IllegalArgumentException("n: " + n);
		if(a == null || a.length != n*n) 
			throw new IllegalArgumentException("a: " + ArrayUtils.toString(a));
		if(ev == null || ev.length != n) 
			throw new IllegalArgumentException("ev: " + ArrayUtils.toString(ev));
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */
		
		final RealMatrix matrix = MatrixConverter.toRealMatrix(a, n, n);
		
		try {
			
			final EigenDecomposition decomposition = new EigenDecomposition(matrix);
			ArraysUtils.copyArray(a, MatrixConverter.toMatrix(decomposition.getV()));
			ArraysUtils.copyArray(ev, decomposition.getRealEigenvalues());
			
		} catch (MaxCountExceededException | MathArithmeticException ex) {
			
			logger.debug("lapackEigen({}, {}, {})", ArrayUtils.toString(a), ArrayUtils.toString(ev), n, ex);
			return false;
		}
		
		return true;
	}
	
	/**
	 * <p> def </p>
	 * 
	 * {@code lapwrap.h : int lap_SVD (double *d, double *a, double *u, lapack_int m, double *vt, lapack_int n) }
	 * 
	 * @param d
	 * 
	 * @param a
	 * 
	 * @param u
	 * 
	 * @param m
	 * 
	 * @param vt
	 * 
	 * @param n
	 * 
	 * @return
	 * 
	 */
	public static boolean lapackSVD(double[] d, double[] a, double[] u, int m, double[] vt, int n) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */

		if(n < 0) 
			throw new IllegalArgumentException("n: " + n);
		if(m < n) 
			throw new IllegalArgumentException("m: " + m);
		if(a == null || a.length != m*n) 
			throw new IllegalArgumentException("a: " + ArrayUtils.toString(a));
		if(d == null || d.length != n) 
			throw new IllegalArgumentException("d: " + ArrayUtils.toString(d));
		if(u == null || u.length != m*m) 
			throw new IllegalArgumentException("m: " + ArrayUtils.toString(m));
		if(vt == null || vt.length != n*n) 
			throw new IllegalArgumentException("vt: " + ArrayUtils.toString(vt));
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */
		
		final RealMatrix matrix = MatrixConverter.toRealMatrix(a, m, n);
		
		try {
			
			final SingularValueDecomposition decomposition = new SingularValueDecomposition(matrix);
			
			// copy only diagonal 
			ArraysUtils.copyArray(d, MatrixUtils.getMainDiagonal(decomposition.getS()));
			
			// copy directly
			ArraysUtils.copyArray(u, MatrixConverter.toMatrix(decomposition.getU()));
			
			// ditto
			ArraysUtils.copyArray(vt, MatrixConverter.toMatrix(decomposition.getVT()));
			
		} catch (MaxCountExceededException | MathArithmeticException ex) {
			
			logger.debug("lapackEigen({}, {}, {}, {}, {}, {})", ArrayUtils.toString(d), ArrayUtils.toString(a), ArrayUtils.toString(u), m, ArrayUtils.toString(vt), n, ex);
			return false;
		}
		
		
		return true;
	}

}
