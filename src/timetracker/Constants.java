package timetracker;

import java.util.regex.Pattern;

/**
 * Konstanten
 */
public final class Constants
{
    private Constants()
    {
    }

    public static final Pattern PATTERN = Pattern.compile("(^[SEP|DEV|FS|DC]+-[0-9]+).*");
    public static final Pattern TIME_PATTERN = Pattern.compile(".*([0-9]+)h ([0-9]+)min ([0-9]+)s");
    public static final Pattern BURN_PATTERN = Pattern.compile(".*([0-9]+)h ([0-9]+)m");
    public static final Pattern DURATION_PATTERN = Pattern.compile("(\\d{2})h (\\d{2})min (\\d{2})s");
    public static final Pattern USER_ID_PATTERN = Pattern.compile("(\\d+-\\d+)");

    public static final String PROPERTIES = TimeTracker.class.getSimpleName() + ".properties";
    public static final String DEFAULT_PROPERTIES = TimeTracker.class.getSimpleName() + ".default.properties";
    public static final String LOGFILE_NAME = "TimeTracker.log";
    public static final String PREFIX_BUTTON = "button.";
    public static final String ACTIONMAP_KEY_CANCEL = "Cancel";
    public static final String ISSUE_SUMMARY = "summary";
    public static final String ISSUE_STATE = "State";
    public static final String ISSUE_VALUE_STATE_PROGRESS = "In Progress";
    public static final String ISSUE_FIX_VERSIONS = "Fix versions";
    public static final String ISSUE_CUSTOM_FIELDS = "fields(projectCustomField(field(name)),value(name))";

    public static final String YOUTRACK_SCHEME = "youtrack.scheme";
    public static final String YOUTRACK_PORT = "youtrack.port";
    public static final String YOUTRACK_HOST = "youtrack.host";
    public static final String YOUTRACK_USERID = "youtrack.userid";
    public static final String YOUTRACK_TOKEN = "youtrack.token";
    public static final String YOUTRACK_CERT = "youtrack.cert";
    public static final String YOUTRACK_TOKEN_PREFIX = "perm:";

    public static final String DEFAULT_HOST = "youtrack";
    public static final String DEFAULT_SCHEME = "http";

    public static final String STRING_EMPTY = "";
    public static final String STRING_SPACE = " ";
    public static final String FIELDS = "fields";
    public static final String NO_CACHE = "no-cache";
    public static final String MIMETYPE_JSON = "application/json";


    public static final String ENTITY = "{" +
                                        "\"date\":%d," +
                                        "\"author\": {\"id\":\"%s\"}," +
                                        "\"duration\":{\"presentation\":\"%s\"}," +
                                        "\"type\":{\"id\":\"%s\"}," +
                                        "\"text\":\"%s\"}";

    public static final String ISSUE_COMMAND = "{\"query\":\"%s: %s\",\n" +
                                               " \"issues\":[{\"id\":\"%s\"}],\n" +
                                               " \"silent\":false}";

    public static final String ISSUE_COMMENT = "{\"text\": \"%s\"}";
}