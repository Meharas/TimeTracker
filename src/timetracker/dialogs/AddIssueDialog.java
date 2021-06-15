package timetracker.dialogs;

import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.actions.AddAction;
import timetracker.actions.EditAction;
import timetracker.data.Issue;
import timetracker.icons.IconFileFilter;
import timetracker.utils.EscapeEvent;
import timetracker.utils.LookAndFeelManager;
import timetracker.utils.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

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
        chooser.setMultiSelectionEnabled(true);
        chooser.setControlButtonsAreShown(false);
        chooser.setFileFilter(new IconFileFilter());

        final JTextField labelField = new JTextField();
        labelField.setForeground(LookAndFeelManager.getFontColor());
        labelField.setPreferredSize(new Dimension(200, 30));
        labelField.setBackground(TimeTracker.MANDATORY);

        final JButton ok = new JButton(Resource.getString(PropertyConstants.TEXT_OK));
        final String name = button != null ? button.getName() : null;
        if(name != null)
        {
            labelField.setText(button.getText());
            ok.setAction(new EditAction(issue, ok, button, labelField, chooser));
        }
        else
        {
            ok.setAction(new AddAction(ok, labelField, chooser));
        }

        final JPanel addButtonPanel = new JPanel();
        addButtonPanel.setLayout(new BoxLayout(addButtonPanel, BoxLayout.Y_AXIS));
        addButtonPanel.setBorder(new EmptyBorder(0, 10, 0, 10));
        add(addButtonPanel);

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

        pack();

        final int width = getWidth();
        final int height = getHeight();
        final Point location = Util.getWindowLocation(new Dimension(width, height));
        setBounds(new Rectangle(location.x, location.y, width, height));
    }
}
