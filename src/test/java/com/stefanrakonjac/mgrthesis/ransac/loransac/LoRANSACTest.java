package com.stefanrakonjac.mgrthesis.ransac.loransac;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.stefanrakonjac.mgrthesis.ransac.AbstractTest;
import com.stefanrakonjac.mgrthesis.ransac.RANSAC;
import com.stefanrakonjac.mgrthesis.ransac.impl.boofcv.util.DisplayUtils;
import com.stefanrakonjac.mgrthesis.ransac.impl.loransac.LoRANSACImpl;
import com.stefanrakonjac.mgrthesis.ransac.util.GroundTruth;
import com.stefanrakonjac.mgrthesis.ransac.util.GroundTruth.GroundTruthBuilder;
import com.stefanrakonjac.mgrthesis.ransac.util.GroundTruth.TYPE;
import com.stefanrakonjac.mgrthesis.ransac.utils.Boundaries;
import com.stefanrakonjac.mgrthesis.ransac.utils.converters.PairsConverter;
import com.stefanrakonjac.mgrthesis.ransac.utils.dataengine.DataEngine;
import com.stefanrakonjac.mgrthesis.ransac.utils.dataengine.DataEngineFactory;
import com.stefanrakonjac.mgrthesis.ransac.utils.dataengine.DataEngineFactory.InitializationException;

import static com.stefanrakonjac.mgrthesis.ransac.FileConstants.*;
import static com.stefanrakonjac.mgrthesis.ransac.utils.LoggingUtils.*;

/**
 * @author Stefan.Rakonjac
 *
 */
public class LoRANSACTest extends AbstractTest {
	
	@Test(dataProvider="allSoulsTestDP", enabled=false)
	public void allSoulsTest(double[][] tentativePairs, double errorThershold, double confidence, int maxSamples, String leftImageURL, String rightImageURL) {
		
		final long ransacStartTime = System.currentTimeMillis();
		
		final RANSAC ransac = new LoRANSACImpl().setTentativePairs(tentativePairs)
												.setThreshold(errorThershold)
												.setBestSolutionProbability(confidence)
												.setMaxIterations(maxSamples);
		
		ransac.run();
		
		logTime(ransacStartTime, System.currentTimeMillis(), "LoRANSAC RUN");

		final double[][] inliers = ransac.getInliers();

		logger.info("Inliers found: {} out of {}; Inliers ration: {}", inliers.length, tentativePairs.length, ((double) inliers.length)/tentativePairs.length);
		
		DisplayUtils.showPairs(leftImageURL, rightImageURL, PairsConverter.toAssociatedPairList(tentativePairs));
		DisplayUtils.showPairs(leftImageURL, rightImageURL, PairsConverter.toAssociatedPairList(inliers));
		
		logger.info("allSoulsTest(double[{}][4], {}, {}, {}, {}, {}) finished correctly", tentativePairs.length, errorThershold, confidence, maxSamples, leftImageURL, rightImageURL);
	}
	
	@Test(dataProvider="allSoulsHessafDP", enabled=false)
	public void allSoulsHessaf(double[][] tentativePairs, double errorThershold, double confidence, int maxSamples, String leftImageURL, String rightImageURL) {

		final long ransacStartTime = System.currentTimeMillis();
		
		final RANSAC ransac = new LoRANSACImpl().setTentativePairs(tentativePairs)
												.setThreshold(errorThershold)
												.setBestSolutionProbability(confidence)
												.setMaxIterations(maxSamples);
		
		ransac.run();
		
		logTime(ransacStartTime, System.currentTimeMillis(), "LoRANSAC RUN");
		
		final double[][] inliers = ransac.getInliers();

		logger.info("Inliers found: {} out of {}; Inliers ration: {}", inliers.length, tentativePairs.length, ((double) inliers.length)/tentativePairs.length);
		
		DisplayUtils.showPairs(leftImageURL, rightImageURL, PairsConverter.toAssociatedPairList(tentativePairs));
		DisplayUtils.showPairs(leftImageURL, rightImageURL, PairsConverter.toAssociatedPairList(inliers));
		
		logger.info("allSoulsHessaf(double[{}][4], {}, {}, {}, {}, {}) finished correctly", tentativePairs.length, errorThershold, confidence, maxSamples, leftImageURL, rightImageURL);
	}
	
