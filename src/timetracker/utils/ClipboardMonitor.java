package timetracker.utils;

import timetracker.TimeTracker;
import timetracker.dialogs.ClipboardDialog;
import timetracker.log.Log;

import java.awt.*;
import java.awt.datatransfer.*;
import java.util.Observable;

/**
 * Überwacht das Clipboard auf Youtrack-Issues
 */
public class ClipboardMonitor extends Observable implements ClipboardOwner, Runnable
{
    private static final ClipboardMonitor monitor = new ClipboardMonitor();
    private static final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    public static boolean disabled = false;

    private ClipboardMonitor()
    {
    }

    private static void gainOwnership(final Transferable content)
    {
        Log.info("gain ownership...");
        try
        {
            if (!ClipboardMonitor.disabled && content != null && content.isDataFlavorSupported(DataFlavor.stringFlavor))
            {
                final Object transferData = content.getTransferData(DataFlavor.stringFlavor);
                if(transferData instanceof String && !((String) transferData).isEmpty())
                {
                    Log.info("String content detected");

                    if(TimeTracker.matches((String) transferData))
                    {
                        final ClipboardDialog dialog = new ClipboardDialog((String) transferData);
                        dialog.setVisible(true);
                    }
                }
            }

            ClipboardMonitor.disabled = false;
            clipboard.setContents(content, monitor);
        }
        catch (final Exception e)
        {
            Log.severe(e.getMessage(), e);
        }
    }

    @Override
    public void lostOwnership(final Clipboard clipboard, final Transferable contents)
    {
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("There can be only one instance of this monitor!");
    }

    public static void resetClipboard()
    {
        gainOwnership(new StringSelection(""));
    }

    public static ClipboardMonitor getMonitor()
    {
        EventQueue.invokeLater(monitor);

        /*final Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine())
        {
            final String buffer = scanner.nextLine();
            if (!buffer.trim().isEmpty())
            {
                monitor.setBuffer(buffer);
            }
        }*/
        return monitor;
    }

    @Override
    public void run()
    {
        resetClipboard();
    }
}