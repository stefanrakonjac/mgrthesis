/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.loransac;

import static com.stefanrakonjac.mgrthesis.ransac.utils.LoggingUtils.logTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.stefanrakonjac.mgrthesis.ransac.AbstractTest;
import com.stefanrakonjac.mgrthesis.ransac.RANSAC;
import com.stefanrakonjac.mgrthesis.ransac.impl.loransac.LoRANSACImpl;
import com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.MatrixUtils;
import com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils.Pair;
import com.stefanrakonjac.mgrthesis.ransac.utils.ArraysUtils;

/**
 * @author Stefan.Rakonjac
 *
 */
public class LebedaDataTest extends AbstractTest {
	
	private static final Logger logger = LoggerFactory.getLogger(LebedaDataTest.class);

	public static final String DATA_ROOT = "./data/lebeda/kusvod2/prepared";
	
	public static final String[] IMAGE_NAMES = { "booksh", "box", "castle", "corr", "graff", "head", "kampa", "Kyoto", 
							  					 "leafs", "plant", "rotunda", "shout", "valbonne", "wall", "wash", "zoom" };

	public static final String ALL_DATA_TXT = "allData.txt";
	public static final String ALL_DATA_C_PARSED_TXT = "allDataCParsed.txt";
	public static final String C_RECOGNIZED_INLIERS_TXT = "cRecognizedInliers-th01-bsp099.txt";
	public static final String C_RECOGNIZED_MODEL_TXT = "cRecognizedModel-th01-bsp099.txt";
	
	public static final String IMAGE_EXTENSION = ".png";
	
	private double totalInliersHitPercent = 0;
	
	@Test(dataProvider="lebedaDataTestDP", enabled=false)
	public void lebedaDataTest(final double[][] tentativePairs, 
							   final double[][] groundTruthPairs, 
							   final double errorThershold, 
							   final double confidence, 
							   final int maxSamples, 
							   final String leftImageURL, 
							   final String rightImageURL, 
							   final double[][] expectedModel, 
							   final String imageName, 
							   final Set<Pair> expectedCRecognizedInliers,
							   final double[][] expectedCRecognizedModel) throws IOException {
		
		logger.info("");
		logger.info("lebedaDataTest(..., '{}', ...) started" , imageName);

		final long ransacStartTime = System.currentTimeMillis();
		
		final RANSAC ransac = new LoRANSACImpl()
												// TODO comment following line
//												.doOptimizations(false)
												.setTentativePairs(tentativePairs)
												.setThreshold(errorThershold)
												.setBestSolutionProbability(confidence)
												.setMaxIterations(maxSamples);
		
		ransac.run();
		
		logTime(ransacStartTime, System.currentTimeMillis(), "LoRANSAC RUN");
		
		final double[][] inliers = ransac.getInliers();

		logger.info("Inliers found: {} out of {}; Inliers ration: {}", inliers.length, tentativePairs.length, ((double) inliers.length)/tentativePairs.length);
		
//		DisplayUtils.showPairs(leftImageURL, rightImageURL, PairsConverter.toAssociatedPairList(tentativePairs));
//		DisplayUtils.showPairs(leftImageURL, rightImageURL, PairsConverter.toAssociatedPairList(inliers));
//		DisplayUtils.showPairs(leftImageURL, rightImageURL, PairsConverter.toAssociatedPairList(groundTruthPairs));
		
		final List<Pair> actualRecognizedInliers = new ArrayList<>();
		for(double[] inlier : inliers) {
			actualRecognizedInliers.add(new Pair(inlier));
		}
		
		logger.debug("Actual inliers that were not among expected ones:");
		for(Pair actualInlier : actualRecognizedInliers) {
			if(!expectedCRecognizedInliers.contains(actualInlier)) {
				logger.debug(actualInlier.toString());
			}
		}
		
		logger.debug("Expected inliers that were not among actual ones:");
		for(Pair expectedInlier : expectedCRecognizedInliers) {
			if(!actualRecognizedInliers.contains(expectedInlier)) {
				logger.debug(expectedInlier.toString());
			}
		}
		
		@SuppressWarnings("unchecked")
		final Collection<Pair> inliersIntersection = CollectionUtils.intersection(expectedCRecognizedInliers, actualRecognizedInliers);

		final double expectedInliersCount = expectedCRecognizedInliers.size();
		final double actualInliersCount = actualRecognizedInliers.size();
		final double inliersIntersectionCount = inliersIntersection.size();
		final double inliersHitPercent = inliersIntersectionCount*100/expectedInliersCount;
		
		logger.info("Actual/expected inliers: {}/{} out of {}", actualInliersCount, expectedInliersCount, tentativePairs.length);
		logger.info("Inliers hit: {} out of {} ({}%)", inliersIntersectionCount, expectedInliersCount, inliersHitPercent);
		
		totalInliersHitPercent += inliersHitPercent;
		
		logger.info("lebedaDataTest(..., '{}', ...) finished correctly", imageName);
	}
	
	@AfterClass
	public void printAverageInliersHitPercent() {

		logger.info("");
		logger.info("Average inliers hit percent: {}", totalInliersHitPercent/IMAGE_NAMES.length);
		logger.info("");
	}
	
	/** ------------------ data providers -------------------- */
	
