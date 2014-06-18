/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.utils.dataengine;

import java.io.IOException;
import java.util.Map;

import com.stefanrakonjac.mgrthesis.ransac.impl.struct.ImageScore;
import com.stefanrakonjac.mgrthesis.ransac.utils.Boundaries;
import com.stefanrakonjac.mgrthesis.ransac.utils.dataengine.DataEngineFactory.InitializationException;

/**
 * @author Stefan.Rakonjac
 *
 */
public interface DataEngine {
	
	/**
	 * <p> Initializes the data engine. This action may be really time consuming. Multiple call of this method will be ignored. </p>
	 * 
	 * @throws InitializationException 
	 * 		May throw initialization exception if any kind of problem arises during the data engine intialization
	 */
	public void init() throws InitializationException;
	
	/**
	 * 
	 * @return
	 * 		Returns {@code true} if the data engine has already been initialized, otherwise returns {@code false}
	 */
	public boolean isInit();
	
	/**
	 * 
	 * @param imageA
	 * @param imageB
	 * @return
	 */
	public double[][] computeMatches(String imageA, String imageB);
	
	/**
	 * 
	 * @param image
	 * @param n
	 * @param boundaries 
	 * 		Can be {@code null}
	 * 
	 * @return
	 */
	public Map<String, Map<String, double[][]>> getMostSimilarN(String image, int n, Boundaries boundaries, Map<String, ImageScore> scores);

	/**
	 * 
	 * @param imageName
	 * @return
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	Map<String, double[][]> getImagePointsByWords(String imageName) throws NumberFormatException, IOException;

	/**
	 * <p> Provides {@code tf-idf} (Term Frequency to Inverse Document Frequency) measure </p>
	 * 
	 * @param word
	 * 		Visual word for which {@code tf-idf} is to be provided
	 * @return
	 * 		Returns {@code tf-idf} for the provided word
	 */
	double getTFIDF(String word, String imageName);
}
