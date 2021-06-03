package timetracker.actions;

import timetracker.Constants;
import timetracker.TimeTracker;
import timetracker.log.Log;

import javax.swing.text.TextAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/**
 * Aktion zur Anzeige des Logfiles
 */
public class OpenLogAction extends TextAction
{
    /**
     * Creates a new JTextAction object.
     */
    public OpenLogAction()
    {
        super("Log");
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final File log = new File(TimeTracker.home + "\\log\\" + Constants.LOGFILE_NAME);
        if (log.exists())
        {
            final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.OPEN))
            {
                try
                {
                    desktop.open(log);
                }
                catch (final IOException ex)
                {
                    Log.severe(ex.getMessage(), ex);
                }
            }
        }
    }
}
