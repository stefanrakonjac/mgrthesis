/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import messif.objects.impl.ObjectFeature;
import messif.objects.impl.ObjectFeatureQuantized;
import messif.objects.impl.ObjectFeatureSet;
import messif.objects.impl.ObjectFeatureSetSumOfSimilar;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.quantization.AbstractVisualVocabulary;
import messif.quantization.kmeans.KMeansVisualVocabulary;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;

import com.stefanrakonjac.mgrthesis.ransac.util.GroundTruth;
import com.stefanrakonjac.mgrthesis.ransac.util.GroundTruth.GroundTruthBuilder;
import com.stefanrakonjac.mgrthesis.ransac.utils.PointsMatcherUtils;
import com.stefanrakonjac.mgrthesis.ransac.utils.dataengine.DataEngine;
import com.stefanrakonjac.mgrthesis.ransac.utils.dataengine.DataEngineFactory;
import com.stefanrakonjac.mgrthesis.ransac.utils.dataengine.DataEngineFactory.InitializationException;

import static com.stefanrakonjac.mgrthesis.ransac.FileConstants.*;
import static com.stefanrakonjac.mgrthesis.ransac.utils.LoggingUtils.*;

/**
 * @author Stefan.Rakonjac
 *
 */
public abstract class AbstractTest {
	
	protected final static Logger logger = LoggerFactory.getLogger(AbstractTest.class);
	
	// to be set for all the tests instances
	protected Map<String, ObjectFeatureSet> data;
	protected AbstractVisualVocabulary vocabulary;
	
	private static DataEngine oxfordHessafDE;
	private static GroundTruth groundTruth;
	
	@BeforeSuite
	public void beforeSuite() throws ClassNotFoundException {
		Class.forName(ObjectFeatureQuantized.class.getName());
	}
	
	/**
	 * @param initialize
	 * 		Whether or not data-engine should get initialized (time-consuming operation)
	 * @return
	 * 		Returns Oxford hessaf data-engine
	 * @throws InitializationException
	 * 		If method parameter {@code initialized} was set to true and there was an exception while initializing the data-engine
	 */
	protected static DataEngine getOxfordHessafDE(boolean initialize) throws InitializationException {
		
		if(oxfordHessafDE == null)
			oxfordHessafDE = DataEngineFactory.getOxfordHessafDataEngine();
		
		if(initialize && !oxfordHessafDE.isInit()) {
			logger.debug("calling init() on oxfordHessafDP");
			oxfordHessafDE.init();
		}
		
		return oxfordHessafDE;
	}

	protected static GroundTruth getGroundTruth() throws IOException {
		return groundTruth == null ? groundTruth = new GroundTruthBuilder(GROUND_TRUTH_ROOT).build() : groundTruth;
	}

	/**
	 * 
	 * @param imageNames
	 * @return
	 */
	protected static final Map<String, String> getImagePaths(Set<String> imageNames) {
		
		final Map<String, String> retval = new HashMap<>(imageNames.size());
		
		for(String imageName : imageNames) {
			retval.put(imageName, getImagePath(imageName));
		}
		
		return retval;
	}

	protected static final String getImagePath(String imageName) {
		return IMAGES_ROOT + imageName + IMAGES_SUFFIX;
	}

	/**
	 * This method returns object consuming more than 5.5GB of space
	 * 
	 * @return
	 * @throws NumberFormatException
	 * @throws IOException
	 * @throws InitializationException
	 */
	@Deprecated
	protected Map<String, Map<String, double[][]>> parseAllHessafData() throws NumberFormatException, IOException, InitializationException {
		
		final Map<String, Map<String, double[][]>> retval = new HashMap<>();
		
		final File hessafRoot = new File(DataEngineFactory.HESSAF_DATA_ROOT);
		final long startTime = System.currentTimeMillis();
		
		int i = 0;
		for(File hessafFile : hessafRoot.listFiles()) {
			
			final String imageName = FilenameUtils.getBaseName(hessafFile.getName()).substring(5);
			
			logger.info("Parsing HESSAF data ({}) for image name: {}", ++i, imageName);
			final Map<String, double[][]> imagePointsByWords = getOxfordHessafDE(true).getImagePointsByWords(imageName);
			
			retval.put(imageName, imagePointsByWords);
		}
		
		logTime(startTime, System.currentTimeMillis(), "Hessaf descriptors READ");
		
		return retval;
	}
	
	protected Map<String, double[][]> getOneToNHessafMatches(String baseImageName, Set<String> associatedImageNames, Map<String, Map<String, double[][]>> hessafData) throws NumberFormatException, IOException, InitializationException {
		
		final Map<String, double[][]> retval = new HashMap<>(associatedImageNames.size());
		
		for(String associatedImageName : associatedImageNames) {
			
			final double[][] hessafMatches = computeHessafMatches(baseImageName, associatedImageName, hessafData);
			retval.put(associatedImageName, hessafMatches);
		}
		
		return retval;
	}

