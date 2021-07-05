package timetracker;

import timetracker.utils.Util;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Zugriff auf das ResourceBundle
 */
public final class Resource
{
    private static final ResourceBundle bundle = ResourceBundle.getBundle("timetracker.TimeTracker", Util.getLocale());

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
    public static String getString(final String key, final String... args)
    {
        final String value;
        try
        {
            value = bundle.getString(key);
        }
        catch (final MissingResourceException e)
        {
            return Constants.STRING_EMPTY;
        }
        if (args == null)
        {
            return value;
        }
        return MessageFormat.format(value, (Object[]) args);
    }
}
