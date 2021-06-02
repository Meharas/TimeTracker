package timetracker;

import timetracker.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Liefert die Versionen
 */
public final class Versions
{
    private Versions()
    {

    }

    private static String selectedVersion = null;
    private static final Map<String, String> VERSION_MAP = new TreeMap<>();
    static
    {
        try(final InputStream versions = TimeTracker.class.getResourceAsStream("Versions.properties"))
        {
            final Properties versionProperties = new Properties();
            versionProperties.load(versions);

            for (final Map.Entry<Object, Object> entry : versionProperties.entrySet())
            {
                final String key = (String) entry.getKey();
                final String value = (String) entry.getValue();
                if(key.startsWith("version."))
                {
                    VERSION_MAP.put(key, value);
                }
                else if("selected.version".equalsIgnoreCase(key))
                {
                    selectedVersion = value;
                }
            }
        }
        catch (final IOException ex)
        {
            Log.severe(ex.getMessage(), ex);
        }
    }

    public static Map<String, String> get()
    {
        return VERSION_MAP;
    }

    public static String getSelectedVersion()
    {
        return selectedVersion;
    }
}