package com.vibrent.drc.util;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

/**
 * utility class to turn off logging during test one class at a time
 */
public class TestLoggingUtil {

    private TestLoggingUtil() {
        // private constructor for util class
    }

    /**
     * change the logging level for a class to a new logging level
     * @param className - class name
     * @param newLevel - new level of logging
     * @return - existing level of logging
     */
    public static Level changeLoggingLevel(String className, Level newLevel) {
        Logger logger = (Logger)LoggerFactory.getLogger(className);

        if (logger==null) {
            return null;
        }

        Level oldLogLevel = logger.getLevel();

        logger.setLevel(newLevel);

        return oldLogLevel;
    }

    /**
     * turn off the logging level for a specific class
     * @param className - class name, ie: (com.abc.MyClass)
     * @return - existing logging level for the
     */
    public static Level turnOffLogging(String className) {
        return changeLoggingLevel(className, Level.OFF);
    }

}
