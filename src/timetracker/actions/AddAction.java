package timetracker.actions;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.TimeTrackerConstants;
import timetracker.client.Client;
import timetracker.log.Log;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static timetracker.TimeTracker.getPropertiesInputStream;

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
        final String filePath = file == null ? TimeTrackerConstants.STRING_EMPTY : file.getPath();
        final int missingNumber = getMissingNumber();
        final String counter = missingNumber < 10 ? ("0" + missingNumber) : Integer.toString(missingNumber);

        final Properties properties = new Properties();
        try(final InputStream inputStream = getPropertiesInputStream(TimeTrackerConstants.PROPERTIES))
        {
            properties.load(inputStream);
        }

        JButton button = null;
        try
        {
            final String propertyKey = TimeTrackerConstants.PREFIX_BUTTON + counter;
            this.timeTracker.storeButtonProperties(properties, propertyKey, text, filePath);

            button = this.timeTracker.addButton(propertyKey, text, filePath);
            this.timeTracker.updateGui(false);
            this.timeTracker.increaseLine();
        }
        catch (final IOException ex)
        {
            Log.severe(ex.getMessage(), ex);
        }

        final Frame frame = this.timeTracker.getParentFrame(this.button);
        if(frame != null)
        {
            frame.dispose();
        }
        return button;
    }

    /**
     * Liefert alle internen IDs der Buttons
     * @return interne IDs der Buttons
     * @throws IOException Wenn beim Lesen der Properties etwas schief ging
     */
    private Set<Integer> getButtonIds() throws IOException
    {
        final Properties properties = new Properties();
        try (final InputStream inputStream = getPropertiesInputStream(TimeTrackerConstants.PROPERTIES))
        {
            properties.load(inputStream);
        }

        final Set<Integer> buttonIds = new TreeSet<>();
        for(String key : properties.stringPropertyNames())
        {
            if(!key.startsWith(TimeTrackerConstants.PREFIX_BUTTON))
            {
                continue;
            }
            key = key.substring(TimeTrackerConstants.PREFIX_BUTTON.length());
            buttonIds.add(Integer.parseInt(key.substring(0, key.indexOf('.'))));
        }
        return buttonIds;
    }

    /**
     * Liefert eine fehlende oder die nächst höhere ID
     * @return eine fehlende oder die nächst höhere ID
     * @throws IOException Wenn beim Lesen der Properties etwas schief ging
     */
    private int getMissingNumber() throws IOException
    {
        final Set<Integer> buttonIds = getButtonIds();
        if(buttonIds.isEmpty())
        {
            return 0;
        }
        int lastNumber = 0;
        int missingNumber = -1;
        for(final int id : buttonIds)
        {
            lastNumber = id;
            if(id - missingNumber > 1)
            {
                return ++missingNumber;
            }
            missingNumber = id;
        }
        return ++lastNumber;
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
                request.setEntity(new StringEntity(String.format(TimeTrackerConstants.ISSUE_COMMAND, TimeTrackerConstants.ISSUE_STATE,
                                                                 TimeTrackerConstants.ISSUE_VALUE_STATE_PROGRESS, issueID), ContentType.APPLICATION_JSON));

                final HttpResponse response = Client.executeRequest(request);
                if (response == null)
                {
                    return;
                }
                Client.logResponse(response);
                dialog.dispose();

                final JButton button = createButton(text, getIssueSummary);
                button.doClick();
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