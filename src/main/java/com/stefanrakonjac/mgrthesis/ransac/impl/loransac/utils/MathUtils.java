/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils;

import org.apache.commons.lang3.ArrayUtils;

import com.stefanrakonjac.mgrthesis.ransac.utils.ArraysUtils;

/**
 * @author Stefan.Rakonjac
 * 
 *         {@code matutl.h}
 */
public class MathUtils {

	/**
	 * <p> def </p>
	 * 
	 * {@code matutl.h : int svduv(double *d,double *a,double *u,int m,double *v,int n) }
	 * 
	 * @param d
	 * 
	 * @param a
	 * 
	 * @param u
	 * 
	 * @param m
	 * 
	 * @param v
	 * 
	 * @param n
	 * 
	 * @return
	 * 
	 */
	public static int svduv(double[] d, double[] a, double[] u, int m, double[] v, int n) {

		if (m < n)
			return -1;

		double s, h, t, sv;
		final double[] w = new double[m + n];

		for (int i = 0, mm = m, nm = n - 1, p = 0; i < n; i++, mm--, nm--, p += n + 1) {

			if (mm > 1) {
				sv = h = 0d;
				s = 0d;
				for (int j = 0, q = p; j < mm; j++, q += n) {
					w[j] = a[q];
					s += a[q] * a[q];
				}

				if (s > 0) {
					
					h = Math.sqrt(s);
					
					if (a[p] < 0) {
						h = -h;
					}
					
					s += a[p] * h;
					s = 1d / s;
					t = 1d / (w[0] += h);
					sv = 1d + Math.abs(a[p] / h);
					
					for (int k = 1, ms = n - i; k < ms; k++) {
						
						double r = 0d;
						
						for (int j = 0, q = p + k; j < mm; q += n) {
							r += w[j++] * a[q];
						}
						
						r *= s;
						
						for (int j = 0, q = p + k; j < mm; q += n) {
							a[q] -= r * w[j++];
						}
					}

					for (int j = 1, q = p; j < mm; ) {
						a[q += n] = t * w[j++];
					}
				}

				a[p] = sv;
				d[i] = -h;
			}

			if (mm == 1) {
				d[i] = a[p];
			}

			int p1 = p + 1;
			sv = h = 0d;
			
			if (nm > 1) {
				
				s = 0d;
				for (int j = 0, q = p1; j < nm; j++, q++) {
					s += a[q] * a[q];
				}

				if (s > 0d) {
					
					h = Math.sqrt(s);
					
					if (a[p1] < 0d) {
						h = -h;
					}
					
					sv = 1d + Math.abs(a[p1] / h);
					s += a[p1] * h;
					s = 1d / s;
					t = 1d / (a[p1] += h);

					for (int k = n, ms = n * (m - i); k < ms; k += n) {
						double r = 0d;
						for (int j = 0, q = p1, pp = p1 + k; j < nm; j++) {
							r += a[q++] * a[pp++];
						}

						r *= s;

						for (int j = 0, q = p1, pp = p1 + k; j < nm; j++) {
							a[pp++] -= r * a[q++];
						}
					}

					for (int j = 1, q = p1 + 1; j < nm; j++) {
						a[q++] *= t;
					}
				}
				a[p1] = sv;
				w[m + i] = -h;
			}

			if (nm == 1) {
				w[m + i] = a[p1];
			}
		}

		ldvmat(a, v, n);
		ldumat(a, u, m, n);
		
		final double[] e = ArrayUtils.subarray(w, m, w.length); // C: w+m
		qrbdv(d, e, u, m, v, n);
		ArraysUtils.copySubArray(w, e, m); // qrbdv(args ...) updates e, push it back to w

		for (int i = 0; i < n; i++) {
			if (d[i] < 0d) {
				d[i] = -d[i];
				for (int j = 0, p = i; j < n; j++, p += n) {
					v[p] = -v[p];
				}
			}
		}

		return 0;
	}

