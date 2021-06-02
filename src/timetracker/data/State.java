package timetracker.data;

import java.util.HashSet;
import java.util.Set;

/**
 * Abbildung der Zustände
 */
public enum State
{
    VERIFIED ("Verified"),
    VERIFIED_OBSOLETE ("Verified obsolete"),
    DUBLICATE ("Duplicate"),
    TO_VERIFY ("To verify");

    private final String name;
    State(final String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public static Set<String> getNames()
    {
        final State[] values = values();
        final Set<String> names = new HashSet<>(values.length);
        for(final State state : values)
        {
            names.add(state.getName());
        }
        return names;
    }
}