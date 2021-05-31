package timetracker.menu;

import timetracker.Versions;

import javax.swing.*;
import java.util.Map;

/**
 * Combobox für die Fix Versions
 */
public final class ComboBoxFixVersions extends JComboBox<String>
{
    private static final long serialVersionUID = 834330093501136441L;

    public ComboBoxFixVersions()
    {
        final Map<String, String> versions = Versions.get();
        for(final Map.Entry<String, String> entry : versions.entrySet())
        {
            final String value = entry.getValue();
            addItem(value);

            if(entry.getKey().equalsIgnoreCase(Versions.getSelectedVersion()))
            {
                setSelectedItem(value);
            }
        }
    }
}