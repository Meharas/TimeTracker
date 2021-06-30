package timetracker.utils;


import timetracker.TimeTracker;
import timetracker.data.Issue;
import timetracker.db.Backend;
import timetracker.log.Log;
import timetracker.misc.Row;

import javax.swing.*;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Automatisches Speichern von Zeiten
 */
public final class AutoSave
{
    private final Timer timer;

    // Innere private Klasse, die erst beim Zugriff durch die umgebende Klasse initialisiert wird
    private static final class InstanceHolder
    {
        // Die Initialisierung von Klassenvariablen geschieht nur einmal und wird vom ClassLoader implizit synchronisiert
        private static final AutoSave instance = new AutoSave();
    }

    private AutoSave()
    {
        this.timer = new Timer((int) TimeUnit.MINUTES.toMillis(1), e -> save());
        this.timer.start();
    }

    public static AutoSave getInstance()
    {
        return InstanceHolder.instance;
    }

    public void stop()
    {
        this.timer.stop();
        save();
    }

    private void save()
    {
        final TimeTracker timeTracker = TimeTracker.getInstance();
        final Collection<Row> rows = timeTracker.getRows();
        for(final Row row : rows)
        {
            final String currentTime = row.getLabel().getText();
            if(currentTime != null && !currentTime.isEmpty())
            {
                final Issue issue = row.getButton().getIssue();
                try
                {
                    Backend.getInstance().saveCurrentDuration(issue.getId(), currentTime);
                }
                catch (final Throwable t)
                {
                    final String msg = Util.getMessage(t);
                    Log.severe(msg, t);
                    JOptionPane.showMessageDialog(timeTracker, String.format("Error while saving spent time for %s:%n%s", currentTime, msg));
                }
            }
        }
    }
}