/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.loransac;

import static com.stefanrakonjac.mgrthesis.ransac.utils.LoggingUtils.logTimeDebug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.stefanrakonjac.mgrthesis.ransac.AbstractTest;
import com.stefanrakonjac.mgrthesis.ransac.RANSAC;
import com.stefanrakonjac.mgrthesis.ransac.impl.boofcv.BoofCVRANSACImpl;
import com.stefanrakonjac.mgrthesis.ransac.impl.boofcv.util.DisplayUtils;
import com.stefanrakonjac.mgrthesis.ransac.impl.boofcv.util.DisplayUtils.PointType;
import com.stefanrakonjac.mgrthesis.ransac.impl.loransac.LoRANSACImpl;
import com.stefanrakonjac.mgrthesis.ransac.impl.struct.ImageScore;
import com.stefanrakonjac.mgrthesis.ransac.impl.struct.PrecisionRecall;
import com.stefanrakonjac.mgrthesis.ransac.util.GroundTruth;
import com.stefanrakonjac.mgrthesis.ransac.util.GroundTruth.TYPE;
import com.stefanrakonjac.mgrthesis.ransac.utils.ArraysUtils;
import com.stefanrakonjac.mgrthesis.ransac.utils.Boundaries;
import com.stefanrakonjac.mgrthesis.ransac.utils.converters.PairsConverter;
import com.stefanrakonjac.mgrthesis.ransac.utils.dataengine.DataEngine;
import com.stefanrakonjac.mgrthesis.ransac.utils.dataengine.DataEngineFactory.InitializationException;

/**
 * @author Stefan.Rakonjac
 *
 */
public class MeanAveragePrecisionTest extends AbstractTest {
	
	private static final Logger logger = LoggerFactory.getLogger(MeanAveragePrecisionTest.class);

	private final Map<String, List<Double>> mAPByGTGroup = new HashMap<>();
	private final Map<String, List<Double>> rerankedMAPByGTGroup = new HashMap<>();
	private int totalTestsRun = 0;
	
	private int ransacRuns;
	private int ransacSuccessfulRuns;
	
