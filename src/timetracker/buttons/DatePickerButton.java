package timetracker.buttons;

import timetracker.icons.Icon;

import javax.swing.*;

/**
 * Button zur Anzeige eines DatePickers
 */
public class DatePickerButton extends JButton implements ITimeTrackerButton
{
    public DatePickerButton (final Icon icon)
    {
        BaseButton.setButtonIcon(this, icon == null ? null : icon.getIcon());
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
