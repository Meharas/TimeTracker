import java.io.IOException;
import java.util.logging.*;

/**
 * Logger
 *
 * @author $Author: beyera $ &copy; forcont business technology gmbh 2001-2019
 * @version $Revision: 1.0 $ $Date: 01.02.2019 10:49 $
 * @since 7.0
 */
public class Log extends Logger
{
    Log(final String name)
    {
        this(name, null);
    }

    Log(final String name, final String resourceBundleName)
    {
        super(name, resourceBundleName);
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

        setLevel(Level.INFO);

        try
        {
            final LogManager manager = LogManager.getLogManager();
            manager.readConfiguration(TimeTracker.class.getResourceAsStream(TimeTrackerConstants.DEFAULT_PROPERTIES));

            final FileHandler fileHandler = new FileHandler(TimeTrackerConstants.LOGFILE_NAME, true);
            fileHandler.setFormatter(new LogFormatter());
            addHandler(fileHandler);
        }
        catch (IOException e)
        {
            log(Level.SEVERE, e.getMessage(), e);
        }
    }
}