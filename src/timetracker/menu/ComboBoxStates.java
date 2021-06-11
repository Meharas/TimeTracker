package timetracker.menu;

import timetracker.data.State;

import javax.swing.*;

/**
 * ComboBox f�r die Zust�nde
 */
public final class ComboBoxStates extends JComboBox<String>
{
    private static final long serialVersionUID = -8972701259329543629L;

    public ComboBoxStates()
    {
        State.getNames().forEach(this::addItem);
    }
}