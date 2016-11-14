package edu.pitt.dbmi.ccd.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author : Jeremy Espino MD Created 4/5/16 2:17 PM
 */
public class MyLogger implements com.jcraft.jsch.Logger {

    final Logger logger = LoggerFactory.getLogger(MyLogger.class);

    static java.util.Hashtable name = new java.util.Hashtable();

    static {
        name.put(new Integer(DEBUG), "DEBUG: ");
        name.put(new Integer(INFO), "INFO: ");
        name.put(new Integer(WARN), "WARN: ");
        name.put(new Integer(ERROR), "ERROR: ");
        name.put(new Integer(FATAL), "FATAL: ");
    }

    public boolean isEnabled(int level) {
        return true;
    }

    public void log(int level, String message) {
        switch (level) {
            case 0:
                logger.debug(message);
                break;
            case 1:
                logger.info(message);
                break;
            case 2:
                logger.warn(message);
                break;
            case 3:
                logger.error(message);
                break;
            case 4:
                logger.error(message);
                break;
            default:
                logger.error("Unknown log level: " + level + " - " + message);
        }
    }

}
