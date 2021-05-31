package timetracker;

/**
 * Pfade
 */
public enum ServicePath
{
    ISSUE("api/issues/%s"),
    COMMENT(ISSUE.restEndPoint + "/comments"),
    WORKITEM(ISSUE.restEndPoint + "/timeTracking/workItems"),
    COMMAND("api/commands"),
    URL("issue/%s"),
    USER("api/admin/users/me");

    public final String restEndPoint;

    ServicePath(final String path)
    {
        this.restEndPoint = path;
    }
}
