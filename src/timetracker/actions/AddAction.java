package timetracker.actions;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.Constants;
import timetracker.client.Client;
import timetracker.data.Issue;
import timetracker.db.Backend;
import timetracker.log.Log;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Aktion zum Hinzufügen eines Tickets
 */
public class AddAction extends BaseAction
{
    private static final long serialVersionUID = 2109270279366930967L;
    private JTextField textInput;
    private JFileChooser icon;

    public AddAction(final JButton button)
    {
        this(button, null, null);
    }

    public AddAction(final JButton button, final JTextField textInput, final JFileChooser icon)
    {
        super( button);
        this.textInput = textInput;
        this.icon = icon;
    }

    public AddAction(final String text)
    {
        super(text);
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final String text = createButtonText();
        if(text == null)
        {
            return;
        }
        handleConfirmationDialog(text, true, false);
    }

    protected String createButtonText()
    {
        return TimeTracker.getTicketSummary(this.textInput);
    }

    protected JButton createButton(String text, final boolean getSummary) throws IOException
    {
        if(getSummary)
        {
            text = Client.getTicketSummary(text);
            if(text == null)
            {
                return null;
            }
        }

        final File file = this.icon == null ? null : this.icon.getSelectedFile();
        final String filePath = file == null ? Constants.STRING_EMPTY : file.getPath();

        String ticket = null;
        TimeTracker.MATCHER.reset(text);
        if(TimeTracker.MATCHER.matches())
        {
            ticket = TimeTracker.MATCHER.group(1);
        }

        final Issue issue = new Issue(ticket, text, null, null, null, filePath, true, false);
        JButton button = null;
        try
        {
            Backend.getInstance().insertIssue(issue);

            button = this.timeTracker.addButton(issue);
            this.timeTracker.updateGui(false);
            this.timeTracker.increaseLine();
        }
        catch (final Throwable t)
        {
            TimeTracker.handleException(t);
        }

        final Frame frame = this.timeTracker.getParentFrame(this.button);
        if(frame != null)
        {
            frame.dispose();
        }
        return button;
    }

    /**
     * Zeigt einen Dialog an, mit welchem ein Ticket in Bearbeitung genommen werden kann
     * @param text Text auf dem Button mit dem Issue
     */
    public void handleConfirmationDialog(final String text, final boolean createButtonOnCancel, final boolean getIssueSummary)
    {
        if (!TimeTracker.matches(text))
        {
            return;
        }

        Log.info(text);

        final String ticket = TimeTracker.MATCHER.group(1);
        final JDialog dialog = this.timeTracker.getDialog(250);

        final JPanel rows = new JPanel(new GridLayout(2, 1));
        rows.setBorder(new EmptyBorder(10, 10, 10, 10));
        dialog.add(rows);

        rows.add(new JLabel(Resource.getString(PropertyConstants.TICKET_IN_PROGRESS, ticket)));

        final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        rows.add(buttonPanel);

        final JButton cancelButton = new JButton(Resource.getString(PropertyConstants.TEXT_NO));
        cancelButton.addActionListener((final ActionEvent event) -> {
            if (createButtonOnCancel)
            {
                try
                {
                    createButton(text, getIssueSummary);
                }
                catch (final IOException ex)
                {
                    Log.severe(ex.getMessage(), ex);
                }
            }
            dialog.dispose();
        });

        final JButton okButton = new JButton(Resource.getString(PropertyConstants.TEXT_YES));
        okButton.addActionListener((final ActionEvent event) -> {
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
                dialog.dispose();

                final JButton button = createButton(text, getIssueSummary);
                Optional.ofNullable(button).ifPresent(AbstractButton::doClick);
            }
            catch (final URISyntaxException | IOException ex)
            {
                Log.severe(ex.getMessage(), ex);
            }
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        SwingUtilities.getRootPane(okButton).setDefaultButton(okButton);

        dialog.pack();
        dialog.setVisible(true);
    }
}