	@DataProvider
	public Object[][] lebedaDataTestDP() throws NumberFormatException, IOException {
		
		/*
		 * one set of data:
		 * 
		 * 	double[][] tentativePairs	-	2D tentative pairs
		 * 	double[][] actualPairs		-	2D actual pairs (manually annotated)
		 * 	double errorThershold		-	threshold
		 * 	double confidence			-	confidence of best solution
		 * 	int maxSamples				-	maximal number of samples for least squares
		 *  String leftImageURL			- 	absolute URL of the left image
		 *  String rightImageUrl		- 	absolute URL of the right image
		 *  double[][] expectedModel	- 	expected epipolar-geometry transformation model
		 * 	
		 */
		
		final Object[][] retval = new Object[IMAGE_NAMES.length][11];
		
		for(int i = 0; i < IMAGE_NAMES.length; i++) {
			
			final String imageName = IMAGE_NAMES[i];
			final File imageRootDir = new File(DATA_ROOT, imageName);
			
			// make a check for any case
			if(!imageRootDir.exists()) 
				throw new IllegalStateException("image root directory not found: " + imageRootDir.getPath());

			final double[][] tentativePairs = readDataFile(new File(imageRootDir, "allData.txt"), true);
			final double[][] groundTruthPairs = readDataFile(new File(imageRootDir, "data.txt"), false); // nothing to ignore--checked

			final Set<Pair> expectedCRecognizedInliers = readCRecognizedInliers(new File(imageRootDir, C_RECOGNIZED_INLIERS_TXT));
			final double[][] expectedCRecognizedModel = readModel(new File(imageRootDir, C_RECOGNIZED_MODEL_TXT));
			
			final double threshold = readThreshold(new File(imageRootDir, "threshold.txt"));
			
			final double[][] model = readModel(new File(imageRootDir, "model.txt"));

			retval[i][0] = tentativePairs;
			retval[i][1] = groundTruthPairs;
			retval[i][2] = threshold;																	// TODO overwritten on next line
			retval[i][2] = .1d;																			// 0.1 px
			retval[i][3] = 0.99;																		// 99%
			retval[i][4] = (int) 1e6;																	// 100 thousand
			retval[i][5] = imageRootDir.getPath() + File.separator + imageName + "A" + IMAGE_EXTENSION;
			retval[i][6] = imageRootDir.getPath() + File.separator + imageName + "B" + IMAGE_EXTENSION;
			retval[i][7] = model;
			retval[i][8] = imageName;
			retval[i][9] = expectedCRecognizedInliers;
			retval[i][10] = expectedCRecognizedModel;
		}
		
		return retval;
	}

	private double[][] readModel(File file) throws NumberFormatException, IOException {
		
		if(file == null)
			throw new IllegalArgumentException("file");
		
		if(!file.exists()) 
			file.createNewFile();
		
		final double[][] retval = new double[3][3];
		
		int index = 0;
		for(String value : FileUtils.readFileToString(file).split("\\s")) {
			
			if(StringUtils.isBlank(value)) continue;
			
			final double valueDouble = Double.parseDouble(value);
			retval[index/3][index%3] = valueDouble;
			index++;
		}
		
		return retval;
	}

	private Set<Pair> readCRecognizedInliers(File file) throws IOException {
		
		if(file == null)
			throw new IllegalArgumentException("file");
		
		if(!file.exists()) 
			file.createNewFile();

		final Set<Pair> retval = new HashSet<>();
		
		for(Object line : FileUtils.readLines(file)) {
			retval.add(new Pair((String) line));
		}
		
		return retval;
	}

	public static double readThreshold(File file) throws IOException {
		final double[] retval = new double[2];
		
		int i = 0;
		for(Object lineObj : FileUtils.readLines(file)) {
			final String line = (String) lineObj;
			retval[i] = Double.parseDouble(StringUtils.trim(line));
			
			if(retval[i] == 0) {
				logger.debug("parsing #{} threshold to 0; file: {}", i+1, file);
			}
			
			i++;
		}
		
		return retval[1] != 0 ? retval[1] : retval[0];
	}

	public static double[][] readDataFile(File file, boolean omitDuplicates) throws NumberFormatException, IOException {

		final List<double[]> retval = new ArrayList<>();
		
		int lineNumer = 0; // enumerating lines from 0 to 3
		for(Object line : FileUtils.readLines(file)) {
			
			int pointNumber = 0; // enumerating points from 0 to N
			for(String point : ((String) line).split("\\s")) {
				
				if(lineNumer == 0) {
					// first iteration, create array of double
					retval.add(new double[4]);
				}
				
				retval.get(pointNumber)[lineNumer] = Double.parseDouble(point);
				pointNumber++;
			}
			
			lineNumer++;
		}
		
		// skip duplicates if needed
		if(omitDuplicates) {

			final Set<Pair> pairs = new HashSet<>();
			final int originalPairs = retval.size();
			
			for(Iterator<double[]> iterator = retval.iterator(); iterator.hasNext(); ) {
				
				final Pair pair = new Pair(iterator.next());
				
				if(pairs.contains(pair)) {
				
					logger.debug("Ignoring pair {} from file '{}'", pair, file);
					iterator.remove();
				
				} else {
					
					pairs.add(pair);
				}
			}

			logger.info("Ignored {} out of {} pairs for file '{}'", originalPairs-pairs.size(), originalPairs, file);
		}
		
		return ArraysUtils.to2Ddouble(retval);
	}
	
}
