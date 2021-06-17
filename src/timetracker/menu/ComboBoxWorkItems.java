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
        setRenderer(new TypeRenderer());

        final String type = issue.getType().getId();
        final List<WorkItemType> workItemTypes = WorkItemType.getTypes();

        for (final WorkItemType t : workItemTypes)
        {
            addItem(t);
            if (t.getId().equalsIgnoreCase(type))
            {
                setSelectedItem(t);
            }
        }
    }
}
