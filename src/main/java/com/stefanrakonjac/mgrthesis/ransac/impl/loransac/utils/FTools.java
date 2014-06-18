package com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils;

import org.apache.commons.lang3.ArrayUtils;

import com.stefanrakonjac.mgrthesis.ransac.utils.ArraysUtils;

import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.UTools.*;
import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.LapWrapper.*;
import static com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.MathUtils.*;

/**
 * @author Stefan.Rakonjac
 *
 */
public class FTools {

	/**
	 * <b> double checked </b>
	 * 
	 * <p> Method determines error (parameter {@code p}) for the provided correspondences </p>
	 * 
	 * {@code Ftools.h : void FDs (const double *u, const double *F, double *p, int len) }
	 * 
	 * @param u 
	 * 		tentative correspondences (6 x {@code length}-element array)
	 * @param f
	 * 		9-element array (matrix)
	 * @param p
	 * 		{@code length}-element array, error
	 * @param length
	 * 		number of correspondences  
	 */
	public static void fds(final double[] u, final double[] f, final double[] p, final int length) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */
		
		if(length < 0) 
			throw new IllegalArgumentException("length: " + length);
		if(u == null || u.length != 6*length) 
			throw new IllegalArgumentException("u: " + ArrayUtils.toString(u));
		if(f == null || f.length != 9) 
			throw new IllegalArgumentException("f: " + ArrayUtils.toString(f));
		if(p == null || p.length != length) 
			throw new IllegalArgumentException("p: " + ArrayUtils.toString(p));
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */
		
		double rx, ry, rwc, ryc, rxc, r;
		
