import java.util.regex.Pattern;

/**
 * Konstanten
 *
 * @author $Author: beyera $ &copy; forcont business technology gmbh 2001-2019
 * @version $Revision: 1.0 $ $Date: 04.02.2019 13:09 $
 * @since 7.0
 */
final class TimeTrackerConstants
{
    TimeTrackerConstants()
    {
    }

    static final Pattern PATTERN = Pattern.compile("([SEP-|DEV-|FS-]+[0-9]+).*");
    static final Pattern TIME_PATTERN = Pattern.compile(".*([0-9]+)h ([0-9]+)min ([0-9]+)s");
    static final Pattern DURATION_PATTERN = Pattern.compile("(\\d{2})h (\\d{2})min (\\d{2})s");

    static final String PROPERTIES = TimeTracker.class.getSimpleName() + ".properties";
    static final String DEFAULT_PROPERTIES = TimeTracker.class.getSimpleName() + ".default.properties";
    static final String LOGFILE_NAME = "TimeTracker.log";
    static final String PREFIX_BUTTON = "button.";
    static final String SUFFIX_LABEL = ".label";
    static final String SUFFIX_ICON = ".icon";
    static final String SUFFIX_TICKET = ".ticket";
    static final String SUFFIX_TYPE = ".type";
    static final String SUFFIX_TIME = ".time";
    static final String SUFFIX_DURATION = ".duration";
    static final String ACTIONMAP_KEY_CANCEL = "Cancel";
    static final String ISSUE_SUMMARY = "summary";
    static final String ISSUE_STATE = "State";
    static final String ISSUE_VALUE_STATE_PROGRESS = "In Progress";
    static final String ISSUE_VALUE_STATE_VERIFY = "To verify";
    static final String ISSUE_FIX_VERSIONS = "Fix versions";
    static final String ISSUE_CUSTOM_FIELDS = "fields(projectCustomField(field(name)),value(name))";

    static final String YOUTRACK_SCHEME = "youtrack.scheme";
    static final String YOUTRACK_PORT = "youtrack.port";
    static final String YOUTRACK_HOST = "youtrack.host";
    static final String YOUTRACK_USERID = "youtrack.userid";
    static final String YOUTRACK_TOKEN = "youtrack.token";
    static final String YOUTRACK_TOKEN_PREFIX = "perm:";

    static final String DEFAULT_HOST = "youtrack";
    static final String DEFAULT_SCHEME = "http";
    static final String DEFAULT_PORT = "80";

    static final String STRING_EMPTY = "";
    static final String STRING_SPACE = " ";
    static final String FIELDS = "fields";
    static final String NO_CACHE = "no-cache";
    static final String MIMETYPE_JSON = "application/json";


    static final String ENTITY = "{" +
                                 "\"date\":%d," +
                                 "\"author\": {\"id\":\"%s\"}," +
                                 "\"duration\":{\"presentation\":\"%s\"}," +
                                 "\"type\":{\"id\":\"%s\"}," +
                                 "\"text\":\"%s\"}";

    static final String ISSUE_COMMAND = "{\"query\":\"%s: %s\",\n" +
                                        " \"issues\":[{\"id\":\"%s\"}],\n" +
                                        " \"silent\":false}";

    static final String ISSUE_COMMENT = "{\"text\": \"%s\"}";
}