package com.stefanrakonjac.mgrthesis.ransac;

public class FileConstants {

	public static final String VOCABULARY_FILE = "./data/vocabulary/kmeanstree_ox_10_6";
	public static final String DATA_FILE_UNQUANTIZED = "./data/oxbuildings_opencv_sets-dimension-key-55_68.txt"; // "./data/oxbuildings_opencv_sets-dimension-key-first-100.txt";
	public static final String DATA_FILE_QUANTIZED = "./data/oxbuildings_opencv_sets-dimension-key-quantized.txt";
	public static final String GROUND_TRUTH_ROOT = "./data/ground-truth/";
	
	public static final String IMAGES_ROOT = "./data/images/";
	public static final String IMAGES_SUFFIX = ".jpg";
	
	/** TODO: change by own preference */
	public static final boolean DATA_QUANTIZED = false;
	public static final boolean USE_VOCABULARY = false;
	public static final String DATA_FILE = DATA_QUANTIZED ? DATA_FILE_QUANTIZED : DATA_FILE_UNQUANTIZED;

}
