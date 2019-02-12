/**
 * Pfade
 *
 * @author $Author: beyera $ © forcont business technology gmbh 2001-2019
 * @version $Revision: 1.0 $$Date: 01.02.2019 14:40 $
 */
public enum ServicePath
{
    ISSUE("api/issues/%s"),
    COMMENT(ISSUE.restEndPoint + "/comments"),
    WORKITEM(ISSUE.restEndPoint + "/timeTracking/workItems"),
    COMMAND("api/commands"),
    URL("issue/%s"),
    USER("api/admin/users/me");

    String restEndPoint;

    ServicePath(final String path)
    {
        this.restEndPoint = path;
    }
}
