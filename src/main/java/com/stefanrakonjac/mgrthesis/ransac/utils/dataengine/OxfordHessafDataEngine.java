package com.stefanrakonjac.mgrthesis.ransac.utils.dataengine;

import static com.stefanrakonjac.mgrthesis.ransac.utils.LoggingUtils.logTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stefanrakonjac.mgrthesis.ransac.impl.struct.ImageScore;
import com.stefanrakonjac.mgrthesis.ransac.utils.ArraysUtils;
import com.stefanrakonjac.mgrthesis.ransac.utils.Boundaries;
import com.stefanrakonjac.mgrthesis.ransac.utils.PointsMatcherUtils;
import com.stefanrakonjac.mgrthesis.ransac.utils.dataengine.DataEngineFactory.InitializationException;

/**
 * @author Stefan.Rakonjac
 *
 */
public final class OxfordHessafDataEngine implements DataEngine {
	
	private static final Logger logger = LoggerFactory.getLogger(OxfordHessafDataEngine.class);
	
	private static final File DATA_ROOT_FILE = new File(DataEngineFactory.HESSAF_DATA_ROOT);

	/** 32 */
	private static final int INITIAL_ARRAY_CAPACITY = 32;
	
	private boolean init = false;
	private final Map<String, short[]> imageIdByWordMap = new HashMap<>();
	private final Map<String, Map<String, Short>> wordCountByImageNameMapByWordMap = new HashMap<>();
	private final Map<String, Short> maxWordCountByImageName = new HashMap<>();
	private final Map<Short, String> imageNameByImageIdMap = new HashMap<>();
	
	OxfordHessafDataEngine() {
		// no-op
	}

	/** {@inheritDoc}
	 */
	@Override
	public void init() throws InitializationException {

		if(isInit()) {
			logger.warn("Calling init() not for the first time. Request ignored");
			return;
		}
		
		try {
			// single action to initialize this data engine: parse data
			parseData();
			
		} catch (Exception ex) {
			
			// we should not try to recover from this
			throw new InitializationException("Could not parse Oxford Hessaf data", ex);
			
		} finally {
			
			init = true;
			logger.debug("{} initialized", this.getClass().getName());
		}
	}

	/** {@inheritDoc}
	 */
	@Override
	public boolean isInit() {
		return init;
	}

	/** {@inheritDoc}
	 */
	@Override
	public double[][] computeMatches(String imageA, String imageB) {
		
		try {
			Map<String, double[][]> imageAPointsByWords = getImagePointsByWords(imageA);
			Map<String, double[][]> imageBPointsByWords = getImagePointsByWords(imageB);
			return PointsMatcherUtils.computeMatchesFromWords(imageAPointsByWords, imageBPointsByWords);
			
		} catch (Exception ex) {
			logger.error("Could not compute matches", ex);
		}
		
		return new double[0][0];
	}

	/** {@inheritDoc}
	 */
	@Override
	public Map<String, Map<String, double[][]>> getMostSimilarN(final String imageName, final int count, final Boundaries boundaries, Map<String, ImageScore> scores) {
		
		final Map<String, Map<String, double[][]>> retval = new LinkedHashMap<>();

		try {
		
			final Map<String, double[][]> imagePointsByWords = boundaries != null ? boundaries.limitAll(getImagePointsByWords(imageName)) 
																				  : getImagePointsByWords(imageName);
			
			Map<Short, ImageScore> scoresByImageID = new HashMap<>();
			for(String word : imagePointsByWords.keySet()) {
				
				final double wordScore = getTFIDF(word, imageName);
				
				for(short imageID : imageIdByWordMap.get(word)) {
					
					// -1 means no more imageIDs in array
					if(imageID == -1) break;
					
					if(!scoresByImageID.containsKey(imageID)) {
						scoresByImageID.put(imageID, new ImageScore(imageID));
					}
					scoresByImageID.get(imageID).increaseScore(wordScore);
				}
			}
			
			List<ImageScore> sortedList = new ArrayList<>(scoresByImageID.values());
			
			// ImageScore is sorted by achieved score
			Collections.sort(sortedList);
			
			// descending ordering is required
			Collections.reverse(sortedList);
			
			int i = 0;
			for(ImageScore score : sortedList) {
				i++;
				
				final String imageBName = imageNameByImageIdMap.get(score.getImageID());
				final Map<String, double[][]> imageBPointsByWords = getImagePointsByWords(imageBName);
				final Map<String, double[][]> matches = PointsMatcherUtils.computeMappedMatchesFromWords(imagePointsByWords, imageBPointsByWords);
				retval.put(imageBName, matches);
				
				if(scores != null) 
					scores.put(imageBName, score.setImageName(imageBName));
				
				if(i == count) break;
			}
			
		
		} catch (Exception ex) {
			logger.error("Could not compute matches", ex);
		}
		
		return retval;
	}

