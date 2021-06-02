package timetracker.menu;

import timetracker.*;
import timetracker.actions.DeleteButtonAction;
import timetracker.actions.ShowAddButtonAction;
import timetracker.client.Client;
import timetracker.data.Issue;
import timetracker.icons.Icon;
import timetracker.actions.BaseAction;
import timetracker.log.Log;
import timetracker.utils.ClipboardMonitor;
import org.apache.http.client.utils.URIBuilder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.TextAction;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Kontextmenü
 */
public class ContextMenu
{
    private static final EmptyBorder BORDER = new EmptyBorder(5, 5, 5, 5);
    private static final EmptyBorder MENU_BORDER = new EmptyBorder(0, 0, 0, 0);

    private ContextMenu()
    {
    }

    public static JPopupMenu create(final TimeTracker timeTracker, final JButton parent, final Issue issue)
    {
        final JPopupMenu menu = new JPopupMenu();
        menu.setBorder(MENU_BORDER);

        final JMenuItem copyItem = new JMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_COPY));
        copyItem.setBorder(BORDER);
        BaseAction.setButtonIcon(copyItem, Icon.COPY);

        copyItem.addActionListener((final ActionEvent e) -> {
            final StringSelection stringSelection = new StringSelection(parent.getText());
            final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            ClipboardMonitor.disabled = true;
            cb.setContents(stringSelection, stringSelection);
        });

        final JMenuItem editItem = new JMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_EDIT));
        editItem.setBorder(BORDER);
        editItem.addActionListener(new ShowAddButtonAction(parent));
        BaseAction.setButtonIcon(editItem, Icon.EDIT);

        final JMenuItem openItem = new JMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_OPEN));
        openItem.setBorder(BORDER);
        openItem.addActionListener(new TextAction(Constants.STRING_EMPTY)
        {
            private static final long serialVersionUID = -8597151290962363254L;

            @Override
            public void actionPerformed(final ActionEvent e)
            {
                final String ticket = issue.getTicket();
                if (ticket != null && !ticket.isEmpty())
                {
                    try
                    {
                        final URIBuilder builder = Client.getURIBuilder(ServicePath.URL, ticket);
                        openWebpage(builder.build());
                    }
                    catch (final URISyntaxException ex)
                    {
                        Log.severe(ex.getMessage(), ex);
                    }
                }
            }

            private void openWebpage(final URI uri)
            {
                final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
                if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE))
                {
                    try
                    {
                        desktop.browse(uri);
                    }
                    catch (final Exception e)
                    {
                        Log.severe(e.getMessage(), e);
                    }
                }
            }
        });
        BaseAction.setButtonIcon(openItem, Icon.OPEN);

        menu.add(openItem);
        timeTracker.addStarItem(menu, parent, issue);
        menu.add(copyItem);
        menu.add(editItem);
        timeTracker.addInProgressItem(menu, parent, issue);
        timeTracker.addRedoItem(menu, parent);
        menu.addSeparator();

        final JMenuItem deleteItem = new JMenuItem(Resource.getString(PropertyConstants.TOOLTIP_DELETE));
        deleteItem.setBorder(BORDER);
        deleteItem.setEnabled(issue.isDeletable());
        BaseAction.setButtonIcon(deleteItem, Icon.REMOVE);
        deleteItem.addActionListener(new DeleteButtonAction(parent, issue));
        menu.add(deleteItem);
        return menu;
    }
}