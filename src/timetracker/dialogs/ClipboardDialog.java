package timetracker.dialogs;

import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.client.Client;
import timetracker.data.Issue;
import timetracker.db.Backend;
import timetracker.log.Log;
import timetracker.utils.EscapeEvent;
import timetracker.utils.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Dialog zum Einfügen eines Tickets aus der Zwischenablage
 */
public class ClipboardDialog extends JFrame
{
    public ClipboardDialog(final String ticket)
    {
        this(ticket, null);
    }

    public ClipboardDialog(final String ticket, final String icon) throws HeadlessException
    {
        super(Resource.getString(PropertyConstants.TEXT_CONFIRMATION));

        setAlwaysOnTop(true);
        setResizable(false);
        EscapeEvent.add(this);

        final JPanel rows = new JPanel(new GridLayout(2, 1));
        rows.setBorder(new EmptyBorder(10, 10, 0, 10));
        add(rows);

        rows.add(new JLabel(Resource.getString(PropertyConstants.TICKET_IN_PROGRESS, ticket)));

        final JPanel buttonPanel = new JPanel(new GridLayout(1, 4));
        buttonPanel.setBorder(new EmptyBorder(20, 10, 10, 10));
        rows.add(buttonPanel);

        final JButton inProgress = new JButton(Resource.getString(PropertyConstants.TEXT_INPROGRESS));
        inProgress.addActionListener((final ActionEvent event) -> {
            try
            {
                Client.setInProgress(ticket);
                dispose();
            }
            catch (final URISyntaxException | IOException ex)
            {
                Log.severe(ex.getMessage(), ex);
            }
        });

        final JButton startTimer = new JButton(Resource.getString(PropertyConstants.TEXT_TIMER_START));
        startTimer.addActionListener((final ActionEvent event) -> {
            final JButton button = createButton(ticket, icon);
            Optional.ofNullable(button).ifPresent(AbstractButton::doClick);
            dispose();
        });

        final JButton both = new JButton(Resource.getString(PropertyConstants.TEXT_BOTH));
        both.addActionListener((final ActionEvent event) -> {
            try
            {
                Client.setInProgress(ticket);
                createButton(ticket, icon);
            }
            catch (final URISyntaxException | IOException ex)
            {
                Log.severe(ex.getMessage(), ex);
            }
            dispose();
        });

        final JButton cancelButton = new JButton(Resource.getString(PropertyConstants.TEXT_CANCEL));
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(inProgress);
        buttonPanel.add(startTimer);
        buttonPanel.add(both);
        buttonPanel.add(cancelButton);

        final Dimension size = getPreferredSize();
        setBounds(Util.getPopUpLocation(size.width, size.height));

        SwingUtilities.getRootPane(inProgress).setDefaultButton(inProgress);

        pack();
    }

    private JButton createButton(final String text, final String icon)
    {
        final String ticket = TimeTracker.getTicketFromText(text);
        final Issue issue = new Issue(ticket, text, null, null, null, icon);
        JButton button;
        try
        {
            final String label = Client.getTicketSummary(text);
            issue.setLabel(label);

            Backend.getInstance().insertIssue(issue);

            final TimeTracker timeTracker = TimeTracker.getInstance();
            button = timeTracker.addButton(issue);
            timeTracker.updateGui(false);
        }
        catch (final Throwable t)
        {
            Util.handleException(t);
            button = Util.getButton(issue);
        }
        return button;
    }
}
