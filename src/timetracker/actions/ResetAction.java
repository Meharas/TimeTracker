package timetracker.actions;

import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Setzt alle Time zurück
 */
public class ResetAction extends BaseAction
{
    private static final long serialVersionUID = -3128930442339113957L;

    public ResetAction(final JButton button)
    {
        super(button);
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final int result = JOptionPane.showConfirmDialog(TimeTracker.getInstance(), Resource.getString(PropertyConstants.TEXT_RESET),
                                                         Resource.getString(PropertyConstants.TEXT_CONFIRMATION), JOptionPane.YES_NO_OPTION);
        if(result != JOptionPane.YES_OPTION)
        {
            return;
        }

        TimerAction.resetTimers();
    }
}