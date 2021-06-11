package timetracker.menu;

import timetracker.data.FixVersion;

import javax.swing.*;
import java.util.List;

/**
 * Combobox für die Fix Versions
 */
public final class ComboBoxFixVersions extends JComboBox<String>
{
    private static final long serialVersionUID = 834330093501136441L;

    public ComboBoxFixVersions()
    {
        final List<String> versions = FixVersion.getNames();
        versions.forEach(this::addItem);
    }
}