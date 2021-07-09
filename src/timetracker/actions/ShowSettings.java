package timetracker.actions;

import timetracker.Constants;
import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.data.WorkItemType;
import timetracker.db.Backend;
import timetracker.menu.ComboBoxWorkItems;
import timetracker.utils.EscapeEvent;
import timetracker.utils.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * Zeigt das Einstellungspanel an
 */
public class ShowSettings implements ActionListener
{
    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final JDialog dialog = new JDialog(TimeTracker.getInstance(), Resource.getString(PropertyConstants.MENU_SETTINGS), true);
        dialog.setResizable(false);
        EscapeEvent.add(dialog);

        final JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        dialog.add(panel);

        final Map<String, String> settings = Backend.getInstance().getSettings();
        for(final Map.Entry<String, String> entry : settings.entrySet())
        {
            final String label = Resource.getString("settings.label." + entry.getKey().toLowerCase());
            switch (entry.getKey())
            {
                case Backend.SETTING_ALWAYS_ON_TOP:
                {
                    final JLabel jLabel = new JLabel(String.format("<html><b>%s</b></html>", label));
                    jLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                    final JLabel desc = new JLabel(Resource.getString("settings.desc." + entry.getKey().toLowerCase()));
                    desc.setAlignmentX(Component.LEFT_ALIGNMENT);

                    final JCheckBox box = new JCheckBox(label, Boolean.parseBoolean(entry.getValue()));
                    box.addActionListener(listener -> settings.put(Backend.SETTING_ALWAYS_ON_TOP, Boolean.toString(box.isSelected())));
                    box.setAlignmentX(Component.LEFT_ALIGNMENT);
                    panel.add(jLabel);
                    panel.add(desc);
                    panel.add(new JPanel());
                    panel.add(box);
                    panel.add(new JPanel());
                    break;
                }
                case Backend.SETTING_DEFAULT_WORKTYPE:
                {
                    final JLabel jLabel = new JLabel(String.format("<html><b>%s</b></html>", label));
                    jLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                    final JLabel desc = new JLabel(Resource.getString("settings.desc." + entry.getKey().toLowerCase()));
                    desc.setAlignmentX(Component.LEFT_ALIGNMENT);

                    final ComboBoxWorkItems workItems = new ComboBoxWorkItems();
                    workItems.addActionListener(e13 -> {
                        final String value = Optional.ofNullable(workItems.getSelectedItem())
                                                     .map(WorkItemType.class::cast)
                                                     .map(WorkItemType::getId).orElse(Constants.STRING_EMPTY);
                        settings.put(Backend.SETTING_DEFAULT_WORKTYPE, value);
                    });

                    workItems.setAlignmentX(Component.LEFT_ALIGNMENT);
                    workItems.setPreferredSize(new Dimension(350,35));
                    final String value = entry.getValue();
                    if(value != null && !value.isEmpty())
                    {
                        final WorkItemType type = WorkItemType.getType(value);
                        workItems.setSelectedItem(type);
                    }
                    else
                    {
                        workItems.setSelectedIndex(-1);
                    }
                    panel.add(new JPanel());
                    panel.add(jLabel);
                    panel.add(desc);
                    panel.add(workItems);
                }
            }
        }

        final JButton save = new JButton(Resource.getString(PropertyConstants.LABEL_SAVE));
        save.addActionListener(listener -> {
            try
            {
                Backend.getInstance().saveSettings(settings);
                dialog.dispose();
                TimeTracker.getInstance().setAlwaysOnTop(Boolean.parseBoolean(settings.get(Backend.SETTING_ALWAYS_ON_TOP)));
            }
            catch (final SQLException ex)
            {
                Util.handleException(ex);
            }
        });

        final JButton cancel = new JButton(Resource.getString(PropertyConstants.TEXT_CANCEL));
        cancel.addActionListener(listener -> dialog.dispose());

        final JPanel btnPanel = new JPanel();
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPanel.add(save);
        btnPanel.add(cancel);
        panel.add(btnPanel);

        final Dimension size = new Dimension(400, 250);
        final Point location = Util.getWindowLocation(size);
        dialog.setBounds(location.x, location.y, size.width, size.height);
        dialog.setVisible(true);
    }
}