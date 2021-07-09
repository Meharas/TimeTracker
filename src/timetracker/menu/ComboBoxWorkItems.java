package timetracker.menu;

import timetracker.TimeTracker;
import timetracker.data.Issue;
import timetracker.data.WorkItemType;
import timetracker.db.Backend;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Combobox zur Anzeige der WorkItems beim Burnen
 */
public class ComboBoxWorkItems extends JComboBox<WorkItemType>
{
    public ComboBoxWorkItems()
    {
        this(null, true);
    }

    public ComboBoxWorkItems(final Issue issue)
    {
        this(issue, false);
    }

    private ComboBoxWorkItems(final Issue issue, final boolean addEmptyItem)
    {
        setMaximumSize(new Dimension(350, 100));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setBackground(TimeTracker.MANDATORY);
        setRenderer(TypeRenderer.getInstance());

        if(addEmptyItem)
        {
            addItem(WorkItemType.EMPTY);
        }

        final List<WorkItemType> workItemTypes = WorkItemType.getTypes();
        workItemTypes.forEach(this::addItem);

        if (issue != null)
        {
            setSelectedItem(issue.getType());
            if(issue.getType() == WorkItemType.EMPTY)
            {
                final Map<String, String> settings = Backend.getInstance().getSettings();
                final String id = Optional.ofNullable(settings).map(s -> s.get(Backend.SETTING_DEFAULT_WORKTYPE)).orElse(null);
                if(id != null && !id.isEmpty())
                {
                    setSelectedItem(WorkItemType.getType(id));
                }
                else
                {
                    setSelectedIndex(-1);
                }
            }
        }
    }
}