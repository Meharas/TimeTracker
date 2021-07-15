package timetracker.dialogs;

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
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Dialog zur Darstellung der möglichen Einstellungen
 */
public class ShowSettingsDialog extends JDialog
{
    public ShowSettingsDialog()
    {
        super(TimeTracker.getInstance(), Resource.getString(PropertyConstants.MENU_SETTINGS), true);

        setResizable(false);
        EscapeEvent.add(this);

        final JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        add(panel);

        final Map<String, String> settings = Backend.getInstance().getSettings();
        addCheckbox(panel, settings, Backend.SETTING_ALWAYS_ON_TOP, true);
        addCheckbox(panel, settings, Backend.SETTING_START_MINIMIZED, false);
        addCheckbox(panel, settings, Backend.SETTING_ENABLE_DRAGNDROP, true);
        addWorkTypePanel(panel, settings);
        addLogLevelComboBox(panel, settings);

        final JButton save = new JButton(Resource.getString(PropertyConstants.LABEL_SAVE));
        save.addActionListener(listener -> {
            try
            {
                Backend.getInstance().saveSettings(settings);
                ShowSettingsDialog.this.dispose();
            }
            catch (final SQLException ex)
            {
                Util.handleException(ex);
            }
        });

        final JButton cancel = new JButton(Resource.getString(PropertyConstants.TEXT_CANCEL));
        cancel.addActionListener(listener -> dispose());

        final JPanel btnPanel = new JPanel();
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        btnPanel.add(save);
        btnPanel.add(cancel);
        panel.add(btnPanel);

        final Dimension panelSize = panel.getPreferredSize();
        final Dimension btnPanelSize = btnPanel.getPreferredSize();
        final Dimension size = new Dimension(panelSize.width, panelSize.height + btnPanelSize.height);
        final Point location = Util.getWindowLocation(size);
        setBounds(location.x, location.y, size.width, size.height);
    }

    private void addWorkTypePanel(final JPanel panel, final Map<String, String> settings)
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
        panel.add(new JPanel());
        panel.add(jLabel);
        panel.add(desc);
        panel.add(workItems);
    }

    private void addLogLevelComboBox(final JPanel panel, final Map<String, String> settings)
    {
        final String label = Resource.getString("settings.label." + Backend.SETTING_LOG_LEVEL.toLowerCase());
        final JLabel jLabel = new JLabel(String.format("<html><b>%s</b></html>", label));
        jLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel desc = new JLabel(Resource.getString("settings.desc." + Backend.SETTING_LOG_LEVEL.toLowerCase()));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JComboBox<String> logLevel = new JComboBox<>();
        logLevel.setAlignmentX(Component.LEFT_ALIGNMENT);

        logLevel.addItem(Level.WARNING.getName());
        logLevel.addItem(Level.SEVERE.getName());
        logLevel.addItem(Level.INFO.getName());
        logLevel.addItem(Level.FINER.getName());
        logLevel.addItem(Level.FINEST.getName());
        logLevel.addItem(Level.ALL.getName());

        logLevel.addActionListener(e -> {
            final String value = Optional.ofNullable(logLevel.getSelectedItem()).map(String.class::cast).orElse(Level.INFO.getName());
            settings.put(Backend.SETTING_LOG_LEVEL, value);
        });

        final String value = settings.get(Backend.SETTING_LOG_LEVEL);
        if (value != null && !value.isEmpty())
        {
            final Level level = Level.parse(value);
            logLevel.setSelectedItem(level.getName());
        }
        else
        {
            logLevel.setSelectedIndex(-1);
        }
        panel.add(new JPanel());
        panel.add(jLabel);
        panel.add(desc);
        panel.add(logLevel);
    }

    private void addCheckbox(final JPanel panel, final Map<String, String> settings, final String setting, boolean value)
    {
        final String label = Resource.getString("settings.label." + setting.toLowerCase());
        final JLabel jLabel = new JLabel(String.format("<html><b>%s</b></html>", label));
        jLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel desc = new JLabel(Resource.getString("settings.desc." + setting.toLowerCase()));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        final String settingsValue = settings.get(setting);
        if(settingsValue != null && !settingsValue.isEmpty())
        {
            value = Boolean.parseBoolean(settingsValue);
        }
        else
        {
            settings.put(setting, Boolean.toString(value));
        }

        final JCheckBox box = new JCheckBox(label, value);
        box.addActionListener(listener -> settings.put(setting, Boolean.toString(box.isSelected())));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(jLabel);
        panel.add(desc);
        panel.add(new JPanel());
        panel.add(box);
        panel.add(new JPanel());
    }
}