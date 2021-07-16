package timetracker.misc;

import timetracker.Constants;
import timetracker.Resource;
import timetracker.data.WorkItemType;
import timetracker.db.Backend;
import timetracker.menu.ComboBoxWorkItems;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;
import java.util.Optional;

/**
 * Panel zur Darstellung der WorkItems in den Einstellungen
 */
public class WorkItemsSettingsPanel extends JPanel
{
    public WorkItemsSettingsPanel(final Map<String, String> settings)
    {
        final String label = Resource.getString("settings.label." + Backend.SETTING_DEFAULT_WORKTYPE.toLowerCase());
        final JLabel jLabel = new JLabel(String.format("<html><b>%s</b></html>", label));
        jLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel desc = new JLabel(Resource.getString("settings.desc." + Backend.SETTING_DEFAULT_WORKTYPE.toLowerCase()));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        final ComboBoxWorkItems workItems = new ComboBoxWorkItems();
        workItems.setAlignmentX(Component.LEFT_ALIGNMENT);
        workItems.addActionListener(e -> {
            final String value = Optional.ofNullable(workItems.getSelectedItem())
                    .map(WorkItemType.class::cast)
                    .map(WorkItemType::getId).orElse(Constants.STRING_EMPTY);
            settings.put(Backend.SETTING_DEFAULT_WORKTYPE, value);
        });

        final String value = settings.get(Backend.SETTING_DEFAULT_WORKTYPE);
        if (value != null && !value.isEmpty())
        {
            final WorkItemType type = WorkItemType.getType(value);
            workItems.setSelectedItem(type);
        }
        else
        {
            workItems.setSelectedIndex(-1);
        }
        add(jLabel);
        add(desc);
        add(workItems);

        setBorder(new EmptyBorder(10, 0, 10, 0));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }
}