package timetracker.updates;

import timetracker.Constants;
import timetracker.TimeTracker;
import timetracker.log.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                final Path source = Paths.get(file.toURI());
                final String fileName = file.getName();
                final Path destination = Paths.get(new File(targetFolder.getAbsolutePath() + File.separator + fileName).toURI());
                try
                {
                    Files.move(source, destination);
                }
                catch (final FileAlreadyExistsException e)
                {
                    try
                    {
                        Files.delete(destination);
                        Files.move(source, destination);
                    }
                    catch (final Exception ex)
                    {
                        logMessages.put(Level.SEVERE, String.format("Deleting file %s failed.", destination));
                        result = false;
                        continue;
                    }
                }
                catch (final IOException e)
                {
                    logMessages.put(Level.SEVERE, String.format("Moving file %s to %s failed.", file.getAbsolutePath(), destination));
                    result = false;
                    continue;
                }

                if("TimeTracker.properties".equalsIgnoreCase(fileName))
                {
                    //Zertifikat eintragen
                    TimeTracker.saveSetting(Constants.YOUTRACK_CERT, "cert.cer");
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