package timetracker.menu;

import timetracker.data.State;

import javax.swing.*;
import java.util.Set;

/**
 * ComboBox für die Zustände
 */
public final class ComboBoxStates extends JComboBox<String>
{
    private static final long serialVersionUID = -8972701259329543629L;

    public ComboBoxStates()
    {
        final Set<String> names = State.getNames();
        for (final String name : names)
        {
            addItem(name);

            if (State.TO_VERIFY.getName().equalsIgnoreCase(name))
            {
                setSelectedItem(name);
            }
        }
    }
}