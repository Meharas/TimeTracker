package timetracker.actions;

import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.data.Issue;
import timetracker.db.Backend;
import timetracker.utils.Util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

/**
 * Löscht eine komplette Zeile.
 */
public class DeleteButtonAction extends BaseAction
{
    private static final long serialVersionUID = -2092965435624779543L;

    public DeleteButtonAction(final JButton button, final Issue issue)
    {
        super(button, issue);
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final String message = Resource.getString(PropertyConstants.TICKET_DELETE, this.issue.getTicket());
        final TimeTracker timeTracker = TimeTracker.getInstance();
        final int result = JOptionPane.showConfirmDialog(timeTracker, message, Resource.getString(PropertyConstants.TEXT_CONFIRMATION), JOptionPane.YES_NO_OPTION);
        if(result != JOptionPane.YES_OPTION)
        {
            return;
        }
        final Backend backend = Backend.getInstance();
        try
        {
            backend.deleteIssue(this.issue);
            timeTracker.removeRow(this.issue);
            timeTracker.updateGui(true);
            backend.commit();
        }
        catch (final Throwable t)
        {
            try
            {
                backend.rollback();
                Util.handleException(t);
            }
            catch (final SQLException ex)
            {
                Util.handleException(t);
            }
        }
    }
}