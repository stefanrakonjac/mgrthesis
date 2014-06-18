/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils;

import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Stefan.Rakonjac
 *
 */
public class UTools {
	
	/**
	 * <b> double checked </b>
	 * 
	 * <p> def </p>
	 * 
	 * {@code utools.h : int nullspace(double *matrix, double *nullspace, int n, int * buffer) }
	 * 
	 * @param matrix
	 * 
	 * @param nullspace
	 * 
	 * @param n
	 * 
	 * @param buffer
	 * 
	 */
	public static int nullSpace(final double[] matrix, final double[] nullspace, final int n, final int[] buffer) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */

		if(matrix == null || matrix.length == 0) 
			throw new IllegalArgumentException("matrix: " + ArrayUtils.toString(matrix));
		if(nullspace == null || nullspace.length == 0) 
			throw new IllegalArgumentException("inliers: " + ArrayUtils.toString(nullspace));
		if(n < 0) 
			throw new IllegalArgumentException("n: " + n);
		if(buffer == null || buffer.length == 0) 
			throw new IllegalArgumentException("buffer: " + ArrayUtils.toString(buffer));
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */

		int pnopivot = 0; // buffer related
		int nonpivot = 0;
		int ppivot = n; // buffer related
		int i = 0, max;
		double pivot, t;
		double tol=1e-12;
		
		
		for (int j = 0; j < n; j++) {
			
			// find pivot, start with diagonal element
			pivot = Math.abs(matrix[n*i + j]); 
			max = i;
		      
			for (int k = i + 1; k < n; k++) {
				t = Math.abs(matrix[n*k + j]);
		        if (pivot < t) { 
		        	pivot = t; 
		        	max = k; 
	        	}
	        }
		      
			if (pivot < tol) {
				buffer[pnopivot++] = j; 
		        nonpivot++;
		        
		        // negligible column, zero out
		        for (int k = i; k < n; k++) {
		        	matrix[n*k + j] = 0;
		        }
		        
		    } else {
		    	
		        buffer[ppivot++] = j;
		        
		        // swap rows i <-> max
		        for (int k = j; k < n; k++) {
		            t = matrix[i*n + k]; 
		            matrix[i*n + k] = matrix[max*n + k];
		            matrix[max*n + k] = t;
		        }

		        pivot = matrix[i*n + j];
		        
		        // divide the pivot row by the pivot element.
		        for (int k = j; k < n; k++) {
		        	matrix[i*n + k] /= pivot;
		        }

		        // Subtract multiples of the pivot row from all the other rows.
		        for (int k = 0; k < i; k++) {
		            
		        	pivot = -matrix[k*n + j];
		            
		            for (int l = j; l < n; l++) {
		            	matrix[k*n + l] += pivot*matrix[i*n+l];
		            }
		         }
		          
		         for (int k = i + 1; k < n; k++) {
		         
		        	 pivot = matrix[k*n + j];
		        	 
		             for (int l = j; l < n; l++) {
		               matrix[k*n + l] -= pivot*matrix[i*n + l];
		             }
		         }
		         
		         i++;
		      }
		   }
		   
		   /* initialize null space vectors */
		   for (int k = 0; k < nonpivot; k++) {      
		      
			   int j = buffer[k];
		      
		       // copy nonpivot -column above diagonal
			   for (int l = 0; l < n-nonpivot; l++) {
				   nullspace[k*n + buffer[n + l]] = -matrix[l*n + j];
			   }
		      
		       for (int l=0; l < nonpivot; l++) {
		    	   nullspace[k*n + buffer[l]] = (j == buffer[l]) ? 1 : 0;
		       }
		   }

