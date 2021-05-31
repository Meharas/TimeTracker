import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Liefert die Versionen
 *
 * @author $Author: beyera $ &copy; forcont business technology gmbh 2001-2019
 * @version $Revision: 1.0 $ $Date: 04.02.2019 10:02 $
 * @since 7.0
 */
final class Versions
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
        catch (IOException ex)
        {
            Log.severe(ex.getMessage(), ex);
        }
    }

    static Map<String, String> get()
    {
        return VERSION_MAP;
    }

    static String getSelectedVersion()
    {
        return selectedVersion;
    }
}