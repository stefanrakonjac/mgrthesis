package com.stefanrakonjac.mgrthesis.ransac.util;

import static com.stefanrakonjac.mgrthesis.ransac.loransac.LebedaDataTest.ALL_DATA_C_PARSED_TXT;
import static com.stefanrakonjac.mgrthesis.ransac.loransac.LebedaDataTest.ALL_DATA_TXT;
import static com.stefanrakonjac.mgrthesis.ransac.loransac.LebedaDataTest.DATA_ROOT;
import static com.stefanrakonjac.mgrthesis.ransac.loransac.LebedaDataTest.IMAGE_NAMES;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.stefanrakonjac.mgrthesis.ransac.loransac.LebedaDataTest;

/**
 * @author Stefan.Rakonjac
 *
 */
public class TransformAllDataTest {
	
	private static final String ROW_FORMAT = "%4.15f, %4.15f, 1.0, %4.15f, %4.15f, 1.0,\n";
	
	private static final Logger logger = LoggerFactory.getLogger(TransformAllDataTest.class);
	
	@Test(dataProvider="allDataDP", enabled=false)
	public void pareseAllDataTest(final String imageName, final File allDataFile, final File allDataParsedFile) throws NumberFormatException, IOException {
		
		final double[][] allData = LebedaDataTest.readDataFile(allDataFile, true);
		
		final StringBuilder builder = new StringBuilder();
		
		for(double[] data : allData) {
			builder.append(String.format(ROW_FORMAT, data[0], data[1], data[2], data[3]));
		}

		if(allDataParsedFile.exists()) {
			logger.info("deleting {}", allDataParsedFile);
			allDataParsedFile.delete();
		}
		
		FileUtils.writeStringToFile(allDataParsedFile, builder.toString());
	}
	
	
	@DataProvider
	public Object[][] allDataDP() throws IOException {
		
		final Object[][] retval = new Object[IMAGE_NAMES.length][3];
		
		for(int i = 0; i < IMAGE_NAMES.length; i++) {
			
			final String imageName = IMAGE_NAMES[i];
			
			final File imageRootDir = new File(DATA_ROOT, imageName);
			final File allDataFile = new File(imageRootDir, ALL_DATA_TXT);
			final File allDataParsedFile = new File(imageRootDir, ALL_DATA_C_PARSED_TXT);
			
			retval[i][0] = imageName;
			retval[i][1] = allDataFile;
			retval[i][2] = allDataParsedFile;
		}
		
		return retval;
	}
}
