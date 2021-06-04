package timetracker.actions;

import timetracker.dialogs.ClipboardDialog;
import timetracker.utils.Util;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Aktion zur Anzeige eines Dialogs zum Hinzufügen aus der Zwischenablage
 */
public class AddClipboardAction extends BaseAction
{
    public AddClipboardAction(final JButton button)
    {
        super(button);
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final String issue = Util.getIssueFromClipboard(this);
        if(issue != null)
        {
            final ClipboardDialog dialog = new ClipboardDialog(issue);
            dialog.setVisible(true);
        }
    }
}
