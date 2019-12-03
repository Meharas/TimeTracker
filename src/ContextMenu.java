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
 *
 * @author $Author: beyera $ &copy; forcont business technology gmbh 2001-2019
 * @version $Revision: 1.0 $ $Date: 08.02.2019 11:04 $
 * @since 7.0
 */
public class ContextMenu
{
    private static final EmptyBorder BORDER = new EmptyBorder(5, 5, 5, 5);
    private static final EmptyBorder MENU_BORDER = new EmptyBorder(0, 0, 0, 0);

    private ContextMenu()
    {
    }

    static JPopupMenu create(final TimeTracker timeTracker, final JButton parent, final int id, final String key)
    {
        final JPopupMenu menu = new JPopupMenu();
        menu.setBorder(MENU_BORDER);

        final JMenuItem copyItem = new JMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_COPY));
        copyItem.setBorder(BORDER);
        timeTracker.setButtonIcon(copyItem, Icon.COPY);

        copyItem.addActionListener((final ActionEvent e) -> {
            final StringSelection stringSelection = new StringSelection(parent.getText());
            final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            cb.setContents(stringSelection, stringSelection);
        });

        final JMenuItem editItem = new JMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_EDIT));
        editItem.setBorder(BORDER);
        editItem.addActionListener(timeTracker.createShowAddButtonAction(parent));
        timeTracker.setButtonIcon(editItem, Icon.EDIT);

        final JMenuItem openItem = new JMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_OPEN));
        openItem.setBorder(BORDER);
        openItem.addActionListener(new TextAction(TimeTrackerConstants.STRING_EMPTY)
        {
            private static final long serialVersionUID = -8597151290962363254L;

            @Override
            public void actionPerformed(final ActionEvent e)
            {
                final String ticket = timeTracker.getTicket(key, parent);
                if (ticket != null && !ticket.isEmpty())
                {
                    try
                    {
                        final URIBuilder builder = timeTracker.getURIBuilder(ServicePath.URL, ticket);
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
        timeTracker.setButtonIcon(openItem, Icon.OPEN);

        menu.add(openItem);
        timeTracker.addStarItem(menu, parent, key, id);
        menu.add(copyItem);
        menu.add(editItem);
        timeTracker.addInProgressItem(menu, parent, id);
        timeTracker.addRedoItem(menu, parent);
        menu.addSeparator();

        final JMenuItem deleteItem = new JMenuItem(Resource.getString(PropertyConstants.TOOLTIP_DELETE));
        deleteItem.setBorder(BORDER);
        deleteItem.setEnabled(id > 3);
        timeTracker.setButtonIcon(deleteItem, Icon.REMOVE);
        deleteItem.addActionListener(timeTracker.createDeleteButtonAction(parent, key));
        menu.add(deleteItem);
        return menu;
    }
}