		for(int i = 0; i < length; i++) {
			
		      rxc = f[0] * u[i*6 + 3] + f[3] * u[i*6 + 4] + f[6];
		      ryc = f[1] * u[i*6 + 3] + f[4] * u[i*6 + 4] + f[7];
		      rwc = f[2] * u[i*6 + 3] + f[5] * u[i*6 + 4] + f[8];
		      
		      r = u[i*6] * rxc + u[i*6 + 1] * ryc + rwc;
		      
		      rx = f[0] * u[i*6] + f[1] * u[i*6 + 1] + f[2];
		      ry = f[3] * u[i*6] + f[4] * u[i*6 + 1] + f[5]; 

		      p[i] = r*r / (rxc*rxc + ryc*ryc + rx*rx + ry*ry);
			
		}
	}
	
	/**
	 * <b> double checked </b>
	 * 
	 * <p> linearizes correspondences with respect to entries of fundamental matrix, 
	 * so that u' F u -> A f </p>
	 * 
	 * {@code FTools.c : void lin_fm(const double *u, double *p, const int* inl, const int len) }
	 * 
	 * @param u
	 * 		tentative correspondences (6 x {@code length}-element array)
	 * @param p
	 * 		{@code length}-element array
	 * @param inliers
	 * 		{@code length}-element array, inlier indices
	 * @param length
	 * 		number of correspondences  
	 */
	public static void lineariseFundamentalMatrix(final double[] u, final double[] p, final int[] inliers, final int length) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */
		
		if(length < 0) 
			throw new IllegalArgumentException("length: " + length);
		if(u == null || u.length < 6*length) 
			throw new IllegalArgumentException("u: " + ArrayUtils.toString(u));
		if(p == null || p.length < 9*length) 
			throw new IllegalArgumentException("p: " + ArrayUtils.toString(p));
		if(inliers == null || inliers.length < length) 
			throw new IllegalArgumentException("inliers: " + ArrayUtils.toString(inliers));
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */
		
		int offset = 0;
		
		for(int i = 0; i < length; i++) {
			
			offset = 0; 
			
			for(int j = 0; j < 3; j++) {
				
				for(int k = 0; k < 3; k++) {
					p[i + offset] = u[6*inliers[i] + j + 3] * u[6*inliers[i] + k];
					offset += length; // will have values: 0, length, 2length, ... 8length
				}
			}
		}
	}
	
	/**
	 * <b> checked <b/>
	 * 
	 * <p> Real roots of the polynomial of degree 3 </p>
	 * 
	 * {@code Ftools.c : int rroots3 (double *po, double *r) }
	 * 
	 * @param polynomial
	 * 		Polynomial of degree 3 whose roots are to be computed 
	 * @param roots
	 * 		Computed roots of provided polynomial
	 * @return
	 * 		Number of solutions
	 */
	public static int realRoots(final double[] polynomial, final double[] roots) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */
		
		if(polynomial == null || polynomial.length != 4) 
			throw new IllegalArgumentException("polynomial: " + ArrayUtils.toString(polynomial));
		if(roots == null || roots.length != 3) 
			throw new IllegalArgumentException("roots: " + ArrayUtils.toString(roots));
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */
		
		final double b = polynomial[1]/polynomial[0];
		final double c = polynomial[2]/polynomial[0];
		final double b2 = b*b;
		final double bt = b/3;
		final double p = (3*c - b2)/9;
		final double q = (2*b2*b/27 - b*c/3 + polynomial[3]/polynomial[0])/2;
		final double d = q*q + p*p*p;
		
		if(d > 0) {
			
			final double a = Math.sqrt(d) - q;
			if(a > 0) {
				
				final double v = Math.pow(a, 1d/3);
				roots[0] = v - p/v - bt;
				
			} else {
				
				final double v = Math.pow(-a, 1d/3);
				roots[0] = p/v - v - bt;
			}
			
			return 1;
		}
		
		final double r = ((q > 0) ? 1 : -1) * Math.sqrt(-p);
		final double pit = 3.14159265358979/3;
		final double cosphi = limit(q/(r*r*r), -1, 1);
		final double phit = Math.acos(cosphi)/3;
		
		roots[0] = -2*r*Math.cos(phit) - bt;
		roots[1] =  2*r*Math.cos(pit - phit) - bt;
		roots[2] =  2*r*Math.cos(pit + phit) - bt;
		
		return 3;
	}
	
	/**
	 * <b> double checked </b>
	 * 
	 * <p> Validates orientation </p>
	 * 
	 * {@code Ftools.c : int all_ori_valid(double *F, double *us, int *idx, int N) }
	 * 
	 * @param f
	 * 
	 * @param u
	 * 
	 * @param indices
	 * 
	 * @param n
	 * 
	 * @return
	 * 		Returns {@code true} if orientation is valid for all {@code n} sample indices, otherwie returns {@code false}
	 */
	public static boolean allOrientationValid(double[] f, double[] u, int[] indices, int n) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */

		if(f == null || f.length != 9) 
			throw new IllegalArgumentException("f: " + ArrayUtils.toString(f));
		if(u == null || u.length == 0) 
			throw new IllegalArgumentException("u: " + ArrayUtils.toString(u));
		if(indices == null || 6*indices.length > u.length) 
			throw new IllegalArgumentException("indices: " + ArrayUtils.toString(indices));
		if(n < 0 || n != indices.length) 
			throw new IllegalArgumentException("n: " + n);
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */
		
		final double[] ec = new double[3];
		epipole(ec, f);
		
		final double sig0 = getOrientationSig(f, ec, ArrayUtils.subarray(u, 6*indices[0], 6*indices[0] + 6));
		
		for(int i = 1; i < n; i++) {
			
			final double sig = getOrientationSig(f, ec, ArrayUtils.subarray(u, 6*indices[i], 6*indices[i] + 6));
			if(sig0*sig < 0) return false;
		}
		
		return true;
	}
	
	
	/**
	 * <b> double checked </b>
	 * 
	 * <p> def </p>
	 * 
	 * {@code Ftools.c : void epipole(double *ec, const double *F) }
	 * 
	 * @param ec
	 * 
	 * @param f
	 * 
	 */
	public static void epipole(final double[] ec, final double[] f) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */

		if(ec == null || ec.length != 3) 
			throw new IllegalArgumentException("ec: " + ArrayUtils.toString(ec));
		if(f == null || f.length != 9) 
			throw new IllegalArgumentException("f: " + ArrayUtils.toString(f));
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */
		
		final double xeps = 1.9984e-15;
		
		final double[] f6 = ArrayUtils.subarray(f, 6, 9);
		crossproduct(ec, f, f6, 1);
		
		for(int i = 0; i < 3; i++) {
			
			if(ec[i] > xeps || ec[i] < -xeps) return;
		}
		
		final double[] f3 = ArrayUtils.subarray(f, 3, 6);
		crossproduct(ec, f3, f6, 1);
	}
	
	/**
	 * <b> double checked </b>
	 * 
	 * <p> def </p>
	 * 
	 * {@code utools.h : void crossprod_st(double *out, const double *a, const double *b, int st)
	 * 
	 * @param out
	 * 		Method updates this parameter
	 * @param a
	 * 		Method does not update this parameter
	 * @param b
	 * 		Method does not update this parameter
	 * @param step
	 * 
	 */
	public static void crossproduct(double[] out, double[] a, double[] b, int step) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */

		if(out == null || out.length != 3) 
			throw new IllegalArgumentException("out: " + ArrayUtils.toString(out));
		if(step <= 0) 
			throw new IllegalArgumentException("st: " + step);
		if(a == null || a.length < 2*step) 
			throw new IllegalArgumentException("a: " + ArrayUtils.toString(a));
		if(b == null || b.length < 2*step) 
			throw new IllegalArgumentException("b: " + ArrayUtils.toString(b));
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */

		out[0] = a[step]*b[2*step] - a[2*step]*b[step];
		out[1] = a[2*step]*b[0] - a[0]*b[2*step];
		out[2] = a[0]*b[step] - a[step]*b[0];
	}
	
	
	/**
	 * <b> double checked </b>
	 * 
	 * <p> def </p>
	 * 
	 * {@code Ftools.c : double getorisig(double *F, double *ec, double *u) }
	 * 
	 * @param f
	 * 		Method does not update this parameter
	 * @param ec
	 * 		Method does not update this parameter
	 * @param u
	 * 		Method does not update this parameter
	 * @return
	 * 
	 */
	public static double getOrientationSig(double[] f, double[] ec, double[] u) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */

		if(f == null || f.length != 9) 
			throw new IllegalArgumentException("f: " + ArrayUtils.toString(f));
		if(ec == null || ec.length == 0) 
			throw new IllegalArgumentException("ec: " + ArrayUtils.toString(ec));
		if(u == null || u.length != 6) 
			throw new IllegalArgumentException("u: " + ArrayUtils.toString(u));
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */
		
		final double s1 = f[0]*u[3] + f[3]*u[4] + f[6]*u[5];
		final double s2 = ec[1]*u[2] - ec[2]*u[1];
		
		return s1*s2;
	}
	
	/**
	 * <b> checked </b>
	 * 
	 * <p> def </p>
	 * 
	 * {@code Ftools.c : void u2f(const double *u, const int *inl, int len, double *F, double *buffer) }
	 * 
	 * @param u
	 * 
	 * @param inliers
	 * 
	 * @param length
	 * 
	 * @param f
	 * 
	 * @param buffer
	 * 
	 */
	public static void u2f(final double[] u, final int[] inliers, final int length, final double[] f, final double[] buffer) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */

		if(u == null || u.length == 0) 
			throw new IllegalArgumentException("u: " + ArrayUtils.toString(u));
		if(inliers == null || inliers.length < length) 
			throw new IllegalArgumentException("inliers: " + ArrayUtils.toString(inliers));
		if(length < 0) 
			throw new IllegalArgumentException("length: " + length);
		if(f == null || f.length != 9) 
			throw new IllegalArgumentException("f: " + ArrayUtils.toString(f));
		if(buffer == null || buffer.length != 9*(u.length/6)) 
			throw new IllegalArgumentException("buffer: " + ArrayUtils.toString(buffer));
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */

		final double[] a1 = new double[3], a2 = new double[3];

		double[] z = buffer != null ? buffer : new double[9*length];
		double[] v = new double[9*9], U = new double[8*8], d = new double[9];

		if (length > 8) {
			
			normu(u, inliers, length, a1, a2);
			lineariseFundamentalMatrixN(u, z, inliers, length, a1, a2);

			covMat(v, z, length, 9);
			lapackEigen(v, d, 9);

		} else {

			lineariseFundamentalMatrix(u, z, inliers, length);
			
			// TODO: check svduv
			svduv(d, z, v, 9, U, 8);

		}

		int p = 0; // V related

		if (length > 8) {

			int j = 0;
			for (int i = 1; i < 9; i++)
				if (d[i] < d[j])
					j = i;
			p = j;

		} else {

			p = 8;
		}

		for (int i = 0; i < 9; i++) {
			f[i] = v[p];
			p += 9;
		}
		
		singulF(f);

		if (length > 8) {
			denormF(f, a1, a2);
		}
	}
	
	/**
	 * 
	 * <b> double checked </b>
	 * 
	 * <p> linearizes correspondences with respect to entries of fundamental matrix, so that u' F u -> A f </p>
	 * 
	 * {@code Ftools.c : void lin_fmN(const double *u, double *p, const int *inl, int len, double *A1, double *A2) }
	 * 
	 * @param u
	 * 
	 * @param p
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
	public static void lineariseFundamentalMatrixN(final double[] u, final double[] p, final int[] inliers, final int length, final double[] a1, final double[] a2) {
		
		/* ---------------------------------------------------------------------------------------
		 * method contract
		 * --------------------------------------------------------------------------------------- */
		
		if(length < 0) 
			throw new IllegalArgumentException("length: " + length);
		if(u == null || u.length < 6*length) 
			throw new IllegalArgumentException("u: " + ArrayUtils.toString(u));
		if(p == null) 
			throw new IllegalArgumentException("p: " + ArrayUtils.toString(p));
		if(inliers == null || inliers.length < length) 
			throw new IllegalArgumentException("inliers: " + ArrayUtils.toString(inliers));
		if(a1 == null || a1.length != 3) 
			throw new IllegalArgumentException("a1: " + ArrayUtils.toString(a1));
		if(a2 == null || a2.length != 3) 
			throw new IllegalArgumentException("a2: " + ArrayUtils.toString(a2));
		
		/* ---------------------------------------------------------------------------------------
		 * method business logic
		 * --------------------------------------------------------------------------------------- */
		
		final double[] a = new double[3], b = new double[3];
		
		a[2] = 1;
		b[2] = 1;
		
		for(int i = 0; i < length; i++) {
			
			final int index = 6 * inliers[i];
			
			a[0] = u[index] * a1[0] + a1[1];
			a[1] = u[index+1] * a1[0] + a1[2];
			b[0] = u[index+3] * a2[0] + a2[1];
			b[1] = u[index+4] * a2[0] + a2[2];
			
			for(int j = 0; j < 3; j++) {
				for(int k = 0; k < 3; k++) {
					
					p[9*i + 3*j + k] = a[k] * b[j];
				}
			}
		}
	}
	
	/**
	 * <b> double checked </b>
	 * 
	 * <p> def </p>
	 * 
	 * Calculates polynomial p in x, so that det(x*a + (1-x)*b) = 0 where a, b are [3][3] arrays and p is [4] array
	 * CHANGES b to a-b so finally det(a + (x-1)*b) = 0
	 * 
	 * {@code ftools.h : void slcm(double *A, double *B, double *p) }
	 * 
	 * @param a
	 * 
	 * @param b
	 * 
	 * @param p
	 * 
	 */
	public static void slcm(double[] a, double[] b, double[] p) {

		// set shortcuts -- a-shortcuts are immutable, b-shortcuts will be updated later
		final double a11 = a[0], a12 = a[1], a13 = a[2], a21 = a[3], a22 = a[4], a23 = a[5], a31 = a[6], a32 = a[7], a33 = a[8];
		double b11 = b[0], b12 = b[1], b13 = b[2], b21 = b[3], b22 = b[4], b23 = b[5], b31 = b[6], b32 = b[7], b33 = b[8];
		
		p[0] = -(b13*b22*b31) + b12*b23*b31 + b13*b21*b32 - 
				b11*b23*b32 - b12*b21*b33 + b11*b22*b33;

		p[1] = -(a33*b12*b21) + a32*b13*b21 + a33*b11*b22 - 
				a31*b13*b22 - a32*b11*b23 + a31*b12*b23 + 
				a23*b12*b31 - a22*b13*b31 - a13*b22*b31 + 
				3*b13*b22*b31 + a12*b23*b31 - 3*b12*b23*b31 - 
				a23*b11*b32 + a21*b13*b32 + a13*b21*b32 - 
				3*b13*b21*b32 - a11*b23*b32 + 3*b11*b23*b32 + 
				(a22*b11 - a21*b12 - a12*b21 + 3*b12*b21 + a11*b22 - 3*b11*b22)*b33;

	    p[2] = -(a21*a33*b12) + a21*a32*b13 + 
	    		a13*a32*b21 - a12*a33*b21 + 2*a33*b12*b21 - 
	    		2*a32*b13*b21 - a13*a31*b22 + a11*a33*b22 - 
	    		2*a33*b11*b22 + 2*a31*b13*b22 + a12*a31*b23 - 
	    		a11*a32*b23 + 2*a32*b11*b23 - 2*a31*b12*b23 + 
	    		2*a13*b22*b31 - 3*b13*b22*b31 - 2*a12*b23*b31 + 
	    		3*b12*b23*b31 + a13*a21*b32 - 2*a21*b13*b32 - 
	    		2*a13*b21*b32 + 3*b13*b21*b32 + 2*a11*b23*b32 - 
	    		3*b11*b23*b32 + a23*(-(a32*b11) + a31*b12 + a12*b31 - 2*b12*b31 - a11*b32 + 2*b11*b32) + 
	    		(-(a12*a21) + 2*a21*b12 + 2*a12*b21 - 3*b12*b21 - 2*a11*b22 + 3*b11*b22)*b33 + 
	    		a22*(a33*b11 - a31*b13 - a13*b31 + 2*b13*b31 + a11*b33 - 2*b11*b33);

	   for (int i = 0; i < 9; i++) b[i] = a[i] - b[i];
	   
	   // reset shortcuts 
	   b11 = b[0]; b12 = b[1]; b13 = b[2]; b21 = b[3]; b22 = b[4]; b23 = b[5]; b31 = b[6]; b32 = b[7]; b33 = b[8];
	 
	   p[3] =-(b13*b22*b31) + b12*b23*b31 + b13*b21*b32 - 
			   b11*b23*b32 - b12*b21*b33 + b11*b22*b33; 
	}
	
	
	/**
	 * <p> des </p>
	 * 
	 * {@code  Ftools.h : void exFDs (const double *u, const double *F, double *p, double *w, int len) }
	 * 
	 * @param u
	 * 
	 * @param serializedModel
	 * 
	 * @param p
	 * 
	 * @param w
	 * 
	 * @param length
	 * 
	 */
	public static void exFDs (final double[] u, final double[] serializedModel, final double[] p, final double[] w, final int length) {

		double rx, ry, rwc, ryc, rxc, r;

		int uIndex = 0;
		int pIndex = 0;
		int wIndex = 0;
		final double f1 = serializedModel[0], f2 = serializedModel[1], f3 = serializedModel[2], 
					 f4 = serializedModel[3], f5 = serializedModel[4], f6 = serializedModel[5], 
					 f7 = serializedModel[6], f8 = serializedModel[7], f9 = serializedModel[8];

		for (int i = 1; i <= length; i++) {
			rxc = f1 * u[uIndex + 3] + f4 * u[uIndex + 4] + f7;
			ryc = f2 * u[uIndex + 3] + f5 * u[uIndex + 4] + f8;
			rwc = f3 * u[uIndex + 3] + f6 * u[uIndex + 4] + f9;
			r = (u[uIndex] * rxc + u[uIndex + 1] * ryc + rwc);
			rx = f1 * u[uIndex] + f2 * u[uIndex + 1] + f3;
			ry = f4 * u[uIndex] + f5 * u[uIndex + 1] + f6;

			w[wIndex] = rxc * rxc + ryc * ryc + rx * rx + ry * ry;
			p[pIndex] = r * r / w[wIndex];
			w[wIndex] = 1 / Math.sqrt(w[wIndex]);

			pIndex += 1;
			wIndex += 1;
			uIndex += 6;
		}
	}
	
	/**
	 * <p> def </p>
	 * 
	 * {@code Ftools.h : void u2fw(const double *u, const int *inl, const double * w, int len, double *F, double *buffer) }
	 * 
	 * @param u
	 * 
	 * @param inliers
	 * 
	 * @param w
	 * 
	 * @param length
	 * 
	 * @param serializedModel
	 * 
	 * @param buffer
	 * 
	 */
	public static void u2fw(final double[] u, final int[] inliers, final double[] w, final int length, final double[] serializedModel, final double[] buffer) {

		   double[] a1 = new double[3], a2 = new double[3];
		   double[] z = buffer, v = new double[9*9], U = new double[8*8], D = new double[9];

		   if (length > 8) {
		      normu(u, inliers, length, a1, a2); 
		      lineariseFundamentalMatrixN(u, z, inliers, length, a1, a2);
		      
		      for (int i = 0, j; i < length; i++) {
		         j = inliers[i];
		         scalmul(z, 9*i, w[j], 9, 1);
		      }
		      
		      covMat(v, z, length, 9);
		      lapackEigen(v, D, 9);
		      
		   } else {
			   
			  lineariseFundamentalMatrix(u, z, inliers, length); 
		      for(int i = 0, j; i < length; i++) {
		    	  
		         j = inliers[i];
		         scalmul(z, i, w[j], 9, 9);
		      }
		      
		      svduv(D,z,v,9,U,8);
		   }
		   
		   int p;

		   if (length > 8) {
		      
			  int j = 0;
		      for (int i = 1; i < 9; i++) {
		         if (D[i] < D[j]) j = i;
		      }
		      
		       p = j; // v related
		   
		   } else {
		      
			   p = 8; // v related
		   }
		   
		   for (int i = 0; i < 9; i++) {
			   serializedModel[i] = v[p];
		       p += 9;
		   }

		   singulF(serializedModel); 

		   if (length > 8) {
		      denormF(serializedModel, a1, a2);
		   }

	}
	
	/**
	 * <p> def </p>
	 * 
	 * {@ code Fools.h : void singulF(double *F) }
	 * 
	 * @param f
	 */
	public static void singulF(double[] f) {
		
		   double[] S = new double[3], u = new double[9], vt = new double[9], d = new double[]{1,0,0,0,1,0,0,0,1}, vd = new double[9];
		   
		   // our 'lapack' does not store column-wise
//		   trnm(f,3);  
		   
		   if(!lapackSVD (S, f, u, 3, vt, 3)) {
			   
		     ArraysUtils.copyArray(f, d);
		     return;
		   }
		   
		   d[0] = S[0];
		   d[4] = S[1];
		   d[8] = 0;
		   
		   final double[] dvt = new double[9];
		   mmul(dvt, d, vt, 3);
		   mmul(f, u, dvt, 3);
		   
//		   mmul(vd, v, d, 3); /*F = U.D.V^T = (V.D^T.U^T)^T = (V.D.U^T)^T*/
//		   mmul(f, vd, ut, 3);
//		   
//		   trnm(f,3);
		
	}
}
