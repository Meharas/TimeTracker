import javax.swing.*;
import java.util.Map;

/**
 * Combobox für die Fix Versions
 *
 * @author $Author: beyera $ &copy; forcont business technology gmbh 2001-2019
 * @version $Revision: 1.0 $ $Date: 04.02.2019 13:32 $
 * @since 7.0
 */
final class ComboBoxFixVersions extends JComboBox<String>
{
    private static final long serialVersionUID = 834330093501136441L;

    ComboBoxFixVersions()
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