package timetracker.actions;

import timetracker.TimeTracker;
import timetracker.dialogs.FinishIssueDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Aktion um das Ticket auf "To Verify" zu setzen.
 */
public class FinishDialogAction extends BaseAction
{
    private static final long serialVersionUID = 7059526162584192854L;

    public FinishDialogAction(final JButton button)
    {
        super(button);
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        TimeTracker.MATCHER.reset(this.button.getText());
        if (TimeTracker.MATCHER.matches())
        {
            final FinishIssueDialog dialog = new FinishIssueDialog(this.button);
            dialog.setVisible(true);
        }
    }
}