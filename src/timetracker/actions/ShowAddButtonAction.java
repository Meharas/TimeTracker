package timetracker.actions;

import timetracker.*;
import timetracker.icons.IconFileFilter;
import timetracker.log.Log;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * Klasse zur Darstellung eines Dialoges, mit welchem ein neues Ticket angelegt werden kann.
 */
public class ShowAddButtonAction extends BaseAction
{
    private static final long serialVersionUID = -2104627297533100111L;

    public ShowAddButtonAction()
    {

    }

    public ShowAddButtonAction(final JButton button)
    {
        super(button);
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final JFileChooser chooser = new JFileChooser();
        chooser.setPreferredSize(new Dimension(600, 300));
        chooser.setMultiSelectionEnabled(true);
        chooser.setControlButtonsAreShown(false);
        chooser.setFileFilter(new IconFileFilter());

        final JTextField labelField = new JTextField();
        labelField.setPreferredSize(new Dimension(200, 25));
        labelField.setBackground(TimeTracker.MANDATORY);

        final JButton ok = new JButton(Resource.getString(PropertyConstants.TEXT_OK));
        final String name = this.button != null ? this.button.getName() : null;
        if(name != null && name.startsWith(TimeTrackerConstants.PREFIX_BUTTON))
        {
            labelField.setText(this.button.getText());
            ok.setAction(new EditAction(ok, this.button, labelField, chooser));
        }
        else
        {
            ok.setAction(new AddAction(ok, labelField, chooser));

            final boolean issueFound = setLabelFromClipboard(labelField);
            if(issueFound)
            {
                ok.doClick();
                return;
            }
        }

        final JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setLocation(100, 100);

        this.timeTracker.addEscapeEvent(frame);

        final JPanel addButtonPanel = new JPanel();
        addButtonPanel.setLayout(new BoxLayout(addButtonPanel, BoxLayout.Y_AXIS));
        addButtonPanel.setBorder(new EmptyBorder(0, 10, 0, 10));
        frame.add(addButtonPanel);

        final JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
        labelPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        addButtonPanel.add(labelPanel);
        addButtonPanel.add(chooser);

        final JPanel okBtnPanel = new JPanel();
        okBtnPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        okBtnPanel.add(ok);
        addButtonPanel.add(okBtnPanel);

        final JLabel label = new JLabel("Label");
        label.setPreferredSize(new Dimension(50, 25));
        labelPanel.add(label);
        labelPanel.add(labelField);

        frame.pack();
        frame.setVisible(true);
        frame.setAlwaysOnTop(true);

        final Frame topMostFrame = this.timeTracker.getParentFrame(this.button);
        if(topMostFrame != null)
        {
            frame.setLocation(topMostFrame.getX() - ((frame.getWidth() - topMostFrame.getWidth()) / 2), timeTracker.getY());
        }
    }

    /**
     * Fügt das Ticket aus der Zwischenablage ein, insofern es sich um ein Ticket handelt
     * @param textField Textfeld, in welches die Ticketnummer eingefügt werden soll.
     * @return <code>true</code>, wenn das Ticket eingefügt wurde, sonst <code>false</code>
     */
    private boolean setLabelFromClipboard(final JTextField textField)
    {
        final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        final Transferable contents = cb.getContents(this);
        if(contents != null)
        {
            try
            {
                final Object transferData = contents.getTransferData(DataFlavor.stringFlavor);
                if(transferData instanceof String)
                {
                    TimeTracker.MATCHER.reset((String) transferData);
                    if(TimeTracker.MATCHER.matches())
                    {
                        textField.setText(TimeTracker.MATCHER.group(1));
                        return true;
                    }
                }
            }
            catch (final UnsupportedFlavorException | IOException ex)
            {
                Log.severe(ex.getMessage(), ex);
            }
        }
        return false;
    }
}