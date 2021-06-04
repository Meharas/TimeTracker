package timetracker.actions;

import timetracker.data.Issue;
import timetracker.dialogs.AddIssueDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Klasse zur Darstellung eines Dialoges, mit welchem ein neues Ticket angelegt werden kann.
 */
public class ShowAddButtonAction extends BaseAction
{
    private static final long serialVersionUID = -2104627297533100111L;

    public ShowAddButtonAction()
    {

    }

    public ShowAddButtonAction(final JButton button)
    {
        this(button, null);
    }

    public ShowAddButtonAction(final JButton button, final Issue issue)
    {
        super(button, issue);
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final AddIssueDialog dialog = new AddIssueDialog(this.button, this.issue);
        dialog.setVisible(true);
    }
}