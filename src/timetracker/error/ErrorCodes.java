package timetracker.error;

import timetracker.utils.Util;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Fehlercodes
 */
public final class ErrorCodes
{
    private static final ResourceBundle bundle = ResourceBundle.getBundle("timetracker.error.ErrorCodes", Util.getLocale());

    public static final String ERROR_ISSUE_EXISTS = "1001";
    public static final String ERROR_ISSUE_NOT_EXISTS = "1002";
    public static final String ERROR_RESPONSE_STATUS_UNKNOWN = "1050";
    public static final String ERROR_DRAG_N_DROP = "1101";

    private ErrorCodes()
    {
    }

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
        return MessageFormat.format(value, (Object[]) args);
    }
}
