package timetracker.updates;

import timetracker.Constants;
import timetracker.TimeTracker;
import timetracker.log.Log;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Kopiert nutzerabhängige Dateien ins User-Verzeichnis
 */
public class CopyUserFiles implements IUpdateMethod
{
    @Override
    public int getUpdateId()
    {
        return 0;
    }

    @Override
    public boolean isPreBackendUpdate()
    {
        return true;
    }

    @Override
    public boolean doUpdate()
    {
        final List<File> filesToMove = new LinkedList<>();
        final File targetFolder = new File(TimeTracker.HOME + Constants.FOLDER_USERDATA);
        if(!targetFolder.exists())
        {
            targetFolder.mkdir();
        }

        final File logFolder = new File(TimeTracker.getHome() + "log");
        final File[] logFiles = logFolder.listFiles((dir, name) -> name.startsWith("TimeTracker.log"));
        if(logFiles != null && logFiles.length > 0)
        {
            filesToMove.addAll(Arrays.asList(logFiles));
        }

        final File homeFolder = new File(TimeTracker.getHome());
        final File[] propertyFiles = homeFolder.listFiles((dir, name) -> "TimeTracker.properties".equalsIgnoreCase(name));
        if(propertyFiles != null && propertyFiles.length > 0)
        {
            filesToMove.addAll(Arrays.asList(propertyFiles));
        }

        final File dbFolder = new File(TimeTracker.getHome() + "db");
        final File[] dbFiles = dbFolder.listFiles((dir, name) -> name.startsWith("timetrackerDB"));
        if(dbFiles != null && dbFiles.length > 0)
        {
            filesToMove.addAll(Arrays.asList(dbFiles));
        }

        boolean result = true;
        if(!filesToMove.isEmpty())
        {
            final Map<Level, String> logMessages = new HashMap<>();
            for(final File file : filesToMove)
            {
                final String fileName = file.getName();
                final File destination = new File(targetFolder.getAbsolutePath() + File.separator + fileName);
                if(file.renameTo(destination))
                {
                    logMessages.put(Level.INFO, String.format("File %s successfully moved to %s", file.getAbsolutePath(), destination.getAbsolutePath()));
                }
                else
                {
                    logMessages.put(Level.SEVERE, String.format("Moving file %s to %s failed.", file.getAbsolutePath(), destination.getAbsolutePath()));
                    result = false;
                }
            }
            for(final Map.Entry<Level, String> messages : logMessages.entrySet())
            {
                Log.log(messages.getKey(), messages.getValue());
            }
        }

        return result;
    }
}