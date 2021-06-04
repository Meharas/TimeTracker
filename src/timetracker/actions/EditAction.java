package timetracker.actions;

import timetracker.TimeTracker;
import timetracker.Constants;
import timetracker.data.Issue;
import timetracker.db.Backend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Action zum Bearbeiten eins Tickets
 */
public class EditAction extends BaseAction
{
    private static final long serialVersionUID = -7024916220743619039L;
    private final JButton issueButton;
    private final JTextField textInput;
    private final JFileChooser icon;

    public EditAction(final Issue issue, final JButton okButton, final JButton issueButton, final JTextField textInput, final JFileChooser icon)
    {
        super(okButton, issue);
        this.issueButton = issueButton;
        this.textInput = textInput;
        this.icon = icon;
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final File file = this.icon.getSelectedFile();
        final String filePath = file == null ? Constants.STRING_EMPTY : file.getPath();
        final String text = TimeTracker.getTicketSummary(this.textInput);

        this.issue.setLabel(text);
        this.issue.setIcon(filePath);
        try
        {
            Backend.getInstance().updateIssue(this.issue);
        }
        catch (final Throwable t)
        {
            TimeTracker.handleException(t);
        }

        this.issueButton.setText(text);
        setButtonIcon(this.issueButton, filePath);

        final Frame frame = this.timeTracker.getParentFrame(this.button);
        if(frame != null)
        {
            frame.dispose();
        }
    }
}