	/** {@inheritDoc}
	 */
	@Override
	public Map<String, double[][]> getImagePointsByWords(String imageName) throws NumberFormatException, IOException {
		
		final Map<String, Set<double[]>> retvalSet = new HashMap<>();
		final Map<String, double[][]> retval = new HashMap<>();
		
		for(Object line : FileUtils.readLines(getImageFile(imageName))) {
			
			final String lineStr = (String) line;
			final String[] exploded = lineStr.split("\\s");
			
			if(exploded.length < 3) continue;
			
			final String word = exploded[0];
			final double x = Double.parseDouble(exploded[1]);
			final double y = Double.parseDouble(exploded[2]);
			
			if(!retvalSet.containsKey(word)) {
				retvalSet.put(word, new HashSet<double[]>());
			}
			
			retvalSet.get(word).add(new double[] { x, y });
		}
		
		// transform from set to array
		for(String word : retvalSet.keySet()) {
			retval.put(word, ArraysUtils.to2Ddouble(retvalSet.get(word)));
		}
		
		return retval;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @see http://en.wikipedia.org/wiki/Tf-idf 
	 */
	@Override
	public double getTFIDF(final String word, final String imageName) {
		
		/* Augmented term frequency, to prevent a bias towards longer documents, e.g. raw frequency divided by the maximum raw frequency 
		 * of any term in the document:
		 */
		final double termFrequency = 0.5 + (0.5*getRawTermFrequencyInDoc(word, imageName))/getMaxRawTermFrequencyInDoc(imageName);
		
		/* The inverse document frequency is a measure of how much information the word provides, that is, whether the term is common or rare 
		 * across all documents. It is the logarithmically scaled fraction of the documents that contain the word, obtained by dividing 
		 * the total number of documents by the number of documents containing the term, and then taking the logarithm of that quotient.
		 */
		
		final Set<Short> documentIDs = new HashSet<>();
		for(Short imageID : imageIdByWordMap.get(word)) {
			if(!documentIDs.contains(imageID)) documentIDs.add(imageID);
		}
		
		final double inverseDocumentFrequency = Math.log(((double) imageNameByImageIdMap.keySet().size())/documentIDs.size());
		
		return termFrequency*inverseDocumentFrequency;
	}
	
	/**
	 * @param imageName
	 * 		Name of the document
	 * @return
	 * 		Returns maximum raw frequency of any term in the document
	 */
	private double getMaxRawTermFrequencyInDoc(String imageName) {
		
		if(imageName == null) 
			throw new IllegalArgumentException("imageName");
		
		if(maxWordCountByImageName .containsKey(imageName)) {
			return maxWordCountByImageName.get(imageName);
		}
		
		return 0;
	}

	/**
	 * @param word
	 * 		Term
	 * @param imageName
	 * 		Name of the document
	 * @return
	 * 		Returns raw frequency of a term in a document, i.e. the number of times that term occurs in document
	 */
	private short getRawTermFrequencyInDoc(String word, String imageName) {
		
		if(word == null) 
			throw new IllegalArgumentException("word");
		if(imageName == null) 
			throw new IllegalArgumentException("imageName");
		
		if(wordCountByImageNameMapByWordMap.containsKey(word) && wordCountByImageNameMapByWordMap.get(word).containsKey(imageName)) {
			return wordCountByImageNameMapByWordMap.get(word).get(imageName);
		}
		
		return 0;
	}

	/* ================================================== BISSINES LOGIC ================================================== */

	private void parseData() throws NumberFormatException, IOException {
		
		final long startTime = System.currentTimeMillis();
		
		short imageID = 0;
		for(File hessafFile : DATA_ROOT_FILE.listFiles()) {
			
			final String imageName = FilenameUtils.getBaseName(hessafFile.getName()).substring(5);
			
			logger.info("Parsing HESSAF data ({}) for image name: {}", imageID, imageName);
			final Map<String, double[][]> imagePointsByWords = getImagePointsByWords(imageName);
			
			imageNameByImageIdMap.put(imageID, imageName);
			
			for(String word : imagePointsByWords.keySet()) {
				
				final double[][] points = imagePointsByWords.get(word);
				
				if(!wordCountByImageNameMapByWordMap.containsKey(word)) {
					wordCountByImageNameMapByWordMap.put(word, new HashMap<String, Short>());
				}
				
				wordCountByImageNameMapByWordMap.get(word).put(imageName, (short) points.length);
				
				if(!maxWordCountByImageName.containsKey(imageName)) {
					maxWordCountByImageName.put(imageName, (short) points.length);
				} else {
					Short maxWordCount = maxWordCountByImageName.get(imageName);
					if(maxWordCount < points.length) {
						maxWordCountByImageName.put(imageName, (short) points.length);
					}
				}
				
				if(!imageIdByWordMap.containsKey(word)) {
					imageIdByWordMap.put(word, provideIntialIICapacity());
				}
				
				// put this image ID in map for every point mapped by this word
				for(int i = 0; i < points.length; i++) {
					
					final short[] currentImageIDs = imageIdByWordMap.get(word);
					
					if(currentImageIDs[currentImageIDs.length-1] != -1) {
					
						final short[] newImageIDs = ensureCapacity(currentImageIDs);
						newImageIDs[currentImageIDs.length] = imageID;
						imageIdByWordMap.put(word, newImageIDs);
					
					} else {
						
						short j = 0;
						while(currentImageIDs[j] != -1) j++;
						currentImageIDs[j] = imageID;
					}
				}
			}
			
			imageID++;
		}
		
		logTime(startTime, "Hessaf descriptors READ");
	}

	/**
	 * Creates new array of short having size {@link #INITIAL_ARRAY_CAPACITY}
	 * 
	 * @return
	 * 		Returns this ne array
	 */
	private short[] provideIntialIICapacity() {
		
		final short[] retval = new short[INITIAL_ARRAY_CAPACITY];
		
		for(int i = 0; i < INITIAL_ARRAY_CAPACITY; i++) {
			retval[i] = -1;
		}
		
		return retval;
	}
	
	/**
	 * Creates a new array of short with double capacity and copies the elements from the old one to it. Sets the rest of the elements to -1
	 * 
	 * @param array
	 * 		Array of short to be doubled
	 * @return
	 * 		Returns the new array
	 */
	private short[] ensureCapacity(short[] array) {
		
		final short[] retval = new short[array.length*2];
		
		for(int i = 0; i < array.length; i++) {
			retval[i] = array[i];
		}
		
		for(int i = array.length; i < array.length*2; i++) {
			retval[i] = -1;
		}
		
		return retval;
	}

	private File getImageFile(String imageName) {
		return new File(DATA_ROOT_FILE, DataEngineFactory.HESSAF_DATA_PREFIX + imageName + DataEngineFactory.HESSAF_DATA_SUFFIX);
	}
}
