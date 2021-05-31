package log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.*;

/**
 * Logger
 */
public class Log
{
    private static final LocalLogger logger = new LocalLogger();

    private Log()
    {
    }

    private static class LocalLogger extends Logger
    {
        private LocalLogger()
        {
            super(Logger.GLOBAL_LOGGER_NAME, null);
            init();
        }

        private void init()
        {
            // suppress the logging output to the console
            final Logger rootLogger = Logger.getLogger(TimeTrackerConstants.STRING_EMPTY);
            final Handler[] handlers = rootLogger.getHandlers();
            if (handlers[0] instanceof ConsoleHandler)
            {
                rootLogger.removeHandler(handlers[0]);
            }

            super.setLevel(Level.INFO);

            final File logFile = new File(TimeTracker.home + TimeTrackerConstants.LOGFILE_NAME);
            System.out.println("Log file: " + logFile.getAbsolutePath());
            if (!logFile.exists())
            {
                System.out.println("Log file does not exist: " + logFile.getAbsolutePath());
                try
                {
                    final Path file = Files.createFile(logFile.toPath());
                    System.out.println("Log file created: " + file.toFile().getAbsolutePath());
                }
                catch (IOException | SecurityException | UnsupportedOperationException e)
                {
                    System.out.println("Error occurred while creating log file: " + e.getMessage());
                }
            }

            try (final InputStream inputStream = new FileInputStream(new File(TimeTracker.home + TimeTrackerConstants.DEFAULT_PROPERTIES)))
            {
                final LogManager manager = LogManager.getLogManager();
                manager.readConfiguration(inputStream);

                final FileHandler fileHandler = new FileHandler(logFile.getAbsolutePath(), true);
                fileHandler.setFormatter(new LogFormatter());
                super.addHandler(fileHandler);
            }
            catch (IOException e)
            {
                super.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    static void log(final Level level, final String msg)
    {
        logger.log(level, msg);
    }

    static void log(final Level level, final String msg, final Object param)
    {
        logger.log(level, msg, param);
    }

    static void log(final Level level, final String msg, final Object[] params)
    {
        logger.log(level, msg, params);
    }

    static void log(final Level level, final String msg, final Throwable thrown)
    {
        logger.log(level, msg, thrown);
    }

    static void severe(final String msg)
    {
        logger.severe(msg);
    }

    static void severe(final String msg, final Throwable thrown)
    {
        logger.log(Level.SEVERE, msg, thrown);
    }

    static void severe(final String msg, final Object param)
    {
        logger.log(Level.SEVERE, msg, param);
    }

    static void warning(final String msg)
    {
        logger.warning(msg);
    }

    static void warning(final String msg, final Object param)
    {
        logger.log(Level.WARNING, msg, param);
    }

    static void warning(final String msg, final Object[] params)
    {
        logger.log(Level.WARNING, msg, params);
    }

    static void info(final String msg)
    {
        logger.info(msg);
    }

    static void info(final String msg, final Object param)
    {
        logger.log(Level.INFO, msg, param);
    }

    static void info(final String msg, final Object[] params)
    {
        logger.log(Level.INFO, msg, params);
    }

    static void fine(final String msg)
    {
        logger.fine(msg);
    }

    static void fine(final String msg, final Object param)
    {
        logger.log(Level.FINE, msg, param);
    }

    static void finer(final String msg)
    {
        logger.finer(msg);
    }

    static void finest(final String msg)
    {
        logger.finest(msg);
    }

    static void setLevel(final Level newLevel)
    {
        logger.setLevel(newLevel);
    }

    Level getLevel()
    {
        return logger.getLevel();
    }

    boolean isLoggable(final Level level)
    {
        return logger.isLoggable(level);
    }
}