package timetracker.dialogs;

import timetracker.Constants;
import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.actions.AddAction;
import timetracker.actions.EditAction;
import timetracker.buttons.IssueButton;
import timetracker.data.Issue;
import timetracker.icons.IconFileFilter;
import timetracker.misc.LimitedTextArea;
import timetracker.utils.EscapeEvent;
import timetracker.utils.LookAndFeelManager;
import timetracker.utils.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;

/**
 * Dialog zum Erfassen eines Tickets
 */
public class AddIssueDialog extends JFrame
{
    public AddIssueDialog(final JButton button, final Issue issue) throws HeadlessException
    {
        super(issue == null ? Resource.getString(PropertyConstants.LABEL_ADD) : Resource.getString(PropertyConstants.MENU_ITEM_EDIT));

        setAlwaysOnTop(true);
        setResizable(false);
        EscapeEvent.add(this);

        final JFileChooser chooser = new JFileChooser();
        chooser.setPreferredSize(new Dimension(600, 300));
        chooser.setMultiSelectionEnabled(false);
        chooser.setControlButtonsAreShown(false);
        chooser.setFileFilter(new IconFileFilter());
        chooser.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JTextField labelField = new JTextField();
        labelField.setForeground(LookAndFeelManager.getFontColor());
        labelField.setPreferredSize(new Dimension(200, 30));
        labelField.setBackground(TimeTracker.MANDATORY);
        labelField.setDocument(new LimitedTextArea());
        labelField.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JTextArea description = new JTextArea(5,30);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setDocument(new LimitedTextArea());
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowOpened(final WindowEvent e )
            {
                labelField.requestFocus();
            }
        });

        final JButton ok = new JButton(Resource.getString(PropertyConstants.TEXT_OK));
        final boolean hasIssue = button instanceof IssueButton;
        if(hasIssue)
        {
            labelField.setText(button.getText());
            description.setText(Optional.ofNullable(issue).map(Issue::getDescription).orElse(Constants.STRING_EMPTY));
            ok.setAction(new EditAction(issue, ok, button, labelField, description, chooser));
        }
        else
        {
            ok.setAction(new AddAction(ok, labelField, description, chooser)
            {
                @Override
                protected JButton createButton(final String text)
                {
                    AddIssueDialog.this.dispose();

                    final String ticket = TimeTracker.getTicketFromText(text);
                    if(ticket != null)
                    {
                        final ClipboardDialog dialog = new ClipboardDialog(ticket);
                        dialog.setVisible(true);
                    }
                    super.createButton(text);
                    return null;
                }
            });
        }

        final JPanel okBtnPanel = new JPanel();
        okBtnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        okBtnPanel.add(ok);

        final JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
        rootPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        rootPanel.add(new JLabel(Resource.getString(PropertyConstants.LABEL_ISSUE)));
        rootPanel.add(labelField);
        rootPanel.add(new JPanel());
        rootPanel.add(new JLabel(Resource.getString(PropertyConstants.LABEL_DESCRIPTION)));
        rootPanel.add(description);
        rootPanel.add(new JPanel());
        rootPanel.add(new JLabel(Resource.getString(PropertyConstants.LABEL_ICON)));
        rootPanel.add(chooser);
        rootPanel.add(okBtnPanel);
        add(rootPanel);

        pack();

        final int width = getWidth();
        final int height = getHeight();
        final Point location = Util.getWindowLocation(new Dimension(width, height));
        setBounds(new Rectangle(location.x, location.y, width, height));
        SwingUtilities.getRootPane(ok).setDefaultButton(ok);
    }
}
