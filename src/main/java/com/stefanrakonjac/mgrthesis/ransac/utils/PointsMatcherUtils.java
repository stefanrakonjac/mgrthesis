package com.stefanrakonjac.mgrthesis.ransac.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import messif.objects.impl.ObjectFeature;
import messif.objects.impl.ObjectFeatureQuantized;
import messif.objects.impl.ObjectFeatureSet;
import messif.objects.keys.DimensionObjectKey;
import messif.quantization.AbstractVisualVocabulary;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stefan.Rakonjac
 *
 */
public class PointsMatcherUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(PointsMatcherUtils.class);
	
	/** Default Max Features Per Word */
	public static final int D_MAX_FEATURES_PER_WORD = 5;

	public static double[][] computeMatchesFromOFSUsingVocabulary(ObjectFeatureSet ofsA, ObjectFeatureSet ofsB, AbstractVisualVocabulary vocabulary) {
		return computeMatchesFromOFSUsingVocabulary(ofsA, ofsB, vocabulary, D_MAX_FEATURES_PER_WORD);
	}

	public static double[][] computeMatchesFromOFSUsingVocabulary(ObjectFeatureSet ofsA, ObjectFeatureSet ofsB, AbstractVisualVocabulary vocabulary, int maxFeatsPerWord) {
		final ObjectFeatureSet ofsAQuantized = vocabulary.convertToFeatureSet(ofsA);
		final ObjectFeatureSet ofsBQuantized = vocabulary.convertToFeatureSet(ofsB);
		return computeMatchesFromQuantizedOFS(ofsAQuantized, ofsBQuantized, maxFeatsPerWord);
	}
	
	public static double[][] computeMatchesFromQuantizedOFS(final ObjectFeatureSet ofsA, final ObjectFeatureSet ofsB) {
		return computeMatchesFromQuantizedOFS(ofsA, ofsB, D_MAX_FEATURES_PER_WORD);
	}

	public static double[][] computeMatchesFromQuantizedOFS(final ObjectFeatureSet ofsA, final ObjectFeatureSet ofsB, int maxFeatsPerWord) {
		if(ofsA == null) throw new IllegalArgumentException("ofsA is null");
		if(ofsB == null) throw new IllegalArgumentException("ofsB is null");
		
		if(maxFeatsPerWord < 0) {
			logger.warn("maxFeatsPerWord is less then 0 ({}); defaulting to 0 (no filtration)", maxFeatsPerWord);
			maxFeatsPerWord = 0;
		}

        Map<String, List<ObjectFeatureQuantized>> ofqListAByWord = getOFQuantizedMappedByWords(ofsA);
        Map<String, List<ObjectFeatureQuantized>> ofqListBByWord = getOFQuantizedMappedByWords(ofsB);

        List<double[]> res = new ArrayList<double[]>(1000);

        for (Entry<String, List<ObjectFeatureQuantized>> listAEntry : ofqListAByWord.entrySet()) {
            final String listAWord = listAEntry.getKey();
            final List<ObjectFeatureQuantized> featuresA = listAEntry.getValue();
            final List<ObjectFeatureQuantized> featuresB = ofqListBByWord.get(listAWord);
            
            if(featuresB == null) {
            	continue;
            }
            
            // maxFeatsPerWord > 0 indicates that more than more then maxFeatsPerWord features 
            // mapped by the same word should not be taken in consideration
            if (maxFeatsPerWord != 0 && (featuresA.size() > maxFeatsPerWord || featuresB.size() > maxFeatsPerWord)) {
            	logger.trace("skipping {} features from list A and {} features from list B mapped by word {}", featuresA.size(), featuresB.size(), listAWord);
            	continue;
            }

            final DimensionObjectKey ofsAObjectKey = (DimensionObjectKey) ofsA.getObjectKey();
            final DimensionObjectKey ofsBObjectKey = (DimensionObjectKey) ofsB.getObjectKey();
            
            // create pairs
            for (ObjectFeatureQuantized featureA : featuresA) {
                for (ObjectFeatureQuantized featureB : featuresB) {
                    res.add(new double[] { featureA.getX() * ofsAObjectKey.getWidth(), featureA.getY() * ofsAObjectKey.getHeight(), featureB.getX() * ofsBObjectKey.getWidth(), featureB.getY() * ofsBObjectKey.getHeight()});
                }
            }
        }
        
        // return as array of double arrays
        return res.toArray(new double[0][0]);
	}
	
    private static Map<String,List<ObjectFeatureQuantized>> getOFQuantizedMappedByWords(ObjectFeatureSet ofsA) {
        Map<String,List<ObjectFeatureQuantized>> retval = new HashMap<String, List<ObjectFeatureQuantized>>();
        
        Iterator<ObjectFeature> iterator = ofsA.iterator();
        while (iterator.hasNext()) {
        	ObjectFeatureQuantized ofq = (ObjectFeatureQuantized) iterator.next();
            String word = ofq.getStringData();
            List<ObjectFeatureQuantized> list = retval.get(word);
            if (list == null) {
                list = new ArrayList<ObjectFeatureQuantized>();
                retval.put(word, list);
            }
            list.add(ofq);
        }
        
        return retval;
    }

	public static double[][] computeMatchesFromOFSByMetricDistance(ObjectFeatureSet ofsA, ObjectFeatureSet ofsB, long distance) {
		
		if(ofsA == null) throw new IllegalArgumentException("ofsA");
		if(ofsB == null) throw new IllegalArgumentException("ofsB");
		if(distance < 1) throw new IllegalArgumentException("distance");

		logger.debug("computeMatchesFromOFSByMetricDistance({}, {}, {})", ofsA.getObjectKey(), ofsB.getObjectKey(), distance);
		
        List<double[]> retval = new ArrayList<double[]>(1000);

        final DimensionObjectKey ofsAObjectKey = (DimensionObjectKey) ofsA.getObjectKey();
        final DimensionObjectKey ofsBObjectKey = (DimensionObjectKey) ofsB.getObjectKey();
		
		for(ObjectFeature featureA : ofsA) {
			for(ObjectFeature featureB : ofsB) {
				if(featureA.getDistance(featureB) < distance) {
					retval.add(new double[] { featureA.getX() * ofsAObjectKey.getWidth(), featureA.getY() * ofsAObjectKey.getHeight(), featureB.getX() * ofsBObjectKey.getWidth(), featureB.getY() * ofsBObjectKey.getHeight()});
				}
			}
		}
			
		return retval.toArray(new double[0][0]);
	}

	public static double[][] computeMatchesFromWords(Map<String, double[][]> imageAPointsByWords, Map<String, double[][]> imageBPointsByWords) {
		
		if(imageAPointsByWords == null) 
			throw new IllegalArgumentException("imageAPointsByWords");
		if(imageBPointsByWords == null) 
			throw new IllegalArgumentException("imageBPointsByWords");

		final List<double[]> retval = new ArrayList<>();
		
		for(String word : imageAPointsByWords.keySet()) {
			
			if(!imageBPointsByWords.containsKey(word)) continue;
			
			for(double[] imageAPoint : imageAPointsByWords.get(word)) {
				for(double[] imageBPoint : imageBPointsByWords.get(word)) {
					retval.add(ArrayUtils.addAll(imageAPoint, imageBPoint));
				}
			}
		}
		
		return ArraysUtils.to2Ddouble(retval);
	}

	public static Map<String, double[][]> computeMappedMatchesFromWords(Map<String, double[][]> imageAPointsByWords, Map<String, double[][]> imageBPointsByWords) {
		
		if(imageAPointsByWords == null) 
			throw new IllegalArgumentException("imageAPointsByWords");
		if(imageBPointsByWords == null) 
			throw new IllegalArgumentException("imageBPointsByWords");

		final Map<String, double[][]> retval = new HashMap<>();
		
		for(String word : imageAPointsByWords.keySet()) {
			
			if(!imageBPointsByWords.containsKey(word)) continue;
			
			final Set<double[]> wordPointsSet = new HashSet<>();
			
			for(double[] imageAPoint : imageAPointsByWords.get(word)) {
				for(double[] imageBPoint : imageBPointsByWords.get(word)) {
					wordPointsSet.add(ArrayUtils.addAll(imageAPoint, imageBPoint));
				}
			}
			
			retval.put(word, ArraysUtils.to2Ddouble(wordPointsSet));
		}
		
		return retval;
	}
}