	protected double[][] computeHessafMatches(String imageAName, String imageBName, Map<String, Map<String, double[][]>> hessafData) throws NumberFormatException, IOException, InitializationException {

		final Map<String, double[][]> imageAPointsByWords = hessafData != null ? hessafData.get(imageAName) : getOxfordHessafDE(true).getImagePointsByWords(imageAName);
		final Map<String, double[][]> imageBPointsByWords = hessafData != null ? hessafData.get(imageBName) : getOxfordHessafDE(true).getImagePointsByWords(imageBName);
		
		return PointsMatcherUtils.computeMatchesFromWords(imageAPointsByWords, imageBPointsByWords);
	}
	
	protected void prepareDataAndVocabulary(Set<String> imageNames) throws IOException {

		this.data = readData(DATA_FILE, imageNames);
		this.vocabulary = DATA_QUANTIZED || !USE_VOCABULARY ? null : readVocabulary(VOCABULARY_FILE); // read vocabulary for non-quantized files
	}
	
	protected Map<String, ObjectFeatureSet> readData(String filePath, Set<String> imageNames) throws IOException {
		logger.info("readData({}, {})", filePath, imageNames);
		
		// create object iterator
    	final long icStartTime = System.currentTimeMillis();
    	// reading: DATA_FILE_QUANTIZED -> all quantized | DATA_FILE_FIRST_100 -> first 100 non-quantized
    	final StreamGenericAbstractObjectIterator<ObjectFeatureSet> iterator = new StreamGenericAbstractObjectIterator<ObjectFeatureSet>(ObjectFeatureSetSumOfSimilar.class, filePath);
    	logTime(icStartTime, System.currentTimeMillis(), "Object Feature Set Iterator CREATE");
    	
    	Map<String, ObjectFeatureSet> data = new HashMap<>();
    	
    	// iterate all images
    	final long irStartTime = System.currentTimeMillis();
    	
    	while(iterator.hasNext()) {
    		final ObjectFeatureSet ofs = iterator.next();
    		final String imageName = ofs.getLocatorURI();
    		if(imageNames.contains(imageName)) {
    			data.put(ofs.getLocatorURI(), ofs);
    		}
    	}
    	
    	logTime(irStartTime, System.currentTimeMillis(), "Object Feature Set Iterator READ");
    	
    	return data;
	}
	
	protected AbstractVisualVocabulary readVocabulary(String vocabularyFile) throws IOException {
		logger.debug("readVocabulary({})", vocabularyFile);

    	final long vStartTime = System.currentTimeMillis();
    	AbstractVisualVocabulary vocabulary = KMeansVisualVocabulary.readVocabulary(vocabularyFile, KMeansVisualVocabulary.class); // null; // 
    	logTime(vStartTime, System.currentTimeMillis(), "Vocabulary READ");
    	
    	return vocabulary;
	}

	protected double[][] computePairs(ObjectFeatureSet ofsA, ObjectFeatureSet ofsB, int threshold) {
		
		final double distanceThresholdPercent = 5;

		filterCloseFeatures(ofsA, distanceThresholdPercent);
		filterCloseFeatures(ofsB, distanceThresholdPercent);
		
		double[][] retval = DATA_QUANTIZED ? PointsMatcherUtils.computeMatchesFromQuantizedOFS(ofsA, ofsB, threshold) : 
								USE_VOCABULARY ? PointsMatcherUtils.computeMatchesFromOFSUsingVocabulary(ofsA, ofsB, vocabulary, threshold) : 
												  PointsMatcherUtils.computeMatchesFromOFSByMetricDistance(ofsA, ofsB, threshold);
						
		return retval;
	}

	private void filterCloseFeatures(ObjectFeatureSet ofs, double distanceThresholdPercent) {
		
		final double distanceThresholdPercentAddopted = distanceThresholdPercent / 100;
		final int originalPointsCount = ofs.getObjectCount();
		int pointsRemoved = 0;

		outerLoop:
		for(Iterator<ObjectFeature> outerIterator = ofs.iterator(); outerIterator.hasNext(); ) {
			
			ObjectFeature outerObjectFeature = outerIterator.next();
			
			innerLoop:
			for(Iterator<ObjectFeature> innerIterator = ofs.iterator(); innerIterator.hasNext(); ) {
				
				ObjectFeature innerObjectFeature = innerIterator.next();
				
				if(outerObjectFeature.equals(innerObjectFeature)) continue outerLoop;
				
				double distanceX = Math.abs(outerObjectFeature.getX() - innerObjectFeature.getX());
				if(distanceX > distanceThresholdPercentAddopted) continue outerLoop;

				double distanceY = Math.abs(outerObjectFeature.getY() - innerObjectFeature.getY());
				if(distanceY > distanceThresholdPercentAddopted) continue outerLoop;
				
				break innerLoop;
			}
			
			logger.info("Removing point [{}, {}] as other points found within range {}", outerObjectFeature.getX(), outerObjectFeature.getY(), distanceThresholdPercent);
			outerIterator.remove();
			
			pointsRemoved++;
		}
		
		logger.info("Removed {} and left {} points (original: {})", pointsRemoved, ofs.getObjectCount(), originalPointsCount);
	}
}
