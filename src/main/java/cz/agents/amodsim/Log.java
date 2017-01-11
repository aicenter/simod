/* 
 * AgentSCAI
 */
package cz.agents.amodsim;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logger,
 * @author david_000
 */
public class Log {
	
	/**
	 * Java logger.
	 */
	private static Logger logger;
	
	
	
	
	/**
	 * Inits logger.
     * @param name Java log name
	 * @param logLevel Java log level for logging to file.
     * @param logFilePath path to log file
	 */
	public static void init(final String name, final Level logLevel, final String logFilePath){
		logger = Logger.getLogger(name);
		logger.setLevel(logLevel);
        
        // do not send log messages to other logs
		logger.setUseParentHandlers(false);
		
		// conslole log settings
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.WARNING);
		consoleHandler.setFormatter(new LogFormater());
		logger.addHandler(consoleHandler);
		
		try {  
			// file log settings
			FileHandler fileHandler = new FileHandler(logFilePath);
			fileHandler.setLevel(logLevel);
			fileHandler.setFormatter(new LogFormater());
			logger.addHandler(fileHandler);
		} catch (IOException ex) {
			Logger.getLogger(Log.class.getName()).log(Level.SEVERE, null, ex);
		} catch (SecurityException ex) {
			Logger.getLogger(Log.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	/**
	 * Log message with no params.
	 * @param caller Caller of the method.
	 * @param level Java log level.
	 * @param message Log message.
	 */
    public static void log(Object caller, Level level, String message){ 
        message = caller.getClass().getName() + ": " + message;
		logger.log(level, message);
    }
    
	/**
	 * Log message.
	 * @param caller Caller of the method.
	 * @param level Java log level.
	 * @param message Log message.
	 * @param params Message parameters.
	 */
    public static void log(Object caller, Level level, String message, Object... params){
        message = caller.getClass().getName() + ": " + message;
		logger.log(level, message, params);
    }
}