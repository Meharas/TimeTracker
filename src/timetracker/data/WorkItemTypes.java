package timetracker.data;

import timetracker.client.Client;
import timetracker.utils.Util;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Liefert die WorkItems zur Zeiterfassung
 */
public class WorkItemTypes
{
    private static final List<WorkItemType> WORKITEM_TYPES = new LinkedList<>();
    static
    {
        try
        {
            final Map<String, String> workItems = Client.getWorkItems();
            if (workItems != null)
            {
                final Map<String, String> result = workItems.entrySet().stream().sorted(Map.Entry.comparingByValue())
                                                                       .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                                 Map.Entry::getValue,
                                                                                                 (e1, e2) -> e1,
                                                                                                 LinkedHashMap::new));
                for(final Map.Entry<String, String> entry : result.entrySet())
                {
                    WORKITEM_TYPES.add(new WorkItemType(entry.getKey(), entry.getValue()));
                }
            }
        }
        catch (final Exception e)
        {
            Util.handleException(e);
        }
    }

    private WorkItemTypes()
    {
    }

    public static List<WorkItemType> get()
    {
        return WORKITEM_TYPES;
    }
}
