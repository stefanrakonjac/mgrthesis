/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.utils.dataengine;

import java.util.Map;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.stefanrakonjac.mgrthesis.ransac.utils.dataengine.DataEngineFactory.InitializationException;

/**
 * @author Stefan.Rakonjac
 *
 */
public class DataEngineFactoryTest {
	
	private static final Logger logger = LoggerFactory.getLogger(DataEngineFactoryTest.class);
	
	private static final String TEST_IMAGE_NAME = "all_souls_000002";
	
	@Test(enabled=false)
	public void testGetOxfordHessafDataEngine() throws InitializationException {
		logger.info("testGetOxfordHessafDataEngine() started");
		
		final DataEngine oxfordHessaf = DataEngineFactory.getOxfordHessafDataEngine();
		
		// engine should not have been initialized yet
		Assert.assertFalse(oxfordHessaf.isInit());
		
		// initialize -- takes up to 3 minutes :(
		oxfordHessaf.init();
		
		final Map<String, Map<String, double[][]>> matches = oxfordHessaf.getMostSimilarN(TEST_IMAGE_NAME, 5, null, null);
		
		Assert.assertEquals(5, matches.size());
		
		logger.info("found following similar images for image {}: {}", TEST_IMAGE_NAME, matches.keySet());

		logger.info("testGetOxfordHessafDataEngine() finished correctly");
	}

}
