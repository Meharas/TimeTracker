package timetracker.data;

import timetracker.Constants;

/**
 * Enum für den Typ eines Tickets, Development oder Meeting
 */
public enum Type
{
    EMPTY(Constants.STRING_EMPTY, Constants.STRING_EMPTY),
    DEVELOPMENT ("136-0", "Development"),
    TESTING ("136-1", "Testing"),
    DOCUMENTATION ("136-2", "Documentation"),
    MEETING ("136-3", "Meeting"),
    SUPPORT ("136-4", "Support"),
    DESIGN ("136-6", "Design");

    private final String id;
    private final String label;
    Type(final String id, final String label)
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

    public static Type getType(final String id)
    {
        for (final Type type : values())
        {
            if(type.id.equalsIgnoreCase(id))
            {
                return type;
            }
        }
        return null;
    }

    @Override
    public String toString()
    {
        return "Type{id='" + id + "', label='" + label + '}';
    }
}