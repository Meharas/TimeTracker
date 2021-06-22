package timetracker.actions;

import timetracker.Constants;
import timetracker.TimeTracker;
import timetracker.client.Client;
import timetracker.data.Issue;
import timetracker.db.Backend;
import timetracker.utils.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Aktion zum Hinzuf�gen eines Tickets
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
            final JButton button = createButton(text);
            if (button != null)
            {
                Client.setInProgress(text);
                button.doClick();
            }
        }
        catch (final IOException | URISyntaxException ex)
        {
            Util.handleException(ex);
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

        final Issue issue = new Issue(ticket, text, null, null, null, filePath);
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
            Util.handleException(t);
        }

        final Frame frame = this.timeTracker.getParentFrame(this.button);
        if(frame != null)
        {
            frame.dispose();
        }
        return button;
    }
}