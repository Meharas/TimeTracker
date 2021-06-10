package timetracker.data;

import timetracker.Constants;

import java.util.List;

/**
 * Enum für den Typ eines Tickets, Development oder Meeting
 */
public class WorkItemType
{
    public static final WorkItemType EMPTY = new WorkItemType();
    private final String id;
    private final String label;

    public WorkItemType()
    {
        this.id = Constants.STRING_EMPTY;
        this.label = Constants.STRING_EMPTY;
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
        final List<WorkItemType> workItemTypes = WorkItemTypes.get();
        return workItemTypes.stream().filter(item -> item.getId().equalsIgnoreCase(id)).findFirst().orElse(WorkItemType.EMPTY);
    }

    @Override
    public String toString()
    {
        return "WorkItemType{id='" + this.id + "', label='" + this.label + '}';
    }
}