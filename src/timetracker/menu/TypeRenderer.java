package timetracker.menu;

import timetracker.data.WorkItemType;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer für die Combobox
 */
public class TypeRenderer extends DefaultListCellRenderer
{
    private static final long serialVersionUID = -4094337354229283799L;

    // Innere private Klasse, die erst beim Zugriff durch die umgebende Klasse initialisiert wird
    private static final class InstanceHolder
    {
        // Die Initialisierung von Klassenvariablen geschieht nur einmal und wird vom ClassLoader implizit synchronisiert
        private static final TypeRenderer instance = new TypeRenderer();
    }

    public static TypeRenderer getInstance()
    {
        return TypeRenderer.InstanceHolder.instance;
    }

    @Override
    public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus)
    {
        final Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value != null)
        {
            final WorkItemType item = (WorkItemType) value;
            setText(item.getLabel());

            final String tooltip = item.getTooltip();
            if (tooltip != null && !tooltip.isEmpty())
            {
                list.setToolTipText(tooltip);
            }
        }
        return component;
    }
}