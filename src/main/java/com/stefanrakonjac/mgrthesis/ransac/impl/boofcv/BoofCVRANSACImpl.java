package com.stefanrakonjac.mgrthesis.ransac.impl.boofcv;

import java.util.List;
import java.util.Random;

import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ejml.data.DenseMatrix64F;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.fitting.DistanceFromModelResidual;
import boofcv.abst.geo.fitting.GenerateEpipolarMatrix;
import boofcv.alg.geo.ModelObservationResidual;
import boofcv.alg.geo.f.FundamentalResidualSampson;
import boofcv.factory.geo.EnumEpipolar;
import boofcv.factory.geo.EpipolarError;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.GeoModelRefine;

import com.stefanrakonjac.mgrthesis.ransac.impl.AbstRANSAC;
import com.stefanrakonjac.mgrthesis.ransac.utils.converters.MatrixConverter;
import com.stefanrakonjac.mgrthesis.ransac.utils.converters.PairsConverter;


/**
 * @author Stefan.Rakonjac
 *
 */
public class BoofCVRANSACImpl extends AbstRANSAC {
	
	private final static Logger logger = LoggerFactory.getLogger(BoofCVRANSACImpl.class);

	@Override
	public void run() {

		final int seed = new Random().nextInt();
		final ModelGenerator<DenseMatrix64F, AssociatedPair> modelGenerator = createModelGenerator();
		final DistanceFromModel<DenseMatrix64F, AssociatedPair> errorMetric = createErrorMetricMeasurer();
        
        // Use RANSAC to estimate the Fundamental matrix
        ModelMatcher<DenseMatrix64F,AssociatedPair> rancas = new DynamicRansac<DenseMatrix64F, AssociatedPair>(seed, modelGenerator, errorMetric, maxIterations, threshold, bestSolutionProbability);

        // Estimate the fundamental matrix while removing outliers
        if(!rancas.process(PairsConverter.toAssociatedPairList(tentativePairs))) {
        	logger.error("Failed to compute the fundamental matrix");
        }

        // save the set of features that were used to compute the fundamental matrix
        final List<AssociatedPair> matches = rancas.getMatchSet();
        inliers = PairsConverter.getPairs(matches);
        
        // Improve the estimate of the fundamental matrix using non-linear optimization
        DenseMatrix64F matrix = new DenseMatrix64F(3,3);
        GeoModelRefine<DenseMatrix64F,AssociatedPair> refine = FactoryMultiView.refineFundamental(1e-8, 400, EpipolarError.SAMPSON);
        
        if( !refine.process(rancas.getModel(), matches, matrix) ) {
        	logger.error("Failed to refine the fundamental matrix");
        }

        geometryEstimation = MatrixConverter.toMatrix(matrix);
	}
	
	/**
	 * @return
	 * 		Returns fundamental matrix (a 3×3 matrix which relates corresponding points in stereo images) model generator 
	 * 		capable of generating models from the provided associated pairs.
	 */
	private static ModelGenerator<DenseMatrix64F, AssociatedPair> createModelGenerator() {

        // fundamental 8-point linear algorithm provides single hypothesis >  numRemoveAmbiguity (-1) parameter is obsolete
        Estimate1ofEpipolar estimateF = FactoryMultiView.computeFundamental_1(EnumEpipolar.FUNDAMENTAL_8_LINEAR, -1);
        
        // Wrapper so that this estimator can be used by the robust estimator
        ModelGenerator<DenseMatrix64F, AssociatedPair> fundamentalMatrixModelGenerator = new GenerateEpipolarMatrix(estimateF);
        
        return fundamentalMatrixModelGenerator;
	}
	
	private static DistanceFromModel<DenseMatrix64F, AssociatedPair> createErrorMetricMeasurer() {

		// function capable of determining Sampson error for the provided model (fundamental matrix) and pair of points
		ModelObservationResidual<DenseMatrix64F, AssociatedPair> function = new FundamentalResidualSampson();
		
		// wrapper for used function
        DistanceFromModel<DenseMatrix64F,AssociatedPair> errorMetricMeasurer = new DistanceFromModelResidual<DenseMatrix64F,AssociatedPair>(function);
        
        return errorMetricMeasurer;
	}


	@Override
	public String name() {
		return BoofCVRANSACImpl.class.getSimpleName();
	}
}
