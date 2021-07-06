package timetracker.menu;

import timetracker.TimeTracker;
import timetracker.data.Issue;
import timetracker.data.WorkItemType;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Combobox zur Anzeige der WorkItems beim Burnen
 */
public class ComboBoxWorkItems extends JComboBox<WorkItemType>
{
    public ComboBoxWorkItems(final Issue issue)
    {
        setMaximumSize(new Dimension(350, 100));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setBackground(TimeTracker.MANDATORY);
        setRenderer(TypeRenderer.getInstance());

        final List<WorkItemType> workItemTypes = WorkItemType.getTypes();
        workItemTypes.forEach(this::addItem);
        setSelectedItem(issue.getType());

        if(issue.getType() == WorkItemType.EMPTY)
        {
            setSelectedIndex(-1);
        }
    }
}