	@Test(dataProvider="mapTestDP", enabled=true)
	public void mapTest(String gtName, String gtGroupName, String imageName, Map<String, double[][]> imageQueryPoints, Set<String> goodImages, Set<String> okImages, Set<String> junkImages, Map<String, Map<String, double[][]>> similarImages, Map<String, ImageScore> sortedSimilarImages) {

		logger.info(""); 
		logger.info("#{} mapTest(...) for ground-truth {} started", ++totalTestsRun, gtName); 

		final Set<String> positiveImages = new HashSet<>();
		positiveImages.addAll(okImages);
		positiveImages.addAll(goodImages);
		
		double area = 0;
		double previousPrecision = 0;
		double previousRecall = 0;
		
		final List<PrecisionRecall> allPrecisionRecallsBefore = new ArrayList<>();
		allPrecisionRecallsBefore.add(new PrecisionRecall().setPrecision(1).setRecall(0));
		
		final List<ImageScore> rankedScores = new ArrayList<>(sortedSimilarImages.values());
		for(int i = 1; i <= sortedSimilarImages.size(); i++) {

			final PrecisionRecall precisionRecall = calculatePrecisionRecall(positiveImages, junkImages, rankedScores, i, false, imageName);
			
			if(i == 1) 
				previousPrecision = precisionRecall.getPrecision();
			
			final double precision = (previousPrecision + precisionRecall.getPrecision())/2;
			final double deltaRecall = Math.abs(previousRecall-precisionRecall.getRecall());
			allPrecisionRecallsBefore.add(precisionRecall);
			
			area += precision*deltaRecall;

			previousPrecision = precisionRecall.getPrecision();
			previousRecall = precisionRecall.getRecall();
					
			logger.trace("mapTest(...)-part#1#{} finished correctly with results: precision={}, recall={}", i, precisionRecall.getPrecision(), precisionRecall.getRecall());

			if(precisionRecall.getRecall() == 1) {
				// this time print scoring
				logger.info("100% recall reached after {} from {} iterations for image: {}", i, sortedSimilarImages.size(), imageName);
				calculatePrecisionRecall(positiveImages, junkImages, rankedScores, i, true, imageName);
				break;
			}
		}
		
		if(previousRecall < 1) {
			logger.warn("not all positive images were in the sortedSimilarImages list; final recall={}", previousRecall);
		}
		
		mAPByGTGroup.get(gtGroupName).add(area);
		logger.info("mapTest(...)-part#1 finished correctly with area under the precision-recall curve: {}", area);
		
		/************************************* LoRANSAC re-ranking ***************************************/

		final double errorThershold = 5d;
		final double confidence = 0.99d;
		final int maxSamples = 100000;
		
		List<ImageScore> reRankedScores = new ArrayList<>();
				
		int exceptionCaughtTimes = 0;
		int tooFewTentativePairsImages = 0;
		for(String similarImageName : similarImages.keySet()) {
			
			final Map<String, double[][]> mappedTentativePairs = similarImages.get(similarImageName);
			
			final Set<double[]> tentativePairsSet = new HashSet<>();
			for(double[][] mappedTentativePair : mappedTentativePairs.values()) {
				for(double[] tentativePair : mappedTentativePair) {
					tentativePairsSet.add(tentativePair);
				}
			}
			
			final double[][] tentativePairs = ArraysUtils.to2Ddouble(tentativePairsSet);

			double score = 0;
			
			if(tentativePairs.length >= 10) {
				
				try {
					final long ransacStartTime = System.currentTimeMillis();
					
					final RANSAC ransac = new LoRANSACImpl()
					// TODO re-comment
//					final RANSAC ransac = new BoofCVRANSACImpl()
					// TODO comment following one line
//											  .doOptimizations(false)
											  .setTentativePairs(tentativePairs)
											  .setThreshold(errorThershold)
											  .setBestSolutionProbability(confidence)
											  .setMaxIterations(maxSamples);
					
					ransac.run();
					
					logTimeDebug(ransacStartTime, "LoRANSAC RUN");
					
					score = calculateTFIDF(imageName, ransac.getInliers(), mappedTentativePairs);
					ransacSuccessfulRuns++;
					
					// TODO comment
					DisplayUtils.showPairs(getImagePath(imageName), getImagePath(similarImageName), PairsConverter.toAssociatedPairList(tentativePairs));
					DisplayUtils.showPairs(getImagePath(imageName), getImagePath(similarImageName), PairsConverter.toAssociatedPairList(ransac.getInliers()));
					
				} catch (Exception ex) {
					exceptionCaughtTimes++;
					logger.debug("{}", ex.getMessage(), ex);
				}
				
				ransacRuns++;
				
			} else {
				
				// this way we keep the original ordering
				score = - ++tooFewTentativePairsImages;
			}
			
			reRankedScores.add(new ImageScore(similarImageName).setScore(score));
		}
		
		Collections.sort(reRankedScores);
		Collections.reverse(reRankedScores);
		
		List<PrecisionRecall> allPrecisionRecallsAfter = new ArrayList<>();
		allPrecisionRecallsAfter.add(new PrecisionRecall().setPrecision(1).setRecall(0));
		
		double rerankedArea = 0;
		previousPrecision = 0;
		previousRecall = 0;
		for(int i = 1; i < sortedSimilarImages.size(); i++) {

			final PrecisionRecall precisionRecall = calculatePrecisionRecall(positiveImages, junkImages, reRankedScores, i, false, imageName);
			
			if(i == 1) 
				previousPrecision = precisionRecall.getPrecision();
			
			final double precision = (previousPrecision + precisionRecall.getPrecision())/2;
			final double deltaRecall = Math.abs(previousRecall-precisionRecall.getRecall());
			allPrecisionRecallsAfter.add(precisionRecall);
			
			rerankedArea += precision*deltaRecall;

			previousPrecision = precisionRecall.getPrecision();
			previousRecall = precisionRecall.getRecall();
					
			logger.trace("mapTest(...)-part#2#{} finished correctly with results: precision={}, recall={}", i, precisionRecall.getPrecision(), precisionRecall.getRecall());
			
			if(precisionRecall.getRecall() == 1) {
				// this time print scoring
				logger.info("100% recall reached after {} from {} iterations for image: {}", i, sortedSimilarImages.size(), imageName);
				calculatePrecisionRecall(positiveImages, junkImages, reRankedScores, i, true, imageName);
				break;
			}
		}
		
		// TODO comment
//		DisplayUtils.showPrecisionRecallCurves(imageName, allPrecisionRecallsBefore, allPrecisionRecallsAfter);

		rerankedMAPByGTGroup.get(gtGroupName).add(rerankedArea);
		logger.info("mapTest(...)-part#2 finished correctly with area under the precision-recall curve: {}", rerankedArea);
		
		if(area > rerankedArea) {
			logger.warn("re-ranked mPA for ground truth decreased of {}% (original: {}, re-ranked: {})", (area-rerankedArea)*100, area, rerankedArea);
		} else {
			logger.info("re-ranked mPA for ground truth increased of {}% (original: {}, re-ranked: {})", (rerankedArea-area)*100, area, rerankedArea);
		}
		
		logger.info("{}# mapTest(...) finished for ground-truth {}, exception was caught {} times during RANSAC evaluation", totalTestsRun, gtName, exceptionCaughtTimes);
	}
	

	
	@Test(dataProvider="mapTestDP", enabled=true, dependsOnMethods="mapTest")
	private double calculateTFIDF(String imageName, double[][] pairs, Map<String, double[][]> mappedPairs) throws InitializationException {

		if(imageName == null) 
			throw new IllegalArgumentException("imageName");
		if(pairs == null) 
			throw new IllegalArgumentException("pairs");
		if(mappedPairs == null) 
			throw new IllegalArgumentException("mappedPairs");
		
		double retval = 0;

		if(pairs.length >= 4) { 
			
			/* We consider spatial verification to be “successful” if we find a transformation 
			 * with at least 4 inlier correspondences.
			 */
		
			/* We re-rank the images by scoring them equal to the sum of the idf values for the inlier words and
			 * place spatially verified images above unverified ones in the ranking.
			 */
			for(double[] inlier : pairs) {
				
				middle_loop:
				for(Entry<String, double[][]> entry : mappedPairs.entrySet()) {
					for(double[] pair : entry.getValue()) {
						// we need to check all 2x2 dimensions if they match
						if(inlier[0] == pair[0] && inlier[1] == pair[1] && inlier[2] == pair[2] && inlier[3] == pair[3]) {
							// we have the matching points pair, use its visual word to get tf-idf score
							retval += getOxfordHessafDE(false).getTFIDF(entry.getKey(), imageName);
							break middle_loop;
						}
					}
				}
			}
		}
		
		return retval;
	}