	@Test(dataProvider="allSoulsHessafMeanAveragePrecisionDP", enabled=false)
	public void allSoulsHessafMeanAveragePrecision(Map<String, double[][]> goodTentativePairs, Map<String, double[][]> okTentativePairs, Map<String, double[][]> junkTentativePairs, double errorThershold, double confidence, int maxSamples, String leftImageURL, Map<String, String> rightImageURLs) {
		logger.debug("allSoulsHessafMeanAveragePrecision(...) started");
		
		for(String goodImage : okTentativePairs.keySet()) {
			
			final double[][] tentativePairs = okTentativePairs.get(goodImage);
			
			if(tentativePairs.length < 10) {
				logger.warn("Skipping test as too few tentative pairs: {}", goodImage);
				continue;
			}
			
			final String rightImageURL = rightImageURLs.get(goodImage);
			
			final long ransacStartTime = System.currentTimeMillis();
			
			final RANSAC ransac = new LoRANSACImpl().setTentativePairs(tentativePairs)
													.setThreshold(errorThershold)
													.setBestSolutionProbability(confidence)
													.setMaxIterations(maxSamples);
			
			ransac.run();
			
			logTime(ransacStartTime, System.currentTimeMillis(), "LoRANSAC RUN");
			
			final double[][] inliers = ransac.getInliers();

			logger.info("Inliers found: {} out of {}; Inliers ration: {}", inliers.length, tentativePairs.length, ((double) inliers.length)/tentativePairs.length);
			
			DisplayUtils.showPairs(leftImageURL, rightImageURL, PairsConverter.toAssociatedPairList(tentativePairs));
			DisplayUtils.showPairs(leftImageURL, rightImageURL, PairsConverter.toAssociatedPairList(inliers));
		}
	}
	
	/** ------------------ data providers -------------------- */
	
	@DataProvider
	public Object[][] allSoulsTestDP() throws IOException {
		
		/*
		 * one set of data:
		 * 
		 * 	double[][] tentativePairs	-	2D tentative pairs
		 * 	double errorThershold		-	threshold
		 * 	double confidence			-	confidence of best solution
		 * 	int maxSamples				-	maximal number of samples for least squares
		 *  String leftImageURL
		 *  String rightImageURL
		 * 	
		 */
		
		final Object[][] retval = new Object[1][6];
		
		final String imageA = "all_souls_000055.jpg"; // 57
		final String imageB = "all_souls_000068.jpg"; // 59
		
		prepareDataAndVocabulary(new HashSet<>(Arrays.asList(imageA, imageB)));
		
		retval[0][0] = computePairs(data.get(imageA), data.get(imageB), DATA_QUANTIZED ? 2 : 100); // max 1 feature per word, max distance 100
		retval[0][1] = DATA_QUANTIZED ? 1 : USE_VOCABULARY ? 1 : 2;
		retval[0][2] = 0.99;
		retval[0][3] = (int) 1e5;
		retval[0][4] = IMAGES_ROOT + imageA;
		retval[0][5] = IMAGES_ROOT + imageB;
		
		return retval;
	}
	
	@DataProvider
	public Object[][] allSoulsHessafDP() throws IOException, InitializationException {
		
		final Object[][] retval = new Object[1][6];

		final String imageAName = "magdalen_000694";
		final String imageBName = "magdalen_000709";
		
		retval[0][0] = getOxfordHessafDE(false).computeMatches(imageAName, imageBName);
		retval[0][1] = 1;
		retval[0][2] = 0.99;
		retval[0][3] = (int) 1e6;
		retval[0][4] = getImagePaths(Collections.singleton(imageAName)).get(imageAName);
		retval[0][5] = getImagePaths(Collections.singleton(imageBName)).get(imageBName);
		
		return retval;
	}

	@DataProvider
	public Object[][] allSoulsHessafMeanAveragePrecisionDP() throws IOException, NumberFormatException, InitializationException {
		logger.debug("allSoulsHessafMeanAveragePrecisionDP()");
		
		final File groundTruthDir = new File(GROUND_TRUTH_ROOT);
		
		final Object[][] retval = new Object[groundTruthDir.list().length/4][8];
		
		final GroundTruth groundTruth = getGroundTruth();
		
		int i = 0;
		for(String gtName : groundTruth.getAllGroundTruthNames()) {
			
			final String imageName = groundTruth.getImageName(gtName);
			final Boundaries boundries = groundTruth.getBoundaries(gtName);
			
			retval[i][0] = boundries.limitAll(getOneToNHessafMatches(imageName, groundTruth.getAssociatedImageNames(gtName, TYPE.GOOD), null));
			retval[i][1] = boundries.limitAll(getOneToNHessafMatches(imageName, groundTruth.getAssociatedImageNames(gtName, TYPE.OK), null));
			retval[i][2] = boundries.limitAll(getOneToNHessafMatches(imageName, groundTruth.getAssociatedImageNames(gtName, TYPE.JUNK), null));
			
			retval[i][3] = 1;
			retval[i][4] = 0.99;
			retval[i][5] = (int) 1e6;
			retval[i][6] = getImagePaths(Collections.singleton(imageName)).get(imageName);
			retval[i][7] = getImagePaths(groundTruth.getAllImageNames());
			
			i++;
		}
		
		return retval;
	}
}
