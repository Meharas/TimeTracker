package timetracker.misc;

import timetracker.Resource;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

/**
 * Panel für die Anzeige einer Einstellung mit Checkbox
 */
public class CheckboxSettingsPanel extends JPanel
{
    public CheckboxSettingsPanel(final Map<String, String> settings, final String setting, boolean value)
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

        add(jLabel);
        add(desc);
        add(box);

        setBorder(new EmptyBorder(10,0,10,0));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }
}