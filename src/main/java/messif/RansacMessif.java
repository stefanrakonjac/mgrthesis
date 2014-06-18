/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.fitting.DistanceFromModelResidual;
import boofcv.abst.geo.fitting.GenerateEpipolarMatrix;
import boofcv.alg.geo.f.FundamentalResidualSampson;
import boofcv.factory.geo.EnumEpipolar;
import boofcv.factory.geo.EpipolarError;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.GeoModelRefine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import messif.objects.impl.ObjectFeature;
import messif.objects.impl.ObjectFeatureQuantized;
import messif.objects.impl.ObjectFeatureSet;
import messif.objects.util.RankedAbstractObject;

import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ejml.data.DenseMatrix64F;

/**
 * Wrapper class encapsulating RANSAC algorithm in BoofCV library.
 * 
 * This class complies to the standard MESSIF interface.
 * 
 * @author Vlastislav Dohnal, dohnal@gmail.com, Faculty of informatics, Masaryk University
 */
public class RansacMessif {
    
/*
 * Taken from:
 * @url http://boofcv.org/index.php?title=Example_Fundamental_Matrix
 * @author Peter Abeles
 * 
		List<AssociatedPair> matches = computeMatches(imageA,imageB);
 
		// Where the fundamental matrix is stored
		DenseMatrix64F F;
		// List of matches that matched the model
		List<AssociatedPair> inliers = new ArrayList<AssociatedPair>();
 
		// estimate and print the results using a robust and simple estimator
		// The results should be difference since there are many false associations in the simple model
		// Also note that the fundamental matrix is only defined up to a scale factor.
		F = robustFundamental(matches, inliers);
		System.out.println("Robust");
		F.print();
 
		F = simpleFundamental(matches);
		System.out.println("Simple");
		F.print();
 
		// display the inlier matches found using the robust estimator
		AssociationPanel panel = new AssociationPanel(20);
		panel.setAssociation(inliers);
		panel.setImages(imageA,imageB);
 
		ShowImages.showWindow(panel, "Inlier Pairs");
*/    

    public static List<RankedAbstractObject> doRansacQuantized(ObjectFeatureSet objModel, Iterator<RankedAbstractObject> itObjs) {
        List<RankedAbstractObject> result = new ArrayList<RankedAbstractObject>(100);
        
        while (itObjs.hasNext()) {
            RankedAbstractObject obj = itObjs.next();
            
            if (objModel.getLocatorURI() != null && objModel.getLocatorURI().equals(obj.getObject().getLocatorURI())) {
                result.add(new RankedAbstractObject(obj.getObject(), 0));
                continue;
            }
            
            List<AssociatedPair> matches = computeMatches(objModel, (ObjectFeatureSet)obj.getObject());
            
            if (matches.isEmpty()) {
                System.err.println("WARNING: No matching quantized descriptors found between " + objModel.getLocatorURI() + " and " + obj.getObject().getLocatorURI());
                continue;
            }
            
            // List of matches that matched the model
            List<AssociatedPair> inliers = new ArrayList<AssociatedPair>();

            // estimate and print the results using a robust and simple estimator
            // The results should be difference since there are many false associations in the simple model
            // Also note that the fundamental matrix is only defined up to a scale factor.
            DenseMatrix64F F = robustFundamental(matches, inliers);
            
            // How the error is measured
            DistanceFromModelResidual<DenseMatrix64F,AssociatedPair> errorMetric =
                            new DistanceFromModelResidual<DenseMatrix64F,AssociatedPair>(new FundamentalResidualSampson());
            
            // Compute error and use it as a distance (for ranking)
            errorMetric.setModel(F);
            double err = 0f;
            for (AssociatedPair p : matches) {
                err += errorMetric.computeDistance(p);
        }
        
            result.add(new RankedAbstractObject(obj.getObject(), (float)err));
            System.out.println();
            System.out.println("inliers/matches ratio: " + inliers.size() + "/"+ matches.size());
            System.out.println("normalized error (1-ratio * error): " + ((1-((float)inliers.size())/matches.size()))*err);

//            result.add(new RankedAbstractObject(obj.getObject(), (1 - inliers.size()) / matches.size()));
//            result.add(new RankedAbstractObject(obj.getObject(), (100 - 100*((float)inliers.size() / (float)matches.size()))));
        }
        
        Collections.sort(result);
        return result;
    }

    /*
        Map<String, Integer> map = new HashMap<String, Integer>();
        double topCoefficient = topPercent / 100.0;
        double bottomCoefficient = bottomPercent / 100.0;

        // Get number of unique descriptors as Map<QuantizedFeature, Number>
        Iterator<ObjectFeatureSet> iterator = featureSets.iterator();
        while (iterator.hasNext()) {
            ObjectFeatureSet featureSet = iterator.next();
            
            for (Iterator<ObjectFeature> it = featureSet.iterator(); it.hasNext();) {
                ObjectFeatureQuantized f = (ObjectFeatureQuantized)it.next();
                String qf = f.getStringData();
                Integer v = map.get(qf);
                map.put(qf, ((v == null) ? 1 : v+1));
            }
        }

        // Sort quantized features according to their number
        Map<String, Integer> sortedMap = sortByValues(map);

     */
    
