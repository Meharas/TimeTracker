package timetracker.dialogs;

import timetracker.Constants;
import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.actions.BaseAction;
import timetracker.client.Client;
import timetracker.data.Issue;
import timetracker.data.WorkItemType;
import timetracker.db.Backend;
import timetracker.menu.TypeRenderer;
import timetracker.utils.EscapeEvent;
import timetracker.utils.LookAndFeelManager;
import timetracker.utils.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Dialog zum Burnen der Zeit eines Issues
 */
public class BurnIssueDialog extends JFrame
{
    private final Issue issue;

    public BurnIssueDialog(final JButton button, final JLabel label, final Issue issue) throws HeadlessException
    {
        super("Burning time");
        this.issue = issue;

        final TimeTracker timeTracker = TimeTracker.getTimeTracker();
        final Point location = timeTracker.getWindowLocation();

        setBounds(location.x, location.y, 400, 350);
        setResizable(false);
        setAlwaysOnTop(true);
        EscapeEvent.add(this);

        final JTextField ticketField = new JTextField();
        ticketField.setMargin(new Insets(0, 5, 0, 5));
        ticketField.setMaximumSize(new Dimension(350, 100));
        ticketField.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0
        ticketField.setBackground(TimeTracker.MANDATORY);
        ticketField.setForeground(LookAndFeelManager.getFontColor());

        final String buttonText = button.getText();
        TimeTracker.MATCHER.reset(buttonText);
        if (TimeTracker.MATCHER.matches())
        {
            ticketField.setText(TimeTracker.MATCHER.group(1));
        }
        else
        {
            final String ticket = issue.getTicket();
            ticketField.setText(ticket);
        }

        final JTextField timeField = new JTextField();
        timeField.setMargin(new Insets(0, 5, 0, 5));
        timeField.setMaximumSize(new Dimension(350, 100));
        timeField.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0
        timeField.setBackground(TimeTracker.MANDATORY);
        timeField.setForeground(LookAndFeelManager.getFontColor());
        timeField.setText(getParsedTime(label.getText()));

        final JComboBox<WorkItemType> typeField = new JComboBox<>();
        typeField.setMaximumSize(new Dimension(350, 100));
        typeField.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0
        typeField.setRenderer(new TypeRenderer());

        final String type = issue.getType().getId();
        final List<WorkItemType> workItemTypes = WorkItemType.getTypes();

        for (final WorkItemType t : workItemTypes)
        {
            typeField.addItem(t);
            if (t.getId().equalsIgnoreCase(type))
            {
                typeField.setSelectedItem(t);
            }
        }

        final JPanel globalPanel = new JPanel();
        globalPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        globalPanel.setLayout(new BoxLayout(globalPanel, BoxLayout.Y_AXIS));

        final JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel(Resource.getString(PropertyConstants.LABEL_TICKET)));
        panel.add(ticketField);
        panel.add(new JLabel(Resource.getString(PropertyConstants.LABEL_TIME)));
        panel.add(timeField);
        panel.add(new JLabel(Resource.getString(PropertyConstants.LABEL_TYPE)));
        panel.add(typeField);
        globalPanel.add(panel);

        final JTextArea textArea = new JTextArea(5, 30);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0

        final JPanel commentPanel = new JPanel();
        commentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        commentPanel.setMinimumSize(new Dimension(200, 100));
        commentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0
        commentPanel.setLayout(new BoxLayout(commentPanel, BoxLayout.Y_AXIS));
        commentPanel.add(new JLabel(Resource.getString(PropertyConstants.LABEL_TEXT)));

        final JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setMaximumSize(new Dimension(525, 50));
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0
        commentPanel.add(scrollPane);
        globalPanel.add(commentPanel);

        final Action action = button.getAction();
        final BaseAction timerAction = action instanceof BaseAction ? (BaseAction) action : null;
        if (timerAction != null && timerAction.timer != null && timerAction.timer.isRunning())
        {
            timerAction.stopWithoutSave();
        }

        final JButton ok = new JButton(new AbstractAction(Resource.getString(PropertyConstants.TEXT_OK))
        {
            private static final long serialVersionUID = -2918616353182983419L;

            @Override
            public void actionPerformed(final ActionEvent e)
            {
                if (burnTime(ticketField, timeField, typeField, textArea))
                {
                    if(timerAction != null)
                    {
                        timerAction.reset();
                    }

                    final String savedDuration = issue.getDurationSaved();
                    timeTracker.setLabelTooltip(savedDuration, label);
                    dispose();
                }
            }
        });

