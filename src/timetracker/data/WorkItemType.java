package timetracker.data;

import timetracker.Constants;
import timetracker.Resource;
import timetracker.client.Client;
import timetracker.utils.Util;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Enum für den Typ eines Tickets, Development oder Meeting
 */
public class WorkItemType implements Serializable
{
    private static final List<WorkItemType> WORKITEM_TYPES = new LinkedList<>();
    static
    {
        try
        {
            final Map<String, String> result = Util.sortByValue(Client.getWorkItems());
            for(final Map.Entry<String, String> entry : result.entrySet())
            {
                WORKITEM_TYPES.add(new WorkItemType(entry.getKey(), entry.getValue()));
            }
        }
        catch (final Exception e)
        {
            Util.handleException(e);
        }
    }

    public static final WorkItemType EMPTY = new WorkItemType();
    private final String id;
    private final String label;

    public WorkItemType()
    {
        this.id = Constants.STRING_EMPTY;
        this.label = "-";
    }

    public WorkItemType(final String id, final String label)
    {
        this.id = id;
        this.label = label;
    }

    public String getId()
    {
        return this.id;
    }

    public String getLabel()
    {
        return this.label;
    }

    public static WorkItemType getType(final String id)
    {
        return WORKITEM_TYPES.stream().filter(item -> item.getId().equalsIgnoreCase(id)).findFirst().orElse(WorkItemType.EMPTY);
    }

    public String getTooltip()
    {
        String key = this.label;
        final int indexSpace = key.indexOf(Constants.STRING_SPACE);
        if(indexSpace > -1)
        {
            key = key.substring(0, indexSpace);
        }
        return Resource.getString("workitem.tooltip." + key.toLowerCase());
    }

    public static List<WorkItemType> getTypes()
    {
        return WORKITEM_TYPES;
    }

    @Override
    public String toString()
    {
        return "WorkItemType{id='" + this.id + "', label='" + this.label + '}';
    }
}