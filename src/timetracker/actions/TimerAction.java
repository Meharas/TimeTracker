package timetracker.actions;

import timetracker.Constants;
import timetracker.buttons.IssueButton;
import timetracker.data.Issue;
import timetracker.db.Backend;
import timetracker.log.Log;
import timetracker.utils.DurationTimer;
import timetracker.utils.LookAndFeelManager;
import timetracker.utils.Util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.logging.Level;

/**
 * Base Action
 */
public class TimerAction extends BaseAction
{
    private static final long serialVersionUID = -3232149832497004005L;

    private final DurationTimer timer;
    private final JLabel label;

    protected TimerAction(final JButton button, final Issue issue)
    {
        this(button, issue, null);
    }

    public TimerAction(final JButton button, final Issue issue, final JLabel label)
    {
        super(button, issue);
        this.timer = new DurationTimer(label);
        this.label = label;
    }

    public DurationTimer getTimer()
    {
        return this.timer;
    }

    public JLabel getLabel()
    {
        return this.label;
    }


    @Override
    public void actionPerformed(final ActionEvent e)
    {
        if (this.timer.isRunning())
        {
            Log.info("Resetting time for " + this.issue);
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
            this.timer.formatTime(0);
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
        Log.log(Level.FINE, "stopTimers()");
        final Collection<IssueButton> buttons = Util.getButtons();
        for (final IssueButton btn : buttons)
        {
            final TimerAction action = btn.getAction();
            if (action != null)
            {
                action.stop();
            }
        }
    }

    public static void resetTimers()
    {
        Log.log(Level.FINE, "resetTimers()");
        final Collection<IssueButton> buttons = Util.getButtons();
        for (final IssueButton btn : buttons)
        {
            final TimerAction action = btn.getAction();
            if (action != null && action.issue != null)
            {
                action.reset();
            }
        }
    }

    void stop()
    {
        Log.log(Level.FINE, "stop()");
        stop(true, false);
    }

    public void stopWithoutSave()
    {
        Log.log(Level.FINE, "stopWithoutSave()");
        stop(false, false);
    }

    void stop(final boolean saveDuration, final boolean reset)
    {
        Log.log(Level.FINE, String.format("stop(%s, %s)", saveDuration, reset));
        if (this.timer != null)
        {
            Log.info("Resetting time for " + this.issue);
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

        Log.info("Resetting timer for " + this.issue);
        this.timer.reset();
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