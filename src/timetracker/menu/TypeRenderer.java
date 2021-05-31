package timetracker.menu;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer für die Combobox
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