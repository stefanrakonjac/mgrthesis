/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stefan.Rakonjac
 *
 */
public class LoggingUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(LoggingUtils.class);
	
	private static final String FORMAT_TIME_ACTION = "{} time: {}ms";
	
	public static void logTimeDebug(long startTime, String action) {
		logTime(startTime, System.currentTimeMillis(), action, LogLevel.DEBUG);
	}
	
	public static void logTimeDebug(long startTime, long endTime, String action) {
		logTime(startTime, endTime, action, LogLevel.DEBUG);
	}
	
	public static void logTime(long startTime, String action) {
		logTime(startTime, System.currentTimeMillis(), action, LogLevel.INFO);
	}
	
	public static void logTime(long startTime, long endTime, String action) {
		logTime(startTime, endTime, action, LogLevel.INFO);
	}
	
	private static void logTime(long startTime, long endTime, String action, LogLevel logLevel) {
		
		switch(logLevel) {
		
			case DEBUG:
				logger.debug(FORMAT_TIME_ACTION, action, endTime-startTime);
			break;
			
			case INFO:
				logger.info(FORMAT_TIME_ACTION, action, endTime-startTime);
			break;
			
			default:
				throw new IllegalArgumentException("logLevel");
		}
	}
	
	private enum LogLevel {
		DEBUG, INFO, WARN, ERROR;
	}
}
