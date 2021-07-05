package timetracker.actions;

import timetracker.Constants;
import timetracker.TimeTracker;
import timetracker.buttons.BaseButton;
import timetracker.data.Issue;
import timetracker.db.Backend;
import timetracker.utils.Util;

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
    private final JTextField label;
    private final JTextArea description;
    private final JFileChooser icon;

    public EditAction(final Issue issue, final JButton okButton, final JButton issueButton, final JTextField label, final JTextArea description, final JFileChooser icon)
    {
        super(okButton, issue);
        this.issueButton = issueButton;
        this.label = label;
        this.description = description;
        this.icon = icon;
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final File file = this.icon.getSelectedFile();
        final String filePath = file == null ? Constants.STRING_EMPTY : file.getPath();
        final String text = TimeTracker.getTicketSummary(this.label);

        this.issue.setLabel(text);
        if(file != null)
        {
            this.issue.setIcon(filePath);
        }

        this.issue.setDescription(this.description.getText());

        try
        {
            Backend.getInstance().updateIssue(this.issue);
        }
        catch (final Throwable t)
        {
            Util.handleException(t);
        }

        this.issueButton.setText(text);
        if (file != null)
        {
            ((BaseButton) this.issueButton).setIcon(filePath);
        }

        final TimeTracker timeTracker = TimeTracker.getInstance();
        final Frame frame = timeTracker.getParentFrame(this.button);
        if(frame != null)
        {
            frame.dispose();
        }
    }
}