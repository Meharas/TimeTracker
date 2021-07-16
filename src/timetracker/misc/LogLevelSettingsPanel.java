package timetracker.misc;

import timetracker.Resource;
import timetracker.db.Backend;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Panel für die Anzeige des LogLevels in den Einstellungen
 */
public class LogLevelSettingsPanel extends JPanel
{
    public LogLevelSettingsPanel(final Map<String, String> settings)
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

        add(jLabel);
        add(desc);
        add(logLevel);

        setBorder(new EmptyBorder(10, 0, 10, 0));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }
}