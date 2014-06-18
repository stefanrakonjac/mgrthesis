/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stefanrakonjac.mgrthesis.ransac.utils.Boundaries;

/**
 * @author Stefan.Rakonjac
 *
 */
public class GroundTruth {
	
	private static final Logger logger = LoggerFactory.getLogger(GroundTruth.class);
	
	private Set<String> allImageNames;
	private Map<String, Map<TYPE, Set<String>>> gtToAssociatedDataMap;
	private Map<String, String> gtToImageNameMap;
	private Map<String, Boundaries> gtToBoundariesMap;
	
	private GroundTruth() {
		// no-op
	}

	public Set<String> getAllImageNames() {
		return new HashSet<>(allImageNames);
	}
	
	public Set<String> getAssociatedImageNames(String gtName, TYPE type) {
		return gtToAssociatedDataMap.get(gtName).get(type);
	}
	
	public String getImageName(String gtName) {
		return gtToImageNameMap.get(gtName);
	}
	
	public Set<String> getAllGroundTruthNames() {
		return new HashSet<>(gtToImageNameMap.keySet());
	}

	public Boundaries getBoundaries(String gtName) {
		return gtToBoundariesMap.get(gtName);
	}
	
	public static final class GroundTruthBuilder {

		private boolean built = false;
		
		private final File gtRoot;
		private final Set<String> allImageNames = new HashSet<>();
		private final Map<String, Map<TYPE, Set<String>>> gtToAssociatedDataMap = new HashMap<>();
		private final Map<String, String> gtToImageNameMap = new HashMap<>();
		private final Map<String, Boundaries> gtToBoundriesMap = new HashMap<>();

		public GroundTruthBuilder(String gtRoot) throws IOException {
			this(new File(gtRoot));
		}

		public GroundTruthBuilder(File gtRoot) throws IOException {
			if(gtRoot == null) 
				throw new IllegalArgumentException("gtRoot");
			
			this.gtRoot = gtRoot;
			parseGTRoot();
		}
		
		private void parseGTRoot() throws IOException {
			
			for(File gtDefinitionFile : gtRoot.listFiles()) {
				final String gtFileName = gtDefinitionFile.getName();
				
				final String gtName = gtFileName.substring(0, gtFileName.lastIndexOf("_"));
				if(gtToAssociatedDataMap.get(gtName) == null) {
					gtToAssociatedDataMap.put(gtName, new HashMap<TYPE, Set<String>>());
				}
				
				final String suffix = gtFileName.substring(gtFileName.lastIndexOf("_")+1, gtFileName.length()-4);
				
				switch(suffix) {
					
					case "query":
						final String queryDefinition = FileUtils.readFileToString(gtDefinitionFile);
						final String[] parts = queryDefinition.split(" ");
						final String imageName = parts[0].substring(5); // removes 'oxc1_'
						allImageNames.add(imageName);
						gtToImageNameMap.put(gtName, imageName);
						gtToBoundriesMap.put(gtName, new Boundaries(parts[1], parts[2], parts[3], parts[4]));
					break;
					
					case "good": case "ok": case "junk":
						Set<String> set = new HashSet<>();
						gtToAssociatedDataMap.get(gtName).put(TYPE.fromValue(suffix), set);
						for(Object line : FileUtils.readLines(gtDefinitionFile)) {
							set.add(line.toString());
						}
						allImageNames.addAll(set);
					break;
					
				}
			}
			
		}
		
		public GroundTruth build() {
			
			logger.debug("build() {} invoked", GroundTruth.class.getName());
			
			if(built) 
				throw new IllegalStateException("Calling build() not for the first time");
			
			final GroundTruth retval = new GroundTruth();
			
			retval.allImageNames = allImageNames;
			retval.gtToImageNameMap = gtToImageNameMap;
			retval.gtToAssociatedDataMap = gtToAssociatedDataMap;
			retval.gtToBoundariesMap = gtToBoundriesMap;
			
			built = true;
			return retval;
		}
	}
	
	public static enum TYPE {
		
		GOOD("good"), OK("ok"), JUNK("junk");
		
		final String value;
		
		TYPE(String value) {
			this.value = value;
		}
		
		private static TYPE fromValue(String value) {
			
			for(TYPE type : values()) {
				if(type.value.equalsIgnoreCase(value)) return type;
			}
			
			throw new IllegalArgumentException("value: " + value);
		}
	}
}
