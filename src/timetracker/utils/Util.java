package timetracker.utils;

import timetracker.TimeTracker;
import timetracker.data.Issue;
import timetracker.error.BackendException;
import timetracker.log.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    public static Locale getLocale()
    {
        return new Locale((String) System.getProperties().get("user.language"));
    }

    public static void handleException(final Throwable t)
    {
        final String msg = getMessage(t);
        if (!(t instanceof BackendException))
        {
            t.printStackTrace();
            Log.severe(msg, t);
        }
        JOptionPane.showMessageDialog(TimeTracker.getTimeTracker(), msg);
    }

    public static String getMessage(final Throwable e)
    {
        if(e == null)
        {
            return "";
        }
        String msg = e.getMessage();
        if(msg == null || msg.isEmpty())
        {
            msg = getMessage(e.getCause());
        }
        if(msg.isEmpty())
        {
            msg = e.getClass().getSimpleName();
        }
        return msg;
    }

    /**
     * Liefert alle Buttons, welche ein Issue darfstellen
     * @return alle Buttons, welche ein Issue darfstellen
     */
    public static Collection<IssueButton> getButtons()
    {
        final Set<Component> components = new HashSet<>();
        final TimeTracker timeTracker = TimeTracker.getTimeTracker();
        timeTracker.collectComponents(timeTracker, components);
        return components.stream().filter(IssueButton.class::isInstance).map(IssueButton.class::cast).collect(Collectors.toList());
    }

    /**
     * Liefert den Button zu einem bestimmten Issue
     * @param issue Issue
     * @return Button zu einem bestimmten Issue
     */
    public static IssueButton getButton(final Issue issue)
    {
        final Collection<IssueButton> buttons = getButtons();
        return buttons.stream().filter(btn -> btn.getName() != null).filter(btn -> btn.getName().equalsIgnoreCase(issue.getId()))
                               .findFirst().orElse(null);
    }

    /**
     * Sortiert eine Map nach dem Wert und gibt diese als neue Map zurück
     * @param map Zu sortierende Map
     * @return Sortierte neue Map
     */
    public static Map<String, String> sortByValue(final Map<String, String> map)
    {
        return map.entrySet().stream().sorted(Map.Entry.comparingByValue())
                             .collect(Collectors.toMap(Map.Entry::getKey,
                                                       Map.Entry::getValue,
                                                       (e1, e2) -> e1,
                                                       LinkedHashMap::new));
    }
}
