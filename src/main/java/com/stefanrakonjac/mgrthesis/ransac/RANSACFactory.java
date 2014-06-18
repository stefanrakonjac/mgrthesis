package com.stefanrakonjac.mgrthesis.ransac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stefanrakonjac.mgrthesis.ransac.impl.boofcv.BoofCVRANSACImpl;
import com.stefanrakonjac.mgrthesis.ransac.impl.loransac.LoRANSACImpl;

/**
 * Factory class for obtaining RANSAC implementations
 * 
 * @author Stefan.Rakonjac
 *
 */
public final class RANSACFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(RANSACFactory.class);

	/**
	 * <p> Provides a new <b>full</b> LoRANSAC implementation (local-optimization step turned {@code on}) </p>
	 * 
	 * @return
	 * 		Returns requested RANSAC implementation
	 */
	public static final RANSAC getLoRANSACOnImpl() {
		
		logger.debug("Creating new full LoRANSAC implementation");
		return new LoRANSACImpl();
	}
	
	/**
	 * <p> Provides a new <b>basic</b> LoRANSAC implementation (local-optimization step turned {@code off}). <br />
	 * This implementation works as a standard RANSAC procedure </p>
	 * 
	 * @return
	 * 		Returns requested RANSAC implementation
	 */
	public static final RANSAC getLoRANSACOffImpl() {
		
		logger.debug("Creating new basic LoRANSAC implementation");
		return new LoRANSACImpl().doOptimizations(false);
	}
	
	/**
	 * <p> Provides a new <b>basic</b> LoRANSAC implementation (local-optimization step turned {@code off}).
	 * This implementation works as a standard RANSAC procedure </p>
	 * 
	 * @return
	 * 		Returns requested RANSAC implementation
	 */
	public static final RANSAC getBoofCVRANSACImpl() {
		
		logger.debug("Creating new BoofCV RANSAC implementation");
		return new BoofCVRANSACImpl();
	}
}