/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * @author Stefan.Rakonjac
 *
 */
public class DummiestTestEver {
	
	private final Logger logger = LoggerFactory.getLogger(DummiestTestEver.class);
	
	@Test
	public void test() {
		
		final int[] testArray = new int[] { 1, 2, 3, 4, 5 };
		logger.info(ArrayUtils.toString(testArray));
		changeArray(testArray);
		logger.info(ArrayUtils.toString(testArray));
		
		
		logger.warn("Satisfied?");
		
	}
	
	private void changeArray(int[] array) {
		for(int i = 0; i < array.length; i++) {
			array[i] = -array[i];
		}
	}

}
