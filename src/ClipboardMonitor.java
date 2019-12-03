import java.awt.*;
import java.awt.datatransfer.*;
import java.util.Observable;

/**
 * Überwacht das Clipboard auf Youtrack-Issues
 */
public class ClipboardMonitor extends Observable implements ClipboardOwner
{
    private static ClipboardMonitor monitor;
    private final TimeTracker tracker;

    private ClipboardMonitor(final TimeTracker tracker)
    {
        this.tracker = tracker;
        gainOwnership();
    }

    private void gainOwnership()
    {
        final Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        try
        {
            final Transferable content = clip.getContents(null);
            final Object transferData = content.getTransferData(DataFlavor.stringFlavor);
            if(transferData instanceof String && !((String) transferData).isEmpty())
            {
                Log.info("String content detected");
                this.tracker.showAddIssueDialog((String) transferData);
                flushClipboard();
            }
            else
            {
                clip.setContents(content, this);
            }
            setChanged();
            notifyObservers(content);
        }
        catch (final Exception e)
        {
            Log.severe(e.getMessage(), e);
        }
    }

    @Override
    public void lostOwnership(final Clipboard clipboard, final Transferable contents)
    {
        Log.info("Ownership lost ...");
        new Thread(() -> {
            try
            {
                Thread.sleep(1000);
                gainOwnership();
            }
            catch (final Exception e)
            {
                Log.severe(e.getMessage(), e);
            }
        }).start();
    }

    private void flushClipboard()
    {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), this);
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("There can be only one instance of this monitor!");
    }

    static ClipboardMonitor getMonitor(final TimeTracker tracker)
    {
        if (monitor == null)
        {
            monitor = new ClipboardMonitor(tracker);
        }
        return monitor;
    }
}