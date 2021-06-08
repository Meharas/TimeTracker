package timetracker.actions;

import timetracker.data.Issue;
import timetracker.db.Backend;
import timetracker.utils.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

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
        try
        {
            Backend.getInstance().deleteIssue(this.issue);
        }
        catch (final Throwable t)
        {
            Util.handleException(t);
        }

        remove();
        this.timeTracker.updateGui(true);
        this.timeTracker.decreaseLine();
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
        this.timeTracker.getPanel().remove(parent);
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