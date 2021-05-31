package timetracker.utils;

import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.actions.ShowAddButtonAction;
import timetracker.icons.Icon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;

/**
 * Erzeugt ein TrayIcon mit Menü
 */
public class TrayIcon
{
    private TrayIcon()
    {

    }

    public static void addTrayIcon() throws AWTException
    {
        if(SystemTray.isSupported())
        {
            final TimeTracker timeTracker = TimeTracker.getTimeTracker();
            final MenuItem aboutItem = new MenuItem("About");
            final int year = LocalDateTime.now().getYear();
            aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(timeTracker, "Version 0.0.1 Leipzig\n\nby Andreas Beyer\n© " + year));

            final MenuItem openItem = new MenuItem(Resource.getString(PropertyConstants.LABEL_OPEN));
            openItem.setEnabled(false);
            openItem.addActionListener(e -> {
                timeTracker.setVisible(true);
                timeTracker.setExtendedState(Frame.NORMAL);
                openItem.setEnabled(false);
            });

            final ImageIcon imageIcon = new ImageIcon(TimeTracker.getHome() + Icon.TIMETRACKER.getIcon());
            final java.awt.TrayIcon icon = new java.awt.TrayIcon(imageIcon.getImage());
            icon.setToolTip(timeTracker.getTitle());
            icon.addMouseListener(new MouseListener()
            {
                @Override
                public void mouseClicked(final MouseEvent e)
                {
                    if(!SwingUtilities.isLeftMouseButton(e))
                    {
                        return;
                    }
                    timeTracker.showFrame(openItem, !timeTracker.isVisible());
                }

                @Override
                public void mousePressed(final MouseEvent e)
                {
                    //doNothing
                }

                @Override
                public void mouseReleased(final MouseEvent e)
                {
                    //doNothing
                }

                @Override
                public void mouseEntered(final MouseEvent e)
                {
                    //doNothing
                }

                @Override
                public void mouseExited(final MouseEvent e)
                {
                    //doNothing
                }
            });


            final MenuItem add = new MenuItem(Resource.getString(PropertyConstants.LABEL_ADD));
            add.addActionListener(new ShowAddButtonAction());

            final PopupMenu popup = new PopupMenu();
            popup.add(aboutItem);
            popup.add(openItem);
            popup.add(add);
            icon.setPopupMenu(popup);
            SystemTray.getSystemTray().add(icon);

            timeTracker.addWindowStateListener((final WindowEvent e) -> {
                if(e.getNewState() == Frame.ICONIFIED || e.getNewState() == 7)
                {
                    timeTracker.showFrame(openItem, false);
                }
                else if(e.getNewState() == Frame.MAXIMIZED_BOTH || e.getNewState() == Frame.NORMAL)
                {
                    timeTracker.showFrame(openItem, true);
                }
            });
        }
    }
}
