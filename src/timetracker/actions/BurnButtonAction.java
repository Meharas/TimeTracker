package timetracker.actions;

import timetracker.data.Issue;
import timetracker.dialogs.BurnIssueDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Klasse zum Burnen von Zeiten
 */
public class BurnButtonAction extends TimerAction
{
    private static final long serialVersionUID = -2092965435624779543L;

    public BurnButtonAction(final JButton button, final JLabel label, final Issue issue)
    {
        super(button, issue, label);
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final BurnIssueDialog dialog = new BurnIssueDialog(this);
        dialog.setVisible(true);
    }
}