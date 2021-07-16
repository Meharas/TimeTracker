package timetracker.dialogs;

import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.db.Backend;
import timetracker.misc.CheckboxSettingsPanel;
import timetracker.misc.LogLevelSettingsPanel;
import timetracker.misc.WorkItemsSettingsPanel;
import timetracker.utils.EscapeEvent;
import timetracker.utils.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.Map;

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
        panel.add(new CheckboxSettingsPanel(settings, Backend.SETTING_ALWAYS_ON_TOP, true));
        addSeparator(panel);
        panel.add(new CheckboxSettingsPanel(settings, Backend.SETTING_START_MINIMIZED, false));
        addSeparator(panel);
        panel.add(new CheckboxSettingsPanel(settings, Backend.SETTING_ENABLE_DRAGNDROP, true));
        addSeparator(panel);
        panel.add(new WorkItemsSettingsPanel(settings));
        addSeparator(panel);
        panel.add(new LogLevelSettingsPanel(settings));

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
        btnPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        btnPanel.add(save);
        btnPanel.add(cancel);
        panel.add(btnPanel);

        final Dimension panelSize = panel.getPreferredSize();
        final Dimension btnPanelSize = btnPanel.getPreferredSize();
        final Dimension size = new Dimension(panelSize.width, panelSize.height + btnPanelSize.height);
        final Point location = Util.getWindowLocation(size);
        setBounds(location.x, location.y, size.width, size.height);
    }

    private void addSeparator(final JPanel panel)
    {
        final JSeparator separator = new JSeparator();
        separator.setBorder(new EmptyBorder(0,0,5,0));
        panel.add(separator);
    }
}