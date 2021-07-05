package timetracker.actions;

import timetracker.buttons.IssueButton;
import timetracker.data.Issue;
import timetracker.dialogs.BurnIssueDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Klasse zum Burnen von Zeiten
 */
public class BurnButtonAction extends BaseAction
{
    private static final long serialVersionUID = -2092965435624779543L;
    private final JLabel label;

    public BurnButtonAction(final IssueButton button, final JLabel label, final Issue issue)
    {
        super(button, issue);
        this.label = label;
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final BurnIssueDialog dialog = new BurnIssueDialog(this);
        dialog.setVisible(true);
    }

    public JLabel getLabel()
    {
        return this.label;
    }

    @Override
    public IssueButton getButton()
    {
        return (IssueButton) this.button;
    }
}