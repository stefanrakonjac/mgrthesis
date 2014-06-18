/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.utils.dataengine;

/**
 * @author Stefan.Rakonjac
 *
 */
public class DataEngineFactory {

	public static final String HESSAF_DATA_ROOT = "./data/word_oxc1_hesaff_sift_16M_1M/";
	public static final String HESSAF_DATA_PREFIX = "oxc1_";
	public static final String HESSAF_DATA_SUFFIX = ".txt";
	
	private static final DataEngine INSTANCE_OXFORD_HESSAF = new OxfordHessafDataEngine();
	
	public static DataEngine getOxfordHessafDataEngine() {
		return INSTANCE_OXFORD_HESSAF;
	}
	
	public static class InitializationException extends Exception {

		private static final long serialVersionUID = 1L;
		
		public InitializationException(String errorMsg) {
			super(errorMsg);
		}
		
		public InitializationException(String errorMsg, Throwable cause) {
			super(errorMsg, cause);
		}
		
		public InitializationException(Throwable cause) {
			super(cause);
		}
	}
}
