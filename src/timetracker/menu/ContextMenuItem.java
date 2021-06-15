package timetracker.menu;

import timetracker.buttons.ITimeTrackerButton;
import timetracker.buttons.BaseButton;
import timetracker.icons.Icon;

import javax.swing.*;

/**
 * Context menu item
 */
public class ContextMenuItem extends JMenuItem implements ITimeTrackerButton
{
    public ContextMenuItem(final String text, final Icon icon)
    {
        super(text);
        BaseButton.setButtonIcon(this, icon.getIcon());
    }

    @Override
    public void setIconName(final String icon)
    {
    }

    @Override
    public void setIcon(final ImageIcon icon)
    {
        super.setIcon(icon);
    }
}