	/**
	 * <p> def </p>
	 * 
	 * {@code matutl.h : void ldvmat(double *a,double *v,int n) }
	 * 
	 * @param d
	 * 
	 * @param w
	 * 
	 * @param u
	 * 
	 * @param m
	 * 
	 * @param v
	 * 
	 * @param n
	 * 
	 */
	public static int qrbdv(double[] dm, double[] em, double[] um, int mm, double[] vm, int m) {
		
		int nm = m;
		double u, x, y, a, b, c, s, t, w;
		int p, q;
		
		t = Math.abs(dm[0]);
		for (int j = 1; j < m; ++j) {
			if((s = Math.abs(dm[j]) + Math.abs(em[j - 1])) > t) t = s;
		}
		    
		t *= 1.e-15;
		
		int j = 0;
		for(; m > 1 && j < 100 * m; ++j) {
			int k = m - 1;
			while(k > 0) {
				
				if(Math.abs(em[k - 1]) < t) break;
				
				if(Math.abs(dm[k - 1]) < t) {
					s = 1d;
					c = 0d;
					
					for(int i = k; i<m; ++i) {
						a = s * em[i-1]; 
						b= dm[i]; 
						em[i-1] *= c;
						dm[i] = u = Math.sqrt(a*a + b*b); 
						s = -a/u; 
						c= b/u;
						p = k - 1; // um related
						for(int jj = 0; jj < mm; ++jj, p += mm) {
							q = p + i - k + 1; // um related
							w = c*um[p] + s*um[q]; 
							um[q] = c*um[q] - s*um[p]; 
							um[p] = w;
						}
					}
					
					break;
		        }
				
				k--;
			}
		    
			y = dm[k]; 
			x = dm[m-1]; 
			u = em[m-2];
		    a = (y + x)*(y - x) - u*u; 
		    s = y*em[k]; 
		    b = s + s;
		    u = Math.sqrt(a*a + b*b);

		    if(u != 0d) {
		    	
		    	c = Math.sqrt((u + a)/(u + u));
		    	
		    	if(c != 0d) {
		    		s /= (c*u); 
		    	} else {
		    		s = 1d;
		    	}

		    	for(int i = k; i < m - 1; ++i) {
		    		
		    		b = em[i];
		    		if(i > k) {
		    			a = s*em[i]; 
		    			b *= c;
		    			em[i-1] = u = Math.sqrt(x*x + a*a);
		    			c = x/u; 
		    			s = a/u;
		    		}
		        
		    		a = c*y + s*b; 
		    		b = c*b - s*y;
		    		
		    		p = i; // vm related
		    		for(int jj = 0; jj < nm; ++jj, p += nm) {
		    			w = c * vm[p] + s*vm[p+1]; 
		    			vm[p+1] = c*vm[p+1] - s*vm[p]; 
		    			vm[p] = w;
		    		}
		    		
		    		s *= dm[i+1]; 
		    		dm[i] = u = Math.sqrt(a*a + s*s);
		    		y = c*dm[i+1]; 
		    		c = a/u; 
		    		s /= u;
		    		x = c*b + s*y;
		    		y = c*y - s*b;
		    		
		    		p = i; // um related
		    		for(int jj=0; jj < mm; ++jj, p += mm) {
		    			w = c*um[p] + s*um[p+1]; 
		    			um[p+1] = c*um[p+1] - s*um[p]; 
		    			um[p] = w;
		    		}
		    	}
		    }
		    
		    em[m-2] = x; 
		    dm[m-1] = y;
		    
		    if(Math.abs(x) < t) --m;
		    if(m == k + 1) --m; 
	    }
		
		return j;
	}

	/**
	 * <b> not checked </b>
	 * 
	 * <p> def </p>
	 * 
	 * {@code matutl.h : void ldvmat(double *a,double *v,int n) }
	 * 
	 * @param a
	 * 
	 * @param v
	 * 
	 * @param n
	 * 
	 */
	public static void ldvmat(double[] a, double[] v, int n) {
		
		double h, s;
		
		for(int i = 0, q = 0; i < n*n ; ++i) {
			v[q++] = 0d;
		}

		v[0]=1d; 
		int q0 = n*n-1; // v related 
		v[q0] = 1d;
		q0 -= n+1;
		int p0= n*n - n - n - 1; // a related
		
		for(int i = n-2, mm=1; i > 0; --i, p0 -= n+1, q0 -= n+1, ++mm) {
		    if(a[p0-1] != 0d) {
		    	
		    	h = 1;
		    	
		    	for(int j = 0, p = p0; j < mm; ++j, ++p) {
		    		h += a[p]*a[p];
		    	}
		    	
		    	h = a[p0-1]; 
		    	v[q0] = 1d - h;
		    	
		    	for(int j = 0, q = q0 + n, p = p0; j < mm; ++j, q += n) {
		    		v[q] = -h * a[p++]; 
		    	}
		    	
		    	for(int k = i + 1, q = q0 + 1; k < n; ++k) {
		    		
		    		s =0d;
		    		
		    		for(int j = 0, qq = q + n, p = p0; j < mm; ++j, qq += n) {
		    			s += v[qq] * a[p++];
		    		}
		    		
		    		s *= h;
		    		
		    		for(int j = 0, qq = q + n, p = p0; j < mm; ++j, qq += n) {
		    			v[qq] -= s * a[p++];
		    		}
		    		
		    		v[q++] = -s;
		    	}
		    	
		    } else {
		    	
		      v[q0] = 1d;
		      
		      for(int j=0, p = q0 + 1, q = q0 + n; j < mm; ++j, q += n) {
		    	  v[q] = a[p++] = 0d;
		      }
		   }
	   }
	}
	
