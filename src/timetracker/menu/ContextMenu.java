package timetracker.menu;

import timetracker.Constants;
import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.actions.*;
import timetracker.client.Client;
import timetracker.data.Issue;
import timetracker.db.Backend;
import timetracker.icons.Icon;
import timetracker.log.Log;
import timetracker.utils.ClipboardMonitor;
import timetracker.utils.LookAndFeelManager;
import timetracker.utils.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.TextAction;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.IOException;
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

    public static JPopupMenu create(final JButton parent, final Issue issue)
    {
        final JPopupMenu menu = new JPopupMenu();
        menu.setBorder(MENU_BORDER);

        final boolean matches = TimeTracker.matches(issue.getTicket());
        final JMenuItem copyItem = new JMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_COPY));
        copyItem.setBorder(BORDER);
        copyItem.setEnabled(matches);
        BaseAction.setButtonIcon(copyItem, Icon.COPY);

        copyItem.addActionListener((final ActionEvent e) -> {
            final StringSelection stringSelection = new StringSelection(parent.getText());
            final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            ClipboardMonitor.disabled = true;
            cb.setContents(stringSelection, stringSelection);
        });

        final JMenuItem editItem = new JMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_EDIT));
        editItem.setBorder(BORDER);
        editItem.addActionListener(new ShowAddButtonAction(parent, issue));
        BaseAction.setButtonIcon(editItem, Icon.EDIT);

        final JMenuItem openItem = new JMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_OPEN));
        openItem.setBorder(BORDER);
        openItem.setEnabled(matches);
        openItem.addActionListener(new OpenUrlAction(issue));
        BaseAction.setButtonIcon(openItem, Icon.OPEN);

        final JMenuItem starItem = new JMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_STAR));
        starItem.setBorder(BORDER);
        starItem.addActionListener(new TextAction(Constants.STRING_EMPTY)
        {
            private static final long serialVersionUID = -101044272648382148L;

            @Override
            public void actionPerformed(final ActionEvent e)
            {
                try
                {
                    issue.setMarked(true);
                    Backend.getInstance().updateIssue(issue);
                    parent.setBackground(LookAndFeelManager.getColorMarked());
                    parent.setForeground(LookAndFeelManager.getFontColor());
                }
                catch (final Throwable t)
                {
                    Util.handleException(t);
                }
            }
        });
        BaseAction.setButtonIcon(starItem, Icon.STAR);
        starItem.setVisible(issue.isDeletable() && !issue.isMarked() && !issue.isInProgress());

        final JMenuItem unStarItem = new JMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_UNSTAR));
        unStarItem.setBorder(BORDER);
        unStarItem.addActionListener(new TextAction(Constants.STRING_EMPTY)
        {
            private static final long serialVersionUID = -101044272648382148L;

            @Override
            public void actionPerformed(final ActionEvent e)
            {
                try
                {
                    issue.setMarked(false);
                    Backend.getInstance().updateIssue(issue);
                    parent.setBackground(null);
                    parent.setForeground(null);
                    parent.setOpaque(false);
                }
                catch (final Throwable t)
                {
                    Util.handleException(t);
                }
            }
        });
        BaseAction.setButtonIcon(unStarItem, Icon.UNSTAR);
        unStarItem.setVisible(issue.isDeletable() && issue.isMarked() && !issue.isInProgress());

        final JMenuItem redoItem = new JMenuItem(Resource.getString(PropertyConstants.TOOLTIP_REDO));
        redoItem.setBorder(BORDER);
        BaseAction.setButtonIcon(redoItem, Icon.STOP);
        redoItem.addActionListener((final ActionEvent event) -> {
            final Action a = parent.getAction();
            ((BaseAction) a).reset();
        });

        menu.add(openItem);
        menu.add(starItem);
        menu.add(unStarItem);
        menu.add(copyItem);
        menu.add(editItem);
        addInProgressItem(menu, parent, issue);
        menu.add(redoItem);
        menu.addSeparator();

        final JMenuItem deleteItem = new JMenuItem(Resource.getString(PropertyConstants.TOOLTIP_DELETE));
        deleteItem.setBorder(BORDER);
        deleteItem.setEnabled(issue.isDeletable());
        BaseAction.setButtonIcon(deleteItem, Icon.REMOVE);
        deleteItem.addActionListener(new DeleteButtonAction(parent, issue));
        menu.add(deleteItem);
        return menu;
    }

    /**
     * Fügt den Menüeintrag "In Bearbeitung nehmen" hinzu. Dabei wird geprüft, ob das Ticket nicht schon in Bearbeitung ist. Ausserdem ist diese Aktion
     * für die Standardaktionen nicht vorgesehen
     * @param menu Menü
     * @param button Issue-Button
     * @param issue Issue
     */
    private static void addInProgressItem(final JPopupMenu menu, final JButton button, final Issue issue)
    {
        try
        {
            final String issueState = issue.isDeletable() ? Client.getIssueState(button.getText()) : null;
            final JMenuItem inProgressItem = new JMenuItem(Resource.getString(PropertyConstants.LABEL_IN_PROGRESS));
            inProgressItem.setBorder(BORDER);
            inProgressItem.setEnabled(issueState != null && !Constants.ISSUE_VALUE_STATE_PROGRESS.equalsIgnoreCase(issueState));
            BaseAction.setButtonIcon(inProgressItem, Icon.PROGRESS);
            inProgressItem.addActionListener(new AddAction(button)
            {
                private static final long serialVersionUID = 922056815591098770L;

                @Override
                protected String createButtonText()
                {
                    return button.getText();
                }

                @Override
                protected JButton createButton(final String text)
                {
                    return button;
                }
            });
            menu.add(inProgressItem);
        }
        catch (final URISyntaxException | IOException ex)
        {
            Log.severe(ex.getMessage(), ex);
        }
    }
}