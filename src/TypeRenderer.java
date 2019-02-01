import javax.swing.*;
import java.awt.*;

/**
 * Renderer für die Combobox
 *
 * @author $Author: beyera $ &copy; forcont business technology gmbh 2001-2019
 * @version $Revision: 1.0 $ $Date: 01.02.2019 11:02 $
 * @since 7.0
 */
public class TypeRenderer extends DefaultListCellRenderer
{
    private static final long serialVersionUID = -4094337354229283799L;

    @Override
    public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus)
    {
        final Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value != null)
        {
            setText(((String[]) value)[1]);
        }
        return component;
    }
}