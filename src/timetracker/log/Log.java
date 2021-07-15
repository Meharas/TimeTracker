package timetracker.log;

import timetracker.Constants;
import timetracker.TimeTracker;
import timetracker.db.Backend;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.*;

/**
 * Logger
 */
public class Log
{
    public static boolean disabled = true;
    private static final LocalLogger logger = new LocalLogger() {
        @Override
        public void setLevel(final Level newLevel) throws SecurityException
        {
            Log.info("Setting log level to " + newLevel);
            super.setLevel(newLevel);
        }
    };
    private static final Map<Level, String> QUEUE = new HashMap<>();

    private Log()
    {
    }

    private static class LocalLogger extends Logger
    {
        private boolean initialized = false;

        private LocalLogger()
        {
            super(Logger.GLOBAL_LOGGER_NAME, null);
            init();
        }

        @Override
        public void log(final Level level, final String msg)
        {
            if(Log.disabled)
            {
                QUEUE.put(level, msg);
            }
            else
            {
                if(!QUEUE.isEmpty())
                {
                    final Iterator<Map.Entry<Level, String>> iterator = QUEUE.entrySet().iterator();
                    while (iterator.hasNext())
                    {
                        final Map.Entry<Level, String> entry = iterator.next();
                        super.log(entry.getKey(), entry.getValue());
                        iterator.remove();
                    }
                    init();
                }
                super.log(level, msg);
            }
        }

        private void init()
        {
            if(Log.disabled || this.initialized)
            {
                return;
            }

            final File logFile = getLogFile();
            if (!logFile.exists())
            {
                System.out.println("Log file does not exist: " + logFile.getAbsolutePath());
                try
                {
                    final Path file = Files.createFile(logFile.toPath());
                    System.out.println("Log file created: " + file.toFile().getAbsolutePath());
                }
                catch (final IOException | SecurityException | UnsupportedOperationException e)
                {
                    System.out.println("Error occurred while creating log file: " + e.getMessage());
                }
            }

            addHandler(new ConsoleHandler());

            try (final InputStream inputStream = new FileInputStream(TimeTracker.getHome() + Constants.DEFAULT_PROPERTIES))
            {
                final Map<String, String> settings = Backend.getInstance().getSettings();
                final String logLevel = Optional.ofNullable(settings.get(Backend.SETTING_LOG_LEVEL)).orElse(null);
                final LogManager manager = LogManager.getLogManager();
                manager.readConfiguration(inputStream);

                final FileHandler fileHandler = new FileHandler(logFile.getAbsolutePath(), true);
                fileHandler.setFormatter(new LogFormatter());
                addHandler(fileHandler);

                final Properties properties = TimeTracker.getProperties();
                final Handler[] handlers = getHandlers();
                for(final Handler handler : handlers)
                {
                    String level = properties.getProperty(handler.getClass().getName() + ".level", Level.INFO.getName());
                    if(handler instanceof FileHandler)
                    {
                        level = Optional.ofNullable(logLevel).orElse(level);
                        super.setLevel(handler.getLevel());
                    }
                    handler.setLevel(Level.parse(level));
                }

                this.initialized = true;
            }
            catch (final IOException e)
            {
                super.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    public static File getLogFile()
    {
        final File file = new File(TimeTracker.getHome() + Constants.FOLDER_USERDATA + File.separator + Constants.LOGFILE_NAME);
        System.out.println("Log file: " + file.getAbsolutePath());
        return file;
    }

    public static void log(final Level level, final String msg)
    {
        logger.log(level, msg);
    }

    public static void log(final Level level, final String msg, final Object param)
    {
        logger.log(level, msg, param);
    }

    public static void log(final Level level, final String msg, final Object[] params)
    {
        logger.log(level, msg, params);
    }

    public static void log(final Level level, final String msg, final Throwable thrown)
    {
        logger.log(level, msg, thrown);
    }

    public static void severe(final String msg)
    {
        logger.severe(msg);
    }

    public static void severe(final String msg, final Throwable thrown)
    {
        logger.log(Level.SEVERE, msg, thrown);
    }

    public static void severe(final String msg, final Object param)
    {
        logger.log(Level.SEVERE, msg, param);
    }

    public static void warning(final String msg)
    {
        logger.warning(msg);
    }

    public static void warning(final String msg, final Object param)
    {
        logger.log(Level.WARNING, msg, param);
    }

    public static void warning(final String msg, final Object[] params)
    {
        logger.log(Level.WARNING, msg, params);
    }

    public static void info(final String msg)
    {
        logger.info(msg);
    }

    public static void info(final String msg, final Object param)
    {
        logger.log(Level.INFO, msg, param);
    }

    public static void info(final String msg, final Object[] params)
    {
        logger.log(Level.INFO, msg, params);
    }

    public static void fine(final String msg)
    {
        logger.fine(msg);
    }

    public static void fine(final String msg, final Object param)
    {
        logger.log(Level.FINE, msg, param);
    }

    public static void finer(final String msg)
    {
        logger.finer(msg);
    }

    public static void finest(final String msg)
    {
        logger.finest(msg);
    }

    public static void setLevel(final Level newLevel)
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