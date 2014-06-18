/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.loransac;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.stefanrakonjac.mgrthesis.ransac.AbstractTest;
import com.stefanrakonjac.mgrthesis.ransac.RANSAC;
import com.stefanrakonjac.mgrthesis.ransac.impl.loransac.LoRANSACImpl;

import static com.stefanrakonjac.mgrthesis.ransac.utils.LoggingUtils.*;

/**
 * @author Stefan.Rakonjac
 *
 */
public class OriginalTest extends AbstractTest {
	
	@Test(dataProvider="originalTestDP", enabled=false)
	public void originalTest(double[][] expectedModel, double[][] inlierPairs, double[][] outlierPairs, double[][] tentativePairs, double errorThershold, double confidence, int maxSamples) {
		
		logger.info("originalTest(...) started");
		
		final long startTime = System.currentTimeMillis();
		
		final RANSAC ransac = new LoRANSACImpl();
		
		ransac.setBestSolutionProbability(confidence)
			  .setMaxIterations(maxSamples)
			  .setTentativePairs(tentativePairs)
			  .setThreshold(errorThershold);
		
		ransac.run();
		
		logTime(startTime, "Original test LoRANSAC RUN");
		
		logger.info("Expected model: {}", ArrayUtils.toString(expectedModel));
		logger.info("Estimated model: {}", ArrayUtils.toString(ransac.getGeometryEstimation()));

		logger.info("Inliers: {}", ArrayUtils.toString(ransac.getInliers()));
		
		Assert.assertEquals(inlierPairs.length, ransac.getInliers().length);
		
		outer:
		for(double[] inlier : ransac.getInliers()) {
			for(double[] inlierPair : inlierPairs) {
				if(Arrays.equals(inlierPair, inlier)) continue outer;
			}
			
			Assert.fail("LoRANSAC inlier " + ArrayUtils.toString(inlier) + " not among actual inliers " + ArrayUtils.toString(inlierPairs));
		}
		
		logger.info("Outliers: {}", ArrayUtils.toString(ransac.getOutliers()));
		
		Assert.assertEquals(outlierPairs.length, ransac.getOutliers().length);
		
		outer:
		for(double[] outlier : ransac.getOutliers()) {
			for(double[] outlierPair : outlierPairs) {
				if(Arrays.equals(outlierPair, outlier)) continue outer;
			}
			
			Assert.fail("LoRANSAC outlier " + ArrayUtils.toString(outlier) + " not among actual outliers " + ArrayUtils.toString(outlierPairs));
		}
		
	}
	
	
	
	/** ------------------ data providers -------------------- */
	