	private PrecisionRecall calculatePrecisionRecall(Set<String> positiveImages, Set<String> neutralImages, List<ImageScore> imageScores, int queryLength, final boolean printScoring, String baseImageName) {

		final PrecisionRecall retval = new PrecisionRecall();
		
		/* In computing the average precision, we use the Good and Ok images as positive examples of the landmark in question,
		 * Absent images as negative examples and Junk images as null examples. These null examples are treated as though 
		 * they are not present in the database – our score is unaffected whether they are returned or not.
		 */
		int positiveImagesFound = 0;
		int neutralImagesFound = 0;
		
		String[] imageNames = null;
		PointType[] pointTypes = null;
		double[] matchesCount = null;
		int displayImageCount = 0;
		if(printScoring) {
			displayImageCount = Math.min(100, queryLength-1);
			imageNames = new String[displayImageCount];
			pointTypes = new PointType[displayImageCount];
			matchesCount = new double[displayImageCount];
		}
		
		int i = 0;
		for(ImageScore imageScore : imageScores) {
			
			final String imageName = imageScore.getImageName();
			
			if(printScoring && i != 0 && i <= displayImageCount) {
				imageNames[i-1] = imageName;
				matchesCount[i-1] = imageScore.getScore();
			}
			
			if(positiveImages.contains(imageName)) {
				positiveImagesFound++;
				
				if(printScoring) {
					logger.debug("#{} image identified as positive to GT: {}", i+1, imageScore);
					if(i != 0 && i <= displayImageCount) pointTypes[i-1] = PointType.POSITIVE;
				}
			
			} else if(neutralImages.contains(imageName)) {
				neutralImagesFound++;
				
				if(printScoring) {
					logger.debug("#{} image identified as neutral to GT: {}", i+1, imageScore);
					if(i != 0 && i <= displayImageCount) pointTypes[i-1] = PointType.NEUTRAL;
				}
			
			} else {
				
				if(printScoring) {
					logger.debug("#{} image identified as negative to GT: {}", i+1, imageScore);
					if(i != 0 && i <= displayImageCount) pointTypes[i-1] = PointType.NEGATIVE;
				}
			}
			
			// limit to the relevant part of the list
			if(++i == queryLength)
				break;
		}
		
		if(printScoring) {
//			DisplayUtils.showScatterPlot(baseImageName, imageNames, pointTypes, matchesCount);
			return null;
		}
		
		/* Precision is defined as the ratio of retrieved positive images to the total number retrieved. 
		 * Recall is defined as the ratio of the number of retrieved positive images to the total number of positive images in the corpus
		 */
		final float precision = ((float) positiveImagesFound)/((i - neutralImagesFound > 0 ) ? (i - neutralImagesFound) : 1);
		final float recall = ((float) positiveImagesFound)/positiveImages.size();
		
		return retval.setPrecision(precision).setRecall(recall);
	}