	/**
	 * <b> checked </b>
	 * 
	 * <p> def </p>
	 * 
	 * {@code matutl.h : void ldumat(double *a,double *u,int m,int n) }
	 * 
	 * @param a
	 * 
	 * @param u
	 * 
	 * @param m
	 * 
	 * @param n
	 * 
	 */
	public static void ldumat(double[] a,double[] u, int m, int n) {

		int p0, q0;
		double h;
		
		final double[] w = new double[m];
		
		// q - u related
		for(int i = 0, q = 0; i < m*m; i++) {
			u[q++] = .0;
		}
		
		p0 = n*n - 1; // a related
		
		q0 = m*m - 1; // u related
		
		int mm = m-n; 
		int i = n-1;
		  
		for(int j = 0; j < mm; j++, q0 -= m + 1) {
			u[q0] = 1.;
		}
		  
		if(mm == 0) { 
			
			p0 -= n + 1; 
			u[q0] = 1.; 
			q0 -= m + 1;
			
			i--; 
			mm++;
		}
		
		for(; i >= 0; i--, mm++, p0 -= n + 1, q0 -= m + 1) {
			
			if(a[p0] != 0.) {
				h = 1.;

				for(int j = 0, p = p0 + n; j < mm; p += n) {
					w[j++]= a[p];
				}
				
				h = a[p0]; 
				u[q0] = 1. - h;
				
				for(int j = 0, q = q0 + m;  j < mm; q += m) {
					u[q] = -h*w[j++];
				}
				for(int k = i + 1, q = q0 + 1; k < m; k++) {

					double s = 0;
					for(int j = 0, p = q + m; j < mm; p += m) {
						s += w[j++]* u[p];
					}
					
					s *= h;
					
					for(int j = 0, p = q + m; j < mm; p += m) {
						u[p] -= s*w[j++];
					}
					
					u[q++] = -s;
		     	}
			} else {
				
				u[q0] = 1.;
				
				for(int j = 0, p = q0 + 1, q = q0 + m; j < mm; ++j, q += m) {
					u[q]= u[p++] = 0.;
				}
		     }
		}
	}
	
	/**
	 * <b> checked </b>
	 * 
	 * <p> def </p>
	 * 
	 * {@code matutl.h : void trnm(double *a,int n) }
	 * 
	 * @param a
	 * @param n
	 */
	public static void trnm(double[] a, int n) {
		
		double s;
		int i,j,e;
		
		int p; // a related
		int q; // a related
		int aIndex = 0;
		
		for(i = 0, e = n - 1; i < n - 1; ++i, --e, aIndex += n + 1) {
			
			for(p= aIndex + 1, q = aIndex + n, j = 0; j < e; ++j) {
		
				s= a[p]; 
				a[p++] = a[q]; 
				a[q] = s; 
				q += n;
			}
		}
	}
	
	/**
	 * <b> checked </b>
	 * 
	 * <p> def </p>
	 * 
	 * {@code matutl.h : void mmul(double *c,double *a,double *b,int n) }
	 * 
	 * @param c
	 * 		Method updates this parameter
	 * @param a
	 * 
	 * @param b
	 * 		
	 * @param n
	 * 
	 */
	public static void mmul(double[] c, double[] a, double[] b, int n) {
		
		trnm(b, n);

		int aIndex = 0, cIndex = 0;

		for (int i = 0; i < n; i++, aIndex += n) {
			for (int j = 0, q = 0; j < n; ++j) {
				
				double s = 0d;
				for (int k = 0, p = aIndex; k < n; k++) {
					s += a[p++] * b[q++];
				}
				
				c[cIndex++] = s;
			}
		}
		
		trnm(b, n);
	}
	
	/**
	 * <p> Simple wrapper function using Math.min() and Math.max() to limit the provided value inside provided range </p>
	 * 
	 * @param value
	 * 		Value to be limited
	 * @param min
	 * 		Minimal value
	 * @param max
	 * 		Maximal value
	 * @return
	 * 		Returns {@code value} if provided value is within range {@code [min, max]}, {@code min} if provided value is less then {@code min} 
	 * 		or {@code max} if provided value is more than {@code max}
	 */
	public static double limit(final double value, final double min, final double max) {
	    return Math.max(min, Math.min(value, max));
	}
}
