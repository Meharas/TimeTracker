package timetracker.utils;

import timetracker.Constants;

import javax.swing.*;
import java.time.Duration;
import java.util.regex.Matcher;

/**
 * Übernimmt die Anzeige der Bearbeitungszeit
 */
public final class DurationTimer
{
    private static final String TIME_PATTERN = "%02dh %02dmin %02ds";
    private static final Matcher DURATION_MATCHER = Constants.DURATION_PATTERN.matcher(Constants.STRING_EMPTY);

    private final Timer timer;
    private final JLabel label;
    private int duration = 0;

    public DurationTimer(final JLabel label)
    {
        this.label = label;
        this.timer = new Timer(1000, e -> formatTime(++this.duration));

        setDuration(label.getText());
    }

    public void formatTime(final int duration)
    {
        final String time = this.label.getText();
        if(duration == 0 && time != null && !time.isEmpty())
        {
            return;
        }
        final Duration dur = Duration.ofSeconds(duration);
        final long seconds = dur.getSeconds();
        this.label.setText(String.format(TIME_PATTERN, seconds / 3600, (seconds % 3600) / 60, seconds % 60));
    }

    private void setDuration(final String time)
    {
        if (time == null || time.isEmpty())
        {
            return;
        }

        DURATION_MATCHER.reset(time);
        if (DURATION_MATCHER.matches())
        {
            final int h = Integer.parseInt(DURATION_MATCHER.group(1));
            final int m = Integer.parseInt(DURATION_MATCHER.group(2));
            final int s = Integer.parseInt(DURATION_MATCHER.group(3));
            this.duration = h * 60 * 60 + m * 60 + s;
        }
    }

    public void reset()
    {
        this.duration = 0;
    }

    public final void start()
    {
        this.timer.start();
    }

    public final void stop()
    {
        this.timer.stop();
    }

    public final boolean isRunning()
    {
        return this.timer.isRunning();
    }
}