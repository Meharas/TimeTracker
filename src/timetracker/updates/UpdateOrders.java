package timetracker.updates;

import timetracker.data.Issue;
import timetracker.db.Backend;
import timetracker.utils.Util;

import java.util.List;

/**
 * Setzt initial alle Orders
 */
public class UpdateOrders implements IUpdateMethod
{
    @Override
    public int getUpdateId()
    {
        return 2;
    }

    @Override
    public boolean isPreBackendUpdate()
    {
        return false;
    }

    @Override
    public boolean doUpdate()
    {
        try
        {
            final Backend backend = Backend.getInstance();
            final List<Issue> issues = backend.getIssues();
            for(final Issue issue : issues)
            {
                backend.updateIssue(issue);
            }
            return true;
        }
        catch (final Throwable t)
        {
            Util.handleException(t);
        }
        return false;
    }
}
