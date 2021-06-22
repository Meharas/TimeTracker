package timetracker;

/**
 * Pfade
 */
public enum ServicePath
{
    ISSUE("api/issues/%s"),
    COMMENT(ISSUE.restEndPoint + "/comments"),
    WORKITEMTYPES("api/admin/projects/%s/timeTrackingSettings"),
    WORKITEM(ISSUE.restEndPoint + "/timeTracking/workItems"),
    PROJECTS("api/admin/projects"),
    COMMAND("api/commands"),
    URL("issue/%s"),
    STATES("api/admin/customFieldSettings/bundles/state/34-0/values"),
    FIX_VERSIONS("api/admin/customFieldSettings/bundles/version/40-7/values"),
    USER("api/admin/users/me");

    public final String restEndPoint;

    ServicePath(final String path)
    {
        this.restEndPoint = path;
    }
}