	    /* number of nullspace vectors */
	    return nonpivot;
	}
	
	/**
	 * <b> double checked </b>
	 * 
	 * <p> def </p>
	 * 
	 * {@code utools.h : void normu (const double *u, const int * inl, int len, double *A1, double *A2) }
	 * 
	 * @param u
	 * 
	 * @param inliers
	 * 
	 * @param length
	 * 
	 * @param a1
	 * 
	 * @param a2
	 * 
	 */
	public static void normu(final double[] u, final int[] inliers, final int length, final double[] a1, final double[] a2) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */

		if(u == null || u.length == 0) 
			throw new IllegalArgumentException("u: " + ArrayUtils.toString(u));
		if(inliers == null || inliers.length < length) 
			throw new IllegalArgumentException("inliers: " + ArrayUtils.toString(inliers));
		if(length < 0) 
			throw new IllegalArgumentException("length: " + length);
		if(a1 == null || a1.length != 3) 
			throw new IllegalArgumentException("a1: " + ArrayUtils.toString(a1));
		if(a2 == null || a2.length != 3) 
			throw new IllegalArgumentException("a2: " + ArrayUtils.toString(a2));
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */
		
		for(int i = 0; i < 3; i++) {
		
			a1[i] = a2[i] = 0;
		}
		
		for(int i = 0; i < length; i++) {
			
			final int index = 6 * inliers[i];
			a1[1] += u[index];
			a1[2] += u[index+1];
			a2[1] += u[index+3];
			a2[2] += u[index+4];
			
		}
		
		if(length > 0) {
			
			for(int i = 1; i < 3; i++) {
				a1[i] /= length;
				a2[i] /= length;
			}
			
		}
		
		double a, b;
		
		for(int i = 0; i < length; ++i) {
			
			final int index = 6 * inliers[i];
			
			a = u[index] - a1[1];
			b = u[index + 1] - a1[2];
			a1[0] += Math.sqrt(a*a + b*b);
			
			a = u[index+3] - a2[1];
			b = u[index+4] - a2[2];
			a2[0] += Math.sqrt(a*a + b*b);
		}
		
		if (a1[0] != 0) a1[0] = length * Math.sqrt(2) / a1[0];
		if (a2[0] != 0) a2[0] = length * Math.sqrt(2) / a2[0];

		a1[1] *= -a1[0]; 
		a1[2] *= -a1[0];
		a2[1] *= -a2[0]; 
		a2[2] *= -a2[0];
		
	}
	
	/**
	 * <b> double checked </b>
	 * 
	 * <p> def </p>
	 * 
	 * {@code utools.h : void cov_mat(double *Cv, const double * Z, int len, int siz) }
	 * 
	 * @param cv
	 * 
	 * @param z
	 * 
	 * @param length
	 * 
	 * @param size
	 * 
	 */
	public static void covMat(final double[] cv, final double[] z, final int length, final int size) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */

		if(cv == null || cv.length < size*size) 
			throw new IllegalArgumentException("cv: " + ArrayUtils.toString(cv));
		if(z == null || z.length < length*size) 
			throw new IllegalArgumentException("z: " + ArrayUtils.toString(z));
		if(length < 0) 
			throw new IllegalArgumentException("length: " + length);
		if(size < 0) 
			throw new IllegalArgumentException("size: " + size);
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */
		
		for(int i = 0; i < size; i++) {
			
			for(int j = 0; j <= i; j++) {
				
				double val = 0;
				for(int k = 0; k < length*size; k += size) {
					
					val += z[k+i] * z[k+j];
				}
				
				cv[size*i + j] = val;
				cv[i + size*j] = val;
				
			}
		}
	}
	
	
	/**
	 * <p> def </p>
	 * 
	 * {@code utools.h : void scalmul (double *data, double m, int len, int step) }
	 * 
	 * @param data
	 * 
	 * @param m
	 * 
	 * @param length
	 * 
	 * @param step
	 * 
	 */
	public static void scalmul(double[] data, int dataIndex, double m, int length, int step) { 
		for(int i = 0; i < length; i++, dataIndex += step) {
			data[dataIndex] *= m;
		}
	}

	/**
	 * <b> checked </b>
	 * 
	 * <p> def </p>
	 * 
	 * {@code utools.h : void denormF (double *F, double *A1, double *A2) }
	 * 
	 * @param serializedModel
	 * 
	 * @param a1
	 * 
	 * @param a2
	 * 
	 */
	public static void denormF (double[] f, double[] a1, double[] a2) {
		
		  double r = a2[0], x = a2[1], y = a2[2];
		  
		  f[6] += x * f[0] + y*f[3];
		  f[7] += x * f[1] + y*f[4];
		  f[8] += x * f[2] + y*f[5];
		  
		  f[0] *= r; 
		  f[1] *= r; 
		  f[2] *= r;
		  f[3] *= r; 
		  f[4] *= r; 
		  f[5] *= r;

		  r = a1[0]; 
		  x = a1[1]; 
		  y = a1[2];
		  
		  f[2] += x * f[0] + y*f[1];
		  f[5] += x * f[3] + y*f[4];
		  f[8] += x * f[6] + y*f[7];
		  f[0] *= r; 
		  f[1] *= r; 
		  f[3] *= r; 
		  f[4] *= r; 
		  f[6] *= r;
		  f[7] *= r;
	}
}
