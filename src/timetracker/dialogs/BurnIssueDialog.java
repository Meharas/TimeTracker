package timetracker.dialogs;

import org.apache.http.client.HttpResponseException;
import timetracker.Constants;
import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.TimeTracker;
import timetracker.actions.BurnButtonAction;
import timetracker.actions.TimerAction;
import timetracker.buttons.DatePickerButton;
import timetracker.client.Client;
import timetracker.data.Issue;
import timetracker.data.WorkItemType;
import timetracker.db.Backend;
import timetracker.icons.Icon;
import timetracker.menu.ComboBoxWorkItems;
import timetracker.utils.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Optional;

/**
 * Dialog zum Burnen der Zeit eines Issues
 */
public class BurnIssueDialog extends JFrame
{
    private final Issue issue;

    public BurnIssueDialog(final BurnButtonAction action) throws HeadlessException
    {
        super("Burning time");
        this.issue = action.getIssue();

        setBounds(Util.getPopUpLocation(400, 400));
        setResizable(false);
        setAlwaysOnTop(true);
        EscapeEvent.add(this);

        final JButton button = action.getButton();
        final JTextField ticketField = createTextField(350, 100);
        ticketField.setText(Optional.ofNullable(TimeTracker.getTicketFromText(button.getText())).orElse(this.issue.getTicket()));

        final JLabel label = action.getLabel();
        final JTextField timeField = createTextField(350, 100);
        timeField.setText(getParsedTime(label.getText()));

        final JPanel globalPanel = new JPanel();
        globalPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        globalPanel.setLayout(new BoxLayout(globalPanel, BoxLayout.Y_AXIS));

        final ComboBoxWorkItems typeField = new ComboBoxWorkItems(issue);

        final JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel(Resource.getString(PropertyConstants.LABEL_TICKET)));
        panel.add(ticketField);
        panel.add(new JLabel(Resource.getString(PropertyConstants.LABEL_DATE)));

        final JPanel datePanel = new JPanel();
        datePanel.setLayout(new BoxLayout(datePanel, BoxLayout.X_AXIS));
        datePanel.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0
        panel.add(datePanel);

        final JTextField dateField = createTextField(315, 100);
        dateField.setText(DatePicker.DATE_FORMAT.format(new Date()));
        datePanel.add(dateField);

        final JButton date = new DatePickerButton(Icon.CALENDAR);
        date.setPreferredSize(new Dimension(20,20));
        date.addActionListener(ae -> dateField.setText(new DatePicker(BurnIssueDialog.this).getSelectedDate()));
        datePanel.add(date);

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

        final TimerAction timerAction = action.getButton().getAction();
        final DurationTimer timer = timerAction.getTimer();
        if (timer != null && timer.isRunning())
        {
            timerAction.stopWithoutSave();
        }

        final TimeTracker timeTracker = TimeTracker.getInstance();
        final JButton ok = new JButton(new AbstractAction(Resource.getString(PropertyConstants.TEXT_OK))
        {
            private static final long serialVersionUID = -2918616353182983419L;

            @Override
            public void actionPerformed(final ActionEvent e)
            {
                boolean success;
                try
                {
                    success = burnTime(ticketField, timeField, dateField, typeField, textArea);
                }
                catch (final HttpResponseException ex)
                {
                    success = true;
                    Util.handleException(ex);
                }
                if (success)
                {
                    timerAction.reset();

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

    private JTextField createTextField(final int width, final int height)
    {
        final JTextField textField = new JTextField();
        textField.setMargin(new Insets(0, 5, 0, 5));
        textField.setMaximumSize(new Dimension(width, height));
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0
        textField.setBackground(TimeTracker.MANDATORY);
        textField.setForeground(LookAndFeelManager.getFontColor());
        return textField;
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
     * @param dateField Feld mit dem Datum
     * @param typeField Feld mit dem Typ
     * @param textArea Feld mit dem Kommentar
     * @return <code>true</code>, wenn erfolgreich, sonst <code>false</code>
     * @throws HttpResponseException Fehler im Response, z.B. Ticket nicht gefunden
     */
    private boolean burnTime(final JTextField ticketField, final JTextField timeField, final JTextField dateField, final JComboBox<WorkItemType> typeField,
                             final JTextArea textArea) throws HttpResponseException
    {
        final String ticket = ticketField.getText();
        final String spentTime = timeField.getText();
        final String date = dateField.getText();
        final Object selectedItem = typeField.getSelectedItem();

        if (selectedItem == null || ticket.isEmpty() || spentTime.isEmpty() || date.isEmpty())
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
            JOptionPane.showMessageDialog(TimeTracker.getInstance(), String.format("Authorization token not found! Please create a token in youtrack and enter it in the " +
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
            return Client.setSpentTime(ticket, spentTime, date, this.issue.getType().getId(), text);
        }
        catch (final HttpResponseException e)
        {
            throw e;
        }
        catch (final URISyntaxException | IOException e)
        {
            Util.handleException(e);
        }
        return false;
    }
}
