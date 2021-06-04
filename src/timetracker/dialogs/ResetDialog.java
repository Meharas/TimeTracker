package timetracker.dialogs;

import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.actions.BaseAction;
import timetracker.utils.EscapeEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Rückfragedialog zum Zurücksetzen aller Zeiten
 */
public class ResetDialog extends JFrame
{
    public ResetDialog(final JButton button) throws HeadlessException
    {
        super(Resource.getString(PropertyConstants.TEXT_CONFIRMATION));

        setAlwaysOnTop(true);
        setLocation(100, 100);
        setResizable(false);
        setPreferredSize(new Dimension(350, 120));

        EscapeEvent.add(this);

        final JPanel addButtonPanel = new JPanel();
        addButtonPanel.setLayout(new BoxLayout(addButtonPanel, BoxLayout.Y_AXIS));
        addButtonPanel.setBorder(new EmptyBorder(0, 10, 0, 10));
        add(addButtonPanel);

        final JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
        labelPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        addButtonPanel.add(labelPanel);

        final JButton ok = new JButton(Resource.getString(PropertyConstants.TEXT_YES));
        ok.addActionListener(e1 -> {
            BaseAction.resetTimers(button);
            dispose();
        });

        final JButton cancel = new JButton(Resource.getString(PropertyConstants.TEXT_NO));
        cancel.addActionListener(e1 -> dispose());

        final JPanel btnPanel = new JPanel();
        btnPanel.setBorder(new EmptyBorder(10, 10, 20, 10));
        btnPanel.add(ok);
        btnPanel.add(cancel);
        addButtonPanel.add(btnPanel);

        final JLabel label = new JLabel(Resource.getString(PropertyConstants.TEXT_RESET));
        label.setPreferredSize(new Dimension(200, 25));
        labelPanel.add(label);

        pack();
        setVisible(true);
        setAlwaysOnTop(true);

        final TimeTracker timeTracker = TimeTracker.getTimeTracker();
        final Frame topMostFrame = timeTracker.getParentFrame(button);
        if(topMostFrame != null)
        {
            setLocation(topMostFrame.getX() - ((getWidth() - topMostFrame.getWidth()) / 2), timeTracker.getY());
        }
    }
}
