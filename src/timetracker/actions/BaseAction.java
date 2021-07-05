package timetracker.actions;

import timetracker.data.Issue;

import javax.swing.*;

/**
 * Base Action
 */
public abstract class BaseAction extends AbstractAction
{
    private static final long serialVersionUID = -3232149832497004005L;

    protected final JButton button;
    final Issue issue;

    BaseAction()
    {
        super();

        this.button = null;
        this.issue = null;
    }

    protected BaseAction(final JButton button)
    {
        this(button, null);
    }

    protected BaseAction(final JButton button, final Issue issue)
    {
        super(button.getText(), button.getIcon());

        this.button = button;
        this.issue = issue;
    }

    public JButton getButton()
    {
        return this.button;
    }

    public Issue getIssue()
    {
        return this.issue;
    }
}