	@DataProvider
	public Object[][] originalTestDP() {
		
		/*
		 * one (two) set(s) of data:
		 * 
		 *  double[][] expectedModel	-	actual epipolar geometry model
		 * 	double[][] inlierPairs		-	2D inlier pairs
		 * 	double[][] outlierPairs		-	2D outlier pairs
		 * 	double[][] tentativePairs	-	2D tentative pairs
		 * 	double errorThershold		-	threshold
		 * 	double confidence			-	confidence of best solution
		 * 	int maxSamples				-	maximal number of samples for least squares
		 * 	
		 */

		Object[][] retval = new Object[2][7];
		
		// create primitive data
		
		retval[0][0] = new double[][] { { -1.0, 0.0, 0.0 }, { 0.0, 1.0, 0.0 }, { 0.0, 0.0, 0.0 } };
		retval[0][1] = new double[][] { { 1.0, 1.0, 2.0, 2.0 }, 	// inliers
										{ 2.0, 1.0, 1.0, 2.0 }, 
										{ 3.0, 3.0, 1.0, 1.0 }, 
										{ 4.0, 1.0, 1.0, 4.0 }, 
										{ 3.0, 5.0, 5.0, 3.0 }, 
										{ 6.0, 2.0, 0.5, 1.5 },	
										{ 2.0, 7.0, 3.5, 1.0 }, 
										{ 3.0, 2.0, 0.5, 0.75 }, 
										{ 5.0, 3.0, 1.5, 2.5 }, 
										{ 4.0, 6.0, 3.0, 2.0 } }; 
		retval[0][2] = new double[][] { { 5.0, 4.0, 5.0, 4.0 }, 	// outliers
										{ 2.0, 3.0, 1.5, 4.0 } };
		retval[0][3] = ArrayUtils.addAll((double[][]) retval[0][1], (double[][]) retval[0][2]);
		retval[0][4] = 0.01;
		retval[0][5] = 0.95;
		retval[0][6] = (int) 1e5;
		
		// create realistic data
		
		retval[1][0] = new double[][] { { -0.00000, -0.00000, -0.00051 }, { -0.00000, -0.00000, 0.00402 }, { -0.00272, -0.00187, 1.00000 } };
		retval[1][1] = new double[][] { { 406.29309082, 200.94827271, 276.36764526, 325.98529053 }, 
										{ 48.14285660, 304.30950928, 570.26495361, 288.50460815 }, 
										{ 48.14285660, 304.30950928, 570.26495361, 288.50460815 }, 
										{ 47.68548203, 410.09274292, 656.23376465, 365.47076416 }, 
										{ 47.68548203, 410.09274292, 656.23376465, 365.47076416 }, 
										{ 423.83465576, 169.04582214, 260.67083740, 282.89862061 }, 
										{ 407.45330811, 156.36381531, 240.96749878, 297.00314331 }, 
										{ 268.88369751, 247.84593201, 270.25946045, 487.06356812 }, 
										{ 59.99473572, 224.13157654, 509.08621216, 213.74137878 }, 
										{ 61.70588303, 233.30882263, 515.96911621, 221.12962341 }, 
										{ 385.66806030, 502.02099609, 529.50000000, 401.55883789 }, 
										{ 51.91345978, 185.38461304, 470.32162476, 200.30540466 }, 
										{ 70.68461609, 307.16921997, 581.54705811, 261.50000000 }, 
										{ 55.42617416, 336.73489380, 599.32110596, 303.29815674 }, 
										{ 40.87777710, 211.96665955, 486.50875854, 235.07894897 }, 
										{ 64.46154022, 215.94230652, 502.48837280, 205.75581360 }, 
										{ 250.21186829, 225.16101074, 250.75000000, 484.31817627 }, 
										{ 68.25862122, 276.31033325, 554.16668701, 244.57575989 }, 
										{ 562.82550049, 236.30207825, 314.89901733, 258.69369507 }, 
										{ 562.82550049, 236.30207825, 314.89901733, 258.69369507 }, 
										{ 633.72497559, 176.52777100, 326.52035522, 112.01110840 }, 
										{ 589.64062500, 195.07031250, 306.37365723, 190.24006653 }, 
										{ 314.83334351, 199.38000488, 239.93589783, 419.65383911 }, 
										{ 28.45121956, 262.64633179, 306.37365723, 190.24006653 }, 
										{ 62.16938782, 413.93673706, 663.09735107, 351.92575073 }, 
										{ 61.85380173, 414.92691040, 663.09735107, 351.92575073 }, 
										{ 62.03333282, 268.25555420, 545.40740967, 245.48147583 }, 
										{ 47.43103409, 270.05172729, 541.17144775, 264.91427612 }, 
										{ 633.79895020, 176.28350830, 326.52035522, 112.01110840 }, 
										{ 633.79895020, 176.28350830, 326.52035522, 112.01110840 }, 
										{ 633.72497559, 176.52777100, 326.52035522, 112.01110840 }, 
										{ 289.41802979, 137.56556702, 189.69999695, 391.63751221 }, 
										{ 288.83731079, 137.78059387, 189.69999695, 391.63751221 }, 
										{ 407.71176147, 88.01764679, 182.18055725, 260.88888550 }, 
										{ 399.95159912, 112.14516449, 197.50970459, 284.81066895 }, 
										{ 416.98300171, 80.17346954, 181.64797974, 244.19058228 }, 
										{ 314.67855835, 141.58927917, 196.62121582, 377.04544067 }, 
										{ 333.67141724, 143.80000305, 200.79032898, 365.95159912 }, 
										{ 573.39129639, 256.04348755, 344.03622437, 244.38406372 }, 
										{ 594.19879150, 257.60842896, 366.90875244, 203.94525146 }, 
										{ 61.21641922, 272.31591797, 548.40722656, 249.72680664 }, 
										{ 61.21641922, 272.31591797, 548.40722656, 249.72680664 }, 
										{ 47.72595978, 269.57211304, 540.88122559, 264.78176880 }, 
										{ 47.72595978, 269.57211304, 540.88122559, 264.78176880 }, 
										{ 34.97169876, 267.37420654, 532.68615723, 280.25375366 }, 
										{ 405.87289429, 200.82608032, 271.27163696, 331.94320679 }, 
										{ 227.44215393, 202.72451782, 140.44897461, 458.67346191 }, 
										{ 267.32916260, 198.53092957, 233.08920288, 452.19717407 }, 
										{ 25.09340668, 265.22528076, 524.77050781, 294.08605957 }, 
										{ 25.09340668, 265.22528076, 524.77050781, 294.08605957 }, 
										{ 255.02000427, 132.88000488, 180.22222900, 413.30554199 } }; 
		retval[1][2] = new double[][] { { 255.02000427, 132.88000488, 180.22222900, 413.30554199 }, 
										{ 290.52880859, 196.06015015, 234.35000610, 433.95999146 }, 
										{ 314.91641235, 196.39904785, 237.66709900, 417.53857422 }, 
										{ 245.61080933, 203.43074036, 233.23281860, 471.62976074 } };
		retval[1][3] = ArrayUtils.addAll((double[][]) retval[1][1], (double[][]) retval[1][2]);
		retval[1][4] = 0.35;
		retval[1][5] = 0.95;
		retval[1][6] = (int) 1e5;
		
		return retval;
	}
}
