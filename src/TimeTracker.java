import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.UnsupportedSchemeException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.TextAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TimeTracking-Tool
 *
 * ToDo Umlaute in Tickets
 *
 * @author $Author: BeyerA $ &copy; forcont business technology gmbh 2001-2018
 * @version $Revision: 1.0 $ $Date: 31.01.2018 07:26 $
 * @since 7.0
 */
public class TimeTracker extends Frame
{
    private static final long serialVersionUID = 7225687129886672540L;

    private static final String PATH_ISSUE = "api/issues/%s";
    private static final String PATH_COMMAND = "api/commands";
    private static final String PATH_WORKITEM = PATH_ISSUE + "/timeTracking/workItems";
    private static final String PATH_USER = "api/admin/users/me";
    private static final String PATH_URL = "issue/%s";

    private static final Pattern PATTERN = Pattern.compile("([SEP-|DEV-|FS-]+[0-9]+).*");
    private static final Matcher MATCHER = PATTERN.matcher("");
    private static final Pattern TIME_PATTERN = Pattern.compile(".*([0-9]+)h ([0-9]+)min ([0-9]+)s");
    private static final Matcher TIME_MATCHER = TIME_PATTERN.matcher("");
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d{2})h (\\d{2})min (\\d{2})s");
    private static final Matcher DURATION_MATCHER = DURATION_PATTERN.matcher("");

    private static final Color MANDATORY = new Color(239, 247, 249);

    private static final String ENTITY = "{" +
                                         "\"date\":%d," +
                                         "\"author\": {\"id\":\"%s\"}," +
                                         "\"duration\":{\"presentation\":\"%s\"}," +
                                         "\"type\":{\"id\":\"%s\"}," +
                                         "\"text\":\"%s\"}";

    private static final String IN_PROGRESS = "{\"query\":\"State: In Progress\",\n" +
                                              " \"issues\":[{\"id\":\"%s\"}],\n" +
                                              " \"silent\":false}";

    private static final String PROPERTIES = TimeTracker.class.getSimpleName() + ".properties";
    private static final String DEFAULT_PROPERTIES = TimeTracker.class.getSimpleName() + ".default.properties";
    private static final String LOGFILE_NAME = "TimeTracker.log";
    private static final String PREFIX_BUTTON = "button.";
    private static final String SUFFIX_LABEL = ".label";
    private static final String SUFFIX_ICON = ".icon";
    private static final String SUFFIX_TICKET = ".ticket";
    private static final String SUFFIX_TYPE = ".type";
    private static final String SUFFIX_TIME = ".time";
    private static final String ACTIONMAP_KEY_CANCEL = "Cancel";
    private static final String ISSUE_SUMMARY = "summary";

    private static final String YOUTRACK_SCHEME = "youtrack.scheme";
    private static final String YOUTRACK_PORT = "youtrack.port";
    private static final String YOUTRACK_HOST = "youtrack.host";
    private static final String YOUTRACK_USERID = "youtrack.userid";
    private static final String YOUTRACK_TOKEN = "youtrack.token";
    private static final String YOUTRACK_TOKEN_PREFIX = "perm:";

    private static final String DEFAULT_HOST = "youtrack";
    private static final String DEFAULT_SCHEME = "http";
    private static final String DEFAULT_PORT = "80";

    private static final transient Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    static
    {
        // suppress the logging output to the console
        final Logger rootLogger = Logger.getLogger("");
        final Handler[] handlers = rootLogger.getHandlers();
        if (handlers[0] instanceof ConsoleHandler)
        {
            //rootLogger.removeHandler(handlers[0]);
        }

        LOGGER.setLevel(Level.INFO);

        try
        {
            final FileHandler fileHandler = new FileHandler(LOGFILE_NAME, true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    enum Path
    {
        WORKITEM(PATH_WORKITEM),
        USER(PATH_USER),
        ISSUE(PATH_ISSUE),
        COMMAND(PATH_COMMAND),
        URL(PATH_URL);

        private String restEndPoint;

        Path(final String path)
        {
            this.restEndPoint = path;
        }
    }

    enum Icon
    {
        ADD("add.png"),
        STOP("stop.png"),
        LOG("log.png"),
        REMOVE("delete_grey.png"),
        BURN("burn.png"),
        OPEN("link.png");

        private String png;
        Icon(final String icon)
        {
            this.png = icon;
        }

        private String getIcon()
        {
            return "icons//" + this.png;
        }
    }

    private int line;
    private JPanel panel;
    private final Frame timeTracker;
    private final transient ResourceBundle bundle = ResourceBundle.getBundle("TimeTracker", new Locale((String) System.getProperties().get("user.language")));

    private final String token;
    private String userId;
    private final String host;
    private final String scheme;
    private final int port;

    private TimeTracker(final Properties properties)
    {
        super("Time Tracker");
        setMinimumSize(new Dimension(575, 0));
        setAlwaysOnTop(true);

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(final WindowEvent windowEvent)
            {
                saveWindowPositionAndSize();
            }
        });

        this.timeTracker = this;
        this.panel = new JPanel(new GridLayout(0, 1));

        this.scheme = properties.getProperty(YOUTRACK_SCHEME, DEFAULT_SCHEME);
        this.host = properties.getProperty(YOUTRACK_HOST, DEFAULT_HOST);
        this.port = Integer.parseInt(properties.getProperty(YOUTRACK_PORT, DEFAULT_PORT));
        this.token = properties.getProperty(YOUTRACK_TOKEN);

        try
        {
            this.userId = getUserID(properties);

            final JButton add = new JButton(this.bundle.getString("button.label.add"));
            setButtonIcon(add, Icon.ADD);
            add.setAction(new ShowAddButtonAction(add));

            final JButton reset = new JButton(this.bundle.getString("button.label.stop"));
            setButtonIcon(reset, Icon.STOP);
            reset.setAction(new ResetAction(reset));

            final JButton openLog = new JButton();
            openLog.setAction(new TextAction("Log")
            {
                private static final long serialVersionUID = 4641196350640457638L;

                @Override
                public void actionPerformed(final ActionEvent e)
                {
                    final File log = new File(LOGFILE_NAME);
                    if (log.exists())
                    {
                        final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
                        if (desktop != null && desktop.isSupported(Desktop.Action.OPEN))
                        {
                            try
                            {
                                desktop.open(log);
                            }
                            catch (IOException ex)
                            {
                                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                            }
                        }
                    }
                }
            });
            setButtonIcon(openLog, Icon.LOG);

            final JPanel globalActionsPanel = new JPanel(new GridLayout(1, 3));
            globalActionsPanel.add(add);
            globalActionsPanel.add(reset);
            globalActionsPanel.add(openLog);
            addToPanel(globalActionsPanel);
            this.line++;
            addToPanel(new JPanel()); //Spacer
            this.line++;

            final Map<String, String[]> labelMap = new TreeMap<>();
            for (final Map.Entry<Object, Object> entry : properties.entrySet())
            {
                final String key = (String) entry.getKey();
                if (key.startsWith(PREFIX_BUTTON) && key.endsWith(SUFFIX_LABEL))
                {
                    final String icon = properties.getProperty(key.replace(SUFFIX_LABEL, SUFFIX_ICON));
                    labelMap.put(key, new String[]{(String) entry.getValue(), icon});
                }
            }

            for (final Map.Entry<String, String[]> entry : labelMap.entrySet())
            {
                final String[] values = entry.getValue();
                final String key = entry.getKey();
                addButton(key.substring(0, key.lastIndexOf('.')), values[0], values[1]);
                this.line++;
            }

            add(this.panel);
            pack();
            restoreWindowPositionAndSize();
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    /**
     * Liefert die UserId entweder aus den Properties, oder wenn noch nicht vorhanden, vom Youtrack
     * @param properties Properties
     * @return UserId
     * @throws URISyntaxException Wenn das Parsen als URI-Referenz schief ging
     * @throws IOException I/O Error
     */
    private String getUserID(final Properties properties) throws URISyntaxException, IOException
    {
        final String userID = properties != null ? properties.getProperty(YOUTRACK_USERID) : null;
        if (userID != null && !userID.isEmpty())
        {
            return userID;
        }

        final URIBuilder builder = getUserURIBuilder();
        final HttpResponse response = execute(builder);
        if (response == null)
        {
            return null;
        }
        return getID(response, YOUTRACK_USERID);
    }

    private HttpResponse execute(final URIBuilder builder) throws IOException, URISyntaxException
    {
        final HttpUriRequest request = new HttpGet(builder.build());
        return executeRequest(request);
    }

    /**
     * Liefert einen URIBuilder zur Ermittlung der User-ID
     * @return URIBuilder
     */
    private URIBuilder getUserURIBuilder()
    {
        return getURIBuilder(Path.USER, null);
    }

    private JButton addButton(final String key, final String label, final String icon)
    {
        if (label == null || label.isEmpty())
        {
            return null;
        }

        final int id = Integer.parseInt(key.substring(PREFIX_BUTTON.length()));

        final JButton button = new JButton(label);
        setButtonIcon(button, icon);
        button.setHorizontalAlignment(SwingConstants.LEFT);

        final JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BorderLayout(0, 0));
        labelPanel.add(button, BorderLayout.CENTER);

        final JLabel timeLabel = new JLabel();
        timeLabel.setName(key);
        timeLabel.setPreferredSize(new Dimension(100, 20));
        timeLabel.setBorder(new EmptyBorder(0, 8, 0, 0));
        labelPanel.add(timeLabel, BorderLayout.EAST);

        setTime(timeLabel, key);

        final TimerAction timerAction = new TimerAction(button, key, timeLabel);
        button.setAction(timerAction);

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setMaximumSize(new Dimension(0, 30));
        buttonPanel.setLayout(new BorderLayout(0, 0));
        buttonPanel.add(labelPanel, BorderLayout.CENTER);

        final JPanel actionsPanel = new JPanel();
        //actionsPanel.setLayout(new GridLayout(1, 4));
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.X_AXIS));

        addAction(actionsPanel, new BurnButtonAction(button, timeLabel, key), this.bundle.getString("button.tooltip.burn"), Icon.BURN);

        final JButton action = addAction(actionsPanel);
        action.setAction(new TextAction("")
        {
            private static final long serialVersionUID = -8597151290962363254L;

            @Override
            public void actionPerformed(final ActionEvent e)
            {
                final String ticket = getTicket(key, button);
                if (ticket != null && !ticket.isEmpty())
                {
                    try
                    {
                        final URIBuilder builder = getURIBuilder(Path.URL, ticket);
                        openWebpage(builder.build());
                    }
                    catch (URISyntaxException ex)
                    {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                }
            }

            private void openWebpage(final URI uri)
            {
                final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
                if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE))
                {
                    try
                    {
                        desktop.browse(uri);
                    }
                    catch (Exception e)
                    {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            }
        });
        setButtonIcon(action, Icon.OPEN);

        actionsPanel.add(Box.createHorizontalStrut(10));
        addAction(actionsPanel, new AbstractAction()
        {
            private static final long serialVersionUID = 4641196350640457638L;

            @Override
            public void actionPerformed(final ActionEvent e)
            {
                timerAction.reset();
            }
        }, this.bundle.getString("button.tooltip.redo"), Icon.STOP);

        final JButton removeButton = addAction(actionsPanel, new DeleteButtonAction(button, key), this.bundle.getString("button.tooltip.delete"), Icon.REMOVE);
        removeButton.setEnabled(id > 3);

        buttonPanel.add(actionsPanel, BorderLayout.EAST);
        addToPanel(buttonPanel);
        return button;
    }

    private JButton addAction(final JPanel parent)
    {
        return addAction(parent, null, null, null);
    }

    private JButton addAction(final JPanel parent, final AbstractAction listener, final String tooltip, final Icon icon)
    {
        final JButton button = new JButton();
        button.setBorder(new EmptyBorder(8, 8, 8, 8));
        button.setToolTipText(tooltip);
        if (listener != null)
        {
            button.setAction(listener);
        }
        setButtonIcon(button, icon);
        parent.add(button);
        return button;
    }

    private void setTime(final JLabel label, final String key)
    {
        final Properties properties = new Properties();

        try (final InputStream inputStream = loadProperties(properties, PROPERTIES))
        {
            if (inputStream != null)
            {
                final String durationKey = getDurationKey(key);
                final Object value = properties.get(durationKey);
                if (value != null)
                {
                    label.setText((String) value);
                }
            }
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void setButtonIcon(final JButton button, final Icon icon)
    {
        if (icon == null)
        {
            return;
        }
        setButtonIcon(button, icon.getIcon());
    }

    private void setButtonIcon(final JButton button, final String icon)
    {
        if (icon == null || icon.isEmpty())
        {
            return;
        }
        try
        {
            try (final InputStream iconStream = getClass().getResourceAsStream(icon))
            {
                if (iconStream != null)
                {
                    button.setIcon(new ImageIcon(getBytes(iconStream)));
                    return;
                }
                final File file = new File(icon);
                if (file.exists())
                {
                    button.setIcon(new ImageIcon(icon));
                }
            }
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private byte[] getBytes(final InputStream in) throws IOException
    {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream())
        {
            int next;
            while ((next = in.read()) > -1)
            {
                bos.write(next);
            }
            bos.flush();
            return bos.toByteArray();
        }
    }

    private void updateGui()
    {
        SwingUtilities.updateComponentTreeUI(this);
        setVisible(false);
        this.panel.updateUI();
        revalidate();
        pack();
        this.panel.updateUI();
        setVisible(true);
    }

    private void addToPanel(final JComponent button)
    {
        final GridLayout layout = (GridLayout) this.panel.getLayout();
        layout.setRows(layout.getRows() + 1);
        this.panel.add(button, this.line);
    }

    private class TimerAction extends AbstractAction
    {
        private static final long serialVersionUID = -3232149832497004005L;

        final Timer timer;
        final JButton button;
        final String key;
        final JLabel label;
        String text;
        int duration = 0;

        TimerAction(final JButton button)
        {
            super(button.getText(), button.getIcon());
            this.button = button;
            this.timer = null;
            this.label = null;
            this.key = null;
        }

        TimerAction(final JButton button, final String key, final JLabel label)
        {
            super(button.getText(), button.getIcon());

            this.button = button;
            this.key = key;
            this.label = label;
            this.text = button.getText();

            this.timer = new Timer(1000, e -> formatTime(++this.duration));

            if (label != null)
            {
                setDuration(label.getText());
            }
        }

        private void formatTime(final int duration)
        {
            final String time = this.label.getText();
            if(duration == 0 && time != null && !time.isEmpty())
            {
                return;
            }
            final Duration dur = Duration.ofSeconds(duration);
            final long seconds = dur.getSeconds();
            this.label.setText(String.format("%02dh %02dmin %02ds", seconds / 3600, (seconds % 3600) / 60, seconds % 60));
        }

        private void setDuration(final String time)
        {
            if (time == null || time.isEmpty())
            {
                return;
            }

            DURATION_MATCHER.reset(time);
            if (DURATION_MATCHER.matches())
            {
                final int h = Integer.parseInt(DURATION_MATCHER.group(1));
                final int m = Integer.parseInt(DURATION_MATCHER.group(2));
                final int s = Integer.parseInt(DURATION_MATCHER.group(3));
                this.duration = h * 60 * 60 + m * 60 + s;
            }
        }

        @Override
        public void actionPerformed(final ActionEvent e)
        {
            if (this.timer.isRunning())
            {
                stop();
                return;
            }
            stopTimers();
            formatTime(0);
            this.timer.start();
            this.button.setBackground(Color.GREEN);
            this.button.setOpaque(true);
        }

        void stopTimers()
        {
            final Set<Component> components = new HashSet<>();
            collectComponents(this.button.getParent().getParent(), components);

            for (final Component c : components)
            {
                if (c instanceof JButton)
                {
                    final Action action = ((JButton) c).getAction();
                    if (action instanceof TimerAction)
                    {
                        ((TimerAction) action).stop();
                    }
                }
            }
        }

        void resetTimers()
        {
            resetTimers(this.button.getParent().getParent().getComponents());
        }

        void resetTimers(final Component[] components)
        {
            if (components == null)
            {
                return;
            }
            for (final Component c : components)
            {
                if (c instanceof JPanel)
                {
                    resetTimers(((JPanel) c).getComponents());
                    continue;
                }
                if (c instanceof JButton)
                {
                    final Action action = ((JButton) c).getAction();
                    if (action instanceof TimerAction)
                    {
                        ((TimerAction) action).reset();
                    }
                }
            }
        }

        void stop()
        {
            stop(false);
        }

        void stop(final boolean reset)
        {
            if (this.timer != null)
            {
                this.timer.stop();
                if (this.label != null)
                {
                    saveSetting(reset ? "" : this.label.getText(), getDurationKey());
                }
            }
            this.button.setBackground(null);
            this.button.setOpaque(false);
        }

        void reset()
        {
            stop(true);
            this.duration = 0;
            if (this.label != null)
            {
                this.label.setText("");
            }
            removeDuration();
        }

        private void removeDuration()
        {
            if (this.key == null)
            {
                return;
            }

            try (final InputStream inputStream = TimeTracker.class.getResourceAsStream(PROPERTIES))
            {

                final Properties properties = new Properties();
                properties.load(inputStream);
                properties.remove(getDurationKey());
                storeProperties(properties);
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

        private String getDurationKey()
        {
            return this.key + ".duration";
        }
    }

    /**
     * Liefert den Schlüssel zum Speichern der Zeitdauer
     * @param key Schlüssel für das Label
     * @return Schlüssel für die Zeitdauer
     */
    private String getDurationKey(final String key)
    {
        return key + ".duration";
    }

    /**
     * Liefert das Ticket zu einem Button. Entweder, wenn es schon gespeichert ist oder geparst aus dem Titel
     * @param key Schlüssel mit der Button-ID
     * @param button Button mit dem Titel des Tickets
     * @return Ticket-ID zum Button
     */
    private String getTicket(final String key, final JButton button)
    {
        final String ticket = loadSetting(key, SUFFIX_TICKET);
        final String label = button.getText();
        if (ticket != null && !ticket.isEmpty())
        {
            return ticket;
        }
        else
        {
            MATCHER.reset(label);
            if (MATCHER.matches())
            {
                return MATCHER.group(1);
            }
        }
        return null;
    }

    /**
     * Lädt eine Einstellung aus den Properties
     * @param key Schlüssel (Label des Buttons)
     * @param setting Einstellung
     * @return Wert
     */
    private String loadSetting(final String key, final String setting)
    {
        final Properties properties = getProperties();
        return properties != null ? properties.getProperty(key + setting) : null;
    }

    /**
     * Klasse zum Burnen von Zeiten
     */
    private class BurnButtonAction extends AbstractAction
    {
        private static final long serialVersionUID = -2092965435624779543L;
        final JButton button;
        private JLabel label;
        private String key;

        BurnButtonAction(final JButton button, final JLabel label, final String key)
        {
            this.button = button;
            this.label = label;
            this.key = key;
        }

        @Override
        public void actionPerformed(final ActionEvent e)
        {
            final Point location = getWindowLocation();

            final JDialog dialog = new JDialog(timeTracker, "Burning time", true);
            dialog.setBounds(location.x, location.y, 250, 200);
            dialog.setResizable(false);

            addEscapeEvent(dialog);

            dialog.getContentPane().setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));

            final JPanel rows = new JPanel(new GridLayout(4, 2));
            rows.setBorder(new EmptyBorder(10, 10, 10, 10));
            dialog.add(rows);

            final JTextField ticketField = new JTextField();
            ticketField.setBackground(MANDATORY);

            final String ticket = loadSetting(this.key, SUFFIX_TICKET);
            final String buttonText = this.button.getText();
            if (ticket != null && !ticket.isEmpty())
            {
                ticketField.setText(ticket);
            }
            else
            {
                MATCHER.reset(buttonText);
                if (MATCHER.matches())
                {
                    ticketField.setText(MATCHER.group(1));
                }
            }

            rows.add(new JLabel(bundle.getString("button.label.ticket")));
            rows.add(ticketField);

            final JTextField timeField = new JTextField();
            timeField.setBackground(MANDATORY);
            timeField.setText(getParsedTime(this.label.getText()));
            rows.add(new JLabel(bundle.getString("button.label.time")));
            rows.add(timeField);

            final JComboBox typeField = new JComboBox();
            timeField.setBackground(MANDATORY);

            final String type = loadSetting(this.key, SUFFIX_TYPE);
            final LinkedList<String[]> items = new LinkedList<>();
            items.add(new String[]{"136-0", "Development"});
            items.add(new String[]{"136-1", "Testing"});
            items.add(new String[]{"136-2", "Documentation"});
            items.add(new String[]{"136-3", "Meeting"});
            items.add(new String[]{"136-4", "Support"});
            items.add(new String[]{"136-6", "Design"});

            for (final String[] item : items)
            {
                //noinspection unchecked
                typeField.addItem(item);
                if (item[0].equalsIgnoreCase(type))
                {
                    typeField.setSelectedItem(item);
                }
            }

            //noinspection unchecked
            typeField.setRenderer(new DefaultListCellRenderer()
            {
                private static final long serialVersionUID = -4094337354229283799L;

                @Override
                public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                              final boolean isSelected, final boolean cellHasFocus)
                {
                    final Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value != null)
                    {
                        setText(((String[]) value)[1]);
                    }
                    return component;
                }
            });

            rows.add(new JLabel(bundle.getString("button.label.type")));
            rows.add(typeField);

            final JTextArea textArea = new JTextArea(5, 20);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            rows.add(new JLabel(bundle.getString("button.label.text")));
            rows.add(new JScrollPane(textArea));

            final JButton ok = new JButton(new AbstractAction("OK")
            {
                private static final long serialVersionUID = -2918616353182983419L;

                @Override
                public void actionPerformed(final ActionEvent e)
                {
                    if (burnTime(ticketField, timeField, typeField, textArea))
                    {
                        dialog.dispose();
                    }
                }
            });

            final JPanel okBtnPanel = new JPanel();
            okBtnPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
            okBtnPanel.add(ok);
            dialog.add(okBtnPanel);

            final Action action = this.button.getAction();
            if (action instanceof TimerAction && ((TimerAction) action).timer.isRunning())
            {
                ((TimerAction) action).stop();
            }

            dialog.setVisible(true);
        }

        /**
         * Liefert einen Formatierten String zum Burnen
         * @param label Die angezeigte Zeit
         * @return Formatierte Zeit zur Burnen
         */
        private String getParsedTime(final String label)
        {
            final String time = loadSetting(this.key, SUFFIX_TIME);
            TIME_MATCHER.reset(label);

            final boolean matches = TIME_MATCHER.matches();
            if (time != null && !time.isEmpty())
            {
                int currentHours = 0;
                int currentMinutes = 0;
                if (matches)
                {
                    //aufaddieren
                    currentHours = Integer.parseInt(TIME_MATCHER.group(1));
                    currentMinutes = Integer.parseInt(TIME_MATCHER.group(2));

                    final int currentSeconds = Integer.parseInt(TIME_MATCHER.group(3));
                    if (currentSeconds > 0)
                    {
                        currentMinutes += 1;
                    }
                }

                final int timeValue = Integer.parseInt(time);
                final int currentTimeValue = currentHours * 60 + currentMinutes;
                if (currentTimeValue < timeValue)
                {
                    //die aktuelle Zeit ist kleiner als die gespeicherte. D.h. die aktuelle Zeit wird geburnt
                    return appendTimeUnits(currentTimeValue);
                }
                //man will erneut burnen. Seit dem letzten Mal ist Zeit dazu gekommen.
                return appendTimeUnits(currentTimeValue - timeValue);
            }
            else if (matches)
            {
                final String hours = TIME_MATCHER.group(1);
                final String minutes = TIME_MATCHER.group(2);
                final String seconds = TIME_MATCHER.group(3);
                return getParsedTime(hours, minutes, seconds);
            }
            return "";
        }

        /**
         * Erzeugt einen Formatstring zum Burnen, ohne Sekunden
         * @param hours Stunden
         * @param minutes Minuten
         * @param seconds Sekunden
         * @return Formatierter String zum Burnen
         */
        private String getParsedTime(final String hours, final String minutes, final String seconds)
        {
            //Sekunden burnen wir mal nicht. Angefangene Minuten werden aufgerundet
            final StringBuilder sb = new StringBuilder();
            final int h = Integer.parseInt(hours);
            if (h > 0)
            {
                sb.append(h).append("h ");
            }
            final int s = Integer.parseInt(seconds);
            int m = Integer.parseInt(minutes);
            if (s > 0)
            {
                m += 1;
            }
            sb.append(m).append("m");
            return sb.toString();
        }

        /**
         * Erzeugt einen formatierten String aus den übergebenen Minuten
         * @param time Minuten
         * @return Formatierter String
         */
        private String appendTimeUnits(int time)
        {
            final StringBuilder sb = new StringBuilder();
            final int hours = time / 60;
            if (hours > 0)
            {
                sb.append(hours).append("h ");
            }
            sb.append(Math.max(1, time - (hours * 60))).append('m');
            return sb.toString();
        }

        /**
         * Burnt Zeit am Ticket
         * @param ticketField Feld mit dem Ticket
         * @param timeField Feld mit der Zeit
         * @param typeField Feld mit dem Typ
         * @param textArea Feld mit dem Kommentar
         * @return <code>true</code>, wenn erfolgreich, sonst <code>false</code>
         */
        private boolean burnTime(final JTextField ticketField, final JTextField timeField, final JComboBox typeField, final JTextArea textArea)
        {
            final String ticket = ticketField.getText();
            final String spentTime = timeField.getText();
            final Object selectedItem = typeField.getSelectedItem();

            if (selectedItem == null || ticket.isEmpty() || spentTime.isEmpty())
            {
                return false;
            }

            final String type = ((String[]) selectedItem)[0];
            final String spentTimeInMinutes = getSpentTime(spentTime);

            saveSetting(ticket, this.key + SUFFIX_TICKET);
            saveSetting(type, this.key + SUFFIX_TYPE);
            saveSetting(spentTimeInMinutes, this.key + SUFFIX_TIME);

            if (token == null || token.isEmpty())
            {
                JOptionPane.showMessageDialog(timeTracker,
                                              String.format("Authorization token not found! Please create a token in youtrack and enter it in the " +
                                                            "%s with the key %s", PROPERTIES, YOUTRACK_TOKEN));
                return false;
            }

            String text = textArea.getText();
            if (text == null)
            {
                text = "";
            }

            try
            {
                final URIBuilder builder = getURIBuilder(Path.WORKITEM, ticket);
                final HttpPost request = new HttpPost(builder.build());
                request.setEntity(
                        new StringEntity(String.format(ENTITY, System.currentTimeMillis(), userId, spentTime, type, text), ContentType.APPLICATION_JSON));

                final HttpResponse response = executeRequest(request);
                if (response == null)
                {
                    return false;
                }
                logResponse(response);
            }
            catch (URISyntaxException | IOException e)
            {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }

            return true;
        }

        /**
         * Parst die verbrauchte Zeit in Minuten
         * @param time  verbrauchte Zeit
         * @return verbrauchte Zeit in Minuten
         */
        private String getSpentTime(final String time)
        {
            final String[] split = time.split(" ");
            String minutes = split[0];

            char unit = 'm';
            int multiplier = 1;

            int result = 0;
            if (split.length > 1)
            {
                //Stunden
                final String hours = split[0];
                result = Integer.parseInt(hours.substring(0, hours.indexOf('h'))) * 60;
                minutes = split[1];
            }
            else if (minutes.contains("h"))
            {
                unit = 'h';
                multiplier = 60;
            }
            result += Integer.parseInt(minutes.substring(0, minutes.indexOf(unit))) * multiplier;
            return Integer.toString(result);
        }
    }

    /**
     * Liefert die aktuelle Position der Anwendung
     * @return aktuelle Position
     */
    private Point getWindowLocation()
    {
        final Point location = this.timeTracker.getLocationOnScreen();
        final int x = location.x + (this.timeTracker.getWidth() / 2) - 125;
        final int y = location.y + (this.timeTracker.getHeight() / 2) - 85;
        return new Point(x, y);
    }

    /**
     * Liefert einen URIBuilder
     * @param path Rest-Endpoint
     * @param ticket Ticket
     * @return URIBuilder
     */
    private URIBuilder getURIBuilder(final Path path, final String ticket, final NameValuePair... parameter)
    {
        final URIBuilder builder = new URIBuilder();
        builder.setScheme(this.scheme);
        builder.setHost(this.host);
        builder.setPort(this.port);

        if (ticket == null)
        {
            builder.setPath(path.restEndPoint);
        }
        else
        {
            builder.setPath(String.format(path.restEndPoint, ticket));
        }
        if(parameter != null && parameter.length > 0)
        {
            builder.setParameters(parameter);
        }
        return builder;
    }

    /**
     * Liefert den Wert eines "id"-Feldes aus dem Response
     * @param response Response
     * @param saveWithKey Schlüssel, unter welchem die ermittelte ID abgespeichert werden soll
     * @return ID als String
     * @throws IOException I/O Error beim Schreiben in den Outputstream
     */
    private String getID(final HttpResponse response, final String saveWithKey) throws IOException
    {
        final String value = getValueFromJson(response, "id");
        if(value != null && saveWithKey != null)
        {
            saveSetting(value, saveWithKey);
        }
        return value;
    }

    /**
     * Liefert einen Wert aus dem Json
     * @param response Response
     * @param key Schlüssel im Json, dessen Wert ermittelt werden soll
     * @return Wert aus dem Json
     * @throws IOException I/O Error
     */
    private String getValueFromJson(final HttpResponse response, final String key) throws IOException
    {
        String result = null;
        StatusLine statusLine = response.getStatusLine();
        int status = statusLine.getStatusCode();
        if (status == HttpStatus.SC_OK)
        {
            JsonParser parser = null;
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
            {
                response.getEntity().writeTo(outputStream);
                outputStream.flush();

                final String msg = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
                LOGGER.info(msg);

                final JsonFactory jsonFactory = new JsonFactory();
                parser = jsonFactory.createParser(msg);

                boolean start = false;
                while ((parser.nextToken()) != null)
                {
                    if (start)
                    {
                        result = parser.getValueAsString();
                        break;
                    }
                    final String name = parser.getCurrentName();
                    if (key.equalsIgnoreCase(name))
                    {
                        start = true;
                    }
                }
            }
            finally
            {
                if (parser != null)
                {
                    parser.close();
                }
            }
        }
        return result;
    }

    /**
     * Setzt den Request ab
     *
     * @param request Request
     * @return Response
     * @throws IOException in case of a problem or the connection was aborted
     */
    private HttpResponse executeRequest(final HttpUriRequest request) throws IOException
    {
        final SchemePortResolver portResolver = new DefaultSchemePortResolver();
        try
        {
            portResolver.resolve(new HttpHost(this.host, this.port));
        }
        catch (UnsupportedSchemeException e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return null;
        }

        final RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
        registryBuilder.register(this.scheme, PlainConnectionSocketFactory.getSocketFactory());

        final RequestConfig requestConfig = RequestConfig.custom()
                .setExpectContinueEnabled(true)
                .setRedirectsEnabled(false)
                .build();

        final ConnectionConfig.Builder builder = ConnectionConfig.custom()
                .setBufferSize(8192)
                .setCharset(StandardCharsets.UTF_8);

        final PoolingHttpClientConnectionManager conman = new PoolingHttpClientConnectionManager(registryBuilder.build());
        conman.setDefaultMaxPerRoute(48);
        conman.setMaxTotal(256);

        final HttpClientBuilder httpClient = HttpClientBuilder.create()
                .setConnectionManager(conman)
                .setDefaultConnectionConfig(builder.build())
                .setDefaultRequestConfig(requestConfig)
                .setSchemePortResolver(portResolver)
                .setDefaultHeaders(Arrays.asList(new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.token),
                                                 new BasicHeader(HttpHeaders.ACCEPT, "application/json"),
                                                 new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
                                                 new BasicHeader(HttpHeaders.CACHE_CONTROL, "no-cache")));

        return httpClient.build().execute(request);
    }

    /**
     * Löscht eine komplette Zeile
     */
    private class DeleteButtonAction extends AbstractAction
    {
        private static final long serialVersionUID = -2092965435624779543L;
        final JButton button;
        final String key;

        DeleteButtonAction(final JButton button, final String key)
        {
            this.button = button;
            this.key = key;
        }

        @Override
        public void actionPerformed(final ActionEvent e)
        {
            try (final InputStream inputStream = TimeTracker.class.getResourceAsStream(PROPERTIES))
            {
                final Properties properties = new Properties();
                properties.load(inputStream);

                final String prefix = this.key + ".";
                for (final Iterator<Map.Entry<Object, Object>> iter = properties.entrySet().iterator(); iter.hasNext(); )
                {
                    final Map.Entry<Object, Object> entry = iter.next();
                    final String propKey = (String) entry.getKey();
                    if (propKey.startsWith(prefix))
                    {
                        iter.remove();
                    }
                }
                storeProperties(properties);

                remove();
                updateGui();
                --line;
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

        /**
         * Entfernt eine komplette Zeile
         */
        private void remove()
        {
            final Container parent = this.button.getParent().getParent();
            if (parent == null)
            {
                return;
            }
            final Component[] components = parent.getComponents();
            for (final Component child : components)
            {
                remove(parent, child);
            }
            panel.remove(parent);
        }

        /**
         * Entfernt die Komponente und deren Kinder vom Parent
         * @param parent Parent
         * @param component Komponente
         */
        private void remove(final Container parent, final Component component)
        {
            if (component instanceof JPanel)
            {
                final Component[] components = ((JPanel) component).getComponents();
                for (final Component child : components)
                {
                    remove((Container) component, child);
                }
            }
            parent.remove(component);
        }
    }

    /**
     * Setzt alle Timer zurück
     */
    private class ResetAction extends TimerAction
    {
        private static final long serialVersionUID = -3128930442339113957L;

        ResetAction(final JButton button)
        {
            super(button);
        }

        @Override
        public void actionPerformed(final ActionEvent e)
        {
            resetTimers();
        }
    }

    /**
     * Fügt an der übergebenen Komponente ein Escape-Event hinzu
     * @param window Fenster, welchem ein Escape-Event hinzufügt
     */
    private void addEscapeEvent(final RootPaneContainer window)
    {
        window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ACTIONMAP_KEY_CANCEL); //$NON-NLS-1$
        window.getRootPane().getActionMap().put(ACTIONMAP_KEY_CANCEL, new AbstractAction(){
            private static final long serialVersionUID = 4056930123544439411L; //$NON-NLS-1$

            @Override
            public void actionPerformed(ActionEvent e)
            {
                ((Window) window).dispose();
            }
        });
    }

    /**
     * Klasse zur Darstellung eines Dialoges, mit welchem ein neues Ticket angelegt werden kann.
     */
    private class ShowAddButtonAction extends TimerAction
    {
        private static final long serialVersionUID = -2104627297533100111L;

        ShowAddButtonAction(final JButton button)
        {
            super(button);
        }

        @Override
        public void actionPerformed(final ActionEvent e)
        {
            final JFrame frame = new JFrame();
            frame.setAlwaysOnTop(true);
            frame.setLocation(100, 100);

            addEscapeEvent(frame);

            final JPanel addButtonPanel = new JPanel();
            addButtonPanel.setLayout(new BoxLayout(addButtonPanel, BoxLayout.Y_AXIS));
            addButtonPanel.setBorder(new EmptyBorder(0, 10, 0, 10));
            frame.add(addButtonPanel);

            final JPanel labelPanel = new JPanel();
            labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
            labelPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
            addButtonPanel.add(labelPanel);

            final JFileChooser chooser = new JFileChooser();
            chooser.setPreferredSize(new Dimension(600, 300));
            chooser.setMultiSelectionEnabled(true);
            chooser.setControlButtonsAreShown(false);
            chooser.setFileFilter(new FileFilter()
            {
                @Override
                public boolean accept(final File f)
                {
                    if (f == null)
                    {
                        return false;
                    }
                    if (f.isDirectory())
                    {
                        return true;
                    }
                    final String fileName = f.getName();
                    final int dot = fileName.lastIndexOf('.');
                    if (dot < 0)
                    {
                        return false;
                    }
                    final String suffix = fileName.substring(dot);
                    return ".png".equalsIgnoreCase(suffix) || ".jpg".equalsIgnoreCase(suffix) || ".jpeg".equalsIgnoreCase(suffix);
                }

                @Override
                public String getDescription()
                {
                    return null;
                }
            });
            addButtonPanel.add(chooser);

            final JPanel okBtnPanel = new JPanel();
            okBtnPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            addButtonPanel.add(okBtnPanel);

            final JTextField labelField = new JTextField();
            labelField.setPreferredSize(new Dimension(200, 25));
            labelField.setBackground(MANDATORY);

            final JLabel label = new JLabel("Label");
            label.setPreferredSize(new Dimension(50, 25));
            labelPanel.add(label);
            labelPanel.add(labelField);

            final JButton ok = new JButton("OK");
            ok.setAction(new AddAction(ok, labelField, chooser));
            okBtnPanel.add(ok);

            frame.pack();
            frame.setVisible(true);
            frame.setAlwaysOnTop(true);

            final Frame topMostFrame = getParentFrame(this.button);
            frame.setLocation(topMostFrame.getX() - ((frame.getWidth() - topMostFrame.getWidth()) / 2), timeTracker.getY());
        }
    }

    /**
     * Klasse zum Hinzufügen eines neuen Eintrags
     */
    private class AddAction extends TimerAction
    {
        private static final long serialVersionUID = 2109270279366930967L;
        private JTextField textInput;
        private JFileChooser icon;

        AddAction(final JButton button, final JTextField textInput, final JFileChooser icon)
        {
            super(button);
            this.textInput = textInput;
            this.icon = icon;
        }

        @Override
        public void actionPerformed(final ActionEvent e)
        {
            final File file = this.icon.getSelectedFile();
            final String filePath = file == null ? "" : file.getPath();
            String text = this.textInput.getText();
            if(text != null)
            {
                text = text.trim();
            }
            if (text == null || text.isEmpty())
            {
                return;
            }

            try
            {
                final String summary = getIssueSummary(text);
                if(summary != null)
                {
                    text = text + " " + summary;
                }
            }
            catch (URISyntaxException | IOException ex)
            {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }

            final String counter = line < 10 ? "0" + line : Integer.toString(line);

            JButton button = null;
            try (final FileOutputStream outputStream = new FileOutputStream(PROPERTIES, true))
            {
                final String propertyKey = PREFIX_BUTTON + counter;

                final Properties properties = new Properties();
                properties.setProperty(propertyKey + SUFFIX_LABEL, text);
                properties.setProperty(propertyKey + SUFFIX_ICON, filePath);
                properties.store(outputStream, null);
                outputStream.flush();

                button = addButton(propertyKey, text, filePath);
                updateGui();
                ++line;
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }

            final Frame frame = getParentFrame(this.button);
            frame.dispose();

            handleConfirmationDialog(button, text);
        }

        private String getIssueSummary(String text) throws URISyntaxException, IOException
        {
            MATCHER.reset(text);
            if (!MATCHER.matches())
            {
                return null;
            }
            final String ticket = MATCHER.group(1);
            text = text.replace(ticket, "");
            if(!text.trim().isEmpty())
            {
                //Sollte der Nutzer was eigenes hingeschrieben haben, so sollte das nicht ersetzt werden
                return null;
            }

            final URIBuilder builder = getURIBuilder(Path.ISSUE, ticket, new BasicNameValuePair("fields", ISSUE_SUMMARY));
            final HttpResponse response = execute(builder);
            if (response == null)
            {
                return null;
            }
            final StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() != HttpStatus.SC_OK)
            {
                return null;
            }
            return getValueFromJson(response, ISSUE_SUMMARY);
        }

        /**
         * Zeigt einen Dialog an, mit welchem ein Ticket in Bearbeitung genommen werden kann
         * @param text Text auf dem Button mit dem Issue
         */
        private void handleConfirmationDialog(final JButton button, final String text)
        {
            MATCHER.reset(text);
            if (MATCHER.matches())
            {
                final String ticket = MATCHER.group(1);
                final Point location = getWindowLocation();

                final JDialog dialog = new JDialog(timeTracker, bundle.getString("text.confirmation"), true);
                dialog.setBounds(location.x, location.y, 250, 200);
                dialog.setResizable(false);
                dialog.getContentPane().setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));

                final JPanel rows = new JPanel(new GridLayout(2, 1));
                rows.setBorder(new EmptyBorder(10, 10, 10, 10));
                dialog.add(rows);

                rows.add(new JLabel(bundle.getString("ticket.inprogress")));

                final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
                buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
                rows.add(buttonPanel);

                final JButton cancelButton = new JButton(bundle.getString("text.no"));
                cancelButton.addActionListener(e1 -> dialog.dispose());

                final JButton okButton = new JButton(bundle.getString("text.yes"));
                okButton.addActionListener(e12 -> {
                    try
                    {
                        final String issueID = getIssueID(ticket);
                        if (issueID == null)
                        {
                            LOGGER.severe("Issue id of " + ticket + " not found");
                            return;
                        }

                        final URIBuilder builder = getCommandURIBuilder();
                        if(builder == null)
                        {
                            return;
                        }

                        final HttpPost request = new HttpPost(builder.build());
                        request.setEntity(new StringEntity(String.format(IN_PROGRESS, issueID), ContentType.APPLICATION_JSON));

                        final HttpResponse response = executeRequest(request);
                        if (response == null)
                        {
                            return;
                        }
                        logResponse(response);
                        dialog.dispose();
                        button.doClick();
                    }
                    catch (URISyntaxException | IOException ex)
                    {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                });

                buttonPanel.add(okButton);
                buttonPanel.add(cancelButton);

                dialog.pack();
                dialog.setVisible(true);
            }
        }

        /**
         * Liefert den URI-Builder für Commands
         * @return URIBuilder
         */
        private URIBuilder getCommandURIBuilder()
        {
            return getURIBuilder(Path.COMMAND, null);
        }

        /**
         * Liefert die interne ID eines Issues. Für manche Operationen, z.b. Commands, kann nicht mit dem Issue gearbeitet werden
         * @param ticket Issue
         * @return Interne ID des Issues
         * @throws URISyntaxException Wenn das Parsen als URI-Referenz schief ging
         * @throws IOException Verbindungsproblem
         */
        private String getIssueID(final String ticket) throws URISyntaxException, IOException
        {
            final URIBuilder getIssueBuilder = getURIBuilder(Path.ISSUE, ticket);
            final HttpGet request = new HttpGet(getIssueBuilder.build());
            final HttpResponse response = executeRequest(request);
            if (response == null)
            {
                return null;
            }
            return getID(response, null);
        }
    }

    /**
     * Loggt einen Response aus
     * @param response Response
     * @throws IOException I/O Error
     */
    private void logResponse(final HttpResponse response) throws IOException
    {
        final StatusLine statusLine = response.getStatusLine();
        final int status = statusLine.getStatusCode();

        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            response.getEntity().writeTo(outputStream);
            outputStream.flush();

            final String msg = new String(outputStream.toByteArray());
            if (status == HttpStatus.SC_OK)
            {
                LOGGER.info(msg);
                LOGGER.info("Success.");
            }
            else
            {
                LOGGER.warning(msg);
            }
        }
    }

    /**
     * Liefet den übergeordneten JFrame
     * @param component Komponente, dessen übergeordneter Frame ermittelt werden soll
     * @return übergeordneter Frame
     */
    private Frame getParentFrame(final Container component)
    {
        final Container parent = component.getParent();
        if (parent instanceof Frame)
        {
            return (Frame) parent;
        }
        return getParentFrame(parent);
    }

    /**
     * Speichert die Fenstergröße und -position
     */
    private void saveWindowPositionAndSize()
    {
        final Properties properties = new Properties();
        try (final InputStream inputStream = loadProperties(properties, PROPERTIES))
        {
            if (inputStream != null)
            {
                final Point location = getLocationOnScreen();
                String value = String.format("%s,%s", Math.max(location.x, 0), Math.max(location.y, 0));
                LOGGER.log(Level.FINE, "Saving window position {0}", value);
                properties.setProperty("window.location", value);

                final Dimension dimension = getSize();
                value = String.format("%s,%s", dimension.width, dimension.height);
                LOGGER.log(Level.FINE, "Saving window dimension {0}", value);
                properties.setProperty("window.dimension", value);

                saveDurations(properties);
                storeProperties(properties);
            }
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        finally
        {
            System.exit(0);
        }
    }

    /**
     * Merkt sich alle Zeiten in den Properties
     * @param properties Properties
     */
    private void saveDurations(final Properties properties)
    {
        final Set<Component> components = new HashSet<>();
        collectComponents(this, components);

        for (final Component component : components)
        {
            if (component instanceof JLabel)
            {
                final String name = component.getName();
                if (name != null)
                {
                    properties.put(getDurationKey(name), ((JLabel) component).getText());
                }
            }
        }
    }

    /**
     * Sammelt die Kinder eines Containers auf
     * @param container Container
     * @param components Sammelbecken
     */
    private void collectComponents(final Container container, final Set<Component> components)
    {
        if (container == null)
        {
            return;
        }

        final Component[] children = container.getComponents();
        if (children == null)
        {
            return;
        }
        for (final Component child : children)
        {
            addPanelComponents(child, components);
        }
        collectComponents(container.getParent(), components);
    }

    /**
     * Sammelt Komponenten auf
     * @param component Komponente, der Kinder gesammelt werden sollen
     * @param components Sammelbecken
     */
    private void addPanelComponents(final Component component, final Set<Component> components)
    {
        if (component instanceof JComponent)
        {
            final Component[] jPanelComponents = ((JComponent) component).getComponents();
            if (jPanelComponents != null)
            {
                for (final Component child : jPanelComponents)
                {
                    addPanelComponents(child, components);
                }
            }
        }
        components.add(component);
    }

    /**
     * Stellt die Fenstergröße und -position aus den Properties wieder her
     */
    private void restoreWindowPositionAndSize()
    {
        final Properties properties = new Properties();
        try (final InputStream inputStream = loadProperties(properties, PROPERTIES))
        {
            if (inputStream != null)
            {
                final String windowLocation = properties.getProperty("window.location");
                if (windowLocation != null && !windowLocation.isEmpty())
                {
                    LOGGER.log(Level.FINE, "Restoring window position {0}", windowLocation);
                    final StringTokenizer tokenizer = new StringTokenizer(windowLocation, ",");
                    final Rectangle rectangle = new Rectangle(new Point(Integer.parseInt(tokenizer.nextToken()), Integer.parseInt(tokenizer.nextToken())));
                    setBounds(rectangle);
                }

                final String windowDimension = properties.getProperty("window.dimension");
                if (windowDimension != null && !windowDimension.isEmpty())
                {
                    LOGGER.log(Level.FINE, "Restoring window dimension {0}", windowDimension);
                    final StringTokenizer tokenizer = new StringTokenizer(windowDimension, ",");
                    final Dimension dimension = new Dimension(Integer.parseInt(tokenizer.nextToken()), Integer.parseInt(tokenizer.nextToken()));
                    setSize(dimension);
                }
            }
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Liefert alle Properties in einem Objekt
     * @return alle Properties
     */
    private static Properties getProperties()
    {
        final Properties properties = new Properties();

        try (@SuppressWarnings("unused") final InputStream defaultInputStream = loadProperties(properties, DEFAULT_PROPERTIES);
             @SuppressWarnings("unused") final InputStream inputStream = loadProperties(properties, PROPERTIES))
        {
            return properties;
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Lädt Properties. Der Stream als Rückgabe muss selbst geschlossen werden.
     * @param properties Properties, welche erweitert werden
     * @param propertyFileName Name des Property-Files, welches ausgelesen werden soll
     * @return InputStream
     * @throws IOException Wenn vom Inputstream nicht gelesen werden konnte
     */
    private static InputStream loadProperties(final Properties properties, final String propertyFileName) throws IOException
    {
        final InputStream inputStream = TimeTracker.class.getResourceAsStream(propertyFileName);
        if (inputStream != null)
        {
            properties.load(inputStream);
            return inputStream;
        }
        LOGGER.log(Level.WARNING, "Property file {0} not found!", propertyFileName);

        LOGGER.log(Level.INFO, "Creating property file {0}", propertyFileName);
        final File propertyFile = new File(propertyFileName);
        final boolean created = propertyFile.createNewFile();
        LOGGER.log(created ? Level.INFO : Level.SEVERE, created ? "Property file created" : "Failed to create property file");
        return null;
    }

    public static void main(final String[] args)
    {
        final Properties properties = getProperties();
        if (properties == null || properties.isEmpty())
        {
            return;
        }

        try
        {
            final String lookAndFeel = properties.getProperty("look.and.feel", "javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.setLookAndFeel(lookAndFeel);
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.log(Level.SEVERE, e.getMessage(), e));

        final String token = properties.getProperty(YOUTRACK_TOKEN);
        if (token == null || token.isEmpty())
        {
            final int frameWidth = 500;
            final int frameHeight = 100;
            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            final JFrame dialog = new JFrame("Your token please");
            dialog.setMinimumSize(new Dimension(frameWidth, frameHeight));
            dialog.setBounds((screenSize.width / 2) - (frameWidth / 2), (screenSize.height / 2) - (frameHeight / 2), frameWidth, frameHeight);
            dialog.setResizable(false);
            dialog.setLayout(new GridLayout(2, 1));

            final JTextField tokenField = new JTextField(YOUTRACK_TOKEN_PREFIX);
            tokenField.setCaretPosition(YOUTRACK_TOKEN_PREFIX.length());
            tokenField.setBackground(MANDATORY);
            dialog.add(tokenField);

            final JButton button = new JButton();
            button.setAction(new TextAction("OK")
            {
                private static final long serialVersionUID = 6877363378286790461L;

                @Override
                public void actionPerformed(final ActionEvent e)
                {
                    String localToken = tokenField.getText();
                    if(localToken == null || localToken.isEmpty())
                    {
                        return;
                    }

                    if(!localToken.startsWith(YOUTRACK_TOKEN_PREFIX))
                    {
                        localToken = YOUTRACK_TOKEN_PREFIX + localToken;
                    }

                    if(localToken.startsWith(YOUTRACK_TOKEN_PREFIX + YOUTRACK_TOKEN_PREFIX))
                    {
                        localToken = localToken.substring(YOUTRACK_TOKEN_PREFIX.length());
                    }

                    if(localToken.length() <= YOUTRACK_TOKEN_PREFIX.length())
                    {
                        return;
                    }

                    saveToken(localToken);

                    properties.setProperty(YOUTRACK_TOKEN, localToken);

                    dialog.dispose();
                    callTimeTracker(properties);
                }
            });
            dialog.add(button);

            dialog.pack();
            dialog.setVisible(true);
            return;
        }
        callTimeTracker(properties);
    }

    /**
     * Startet den TimeTracker
     * @param properties alle Properties
     */
    private static void callTimeTracker(final Properties properties)
    {
        SwingUtilities.invokeLater(() -> {
            final TimeTracker timer = new TimeTracker(properties);
            timer.setVisible(true);
        });
    }

    /**
     * Speichert den Authentifizierungstoken
     * @param token Token
     */
    private static void saveToken(final String token)
    {
        saveSetting(token, YOUTRACK_TOKEN);
    }

    /**
     * Speichert einen Wert in den Properties
     * @param value Wert
     * @param key Schlüssel
     */
    private static void saveSetting(final String value, final String key)
    {
        try (final InputStream inputStream = TimeTracker.class.getResourceAsStream(PROPERTIES))
        {
            final Properties properties = new Properties();
            properties.load(inputStream);
            properties.remove(key);
            properties.put(key, value);
            storeProperties(properties);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Speichert die Properties
     * @param properties Properties
     * @throws IOException I/O Error
     */
    private static void storeProperties(final Properties properties) throws IOException
    {
        for(final Map.Entry<Object, Object> entry : properties.entrySet())
        {
            LOGGER.log(Level.INFO, "property key={0}, value={1}", new Object[]{entry.getKey(), entry.getValue()});
        }

        OutputStream outputStream = null;
        try
        {
            outputStream = Files.newOutputStream(Paths.get(PROPERTIES), StandardOpenOption.TRUNCATE_EXISTING);
            properties.store(outputStream, null);
            outputStream.flush();
        }
        finally
        {
            if(outputStream != null)
            {
                try
                {
                    outputStream.close();
                }
                catch (IOException e)
                {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
    }
}