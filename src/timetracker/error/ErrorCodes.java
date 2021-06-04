package timetracker.error;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Fehlercodes
 */
public final class ErrorCodes
{
    private static final ResourceBundle bundle = ResourceBundle.getBundle("timetracker.error.ErrorCodes", new Locale((String) System.getProperties().get("user.language")));

    public static final String ERROR_ISSUE_EXISTS = "1001";

    /**
     * Liefert einen String aus dem ResourceBundle
     * @param key Schlüssel
     * @param args Argumente
     * @return String aus dem ResourceBundle
     */
    public static String getString(final String key, final String... args)
    {
        final String value = bundle.getString(key);
        if (args == null)
        {
            return value;
        }
        return MessageFormat.format(value, args);
    }
}