/**
 * Pfade
 *
 * @author $Author: beyera $ © forcont business technology gmbh 2001-2019
 * @version $Revision: 1.0 $$Date: 01.02.2019 14:40 $
 */
public enum Path
{
    WORKITEM("api/issues/%s/timeTracking/workItems"),
    USER("api/admin/users/me"),
    ISSUE("api/issues/%s"),
    COMMAND("api/commands"),
    URL("issue/%s");

    String restEndPoint;

    Path(final String path)
    {
        this.restEndPoint = path;
    }
}
