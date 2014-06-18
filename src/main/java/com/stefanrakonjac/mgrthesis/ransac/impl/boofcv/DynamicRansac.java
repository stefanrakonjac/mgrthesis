package com.stefanrakonjac.mgrthesis.ransac.impl.boofcv;

import java.util.List;

import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stefanrakonjac.mgrthesis.ransac.impl.AbstRANSAC;

/**
 * @author Stefan.Rakonjac
 *
 */

class DynamicRansac<Model, Point> extends Ransac<Model, Point> {
	
	private static final Logger logger = LoggerFactory.getLogger(DynamicRansac.class);
	
	private final double bestSolutionProbability;

	DynamicRansac(final long randSeed, final ModelGenerator<Model, Point> modelGenerator, final DistanceFromModel<Model, Point> modelDistance, final int maxIterations, final double thresholdFit, final double bestSolutionProbability) {
		super(randSeed, modelGenerator, modelDistance, maxIterations, thresholdFit);
		this.bestSolutionProbability = bestSolutionProbability;
	}
	
	@Override
	public boolean process(List<Point> dataSet) {

		// see if it has the minimum number of points
		if (dataSet.size() < modelGenerator.getMinimumPoints() )
			return false;

		// configure internal data structures
		initialize(dataSet);

		// iterate until it has exhausted all iterations or stop if the entire data set
		// is in the inlier set
		int maximalNumerOfIterations = maxIterations;
		for (int i = 0; i < maximalNumerOfIterations && bestFitPoints.size() != dataSet.size(); i++) {
			// sample the a small set of points
			randomDraw(dataSet, sampleSize, initialSample, rand);
			
			// get the candidate(s) for this sample set
			if( modelGenerator.generate(initialSample, candidateParam ) ) {

				// see if it can find a model better than the current best one
				selectMatchSet(dataSet, thresholdFit, candidateParam);

				// save this results
				if (bestFitPoints.size() < candidatePoints.size()) {
					swapCandidateWithBest();
					
					// updating number of samples needed
					
					final int newTotalIterations = AbstRANSAC.recalculateMaxIterations(bestFitPoints.size() + 1, bestSolutionProbability, 7, dataSet.size());
					if (newTotalIterations < maximalNumerOfIterations) {
						logger.debug("Downsizing total number of iterations from {} to {} (inliers found: {}; best solution prob: {}; minimal inliers: {}, tentative pairs count: {}", 
								maximalNumerOfIterations, newTotalIterations, bestFitPoints.size(), bestSolutionProbability, 7, dataSet.size());
						
						maximalNumerOfIterations = newTotalIterations;
					}
				}
			}
		}

		return bestFitPoints.size() > 0;
	}

}
