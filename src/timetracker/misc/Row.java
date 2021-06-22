package timetracker.misc;

import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.actions.BaseAction;
import timetracker.actions.BurnButtonAction;
import timetracker.actions.DeleteButtonAction;
import timetracker.actions.FinishDialogAction;
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

        final JLabel timeLabel = new JLabel();
        timeLabel.setName(issue.getId());
        timeLabel.setPreferredSize(new Dimension(100, 20));
        timeLabel.setBorder(new EmptyBorder(0, 8, 0, 0));

        final String savedDuration = issue.getDurationSaved();
        if(savedDuration != null && !savedDuration.isEmpty())
        {
            final TimeTracker timeTracker = TimeTracker.getTimeTracker();
            timeTracker.setLabelTooltip(savedDuration, timeLabel);
        }
        labelPanel.add(timeLabel, BorderLayout.EAST);

        setTime(timeLabel, issue);

        final BaseAction timerAction = new BaseAction(this.button, issue, timeLabel);
        this.button.setAction(timerAction);

        final JButton burnAction = new IssueActionButton(timetracker.icons.Icon.BURN);
        burnAction.setToolTipText(Resource.getString(PropertyConstants.TOOLTIP_BURN));
        burnAction.addActionListener(new BurnButtonAction(this.button, timeLabel, issue));

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