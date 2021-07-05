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
 * Dialog zum Einf�gen eines Tickets aus der Zwischenablage
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

        final JPanel buttonPanel = new JPanel(new GridLayout(1, 3));
        buttonPanel.setBorder(new EmptyBorder(20, 10, 10, 10));
        rows.add(buttonPanel);

        final JButton noButton = new JButton(Resource.getString(PropertyConstants.TEXT_NO));
        noButton.addActionListener((final ActionEvent event) -> {
            createButton(ticket, icon);
            dispose();
        });

        final JButton yesButton = new JButton(Resource.getString(PropertyConstants.TEXT_YES));
        yesButton.addActionListener((final ActionEvent event) -> {
            try
            {
                Client.setInProgress(ticket);
                dispose();

                final JButton button = createButton(ticket, icon);
                Optional.ofNullable(button).ifPresent(AbstractButton::doClick);
            }
            catch (final URISyntaxException | IOException ex)
            {
                Log.severe(ex.getMessage(), ex);
            }
        });

        final JButton cancelButton = new JButton(Resource.getString(PropertyConstants.TEXT_CANCEL));
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        buttonPanel.add(cancelButton);

        final Dimension size = getPreferredSize();
        setBounds(Util.getPopUpLocation(size.width, size.height));

        SwingUtilities.getRootPane(yesButton).setDefaultButton(yesButton);

        pack();
    }

    private JButton createButton(final String text, final String icon)
    {
        String ticket = null;
        if(TimeTracker.matches(text))
        {
            ticket = TimeTracker.MATCHER.group(1);
        }

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
