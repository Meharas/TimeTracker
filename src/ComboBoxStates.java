import javax.swing.*;
import java.util.Set;

/**
 * ComboBox für die Zustände
 *
 * @author $Author: beyera $ &copy; forcont business technology gmbh 2001-2019
 * @version $Revision: 1.0 $ $Date: 04.02.2019 13:35 $
 * @since 7.0
 */
final class ComboBoxStates extends JComboBox<String>
{
    private static final long serialVersionUID = -8972701259329543629L;

    ComboBoxStates()
    {
        final Set<String> names = State.getNames();
        for (final String name : names)
        {
            addItem(name);

            if (State.TO_VERIFY.getName().equalsIgnoreCase(name))
            {
                setSelectedItem(name);
            }
        }
    }
}