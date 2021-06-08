package timetracker.dialogs;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import timetracker.Constants;
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
                final String issueID = Client.getIssueID(ticket);
                if (issueID == null)
                {
                    Log.severe("Issue id of " + ticket + " not found");
                    return;
                }

                final URIBuilder builder = Client.getCommandURIBuilder();
                final HttpPost request = new HttpPost(builder.build());
                request.setEntity(new StringEntity(String.format(Constants.ISSUE_COMMAND, Constants.ISSUE_STATE,
                                                                 Constants.ISSUE_VALUE_STATE_PROGRESS, issueID), ContentType.APPLICATION_JSON));

                final HttpResponse response = Client.executeRequest(request);
                if (response == null)
                {
                    return;
                }
                Client.logResponse(response);
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

        final TimeTracker timeTracker = TimeTracker.getTimeTracker();

        final Dimension size = getPreferredSize();
        final int width = size.width;
        final int height = size.height;
        final int x = timeTracker.getX() + ((timeTracker.getWidth() - width) / 2);
        final int y = timeTracker.getY() + ((timeTracker.getHeight() - height) / 2);
        setBounds(new Rectangle(x, y, width, height));

        SwingUtilities.getRootPane(yesButton).setDefaultButton(yesButton);

        pack();
    }

    private JButton createButton(final String text, final String icon)
    {
        String ticket = null;
        TimeTracker.MATCHER.reset(text);
        if(TimeTracker.MATCHER.matches())
        {
            ticket = TimeTracker.MATCHER.group(1);
        }

        final Issue issue = new Issue(ticket, text, null, null, null, icon, true, false);
        JButton button = null;
        try
        {
            final String label = Client.getTicketSummary(text);
            issue.setLabel(label);

            Backend.getInstance().insertIssue(issue);

            final TimeTracker timeTracker = TimeTracker.getTimeTracker();
            button = timeTracker.addButton(issue);
            timeTracker.updateGui(false);
            timeTracker.increaseLine();
        }
        catch (final Throwable t)
        {
            Util.handleException(t);
        }
        return button;
    }
}
