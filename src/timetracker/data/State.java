package timetracker.data;

import timetracker.client.Client;
import timetracker.utils.Util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Abbildung der Zustände
 */
public class State
{
    private static final Map<String, String> STATES = new LinkedHashMap<>();
    static
    {
        try
        {
            final Map<String, String> states = Util.sortByValue(Client.getStates());
            for(final Map.Entry<String, String> entry : states.entrySet())
            {
                STATES.put(entry.getKey(), entry.getValue());
            }
        }
        catch (final Exception e)
        {
            Util.handleException(e);
        }
    }

    private final String name;
    State(final String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public static Collection<String> getNames()
    {
        return STATES.values();
    }
}