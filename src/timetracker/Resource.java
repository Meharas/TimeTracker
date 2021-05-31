import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Zugriff auf das ResourceBundle
 *
 * @author $Author: beyera $ &copy; forcont business technology gmbh 2001-2019
 * @version $Revision: 1.0 $ $Date: 08.02.2019 10:55 $
 * @since 7.0
 */
public final class Resource
{
    private static final ResourceBundle bundle = ResourceBundle.getBundle("TimeTracker", new Locale((String) System.getProperties().get("user.language")));

    private Resource()
    {
    }

    /**
     * Liefert einen String aus dem ResourceBundle
     * @param key Schlüssel
     * @return String aus dem ResourceBundle
     */
    static String getString(final String key)
    {
        return getString(key, (String) null);
    }

    /**
     * Liefert einen String aus dem ResourceBundle
     * @param key Schlüssel
     * @param args Argumente
     * @return String aus dem ResourceBundle
     */
    static String getString(final String key, final String... args)
    {
        final String value = bundle.getString(key);
        if (args == null)
        {
            return value;
        }
        return MessageFormat.format(value, args);
    }
}