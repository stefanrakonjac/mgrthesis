package com.stefanrakonjac.mgrthesis.ransac;

import static com.stefanrakonjac.mgrthesis.ransac.FileConstants.*;

import java.io.File;
import java.io.IOException;

import messif.objects.impl.ObjectFeatureSet;

import org.ejml.data.DenseMatrix64F;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import boofcv.abst.geo.fitting.DistanceFromModelResidual;
import boofcv.alg.geo.f.FundamentalResidualSampson;
import boofcv.struct.geo.AssociatedPair;

import com.stefanrakonjac.mgrthesis.ransac.impl.boofcv.BoofCVRANSACImpl;
import com.stefanrakonjac.mgrthesis.ransac.impl.boofcv.util.DisplayUtils;
import com.stefanrakonjac.mgrthesis.ransac.util.GroundTruth;
import com.stefanrakonjac.mgrthesis.ransac.util.GroundTruth.GroundTruthBuilder;
import com.stefanrakonjac.mgrthesis.ransac.util.GroundTruth.TYPE;
import com.stefanrakonjac.mgrthesis.ransac.utils.converters.MatrixConverter;
import com.stefanrakonjac.mgrthesis.ransac.utils.converters.PairsConverter;

/**
 * @author Stefan.Rakonjac
 *
 */
public class BoofCVRANSACTest extends AbstractTest {

	@Test(dataProvider="testRansacDP", enabled=false)
	public void testRansac(String name, String[] good, String[] ok, String[] junk) {
		logger.info("name={}; good={}; ok={}; junk={}", name, good, ok, junk);
		
		ObjectFeatureSet ofsA = data.get(name);
		
		if(ofsA == null) {
			logger.warn("ObjectFeatureSet A, named {} could not be found", name);
			return;
		}
		
		double goodScore = 0;
		int goodCount = 0;
		for(String goodName : good) {
			final ObjectFeatureSet ofsB = data.get(goodName);
			if(ofsB == null) {
				logger.warn("ObjectFeatureSet B, named {} could not be found", goodName);
				continue;
			}
			final double singleScore = runRANSACAndComputeAverageScore(ofsA, ofsB);
			
			logger.info("image pair {}-{} scored an average of {}", name, goodName, singleScore);
			goodScore += singleScore;
			goodCount++;
		}
		logger.info("'good' data-set reached score of {} for {} images (average: {})", goodScore, goodCount, goodScore/goodCount);
		
		double okScore = 0;
		int okCount = 0;
		for(String okName : ok) {
			final ObjectFeatureSet ofsB = data.get(okName);
			if(ofsB == null) {
				logger.warn("ObjectFeatureSet named {} could not be found", okName);
				continue;
			}
			final double singleScore = runRANSACAndComputeAverageScore(ofsA, ofsB);
			
			logger.info("image pair {}-{} scored an average of {}", name, okName, singleScore);
			okScore += singleScore;
			okCount++;
		}
		logger.info("'ok' data-set reached score of {} for {} images (average: {})", okScore, okCount, okScore/okCount);
		
		double junkScore = 0;
		int junkCount = 0;
		for(String goodName : good) {
			final ObjectFeatureSet ofsB = data.get(goodName);
			if(ofsB == null) {
				logger.warn("ObjectFeatureSet named {} could not be found", goodName);
				continue;
			}
			final double singleScore = runRANSACAndComputeAverageScore(ofsA, ofsB);
			
			logger.info("image pair {}-{} scored an average of {}", name, goodName, singleScore);
			junkScore += singleScore;
			junkCount++;
		}
		logger.info("'junk' data-set reached score of {} for {} images (average: {})", junkScore, junkCount, junkScore/junkCount);
	}
	
	
	private double runRANSACAndComputeAverageScore(ObjectFeatureSet ofsA, ObjectFeatureSet ofsB) {
		double retval = 0;
		
		final double[][] pairs = computePairs(ofsA, ofsB, DATA_QUANTIZED ? 5 : 200);
		
		RANSAC ransac = new BoofCVRANSACImpl();
		ransac.setTentativePairs(pairs);
		ransac.run();
		final double[][] inliers = ransac.getInliers();
		final double[][] geometryEstimation = ransac.getGeometryEstimation();
		final DenseMatrix64F geometryEstimationMatrix = MatrixConverter.toDenseMatrix64F(geometryEstimation);

        // How the error is measured
        DistanceFromModelResidual<DenseMatrix64F,AssociatedPair> errorMetric = new DistanceFromModelResidual<DenseMatrix64F,AssociatedPair>(new FundamentalResidualSampson());
        
        // Compute error and use it as a distance (for scoring)
        errorMetric.setModel(geometryEstimationMatrix);
        for (AssociatedPair p : PairsConverter.toAssociatedPairList(inliers)) {
        	retval += errorMetric.computeDistance(p);
        }
        
        DisplayUtils.showPairs(IMAGES_ROOT + ofsA.getLocatorURI(), IMAGES_ROOT + ofsB.getLocatorURI(), PairsConverter.toAssociatedPairList(inliers));
        
		return retval * (inliers.length/pairs.length); // average score
	}

	/** ------------------ data providers -------------------- */
	
	@DataProvider(name="testRansacDP")
	public Object[][] testRansacDP() throws IOException {
		logger.debug("testRansacDP()");
		
		final File groundTruthDir = new File(GROUND_TRUTH_ROOT);
		
		final Object[][] retval = new Object[groundTruthDir.list().length/4][4];
		
		GroundTruth groundTruth = new GroundTruthBuilder(GROUND_TRUTH_ROOT).build();
		
		int i = 0;
		for(String gtName : groundTruth.getAllGroundTruthNames()) {
			retval[i][0] = groundTruth.getImageName(gtName);
			retval[i][1] = groundTruth.getAssociatedImageNames(gtName, TYPE.GOOD).toArray(new String[0]);
			retval[i][2] = groundTruth.getAssociatedImageNames(gtName, TYPE.OK).toArray(new String[0]);
			retval[i][3] = groundTruth.getAssociatedImageNames(gtName, TYPE.JUNK).toArray(new String[0]);
			i++;
		}
		
		prepareDataAndVocabulary(groundTruth.getAllImageNames());
		
		return retval;
	}
}
