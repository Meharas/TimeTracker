package timetracker.actions;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import timetracker.*;
import timetracker.client.Client;
import timetracker.data.Issue;
import timetracker.data.Type;
import timetracker.db.Backend;
import timetracker.log.Log;
import timetracker.menu.TypeRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Klasse zum Burnen von Zeiten
 */
public class BurnButtonAction extends BaseAction
{
    private static final long serialVersionUID = -2092965435624779543L;

    public BurnButtonAction(final JButton button, final JLabel label, final Issue issue)
    {
        super(button, issue, label);
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final Point location = this.timeTracker.getWindowLocation();

        final JDialog dialog = new JDialog(this.timeTracker, "Burning time", true);
        dialog.setBounds(location.x, location.y, 400, 350);
        dialog.setResizable(false);
        this.timeTracker.addEscapeEvent(dialog);

        final JTextField ticketField = new JTextField();
        ticketField.setMargin(new Insets(0, 5, 0, 5));
        ticketField.setMaximumSize(new Dimension(350, 100));
        ticketField.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0
        ticketField.setBackground(TimeTracker.MANDATORY);

        final String buttonText = this.button.getText();
        TimeTracker.MATCHER.reset(buttonText);
        if (TimeTracker.MATCHER.matches())
        {
            ticketField.setText(TimeTracker.MATCHER.group(1));
        }
        else
        {
            final String ticket = this.issue.getTicket();
            ticketField.setText(ticket);
        }

        final JTextField timeField = new JTextField();
        timeField.setMargin(new Insets(0, 5, 0, 5));
        timeField.setMaximumSize(new Dimension(350, 100));
        timeField.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0
        timeField.setBackground(TimeTracker.MANDATORY);
        timeField.setText(getParsedTime(this.label.getText()));

        final JComboBox<String[]> typeField = new JComboBox<>();
        typeField.setMaximumSize(new Dimension(350, 100));
        typeField.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0
        typeField.setRenderer(new TypeRenderer());

        final String type = Optional.ofNullable(this.issue.getType()).map(Type::getId).orElse(null);
        final LinkedList<String[]> items = Arrays.stream(Type.values()).map(t -> new String[]{t.getId(), t.getLabel()}).collect(Collectors.toCollection(LinkedList::new));

        for (final String[] item : items)
        {
            typeField.addItem(item);
            if (item[0].equalsIgnoreCase(type))
            {
                typeField.setSelectedItem(item);
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

        final Action action = this.button.getAction();
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

                    final String savedDuration = BurnButtonAction.this.issue.getDurationSaved();
                    BurnButtonAction.this.timeTracker.setLabelTooltip(savedDuration, label);
                    dialog.dispose();
                }
            }
        });

        final JPanel okBtnPanel = new JPanel();
        okBtnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0
        okBtnPanel.add(ok);
        globalPanel.add(okBtnPanel);
        dialog.add(globalPanel);
        dialog.setVisible(true);
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
    private boolean burnTime(final JTextField ticketField, final JTextField timeField, final JComboBox<String[]> typeField, final JTextArea textArea)
    {
        final String ticket = ticketField.getText();
        final String spentTime = timeField.getText();
        final Object selectedItem = typeField.getSelectedItem();

        if (selectedItem == null || ticket.isEmpty() || spentTime.isEmpty())
        {
            return false;
        }

        final String type = ((String[]) selectedItem)[0];
        this.issue.setTicket(ticket);
        this.issue.setType(Type.getType(type));

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
            TimeTracker.handleException(t);
        }

        if (!Client.hasToken())
        {
            JOptionPane.showMessageDialog(timeTracker, String.format("Authorization token not found! Please create a token in youtrack and enter it in the " +
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
            final URIBuilder builder = Client.getURIBuilder(ServicePath.WORKITEM, ticket);
            final HttpPost request = new HttpPost(builder.build());
            request.setEntity(new StringEntity(String.format(Constants.ENTITY, System.currentTimeMillis(), Client.getUserId(), spentTime, type, text), ContentType.APPLICATION_JSON));

            final HttpResponse response = Client.executeRequest(request);
            if (response == null)
            {
                return false;
            }
            Client.logResponse(response);
        }
        catch (final URISyntaxException | IOException e)
        {
            Log.severe(e.getMessage(), e);
        }

        return true;
    }
}