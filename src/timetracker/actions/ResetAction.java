package timetracker.actions;

import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.utils.EscapeEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Setzt alle Time zurück
 */
public class ResetAction extends BaseAction
{
    private static final long serialVersionUID = -3128930442339113957L;

    public ResetAction(final JButton button)
    {
        super(button);
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final JFrame frame = new JFrame(Resource.getString(PropertyConstants.TEXT_CONFIRMATION));
        frame.setAlwaysOnTop(true);
        frame.setLocation(100, 100);
        frame.setResizable(false);
        frame.setPreferredSize(new Dimension(350, 120));

        EscapeEvent.add(frame);

        final JPanel addButtonPanel = new JPanel();
        addButtonPanel.setLayout(new BoxLayout(addButtonPanel, BoxLayout.Y_AXIS));
        addButtonPanel.setBorder(new EmptyBorder(0, 10, 0, 10));
        frame.add(addButtonPanel);

        final JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
        labelPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        addButtonPanel.add(labelPanel);

        final JButton ok = new JButton(Resource.getString(PropertyConstants.TEXT_YES));
        ok.addActionListener(e1 -> {
            resetTimers();
            frame.dispose();
        });

        final JButton cancel = new JButton(Resource.getString(PropertyConstants.TEXT_NO));
        cancel.addActionListener(e1 -> frame.dispose());

        final JPanel btnPanel = new JPanel();
        btnPanel.setBorder(new EmptyBorder(10, 10, 20, 10));
        btnPanel.add(ok);
        btnPanel.add(cancel);
        addButtonPanel.add(btnPanel);

        final JLabel label = new JLabel(Resource.getString(PropertyConstants.TEXT_RESET));
        label.setPreferredSize(new Dimension(200, 25));
        labelPanel.add(label);

        frame.pack();
        frame.setVisible(true);
        frame.setAlwaysOnTop(true);

        final Frame topMostFrame = this.timeTracker.getParentFrame(this.button);
        if(topMostFrame != null)
        {
            frame.setLocation(topMostFrame.getX() - ((frame.getWidth() - topMostFrame.getWidth()) / 2), this.timeTracker.getY());
        }
    }
}