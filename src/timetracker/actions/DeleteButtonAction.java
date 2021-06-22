package timetracker.actions;

import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.data.Issue;
import timetracker.db.Backend;
import timetracker.utils.Util;

import javax.swing.*;
import java.awt.*;
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
        super(button, issue, null);
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final String message = Resource.getString(PropertyConstants.TICKET_DELETE, this.issue.getTicket());
        final int result = JOptionPane.showConfirmDialog(TimeTracker.getTimeTracker(), message, Resource.getString(PropertyConstants.TEXT_CONFIRMATION), JOptionPane.YES_NO_OPTION);
        if(result != JOptionPane.YES_OPTION)
        {
            return;
        }
        final Backend backend = Backend.getInstance();
        try
        {
            backend.deleteIssue(this.issue);
            this.timeTracker.removeRow(this.issue);
            this.timeTracker.updateGui(true);
            this.timeTracker.decreaseLine();
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

    /**
     * Entfernt eine komplette Zeile
     */
    private void remove()
    {
        final Container parent = this.button.getParent().getParent();
        if (parent == null)
        {
            return;
        }
        final Component[] components = parent.getComponents();
        for (final Component child : components)
        {
            remove(parent, child);
        }
        this.timeTracker.getContentPane().remove(parent);
    }

    /**
     * Entfernt die Komponente und deren Kinder vom Parent
     * @param parent Parent
     * @param component Komponente
     */
    private void remove(final Container parent, final Component component)
    {
        if (component instanceof JPanel)
        {
            final Component[] components = ((JPanel) component).getComponents();
            for (final Component child : components)
            {
                remove((Container) component, child);
            }
        }
        parent.remove(component);
    }
}