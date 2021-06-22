package timetracker.actions;

import timetracker.Constants;
import timetracker.TimeTracker;
import timetracker.buttons.IssueButton;
import timetracker.data.Issue;
import timetracker.db.Backend;
import timetracker.utils.LookAndFeelManager;
import timetracker.utils.Util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.time.Duration;
import java.util.Collection;

/**
 * Base Action
 */
public class BaseAction extends AbstractAction
{
    private static final long serialVersionUID = -3232149832497004005L;

    protected final TimeTracker timeTracker = TimeTracker.getTimeTracker();
    public final Timer timer;
    protected final JButton button;
    final Issue issue;
    final JLabel label;
    String text;
    int duration = 0;

    BaseAction()
    {
        super();

        this.button = null;
        this.timer = null;
        this.label = null;
        this.issue = null;
    }

    protected BaseAction(final JButton button)
    {
        this(button, null);
    }

    protected BaseAction(final JButton button, final Issue issue)
    {
        this(button, issue, null);
    }

    protected BaseAction(final String text)
    {
        super(text, null);

        this.button = null;
        this.timer = null;
        this.label = null;
        this.issue = null;
    }

    public BaseAction(final JButton button, final Issue issue, final JLabel label)
    {
        super(button.getText(), button.getIcon());

        this.button = button;
        this.issue = issue;
        this.label = label;
        this.text = button.getText();

        this.timer = new Timer(1000, e -> formatTime(++this.duration));

        if (label != null)
        {
            setDuration(label.getText());
        }
    }

    private void formatTime(final int duration)
    {
        final String time = this.label.getText();
        if(duration == 0 && time != null && !time.isEmpty())
        {
            return;
        }
        final Duration dur = Duration.ofSeconds(duration);
        final long seconds = dur.getSeconds();
        this.label.setText(String.format("%02dh %02dmin %02ds", seconds / 3600, (seconds % 3600) / 60, seconds % 60));
    }

    private void setDuration(final String time)
    {
        if (time == null || time.isEmpty())
        {
            return;
        }

        TimeTracker.DURATION_MATCHER.reset(time);
        if (TimeTracker.DURATION_MATCHER.matches())
        {
            final int h = Integer.parseInt(TimeTracker.DURATION_MATCHER.group(1));
            final int m = Integer.parseInt(TimeTracker.DURATION_MATCHER.group(2));
            final int s = Integer.parseInt(TimeTracker.DURATION_MATCHER.group(3));
            this.duration = h * 60 * 60 + m * 60 + s;
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        if (this.timer.isRunning())
        {
            stop();
            return;
        }

        if(this.issue.isPreventTimer())
        {
            return;
        }

        try
        {
            this.issue.setMarked(false);
            Backend.getInstance().updateIssue(this.issue);

            stopTimers();
            formatTime(0);
            this.timer.start();
            this.button.setBackground(LookAndFeelManager.getColorInProgress());
            this.button.setForeground(LookAndFeelManager.getFontColor());
            this.button.setOpaque(true);
            this.issue.setInProgress(true);
        }
        catch (final Throwable t)
        {
            Util.handleException(t);
        }
    }

    void stopTimers()
    {
        final Collection<IssueButton> buttons = Util.getButtons();
        for (final IssueButton btn : buttons)
        {
            final Action action = btn.getAction();
            if (action instanceof BaseAction)
            {
                ((BaseAction) action).stop();
            }
        }
    }

    public static void resetTimers()
    {
        final Collection<IssueButton> buttons = Util.getButtons();
        for (final IssueButton btn : buttons)
        {
            final Action action = btn.getAction();
            if (action instanceof BaseAction && ((BaseAction) action).issue != null)
            {
                ((BaseAction) action).reset();
            }
        }
    }

    void stop()
    {
        stop(true, false);
    }

    public void stopWithoutSave()
    {
        stop(false, false);
    }

    void stop(final boolean saveDuration, final boolean reset)
    {
        if (this.timer != null)
        {
            this.timer.stop();
            if (saveDuration && this.label != null)
            {
                saveDuration(reset);
            }
            this.issue.setInProgress(false);
        }

        if(this.issue.isMarked())
        {
            //Der Button wurde markiert
            return;
        }
        this.button.setBackground(null);
        this.button.setForeground(null);
        this.button.setOpaque(false);
    }

    private void saveDuration(final boolean reset)
    {
        final String currentDuration = this.label.getText();
        this.issue.setDuration(reset ? Constants.STRING_EMPTY : currentDuration);
        try
        {
            Backend.getInstance().updateIssue(this.issue);
        }
        catch (final Throwable t)
        {
            Util.handleException(t);
        }
    }

    public void reset()
    {
        stop(true, true);
        this.duration = 0;
        if (this.label != null)
        {
            this.label.setText(Constants.STRING_EMPTY);
        }
        try
        {
            removeDuration();
        }
        catch (final Throwable t)
        {
            Util.handleException(t);
        }
    }

    private void removeDuration() throws Throwable
    {
        Backend.getInstance().removeDuration(this.issue);
    }
}