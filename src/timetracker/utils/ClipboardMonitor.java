package timetracker.utils;

import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.dialogs.ClipboardDialog;
import timetracker.log.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.concurrent.TimeUnit;

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

    private void gainOwnership(final Transferable content)
    {
        try
        {
            if (!ClipboardMonitor.disabled && content != null && content.isDataFlavorSupported(DataFlavor.stringFlavor))
            {
                final Object transferData = content.getTransferData(DataFlavor.stringFlavor);
                if(transferData instanceof String && !((String) transferData).isEmpty())
                {
                    final TimeTracker timeTracker = TimeTracker.getInstance();
                    final boolean showPopUp = timeTracker.isVisible() && !timeTracker.isAlwaysOnTop() || !timeTracker.isVisible();

                    final String ticket;
                    if(showPopUp && (ticket = TimeTracker.getTicketFromText((String) transferData)) != null)
                    {
                        final JPopupMenu menu = new JPopupMenu();
                        final JMenuItem add = new JMenuItem(Resource.getString(PropertyConstants.TEXT_ADD_CLIPBOARD, ticket));
                        final Font current = add.getFont();
                        final Font font = new Font(current.getName(), current.getStyle(), 16);
                        add.setFont(font);
                        add.setHorizontalTextPosition(SwingConstants.CENTER);
                        add.invalidate();

                        final Dimension dimension = new Dimension(350, 100);
                        add.setPreferredSize(dimension);
                        add.addMouseListener(new MouseClickListener()
                        {
                            @Override
                            public void mouseClicked(final MouseEvent e)
                            {
                                final ClipboardDialog dialog = new ClipboardDialog(ticket);
                                dialog.setVisible(true);
                                menu.setVisible(false);
                            }
                        });

                        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                        menu.setLocation(screenSize.width - dimension.width, screenSize.height - dimension.height - 50);
                        menu.setPreferredSize(dimension);
                        menu.add(add);
                        menu.setVisible(true);

                        final Timer timer = new Timer((int) TimeUnit.SECONDS.toMillis(5), e -> menu.setVisible(false));
                        timer.setRepeats(false);
                        timer.start();
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
        try
        {
            Thread.sleep(50);
        }
        catch (final InterruptedException e)
        {
            Util.handleException(e);
        }
        gainOwnership(clipboard.getContents(this));
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("There can be only one instance of this monitor!");
    }

    public void resetClipboard()
    {
        gainOwnership(new StringSelection(""));
    }

    public static ClipboardMonitor getMonitor()
    {
        EventQueue.invokeLater(monitor);
        return monitor;
    }

    @Override
    public void run()
    {
        resetClipboard();
    }
}