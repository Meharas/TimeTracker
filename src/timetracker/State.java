import java.util.HashSet;
import java.util.Set;

/**
 * Abbildung der Zustände
 *
 * @author $Author: beyera $ &copy; forcont business technology gmbh 2001-2019
 * @version $Revision: 1.0 $ $Date: 04.02.2019 13:00 $
 * @since 7.0
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