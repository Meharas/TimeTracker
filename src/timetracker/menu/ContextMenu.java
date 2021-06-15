package timetracker.menu;

import timetracker.Constants;
import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.actions.AddAction;
import timetracker.actions.BaseAction;
import timetracker.actions.OpenUrlAction;
import timetracker.actions.ShowAddButtonAction;
import timetracker.buttons.IssueButton;
import timetracker.client.Client;
import timetracker.data.Issue;
import timetracker.db.Backend;
import timetracker.icons.Icon;
import timetracker.log.Log;
import timetracker.utils.ClipboardMonitor;
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

    public static JPopupMenu create(final IssueButton button, final Issue issue)
    {
        final JPopupMenu menu = new JPopupMenu();
        menu.setBorder(MENU_BORDER);

        final boolean matches = TimeTracker.matches(issue.getTicket());
        final JMenuItem copyItem = new ContextMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_COPY), Icon.COPY);
        copyItem.setBorder(BORDER);
        copyItem.setEnabled(matches);

        copyItem.addActionListener((final ActionEvent e) -> {
            final StringSelection stringSelection = new StringSelection(button.getText());
            final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            ClipboardMonitor.disabled = true;
            cb.setContents(stringSelection, stringSelection);
        });

        final JMenuItem editItem = new ContextMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_EDIT), Icon.EDIT);
        editItem.setBorder(BORDER);
        editItem.addActionListener(new ShowAddButtonAction(button, issue));

        final JMenuItem removeIcon = new ContextMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_REMOVE_ICON), Icon.REMOVE_ICON);
        removeIcon.setEnabled(issue.getIcon() != null && !issue.getIcon().isEmpty());
        removeIcon.setBorder(BORDER);
        removeIcon.addActionListener(e -> {
            try
            {
                issue.setIcon(null);
                Backend.getInstance().updateIssue(issue);
            }
            catch (final Throwable t)
            {
                Util.handleException(t);
            }
        });

        final JMenuItem openItem = new ContextMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_OPEN), Icon.OPEN);
        openItem.setBorder(BORDER);
        openItem.setEnabled(matches);
        openItem.addActionListener(new OpenUrlAction(issue));

        final JMenuItem starItem = new ContextMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_STAR), Icon.STAR);
        starItem.setVisible(issue.isDeletable() && !issue.isMarked() && !issue.isInProgress());
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
                }
                catch (final Throwable t)
                {
                    Util.handleException(t);
                }
            }
        });

        final JMenuItem unStarItem = new ContextMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_UNSTAR), Icon.UNSTAR);
        unStarItem.setVisible(issue.isDeletable() && issue.isMarked() && !issue.isInProgress());
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
                }
                catch (final Throwable t)
                {
                    Util.handleException(t);
                }
            }
        });

        final JMenuItem redoItem = new ContextMenuItem(Resource.getString(PropertyConstants.TOOLTIP_REDO), Icon.STOP);
        redoItem.setBorder(BORDER);
        redoItem.addActionListener((final ActionEvent event) -> {
            final Action a = button.getAction();
            ((BaseAction) a).reset();
        });

        menu.add(openItem);
        menu.add(starItem);
        menu.add(unStarItem);
        menu.add(copyItem);
        menu.add(editItem);
        menu.add(removeIcon);
        addInProgressItem(menu, button, issue);
        menu.add(redoItem);
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
            final JMenuItem inProgressItem = new ContextMenuItem(Resource.getString(PropertyConstants.LABEL_IN_PROGRESS), Icon.PROGRESS);
            inProgressItem.setBorder(BORDER);
            inProgressItem.setEnabled(issueState != null && !Constants.ISSUE_VALUE_STATE_PROGRESS.equalsIgnoreCase(issueState));
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