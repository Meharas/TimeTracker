package timetracker.actions;

import timetracker.TimeTracker;
import timetracker.TimeTrackerConstants;
import timetracker.log.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Action zum Bearbeiten eins Tickets
 */
public class EditAction extends BaseAction
{
    private static final long serialVersionUID = -7024916220743619039L;
    private final JButton issueButton;
    private final JTextField textInput;
    private final JFileChooser icon;

    public EditAction(final JButton okButton, final JButton issueButton, final JTextField textInput, final JFileChooser icon)
    {
        super(okButton);
        this.issueButton = issueButton;
        this.textInput = textInput;
        this.icon = icon;
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final File file = this.icon.getSelectedFile();
        final String filePath = file == null ? TimeTrackerConstants.STRING_EMPTY : file.getPath();
        final String text = TimeTracker.getTicketSummary(this.textInput);

        this.issueButton.setText(text);
        setButtonIcon(this.issueButton, filePath);

        final Properties properties = new Properties();
        try(final InputStream inputStream = TimeTracker.getPropertiesInputStream(TimeTrackerConstants.PROPERTIES))
        {
            properties.load(inputStream);
        }
        catch (final IOException ex)
        {
            Log.severe(ex.getMessage(), ex);
            return;
        }

        try
        {
            final String propertyKey = TimeTrackerConstants.PREFIX_BUTTON + this.issueButton.getName().substring(TimeTrackerConstants.PREFIX_BUTTON.length());
            properties.remove(propertyKey + TimeTrackerConstants.SUFFIX_LABEL);
            properties.remove(propertyKey + TimeTrackerConstants.SUFFIX_ICON);
            this.timeTracker.storeButtonProperties(properties, propertyKey, text, filePath);
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
    }
}