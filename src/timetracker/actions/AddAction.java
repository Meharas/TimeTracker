package timetracker.actions;

import timetracker.Constants;
import timetracker.TimeTracker;
import timetracker.client.Client;
import timetracker.data.Issue;
import timetracker.db.Backend;

import javax.swing.*;
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
    private final JTextField textInput;
    private final JFileChooser icon;

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

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final String text = createButtonText();
        if(text == null)
        {
            return;
        }
        try
        {
            if (Client.setInProgress(text))
            {
                final JButton button = createButton(text);
                Optional.ofNullable(button).ifPresent(AbstractButton::doClick);
            }
        }
        catch (final IOException | URISyntaxException ex)
        {
            TimeTracker.handleException(ex);
        }
    }

    protected String createButtonText()
    {
        return TimeTracker.getTicketSummary(this.textInput);
    }

    protected JButton createButton(String text)
    {
        text = Client.getTicketSummary(text);
        if(text == null)
        {
            return null;
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
}