package timetracker.utils;

import timetracker.TimeTracker;
import timetracker.log.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Utility-Klasse
 */
public class Util
{
    private Util()
    {
    }

    public static Point getWindowLocation(final Dimension dimension)
    {
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = (screenSize.width / 2) - (dimension.width /2);
        final int y = (screenSize.height / 2) - (dimension.height) / 2;
        return new Point(x, y);
    }


    /**
     * Liefert das Ticket aus der Zwischenablage ein, insofern es sich um ein Ticket handelt
     * @param requestor anfordernde Komponente
     * @return Ticket aus der Zwischenablage
     */
    public static String getIssueFromClipboard(final Object requestor)
    {
        final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        final Transferable contents = cb.getContents(requestor);
        if(contents != null)
        {
            try
            {
                final Object transferData = contents.getTransferData(DataFlavor.stringFlavor);
                if(transferData instanceof String)
                {
                    TimeTracker.MATCHER.reset((String) transferData);
                    if(TimeTracker.MATCHER.matches())
                    {
                        return TimeTracker.MATCHER.group(1);
                    }
                }
            }
            catch (final UnsupportedFlavorException | IOException ex)
            {
                Log.severe(ex.getMessage(), ex);
            }
        }
        return null;
    }
}
