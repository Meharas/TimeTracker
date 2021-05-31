package utils;

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

    static boolean disabled = false;

    private ClipboardMonitor()
    {
    }

    private void gainOwnership(final Transferable content)
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
                    TimeTracker.getTimeTracker().showAddIssueDialog((String) transferData);
                }
            }

            ClipboardMonitor.disabled = false;
            clipboard.setContents(content, this);
        }
        catch (final Exception e)
        {
            Log.severe(e.getMessage(), e);
        }
    }
    private void setBuffer(final String buffer)
    {
        gainOwnership(new StringSelection(buffer));
    }

    @Override
    public void lostOwnership(final Clipboard clipboard, final Transferable contents)
    {
        Log.info("Ownership lost ...");

        try
        {
            Thread.sleep(50);
        }
        catch (final Exception e)
        {
            Log.severe(e.getMessage(), e);
        }
        gainOwnership(clipboard.getContents(this));
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("There can be only one instance of this monitor!");
    }

    static ClipboardMonitor getMonitor()
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
        setBuffer("");
    }
}