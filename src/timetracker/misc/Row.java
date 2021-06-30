package timetracker.misc;

import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.actions.BurnButtonAction;
import timetracker.actions.DeleteButtonAction;
import timetracker.actions.FinishDialogAction;
import timetracker.actions.TimerAction;
import timetracker.buttons.IssueActionButton;
import timetracker.buttons.IssueButton;
import timetracker.data.Issue;
import timetracker.db.Backend;
import timetracker.icons.Icon;
import timetracker.log.Log;
import timetracker.utils.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Collection;

/**
 * Eine Zeile im Timetracker mit dem Button, Label und Aktionen
 */
public class Row extends JPanel
{
    private final IssueButton button;
    private final JLabel label;

    public Row (final Issue issue)
    {
        this.button = new IssueButton(issue);
        final Collection<IssueButton> buttons = Util.getButtons();
        if (!buttons.isEmpty())
        {
            final IssueButton firstButton = buttons.iterator().next();
            this.button.setPreferredSize(firstButton.getPreferredSize());
        }

        final JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BorderLayout(0, 0));
        labelPanel.add(this.button, BorderLayout.CENTER);

        this.label = new JLabel();
        this.label.setName(issue.getId());
        this.label.setPreferredSize(new Dimension(100, 20));
        this.label.setBorder(new EmptyBorder(0, 8, 0, 0));

        final String savedDuration = issue.getDurationSaved();
        if(savedDuration != null && !savedDuration.isEmpty())
        {
            final TimeTracker timeTracker = TimeTracker.getInstance();
            timeTracker.setLabelTooltip(savedDuration, this.label);
        }
        labelPanel.add(this.label, BorderLayout.EAST);

        setTime(this.label, issue);

        final TimerAction timerAction = new TimerAction(this.button, issue, this.label);
        this.button.setAction(timerAction);

        final JButton burnAction = new IssueActionButton(timetracker.icons.Icon.BURN);
        burnAction.setToolTipText(Resource.getString(PropertyConstants.TOOLTIP_BURN));
        burnAction.addActionListener(new BurnButtonAction(this.button, this.label, issue));

        final JButton action = new IssueActionButton(timetracker.icons.Icon.FINISH);
        action.setToolTipText(Resource.getString(PropertyConstants.LABEL_FINISH));
        action.addActionListener(new FinishDialogAction(this.button));
        action.setEnabled(issue.canBeFinished());

        final JButton deleteItem = new IssueActionButton(Icon.REMOVE);
        deleteItem.setToolTipText(Resource.getString(PropertyConstants.TOOLTIP_DELETE));
        deleteItem.addActionListener(new DeleteButtonAction(button, issue));

        final JPanel actionsPanel = new JPanel();
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.X_AXIS));
        actionsPanel.add(burnAction);
        actionsPanel.add(action);
        actionsPanel.add(deleteItem);

        setMaximumSize(new Dimension(0, 30));
        setLayout(new BorderLayout(0, 0));
        add(labelPanel, BorderLayout.CENTER);
        add(actionsPanel, BorderLayout.EAST);
    }

    private void setTime(final JLabel label, final Issue issue)
    {
        final String value = issue.getDuration();
        if (value != null && !value.isEmpty())
        {
            label.setText(value);
        }
    }

    public IssueButton getButton()
    {
        return this.button;
    }

    public JLabel getLabel()
    {
        return this.label;
    }

    public int getIndex()
    {
        return Math.max(2, this.button.getIssue().getOrder() + 1);
    }

    public void setIndex(final int index)
    {
        final Issue issue = this.button.getIssue();
        issue.setOrder(index - 1);
        try
        {
            Backend.getInstance().updateIssue(issue);
            String ticket = issue.getTicket();
            if(ticket.isEmpty())
            {
                ticket = issue.getLabel();
            }
            Log.info(String.format("New order of issue %s: %d", ticket, issue.getOrder()));
        }
        catch (final Throwable t)
        {
            Util.handleException(t);
        }
    }
}