    private static Map<String,List<ObjectFeatureQuantized>> getFeaturesAndTheirCounts(ObjectFeatureSet fs) {
        Map<String,List<ObjectFeatureQuantized>> map = new HashMap<String, List<ObjectFeatureQuantized>>();
        for (Iterator<ObjectFeature> it = fs.iterator(); it.hasNext();) {
            ObjectFeatureQuantized f = (ObjectFeatureQuantized)it.next();
            String qf = f.getStringData();
            List<ObjectFeatureQuantized> l = map.get(qf);
            if (l == null) {
                l = new ArrayList<ObjectFeatureQuantized>();
                map.put(qf, l);
            }
            l.add(f);
        }
        return map;
    }
    
    private static List<AssociatedPair> computeMatches(ObjectFeatureSet objModel, ObjectFeatureSet objFit) {
    	// TODO: originaly 5
        int maxFeatsPerWord = 5; // Max. number of features having the same quantization (to be used in pairing)
        Map<String, List<ObjectFeatureQuantized>> cntModel = getFeaturesAndTheirCounts(objModel);
        Map<String, List<ObjectFeatureQuantized>> cntFit = getFeaturesAndTheirCounts(objFit);

        // Find matching pairs, ignore too frequent quantizations
        List<AssociatedPair> res = new ArrayList<AssociatedPair>(1000);
        for (Map.Entry<String, List<ObjectFeatureQuantized>> eM : cntModel.entrySet()) {
            String qM = eM.getKey();
            List<ObjectFeatureQuantized> lM = eM.getValue();
            
            if (lM.size() > maxFeatsPerWord)
                continue;
            
            List<ObjectFeatureQuantized> lF = cntFit.get(qM);
            if (lF == null || lF.size() > maxFeatsPerWord)
                continue;
            
            // Insert all pairs...
            for (ObjectFeatureQuantized fM : lM) {
                for (ObjectFeatureQuantized fF : lF) {
                    res.add(new AssociatedPair(fM.getX(), fM.getY(), fF.getX(), fF.getY()));
                }
            }
        }
        return res;
        
        //        for (int i = 0; i < objModel.getObjectCount(); i++) {
        //            ObjectFeatureQuantized featModel = (ObjectFeatureQuantized)objModel.getObject(i);
        //            for (int j = 0; j < objFit.getObjectCount(); j++) {
        //                ObjectFeatureQuantized featFit = (ObjectFeatureQuantized)objFit.getObject(j);
        //
        //                if (featModel.dataQuantizedEquals(featFit))
        //                    res.add(new AssociatedPair(featModel.getX(), featModel.getY(), featFit.getX(), featFit.getY()));
        //            }
        //        }
        //
    }

    /**
     * Given a set of noisy observations, compute the Fundamental matrix while removing
     * the noise.
     *
     * @param matches List of associated features between the two images
     * @param inliers List of feature pairs that were determined to not be noise.
     * @return The found fundamental matrix.
     *
     * Taken from:
     * @url http://boofcv.org/index.php?title=Example_Fundamental_Matrix
     * @author Peter Abeles
    */
    private static DenseMatrix64F robustFundamental(List<AssociatedPair> matches, List<AssociatedPair> inliers) {

            // Select which linear algorithm is to be used.  Try playing with the number of remove ambiguity points
            Estimate1ofEpipolar estimateF = FactoryMultiView.computeFundamental_1(EnumEpipolar.FUNDAMENTAL_8_LINEAR, 20);
            // Wrapper so that this estimator can be used by the robust estimator
            GenerateEpipolarMatrix generateF = new GenerateEpipolarMatrix(estimateF);

            // How the error is measured
            DistanceFromModelResidual<DenseMatrix64F,AssociatedPair> errorMetric =
                            new DistanceFromModelResidual<DenseMatrix64F,AssociatedPair>(new FundamentalResidualSampson());

            // Use RANSAC to estimate the Fundamental matrix
            ModelMatcher<DenseMatrix64F,AssociatedPair> robustF =
                            new Ransac<DenseMatrix64F, AssociatedPair>(123123,generateF,errorMetric,3000,0.01);

            // Estimate the fundamental matrix while removing outliers
            if( !robustF.process(matches) )
                throw new IllegalArgumentException("Failed to compute the fundamental matrix");

            // save the set of features that were used to compute the fundamental matrix
            inliers.addAll(robustF.getMatchSet());
            
            // Improve the estimate of the fundamental matrix using non-linear optimization
            DenseMatrix64F F = new DenseMatrix64F(3,3);
            GeoModelRefine<DenseMatrix64F,AssociatedPair> refine =
                            FactoryMultiView.refineFundamental(1e-8, 400, EpipolarError.SAMPSON);
            if( !refine.process(robustF.getModel(), inliers, F) )
                throw new IllegalArgumentException("Failed to refine the fundamental matrix");

            // Return the solution
            return F;
    }
}