        final JPanel okBtnPanel = new JPanel();
        okBtnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0
        okBtnPanel.add(ok);
        globalPanel.add(okBtnPanel);
        add(globalPanel);
    }


    /**
     * Liefert einen Formatierten String zum Burnen
     * @param currentDuration Die angezeigte Zeit
     * @return Formatierte Zeit zur Burnen
     */
    private String getParsedTime(final String currentDuration)
    {
        final int[] timeUnits = getTimeUnits(currentDuration);
        final int currentHours = timeUnits[0];
        final int currentMinutes = timeUnits[1];
        return getParsedTime(Integer.toString(Math.max(0, currentHours)), Integer.toString(Math.max(1, currentMinutes)));
    }

    private int[] getTimeUnits(final String time)
    {
        int currentHours = 0;
        int currentMinutes = 0;

        if (time != null && !time.isEmpty())
        {
            TimeTracker.TIME_MATCHER.reset(time);
            if (TimeTracker.TIME_MATCHER.matches())
            {
                //aufaddieren
                currentHours = Integer.parseInt(TimeTracker.TIME_MATCHER.group(1));
                currentMinutes = Integer.parseInt(TimeTracker.TIME_MATCHER.group(2));

                final int currentSeconds = Integer.parseInt(TimeTracker.TIME_MATCHER.group(3));
                if (currentSeconds > 0)
                {
                    currentMinutes += 1;
                }
            }
            else
            {
                TimeTracker.BURN_MATCHER.reset(time);
                if(TimeTracker.BURN_MATCHER.matches())
                {
                    currentHours = Integer.parseInt(TimeTracker.BURN_MATCHER.group(1));
                    currentMinutes = Integer.parseInt(TimeTracker.BURN_MATCHER.group(2));
                }
            }
        }
        return new int[]{currentHours, currentMinutes};
    }

    /**
     * Erzeugt einen Formatstring zum Burnen, ohne Sekunden
     * @param hours Stunden
     * @param minutes Minuten
     * @return Formatierter String zum Burnen
     */
    private String getParsedTime(final String hours, final String minutes)
    {
        return String.format("%sh %sm", hours, minutes);
    }

    /**
     * Burnt Zeit am Ticket
     * @param ticketField Feld mit dem Ticket
     * @param timeField Feld mit der Zeit
     * @param typeField Feld mit dem Typ
     * @param textArea Feld mit dem Kommentar
     * @return <code>true</code>, wenn erfolgreich, sonst <code>false</code>
     */
    private boolean burnTime(final JTextField ticketField, final JTextField timeField, final JComboBox<WorkItemType> typeField, final JTextArea textArea)
    {
        final String ticket = ticketField.getText();
        final String spentTime = timeField.getText();
        final Object selectedItem = typeField.getSelectedItem();

        if (selectedItem == null || ticket.isEmpty() || spentTime.isEmpty())
        {
            return false;
        }

        this.issue.setTicket(ticket);
        this.issue.setType((WorkItemType) selectedItem);

        final int[] spentTimeUnits = getTimeUnits(spentTime);
        int spentHours = spentTimeUnits[0];
        int spentMinutes = spentTimeUnits[1];

        final String savedDuration = this.issue.getDurationSaved();
        final int[] savedTimeUnits = getTimeUnits(savedDuration);
        final int savedHours = savedTimeUnits[0];
        int savedMinutes = savedTimeUnits[1];

        final int additionalHours = savedHours / 60;
        savedMinutes = savedMinutes % 60;

        spentHours += savedHours + additionalHours;
        spentMinutes += savedMinutes;

        spentHours += spentMinutes / 60;
        spentMinutes = spentMinutes % 60;

        final String parsedTime = getParsedTime(Integer.toString(spentHours), Integer.toString(spentMinutes));
        this.issue.setDurationSaved(parsedTime);

        try
        {
            Backend.getInstance().updateIssue(this.issue);
        }
        catch (final Throwable t)
        {
            Util.handleException(t);
        }

        if (!Client.hasToken())
        {
            JOptionPane.showMessageDialog(TimeTracker.getTimeTracker(), String.format("Authorization token not found! Please create a token in youtrack and enter it in the " +
                                                                                      "TimeTracker.properties with the key %s", Constants.YOUTRACK_TOKEN));
            return false;
        }

        String text = textArea.getText();
        if (text == null)
        {
            text = Constants.STRING_EMPTY;
        }

        try
        {
            return Client.setSpentTime(ticket, spentTime, this.issue.getType().getId(), text);
        }
        catch (final URISyntaxException | IOException e)
        {
            Util.handleException(e);
        }
        return false;
    }
}
