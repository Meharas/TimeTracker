package timetracker.data;

import timetracker.client.Client;
import timetracker.utils.Util;

import java.util.*;

/**
 * Abbildung der Fix Versions
 */
public final class FixVersion
{
    private static final List<String> FIX_VERSIONS = new LinkedList<>();
    static
    {
        try
        {
            final Map<String, String> fixVersions = Util.sortByValue(Client.getFixVersions());
            FIX_VERSIONS.addAll(fixVersions.values());
            Collections.reverse(FIX_VERSIONS);
        }
        catch (final Exception e)
        {
            Util.handleException(e);
        }
    }

    private FixVersion()
    {
    }

    public static List<String> getNames()
    {
        return FIX_VERSIONS;
    }
}