	@AfterClass
	public void calculateMAPs() {

		logger.info("\n\n------------------------------------------------------------\n");
		
		double finalMAP = 0;
		for(String gtGroupName : mAPByGTGroup.keySet()) {
			final List<Double> mAPs = mAPByGTGroup.get(gtGroupName);
			
			double gtMAP = 0;
			for(double mAP : mAPs) {
				gtMAP += mAP;
			}
			gtMAP /= mAPs.size();
			
			logger.info("GT group {} mAP: {}", gtGroupName, gtMAP);
			finalMAP += gtMAP;
		}
		finalMAP /= mAPByGTGroup.size();

		logger.info("Final mAP: {}", finalMAP);

		logger.info("\n\n------------------------------------------------------------\n");
		
		double rerankedFinalMAP = 0;
		for(String gtGroupName : rerankedMAPByGTGroup.keySet()) {
			final List<Double> mAPs = rerankedMAPByGTGroup.get(gtGroupName);
			
			double gtMAP = 0;
			for(double mAP : mAPs) {
				gtMAP += mAP;
			}
			gtMAP /= mAPs.size();
			
			logger.info("Re-ranked GT group {} mAP: {}", gtGroupName, gtMAP);
			rerankedFinalMAP += gtMAP;
		}
		rerankedFinalMAP /= mAPByGTGroup.size();
		
		logger.info("Final re-ranked maP: {}", rerankedFinalMAP);

		logger.info("\n\n------------------------------------------------------------\n");
		
		logger.info("RANSAC successful runs: {}%", ((double) 100*ransacSuccessfulRuns)/ransacRuns);
		
		logger.warn("happy?");
	}
	
	
	/* ------------------ data providers -------------------- */

	@DataProvider
	public Object[][] mapTestDP() throws IOException, InitializationException {
		
		logger.debug("mapTestDP() invoked");
		
		/*
		 * 1) gtName
		 * 2) gtGroupName
		 * 3) imageName
		 * 4) imageQueryPoints
		 * 5) set of good images
		 * 6) set of ok images
		 * 7) set of junk images
		 * 8) first N most similar images mapping point pars
		 * 9) sorting of the first N most similar images
		 */
		
		final GroundTruth groundTruth = getGroundTruth();
		final DataEngine oxfordHessafDE = getOxfordHessafDE(true);
		
		final Set<String> gtNames = groundTruth.getAllGroundTruthNames();
		
		// finally define retval
//		final Object[][] retval = new Object[gtNames.size()][9];
		// TODO re-comment
		final Object[][] retval = new Object[5][9];

		final long startTime = System.currentTimeMillis();
		
		int i = 0;
		for(String gtName : gtNames) {
			
			logger.info("Preparing test expectations for #{} gtName: {}", i+1, gtName);
			
			final String gtGroupName = gtName.substring(0, gtName.lastIndexOf('_'));
			
			// TODO comment
			if(!"all_souls".equals(gtGroupName)) continue;
			
			if(!mAPByGTGroup.containsKey(gtGroupName)) {
				
				// put two new array lists for this ground-truth group
				mAPByGTGroup.put(gtGroupName, new ArrayList<Double>());
				rerankedMAPByGTGroup.put(gtGroupName, new ArrayList<Double>());
			}
			
			final String imageName = groundTruth.getImageName(gtName);
			final Boundaries boundaries = groundTruth.getBoundaries(gtName);
			final Map<String, double[][]> imageQueryPoints = boundaries.limitAll(oxfordHessafDE.getImagePointsByWords(imageName));

			final Set<String> goodImages = groundTruth.getAssociatedImageNames(gtName, TYPE.GOOD);
			final Set<String> okImages = groundTruth.getAssociatedImageNames(gtName, TYPE.OK);
			final Set<String> junkImages = groundTruth.getAssociatedImageNames(gtName, TYPE.JUNK);
			
			// mapping the actual scoring (calculated by applying tf-idf weight function)
			final Map<String, ImageScore> scores = new LinkedHashMap<>();
			
			// this is an actual linked hash map preserving order of the ranked images
			final Map<String, Map<String, double[][]>> tentativePairsByImageNames = oxfordHessafDE.getMostSimilarN(imageName, 5064, boundaries, scores);
			
			// we want ALL images so that 100% recall can be achieved
			// TODO: groundTruth returns only the set of image names ever mentioned in ground truth files
			for(String singleImageName : groundTruth.getAllImageNames()) {
				if(!tentativePairsByImageNames.containsKey(singleImageName)) tentativePairsByImageNames.put(singleImageName, new HashMap<String, double[][]>());
				if(!scores.containsKey(singleImageName)) scores.put(singleImageName, new ImageScore(singleImageName).setScore(0));
			}

			retval[i][0] = gtName;
			retval[i][1] = gtGroupName;
			retval[i][2] = imageName;
			retval[i][3] = imageQueryPoints;
			retval[i][4] = goodImages;
			retval[i][5] = okImages;
			retval[i][6] = junkImages;
			retval[i][7] = tentativePairsByImageNames;
			retval[i][8] = scores;
			
			i++;
		}
		
		logTimeDebug(startTime, "mapTestDP() PREPARE GROUND-TRUTH");
		
		return retval;
	}
}
