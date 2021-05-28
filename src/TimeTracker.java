import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
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
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.TextAction;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;

/**
 * TimeTracking-Tool
 */
public class TimeTracker extends Frame
{
    private static final long serialVersionUID = 7225687129886672540L;

    private static final Matcher MATCHER = TimeTrackerConstants.PATTERN.matcher(TimeTrackerConstants.STRING_EMPTY);
    private static final Matcher TIME_MATCHER = TimeTrackerConstants.TIME_PATTERN.matcher(TimeTrackerConstants.STRING_EMPTY);
    private static final Matcher BURN_MATCHER = TimeTrackerConstants.BURN_PATTERN.matcher(TimeTrackerConstants.STRING_EMPTY);
    private static final Matcher DURATION_MATCHER = TimeTrackerConstants.DURATION_PATTERN.matcher(TimeTrackerConstants.STRING_EMPTY);
    private static final Matcher USER_ID_MATCHER = TimeTrackerConstants.USER_ID_PATTERN.matcher(TimeTrackerConstants.STRING_EMPTY);

    private static final Color MANDATORY = new Color(200, 221, 242);
    private static final EmptyBorder BORDER = new EmptyBorder(5, 5, 5, 5);

    static String home = TimeTrackerConstants.STRING_EMPTY;
    private static TimeTracker timeTracker;
    private static final Object syncObject = new Object();

    private static final ListCellRenderer RENDERER = new TypeRenderer();

    private int line;
    private final JPanel panel;
    private final String token;
    private String userId;
    private final String host;
    private final String scheme;
    private final int port;
    private transient HttpClient client;

    private TimeTracker(final Properties properties) throws Exception
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

        this.panel = new JPanel(new GridLayout(0, 1));
        this.scheme = properties.getProperty(TimeTrackerConstants.YOUTRACK_SCHEME, TimeTrackerConstants.DEFAULT_SCHEME);
        this.host = properties.getProperty(TimeTrackerConstants.YOUTRACK_HOST, TimeTrackerConstants.DEFAULT_HOST);

        final String p = properties.getProperty(TimeTrackerConstants.YOUTRACK_PORT);
        this.port = p == null || p.isEmpty() ? -1 : Integer.parseInt(p);
        this.token = properties.getProperty(TimeTrackerConstants.YOUTRACK_TOKEN);

        try
        {
            setUserID(properties);

            final JButton add = new JButton(Resource.getString(PropertyConstants.LABEL_ADD));
            setButtonIcon(add, Icon.ADD);
            add.setAction(new ShowAddButtonAction(add));

            final JButton reset = new JButton(Resource.getString(PropertyConstants.LABEL_STOP));
            setButtonIcon(reset, Icon.STOP);
            reset.setAction(new ResetAction(reset));

            final JButton openLog = getOpenLogButton();

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
                if (key.startsWith(TimeTrackerConstants.PREFIX_BUTTON) && key.endsWith(TimeTrackerConstants.SUFFIX_LABEL))
                {
                    final String icon = properties.getProperty(key.replace(TimeTrackerConstants.SUFFIX_LABEL, TimeTrackerConstants.SUFFIX_ICON));
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
            addTrayIcon();
        }
        catch (final Exception e)
        {
            final String msg = Optional.ofNullable(e.getMessage()).orElse(e.getCause().getMessage());
            Log.severe(msg, e);
            JOptionPane.showMessageDialog(this, msg);
            throw e;
        }
    }

    private JButton getOpenLogButton()
    {
        final JButton openLog = new JButton();
        openLog.setAction(new TextAction("Log")
        {
            private static final long serialVersionUID = 4641196350640457638L;

            @Override
            public void actionPerformed(final ActionEvent e)
            {
                final File log = new File(TimeTracker.home + TimeTrackerConstants.LOGFILE_NAME);
                if (log.exists())
                {
                    final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
                    if (desktop != null && desktop.isSupported(Desktop.Action.OPEN))
                    {
                        try
                        {
                            desktop.open(log);
                        }
                        catch (final IOException ex)
                        {
                            Log.severe(ex.getMessage(), ex);
                        }
                    }
                }
            }
        });
        setButtonIcon(openLog, Icon.LOG);
        return openLog;
    }

    @Override
    public final synchronized void addWindowListener(final WindowListener l)
    {
        super.addWindowListener(l);
    }

    @Override
    public final void setMinimumSize(final Dimension minimumSize)
    {
        super.setMinimumSize(minimumSize);
    }

    @Override
    public final Component add(final Component comp)
    {
        return super.add(comp);
    }

    @Override
    public final void pack()
    {
        super.pack();
    }

    private void addTrayIcon() throws AWTException
    {
        if(SystemTray.isSupported())
        {
            final MenuItem aboutItem = new MenuItem("About");
            final int year = LocalDateTime.now().getYear();
            aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this, "Version 0.0.1 Annaberg-Buchholz\n\nby Andreas Beyer\n� " + year));

            final MenuItem openItem = new MenuItem(Resource.getString(PropertyConstants.LABEL_OPEN));
            openItem.setEnabled(false);
            openItem.addActionListener(e -> {
                setVisible(true);
                setExtendedState(Frame.NORMAL);
                openItem.setEnabled(false);
            });

            final ImageIcon imageIcon = new ImageIcon(TimeTracker.home + Icon.TIMETRACKER.getIcon());
            final TrayIcon icon = new TrayIcon(imageIcon.getImage());
            icon.setToolTip(getTitle());
            icon.addMouseListener(new MouseListener()
            {
                @Override
                public void mouseClicked(final MouseEvent e)
                {
                    if(!SwingUtilities.isLeftMouseButton(e))
                    {
                        return;
                    }
                    showFrame(openItem, !isVisible());
                }

                @Override
                public void mousePressed(final MouseEvent e)
                {
                    //doNothing
                }

                @Override
                public void mouseReleased(final MouseEvent e)
                {
                    //doNothing
                }

                @Override
                public void mouseEntered(final MouseEvent e)
                {
                    //doNothing
                }

                @Override
                public void mouseExited(final MouseEvent e)
                {
                    //doNothing
                }
            });


            final MenuItem add = new MenuItem(Resource.getString(PropertyConstants.LABEL_ADD));
            add.addActionListener(new ShowAddButtonAction());

            final PopupMenu popup = new PopupMenu();
            popup.add(aboutItem);
            popup.add(openItem);
            popup.add(add);
            icon.setPopupMenu(popup);
            SystemTray.getSystemTray().add(icon);

            addWindowStateListener((final WindowEvent e) -> {
                if(e.getNewState() == ICONIFIED || e.getNewState() == 7)
                {
                    showFrame(openItem, false);
                }
                else if(e.getNewState() == MAXIMIZED_BOTH || e.getNewState() == NORMAL)
                {
                    showFrame(openItem, true);
                }
            });
        }
    }

    private void showFrame(final MenuItem openItem, final boolean show)
    {
        setVisible(show);
        openItem.setEnabled(!show);
    }

    /**
     * Setzt die UserId entweder aus den Properties, oder wenn noch nicht vorhanden, vom Youtrack
     * @param properties Properties
     * @throws URISyntaxException Wenn das Parsen als URI-Referenz schief ging
     * @throws IOException I/O Error
     */
    private void setUserID(final Properties properties) throws URISyntaxException, IOException
    {
        this.userId = properties != null ? properties.getProperty(TimeTrackerConstants.YOUTRACK_USERID) : null;
        if (this.userId != null && !this.userId.isEmpty())
        {
            USER_ID_MATCHER.reset(this.userId);
            if(USER_ID_MATCHER.matches())
            {
                return;
            }
        }
        requestUserID();
    }

    /**
     * Ermittelt die Nutzer-ID
     * @throws URISyntaxException Wenn das Parsen als URI-Referenz schief ging
     * @throws IOException I/O Error
     */
    private void requestUserID() throws IOException, URISyntaxException
    {
        final URIBuilder builder = getUserURIBuilder();
        final HttpResponse response = execute(builder);
        if (response == null)
        {
            return;
        }
        this.userId = getID(response, TimeTrackerConstants.YOUTRACK_USERID);
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
        return getURIBuilder(ServicePath.USER, null, new BasicNameValuePair("fields", "id"));
    }

    private JButton addButton(final String key, final String label, final String icon)
    {
        if (label == null || label.isEmpty())
        {
            return null;
        }

        final int id = Integer.parseInt(key.substring(TimeTrackerConstants.PREFIX_BUTTON.length()));

        final JButton button = new JButton(label);
        setButtonIcon(button, icon);
        button.setName(key);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(final MouseEvent e)
            {
                showPopUp(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e)
            {
                showPopUp(e);
            }

            private void showPopUp(final MouseEvent e)
            {
                if(e.isPopupTrigger())
                {
                    final JPopupMenu menu = ContextMenu.create(TimeTracker.this, button, id, key);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        setButtonMarked(button, key);

        final JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BorderLayout(0, 0));
        labelPanel.add(button, BorderLayout.CENTER);

        final JLabel timeLabel = new JLabel();
        timeLabel.setName(key);
        timeLabel.setPreferredSize(new Dimension(100, 20));
        timeLabel.setBorder(new EmptyBorder(0, 8, 0, 0));

        final String savedDuration = loadSetting(key, TimeTrackerConstants.SUFFIX_DURATION_SAVED);
        if(savedDuration != null && !savedDuration.isEmpty())
        {
            setLabelTooltip(savedDuration, timeLabel);
        }
        labelPanel.add(timeLabel, BorderLayout.EAST);

        setTime(timeLabel, key);

        final TimerAction timerAction = new TimerAction(button, key, timeLabel);
        button.setAction(timerAction);

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setMaximumSize(new Dimension(0, 30));
        buttonPanel.setLayout(new BorderLayout(0, 0));
        buttonPanel.add(labelPanel, BorderLayout.CENTER);

        final JPanel actionsPanel = new JPanel();
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.X_AXIS));

        final JButton burnAction = addAction(actionsPanel,  Resource.getString(PropertyConstants.TOOLTIP_BURN), Icon.BURN);
        burnAction.addActionListener(new BurnButtonAction(button, timeLabel, key));

        final JButton action = addAction(actionsPanel, Resource.getString(PropertyConstants.LABEL_FINISH), Icon.FINISH);
        action.addActionListener(new FinishDialogAction(button));
        action.setEnabled(id > 3);

        buttonPanel.add(actionsPanel, BorderLayout.EAST);
        addToPanel(buttonPanel);
        return button;
    }

    private void setButtonMarked(final JButton button, final String key)
    {
        final Properties properties = new Properties();
        try (final InputStream inputStream = loadProperties(properties, TimeTrackerConstants.PROPERTIES))
        {
            if (inputStream != null)
            {
                final String value = properties.getProperty(key + TimeTrackerConstants.SUFFIX_MARKED);
                button.setBackground(Boolean.parseBoolean(value) ? Color.YELLOW : null);
            }
        }
        catch (final IOException e)
        {
            Log.severe(e.getMessage(), e);
        }
    }

    /**
     * F�gt den Men�eintrag "In Bearbeitung nehmen" hinzu. Dabei wird gepr�ft, ob das Ticket nicht schon in Bearbeitung ist. Ausserdem ist diese Aktion
     * f�r die Standardaktionen nicht vorgesehen
     * @param menu Men�
     * @param button Issue-Button
     * @param id Id
     */
    void addInProgressItem(final JPopupMenu menu, final JButton button, final int id)
    {
        try
        {
            final String issueState = id > 3 ? getIssueState(button.getText()) : null;
            final JMenuItem inProgressItem = new JMenuItem(Resource.getString(PropertyConstants.LABEL_IN_PROGRESS));
            inProgressItem.setBorder(BORDER);
            inProgressItem.setEnabled(issueState != null && !TimeTrackerConstants.ISSUE_VALUE_STATE_PROGRESS.equalsIgnoreCase(issueState));
            setButtonIcon(inProgressItem, Icon.PROGRESS);
            inProgressItem.addActionListener(new AddAction(button)
            {
                private static final long serialVersionUID = 922056815591098770L;

                @Override
                protected String createButtonText()
                {
                    return button.getText();
                }

                @Override
                protected JButton createButton(final String text, final boolean getSummary)
                {
                    return button;
                }
            });
            menu.add(inProgressItem);
        }
        catch (final URISyntaxException | IOException ex)
        {
            Log.severe(ex.getMessage(), ex);
        }
    }

    /**
     * F�gt den Men�eintrag "Zur�cksetzen" hinzu
     * @param menu Men�
     * @param button Issue-Button
     */
    void addRedoItem(final JPopupMenu menu, final JButton button)
    {
        final JMenuItem redoItem = new JMenuItem(Resource.getString(PropertyConstants.TOOLTIP_REDO));
        redoItem.setBorder(BORDER);
        setButtonIcon(redoItem, Icon.STOP);
        redoItem.addActionListener((final ActionEvent event) -> {
            final Action a = button.getAction();
            ((TimerAction) a).reset();
        });
        menu.add(redoItem);
    }

    void addStarItem(final JPopupMenu menu, final JButton button, final String key, final int id)
    {
        final JMenuItem starItem = new JMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_STAR));
        starItem.setBorder(BORDER);
        starItem.addActionListener(new TextAction(TimeTrackerConstants.STRING_EMPTY)
        {
            private static final long serialVersionUID = -101044272648382148L;

            @Override
            public void actionPerformed(final ActionEvent e)
            {
                button.setBackground(Color.YELLOW);
                saveSetting(Boolean.toString(true), key + TimeTrackerConstants.SUFFIX_MARKED);
            }
        });
        setButtonIcon(starItem, Icon.STAR);
        starItem.setEnabled(id > 3);
        menu.add(starItem);
    }

    private JButton addAction(final JPanel parent, final String tooltip, final Icon icon)
    {
        final JButton button = new JButton();
        button.setBorder(new EmptyBorder(8, 8, 8, 8));
        button.setToolTipText(tooltip);
        setButtonIcon(button, icon);
        parent.add(button);
        return button;
    }

    private void setTime(final JLabel label, final String key)
    {
        final Properties properties = new Properties();

        try (final InputStream inputStream = loadProperties(properties, TimeTrackerConstants.PROPERTIES))
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
        catch (final IOException e)
        {
            Log.severe(e.getMessage(), e);
        }
    }

    final void setButtonIcon(final AbstractButton button, final Icon icon)
    {
        if (icon == null)
        {
            return;
        }
        setButtonIcon(button, icon.getIcon());
    }

    private void setButtonIcon(final AbstractButton button, final String icon)
    {
        if (icon == null || icon.isEmpty())
        {
            button.setIcon(null);
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
        catch (final IOException e)
        {
            Log.severe(e.getMessage(), e);
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

    private void updateGui(final boolean removeLine)
    {
        if (removeLine)
        {
            final GridLayout layout = (GridLayout) this.panel.getLayout();
            layout.setRows(layout.getRows() - 1);
        }
        pack();
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

        TimerAction()
        {
            super();
            this.button = null;
            this.timer = null;
            this.label = null;
            this.key = null;
        }

        TimerAction(final JButton button)
        {
            super(button.getText(), button.getIcon());
            this.button = button;
            this.timer = null;
            this.label = null;
            this.key = null;
        }

        TimerAction(final String text)
        {
            super(text, null);
            this.button = null;
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
            saveSetting(Boolean.toString(false), this.key + TimeTrackerConstants.SUFFIX_MARKED);
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
            stop(true, false);
        }

        void stopWithoutSave()
        {
            stop(false, false);
        }

        void stop(final boolean saveDuration, final boolean reset)
        {
            if (this.timer != null)
            {
                this.timer.stop();
                if (saveDuration && this.label != null)
                {
                    saveDuration(reset);
                }
            }

            if(this.button.getBackground() == Color.YELLOW)
            {
                //Der Button wurde markiert
                return;
            }
            this.button.setBackground(null);
            this.button.setOpaque(false);
        }

        private void saveDuration(final boolean reset)
        {
            final String currentDuration = this.label.getText();
            saveSetting(reset ? TimeTrackerConstants.STRING_EMPTY : currentDuration, getDurationKey());
        }

        void reset()
        {
            stop(true, true);
            this.duration = 0;
            if (this.label != null)
            {
                this.label.setText(TimeTrackerConstants.STRING_EMPTY);
            }
            removeDuration();
        }

        private void removeDuration()
        {
            if (this.key == null)
            {
                return;
            }

            try (final InputStream inputStream = TimeTracker.class.getResourceAsStream(TimeTrackerConstants.PROPERTIES))
            {

                final Properties properties = new Properties();
                properties.load(inputStream);
                properties.remove(this.key + TimeTrackerConstants.SUFFIX_DURATION);
                //properties.remove(this.key + TimeTrackerConstants.SUFFIX_DURATION_SAVED);
                storeProperties(properties);
            }
            catch (final IOException ex)
            {
                Log.severe(ex.getMessage(), ex);
            }
        }

        private String getDurationKey()
        {
            return this.key + TimeTrackerConstants.SUFFIX_DURATION;
        }
    }

    /**
     * Liefert den Schl�ssel zum Speichern der Zeitdauer
     * @param key Schl�ssel f�r das Label
     * @return Schl�ssel f�r die Zeitdauer
     */
    private String getDurationKey(final String key)
    {
        return key + TimeTrackerConstants.SUFFIX_DURATION;
    }

    /**
     * Liefert das Ticket zu einem Button. Entweder, wenn es schon gespeichert ist oder geparst aus dem Titel
     * @param key Schl�ssel mit der Button-ID
     * @param button Button mit dem Titel des Tickets
     * @return Ticket-ID zum Button
     */
    String getTicket(final String key, final JButton button)
    {
        final String ticket = loadSetting(key, TimeTrackerConstants.SUFFIX_TICKET);
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
     * L�dt eine Einstellung aus den Properties
     * @param key Schl�ssel (Label des Buttons)
     * @param setting Einstellung
     * @return Wert
     */
    private String loadSetting(final String key, final String setting)
    {
        final Properties properties = getProperties();
        return properties != null ? properties.getProperty(key + setting) : null;
    }

    private void setLabelTooltip(final String savedDuration, final JLabel timeLabel)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("<html>").append(Resource.getString(PropertyConstants.TIME_SAVED)).append(TimeTrackerConstants.STRING_SPACE).append(savedDuration);
        timeLabel.setToolTipText(sb.append("</html>").toString());
    }

    /**
     * Klasse zum Burnen von Zeiten
     */
    private class BurnButtonAction extends AbstractAction
    {
        private static final long serialVersionUID = -2092965435624779543L;
        final JButton button;
        private final JLabel label;
        private final String key;

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

            final String buttonText = this.button.getText();
            MATCHER.reset(buttonText);
            if (MATCHER.matches())
            {
                ticketField.setText(MATCHER.group(1));
            }
            else
            {
                final String ticket = loadSetting(this.key, TimeTrackerConstants.SUFFIX_TICKET);
                ticketField.setText(ticket);
            }

            rows.add(new JLabel(Resource.getString(PropertyConstants.LABEL_TICKET)));
            rows.add(ticketField);

            final JTextField timeField = new JTextField();
            timeField.setBackground(MANDATORY);
            timeField.setText(getParsedTime(this.label.getText()));
            rows.add(new JLabel(Resource.getString(PropertyConstants.LABEL_TIME)));
            rows.add(timeField);

            final JComboBox<String[]> typeField = new JComboBox<>();
            timeField.setBackground(MANDATORY);

            final String type = loadSetting(this.key, TimeTrackerConstants.SUFFIX_TYPE);
            final LinkedList<String[]> items = new LinkedList<>();
            items.add(new String[]{"136-0", "Development"});
            items.add(new String[]{"136-1", "Testing"});
            items.add(new String[]{"136-2", "Documentation"});
            items.add(new String[]{"136-3", "Meeting"});
            items.add(new String[]{"136-4", "Support"});
            items.add(new String[]{"136-6", "Design"});

            for (final String[] item : items)
            {
                typeField.addItem(item);
                if (item[0].equalsIgnoreCase(type))
                {
                    typeField.setSelectedItem(item);
                }
            }

            //noinspection unchecked
            typeField.setRenderer(RENDERER);

            rows.add(new JLabel(Resource.getString(PropertyConstants.LABEL_TYPE)));
            rows.add(typeField);

            final JTextArea textArea = new JTextArea(5, 20);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            rows.add(new JLabel(Resource.getString(PropertyConstants.LABEL_TEXT)));
            rows.add(new JScrollPane(textArea));

            final Action action = this.button.getAction();
            final TimerAction timerAction = action instanceof TimerAction ? (TimerAction) action : null;
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

                        final String savedDuration = loadSetting(key, TimeTrackerConstants.SUFFIX_DURATION_SAVED);
                        setLabelTooltip(savedDuration, label);
                        dialog.dispose();
                    }
                }
            });

            final JPanel okBtnPanel = new JPanel();
            okBtnPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
            okBtnPanel.add(ok);
            dialog.add(okBtnPanel);
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
                TIME_MATCHER.reset(time);
                if (TIME_MATCHER.matches())
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
                else
                {
                    BURN_MATCHER.reset(time);
                    if(BURN_MATCHER.matches())
                    {
                        currentHours = Integer.parseInt(BURN_MATCHER.group(1));
                        currentMinutes = Integer.parseInt(BURN_MATCHER.group(2));
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
            saveSetting(ticket, this.key + TimeTrackerConstants.SUFFIX_TICKET);
            saveSetting(type, this.key + TimeTrackerConstants.SUFFIX_TYPE);

            final int[] spentTimeUnits = getTimeUnits(spentTime);
            int spentHours = spentTimeUnits[0];
            int spentMinutes = spentTimeUnits[1];

            final String savedDuration = loadSetting(this.key, TimeTrackerConstants.SUFFIX_DURATION_SAVED);
            final int[] savedTimeUnits = getTimeUnits(savedDuration);
            final int savedHours = savedTimeUnits[0];
            int savedMinutes = savedTimeUnits[1];

            final int additionalHours = savedHours / 60;
            savedMinutes = savedMinutes % 60;

            spentHours += savedHours + additionalHours;
            spentMinutes += savedMinutes;

            spentHours += spentMinutes / 60;
            spentMinutes = spentMinutes % 60;

            saveSetting(getParsedTime(Integer.toString(spentHours), Integer.toString(spentMinutes)), this.key + TimeTrackerConstants.SUFFIX_DURATION_SAVED);

            if (token == null || token.isEmpty())
            {
                JOptionPane.showMessageDialog(timeTracker, String.format("Authorization token not found! Please create a token in youtrack and enter it in the " +
                                                                         "%s with the key %s", PROPERTIES, TimeTrackerConstants.YOUTRACK_TOKEN));
                return false;
            }

            String text = textArea.getText();
            if (text == null)
            {
                text = TimeTrackerConstants.STRING_EMPTY;
            }

            try
            {
                final URIBuilder builder = getURIBuilder(ServicePath.WORKITEM, ticket);
                final HttpPost request = new HttpPost(builder.build());
                request.setEntity(new StringEntity(String.format(TimeTrackerConstants.ENTITY, System.currentTimeMillis(), userId, spentTime, type, text), ContentType.APPLICATION_JSON));

                final HttpResponse response = executeRequest(request);
                if (response == null)
                {
                    return false;
                }
                logResponse(response);
            }
            catch (final URISyntaxException | IOException e)
            {
                Log.severe(e.getMessage(), e);
            }

            return true;
        }
    }

    /**
     * Liefert die aktuelle Position der Anwendung
     * @return aktuelle Position
     */
    private Point getWindowLocation()
    {
        if(!timeTracker.isShowing())
        {
            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            final int x = screenSize.width / 2 - 125;
            final int y = screenSize.height / 2 - 85;
            return new Point(x, y);
        }
        final Point location = timeTracker.getLocationOnScreen();
        final int x = location.x + (timeTracker.getWidth() / 2) - 125;
        final int y = location.y + (timeTracker.getHeight() / 2) - 85;
        return new Point(x, y);
    }

    /**
     * Liefert einen URIBuilder
     * @param path Rest-Endpoint
     * @param ticket Ticket
     * @return URIBuilder
     */
    URIBuilder getURIBuilder(final ServicePath path, final String ticket, final NameValuePair... parameters)
    {
        final URIBuilder builder = new URIBuilder();
        if(path != ServicePath.USER && !checkUserId())
        {
            Log.severe("User id {0} does not match", this.userId);
            return builder;
        }

        builder.setScheme(this.scheme);
        builder.setHost(this.host);

        if(this.port != -1)
        {
            builder.setPort(this.port);
        }

        if (ticket == null)
        {
            builder.setPath(path.restEndPoint);
        }
        else
        {
            builder.setPath(String.format(path.restEndPoint, ticket));
        }
        if(parameters != null && parameters.length > 0)
        {
            builder.setParameters(parameters);
        }
        return builder;
    }

    private boolean checkUserId()
    {
        if(this.userId != null)
        {
            USER_ID_MATCHER.reset(this.userId);
        }
        if(this.userId == null || !USER_ID_MATCHER.matches())
        {
            Log.warning("User id {0} does not match", this.userId);
            try
            {
                requestUserID();
            }
            catch (final IOException | URISyntaxException ex)
            {
                Log.severe(ex.getMessage(), ex);
            }
        }
        USER_ID_MATCHER.reset(this.userId);
        return USER_ID_MATCHER.matches();
    }

    /**
     * Liefert den Wert eines "id"-Feldes aus dem Response
     * @param response Response
     * @param saveWithKey Schl�ssel, unter welchem die ermittelte ID abgespeichert werden soll
     * @return ID als String
     * @throws IOException I/O Error beim Schreiben in den Outputstream
     */
    private String getID(final HttpResponse response, final String saveWithKey) throws IOException
    {
        final String value = getValueFromJson(response, "id", false);
        if(value != null && saveWithKey != null)
        {
            saveSetting(value, saveWithKey);
        }
        return value;
    }

    /**
     * Liefert einen Wert aus dem Json
     * @param response Response
     * @param key Schl�ssel im Json, dessen Wert ermittelt werden soll
     * @return Wert aus dem Json
     * @throws IOException I/O Error
     */
    private String getValueFromJson(final HttpResponse response, final String key, final boolean isCustomField) throws IOException
    {
        String result = null;
        final StatusLine statusLine = response.getStatusLine();
        final int status = statusLine.getStatusCode();
        if (status == HttpStatus.SC_OK)
        {
            JsonParser parser = null;
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
            {
                response.getEntity().writeTo(outputStream);
                outputStream.flush();

                final String msg = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
                Log.info(msg);

                final JsonFactory jsonFactory = new JsonFactory();
                parser = jsonFactory.createParser(msg);
                result = getValueFromParser(parser, key, isCustomField);
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
     * Liefert den Wert zum �bergebenen Schl�ssel
     * @param parser JsonParser
     * @param key Schl�ssel
     * @param isCustomField <code>true</code>, wenn es sich um ein CustomField handelt. Dann steht der Wert woanders. Sonst <code>false</code>
     * @return Wert zum �bergebenen Schl�ssel
     * @throws IOException I/O Error
     */
    private String getValueFromParser(final JsonParser parser, final String key, final boolean isCustomField) throws IOException
    {
        boolean start = false;
        while ((parser.nextToken()) != null)
        {
            if (start)
            {
                final String name = parser.getCurrentName();
                if(!"name".equalsIgnoreCase(name))
                {
                    continue;
                }
                return parser.nextTextValue();
            }
            final String name = parser.getCurrentName();
            if (key.equalsIgnoreCase(name))
            {
                return parser.nextTextValue();
            }
            else if(isCustomField && key.equalsIgnoreCase(parser.getText()))
            {
                start = true;
            }
        }
        return null;
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
        initHttpClient();
        return this.client.execute(request);
    }

    private void initHttpClient() throws IOException
    {
        if(this.client != null)
        {
            return;
        }

        final SchemePortResolver portResolver = new DefaultSchemePortResolver();
        try
        {
            portResolver.resolve(new HttpHost(this.host, this.port, this.scheme));
        }
        catch (final UnsupportedSchemeException e)
        {
            Log.severe(e.getMessage(), e);
            return;
        }

        final RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
        if("https".equalsIgnoreCase(this.scheme))
        {
            final SSLContext sslContext = createSSLContext();
            registryBuilder.register(this.scheme, new SSLConnectionSocketFactory(sslContext));
        }
        else
        {
            registryBuilder.register(this.scheme, PlainConnectionSocketFactory.getSocketFactory());
        }

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
                                                 new BasicHeader(HttpHeaders.ACCEPT, TimeTrackerConstants.MIMETYPE_JSON),
                                                 new BasicHeader(HttpHeaders.CONTENT_TYPE, TimeTrackerConstants.MIMETYPE_JSON),
                                                 new BasicHeader(HttpHeaders.CACHE_CONTROL, TimeTrackerConstants.NO_CACHE)));
        this.client = httpClient.build();
    }

    private SSLContext createSSLContext() throws IOException
    {
        final String certFileName = Optional.ofNullable(getProperties()).map(props -> props.getProperty(TimeTrackerConstants.YOUTRACK_CERT)).orElse(null);
        if(certFileName == null || certFileName.isEmpty())
        {
            final String msg = String.format("Value of property %s missing.", TimeTrackerConstants.YOUTRACK_CERT);
            JOptionPane.showMessageDialog(this, msg);
            throw new IOException(msg);
        }

        final File certificate = new File(TimeTracker.home + "security\\" + certFileName);
        if(!certificate.exists())
        {
            final String msg = "Certificate file missing.";
            JOptionPane.showMessageDialog(this, msg);
            throw new IOException(msg);
        }

        final SSLContext sslContext;
        try
        {
            final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null);

            try(final FileInputStream inputStream = new FileInputStream(certificate))
            {
                final X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(inputStream);
                final String alias = cert.getSubjectX500Principal().getName();
                trustStore.setCertificateEntry(alias, cert);
            }

            final TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(trustStore);

            final TrustManager[] trustManagers = tmf.getTrustManagers();
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, null);
        }
        catch (final IOException e)
        {
            throw e;
        }
        catch (final Exception e)
        {
            final String msg = e.getClass().getSimpleName() + ": " + Optional.ofNullable(e.getMessage()).orElse(e.getCause().getMessage());
            System.err.println(msg);
            JOptionPane.showMessageDialog(this, msg);
            throw new IOException(msg, e.getCause());
        }
        return sslContext;
    }


    DeleteButtonAction createDeleteButtonAction(final JButton button, final String key)
    {
        return new DeleteButtonAction(button, key);
    }

    /**
     * L�scht eine komplette Zeile
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
            try (final InputStream inputStream = TimeTracker.class.getResourceAsStream(TimeTrackerConstants.PROPERTIES))
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
                updateGui(true);
                --line;
            }
            catch (final IOException ex)
            {
                Log.severe(ex.getMessage(), ex);
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
     * Setzt alle Timer zur�ck
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
     * F�gt an der �bergebenen Komponente ein Escape-Event hinzu
     * @param window Fenster, welchem ein Escape-Event hinzuf�gt
     */
    private void addEscapeEvent(final RootPaneContainer window)
    {
        window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), TimeTrackerConstants.ACTIONMAP_KEY_CANCEL); //$NON-NLS-1$
        window.getRootPane().getActionMap().put(TimeTrackerConstants.ACTIONMAP_KEY_CANCEL, new AbstractAction(){
            private static final long serialVersionUID = 4056930123544439411L; //$NON-NLS-1$

            @Override
            public void actionPerformed(final ActionEvent e)
            {
                ((Window) window).dispose();
            }
        });
    }

    ShowAddButtonAction createShowAddButtonAction(final JButton button)
    {
        return new ShowAddButtonAction(button);
    }

    /**
     * Klasse zur Darstellung eines Dialoges, mit welchem ein neues Ticket angelegt werden kann.
     */
    private class ShowAddButtonAction extends TimerAction
    {
        private static final long serialVersionUID = -2104627297533100111L;

        ShowAddButtonAction()
        {
            super();
        }

        ShowAddButtonAction(final JButton button)
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
            labelField.setBackground(MANDATORY);

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

            addEscapeEvent(frame);

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

            final Frame topMostFrame = getParentFrame(this.button);
            if(topMostFrame != null)
            {
                frame.setLocation(topMostFrame.getX() - ((frame.getWidth() - topMostFrame.getWidth()) / 2), timeTracker.getY());
            }
        }

        /**
         * F�gt das Ticket aus der Zwischenablage ein, insofern es sich um ein Ticket handelt
         * @param textField Textfeld, in welches die Ticketnummer eingef�gt werden soll.
         * @return <code>true</code>, wenn das Ticket eingef�gt wurde, sonst <code>false</code>
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
                        MATCHER.reset((String) transferData);
                        if(MATCHER.matches())
                        {
                            textField.setText(MATCHER.group(1));
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

    /**
     * Liefert die interne ID eines Issues. F�r manche Operationen, z.b. Commands, kann nicht mit dem Issue gearbeitet werden
     * @param ticket Issue
     * @return Interne ID des Issues
     * @throws URISyntaxException Wenn das Parsen als URI-Referenz schief ging
     * @throws IOException Verbindungsproblem
     */
    private String getIssueID(final String ticket) throws URISyntaxException, IOException
    {
        final URIBuilder getIssueBuilder = getURIBuilder(ServicePath.ISSUE, ticket);
        final HttpGet request = new HttpGet(getIssueBuilder.build());
        final HttpResponse response = executeRequest(request);
        if (response == null)
        {
            return null;
        }
        return getID(response, null);
    }

    /**
     * Ermittelt die Beschreibung zu einem Ticket
     * @param text Ticket
     * @return die Beschreibung zu einem Ticket
     * @throws URISyntaxException Invalide URL
     * @throws IOException I/O Error
     */
    private String getIssueSummary(String text) throws URISyntaxException, IOException
    {
        if (!matches(text))
        {
            return null;
        }
        final String ticket = MATCHER.group(1);
        text = text.replace(ticket, TimeTrackerConstants.STRING_EMPTY);
        if(!text.trim().isEmpty())
        {
            //Sollte der Nutzer was eigenes hingeschrieben haben, so sollte das nicht ersetzt werden
            return null;
        }
        return getSummaryFromJson(ticket);
    }

    /**
     * Liefert den Ticket-Status
     * @param text Ticket, ggf. mit Beschreibung
     * @return Ticket-Status
     * @throws URISyntaxException Invalide URL
     * @throws IOException I/O Error
     */
    private String getIssueState(final String text) throws URISyntaxException, IOException
    {
        if (!matches(text))
        {
            return null;
        }
        return getValueFromJson(MATCHER.group(1), TimeTrackerConstants.ISSUE_CUSTOM_FIELDS, TimeTrackerConstants.ISSUE_STATE);
    }

    private String getSummaryFromJson(final String ticket) throws IOException, URISyntaxException
    {
        return getValueFromJson(ticket, TimeTrackerConstants.ISSUE_SUMMARY, TimeTrackerConstants.ISSUE_SUMMARY);
    }

    private String getValueFromJson(final String ticket, final String fields, final String attribute) throws IOException, URISyntaxException
    {
        final URIBuilder builder = getURIBuilder(ServicePath.ISSUE, ticket, new BasicNameValuePair(TimeTrackerConstants.FIELDS, fields));
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
        final boolean isCustomField = TimeTrackerConstants.ISSUE_CUSTOM_FIELDS.equalsIgnoreCase(fields);
        return getValueFromJson(response, attribute, isCustomField);
    }

    private class EditAction extends TimerAction
    {
        private static final long serialVersionUID = -7024916220743619039L;
        private final JButton issueButton;
        private final JTextField textInput;
        private final JFileChooser icon;

        EditAction(final JButton okButton, final JButton issueButton, final JTextField textInput, final JFileChooser icon)
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
            final String text = getTicketSummary(this.textInput);

            this.issueButton.setText(text);
            setButtonIcon(this.issueButton, filePath);

            final Properties properties = new Properties();
            try(final InputStream inputStream = getPropertiesInputStream(TimeTrackerConstants.PROPERTIES))
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
                storeButtonProperties(properties, propertyKey, text, filePath);
            }
            catch (final IOException ex)
            {
                Log.severe(ex.getMessage(), ex);
            }

            final Frame frame = getParentFrame(this.button);
            if(frame != null)
            {
                frame.dispose();
            }
        }
    }

    protected String getTicketSummary(final JTextField textInput)
    {
        String text = textInput == null ? null : textInput.getText();
        if(text != null)
        {
            text = text.trim();
        }
        if (text == null || text.isEmpty())
        {
            return null;
        }
        return getTicketSummary(text);
    }

    private String getTicketSummary(final String text)
    {
        try
        {
            final String summary = getIssueSummary(text);
            if(summary != null)
            {
                return text + TimeTrackerConstants.STRING_SPACE + summary;
            }
        }
        catch (final URISyntaxException | IOException ex)
        {
            Log.severe(ex.getMessage(), ex);
        }
        return text;
    }

    private void storeButtonProperties(final Properties properties, final String propertyKey, final String text, final String filePath) throws IOException
    {
        properties.setProperty(propertyKey + TimeTrackerConstants.SUFFIX_LABEL, text);
        properties.setProperty(propertyKey + TimeTrackerConstants.SUFFIX_ICON, filePath);
        storeProperties(properties);
    }

    void showAddIssueDialog(final String text)
    {
        if(!matches(text))
        {
            return;
        }
        final AddAction action = new AddAction(Resource.getString(PropertyConstants.TEXT_OK));
        action.handleConfirmationDialog(text, false, true);
    }

    private boolean matches(final String text)
    {
        MATCHER.reset(text);
        return MATCHER.matches();
    }

    /**
     * Klasse zum Hinzuf�gen eines neuen Eintrags
     */
    class AddAction extends TimerAction
    {
        private static final long serialVersionUID = 2109270279366930967L;
        private JTextField textInput;
        private JFileChooser icon;

        AddAction(final JButton button)
        {
            this(button, null, null);
        }

        AddAction(final JButton button, final JTextField textInput, final JFileChooser icon)
        {
            super(button);
            this.textInput = textInput;
            this.icon = icon;
        }

        public AddAction(final String text)
        {
            super(text);
        }

        @Override
        public void actionPerformed(final ActionEvent e)
        {
            final String text = createButtonText();
            if(text == null)
            {
                return;
            }
            handleConfirmationDialog(text, true, false);
        }

        protected String createButtonText()
        {
            return getTicketSummary(this.textInput);
        }

        protected JButton createButton(String text, final boolean getSummary) throws IOException
        {
            if(getSummary)
            {
                text = getTicketSummary(text);
                if(text == null)
                {
                    return null;
                }
            }

            final File file = this.icon == null ? null : this.icon.getSelectedFile();
            final String filePath = file == null ? TimeTrackerConstants.STRING_EMPTY : file.getPath();
            final int missingNumber = getMissingNumber();
            final String counter = missingNumber < 10 ? ("0" + missingNumber) : Integer.toString(missingNumber);

            final Properties properties = new Properties();
            try(final InputStream inputStream = getPropertiesInputStream(TimeTrackerConstants.PROPERTIES))
            {
                properties.load(inputStream);
            }

            JButton button = null;
            try
            {
                final String propertyKey = TimeTrackerConstants.PREFIX_BUTTON + counter;
                storeButtonProperties(properties, propertyKey, text, filePath);

                button = addButton(propertyKey, text, filePath);
                updateGui(false);
                ++line;
            }
            catch (final IOException ex)
            {
                Log.severe(ex.getMessage(), ex);
            }

            final Frame frame = getParentFrame(this.button);
            if(frame != null)
            {
                frame.dispose();
            }
            return button;
        }

        /**
         * Liefert alle internen IDs der Buttons
         * @return interne IDs der Buttons
         * @throws IOException Wenn beim Lesen der Properties etwas schief ging
         */
        private Set<Integer> getButtonIds() throws IOException
        {
            final Properties properties = new Properties();
            try (final InputStream inputStream = getPropertiesInputStream(TimeTrackerConstants.PROPERTIES))
            {
                properties.load(inputStream);
            }

            final Set<Integer> buttonIds = new TreeSet<>();
            for(String key : properties.stringPropertyNames())
            {
                if(!key.startsWith(TimeTrackerConstants.PREFIX_BUTTON))
                {
                    continue;
                }
                key = key.substring(TimeTrackerConstants.PREFIX_BUTTON.length());
                buttonIds.add(Integer.parseInt(key.substring(0, key.indexOf('.'))));
            }
            return buttonIds;
        }

        /**
         * Liefert eine fehlende oder die n�chst h�here ID
         * @return eine fehlende oder die n�chst h�here ID
         * @throws IOException Wenn beim Lesen der Properties etwas schief ging
         */
        private int getMissingNumber() throws IOException
        {
            final Set<Integer> buttonIds = getButtonIds();
            if(buttonIds.isEmpty())
            {
                return 0;
            }
            int lastNumber = 0;
            int missingNumber = -1;
            for(final int id : buttonIds)
            {
                lastNumber = id;
                if(id - missingNumber > 1)
                {
                    return ++missingNumber;
                }
                missingNumber = id;
            }
            return ++lastNumber;
        }

        /**
         * Zeigt einen Dialog an, mit welchem ein Ticket in Bearbeitung genommen werden kann
         * @param text Text auf dem Button mit dem Issue
         */
        private void handleConfirmationDialog(final String text, final boolean createButtonOnCancel, final boolean getIssueSummary)
        {
            if (!matches(text))
            {
                return;
            }

            Log.info(text);

            final String ticket = MATCHER.group(1);
            final JDialog dialog = getDialog(250);

            final JPanel rows = new JPanel(new GridLayout(2, 1));
            rows.setBorder(new EmptyBorder(10, 10, 10, 10));
            dialog.add(rows);

            rows.add(new JLabel(Resource.getString(PropertyConstants.TICKET_IN_PROGRESS, ticket)));

            final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
            buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            rows.add(buttonPanel);

            final JButton cancelButton = new JButton(Resource.getString(PropertyConstants.TEXT_NO));
            cancelButton.addActionListener((final ActionEvent event) -> {
                if (createButtonOnCancel)
                {
                    try
                    {
                        createButton(text, getIssueSummary);
                    }
                    catch (final IOException ex)
                    {
                        Log.severe(ex.getMessage(), ex);
                    }
                }
                dialog.dispose();
            });

            final JButton okButton = new JButton(Resource.getString(PropertyConstants.TEXT_YES));
            okButton.addActionListener((final ActionEvent event) -> {
                try
                {
                    final String issueID = getIssueID(ticket);
                    if (issueID == null)
                    {
                        Log.severe("Issue id of " + ticket + " not found");
                        return;
                    }

                    final URIBuilder builder = getCommandURIBuilder();
                    if(builder == null)
                    {
                        return;
                    }

                    final HttpPost request = new HttpPost(builder.build());
                    request.setEntity(new StringEntity(String.format(TimeTrackerConstants.ISSUE_COMMAND, TimeTrackerConstants.ISSUE_STATE,
                                                                     TimeTrackerConstants.ISSUE_VALUE_STATE_PROGRESS, issueID), ContentType.APPLICATION_JSON));

                    final HttpResponse response = executeRequest(request);
                    if (response == null)
                    {
                        return;
                    }
                    logResponse(response);
                    dialog.dispose();

                    final JButton button = createButton(text, getIssueSummary);
                    button.doClick();
                }
                catch (final URISyntaxException | IOException ex)
                {
                    Log.severe(ex.getMessage(), ex);
                }
            });

            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);

            SwingUtilities.getRootPane(okButton).setDefaultButton(okButton);

            dialog.pack();
            dialog.setVisible(true);
        }
    }

    private class FinishDialogAction extends TimerAction
    {
        private static final long serialVersionUID = 7059526162584192854L;

        FinishDialogAction(final JButton button)
        {
            super(button);
        }

        @Override
        public void actionPerformed(final ActionEvent e)
        {
            MATCHER.reset(button.getText());
            if (MATCHER.matches())
            {
                final JDialog dialog = getDialog(200);
                final JPanel rows = new JPanel();
                rows.setBorder(new EmptyBorder(10, 10, 10, 10));
                dialog.add(rows);

                rows.add(new JLabel(Resource.getString(PropertyConstants.LABEL_STATE)));
                final JComboBox<String> statesBox = new ComboBoxStates();
                rows.add(statesBox);

                rows.add(new JLabel(Resource.getString(PropertyConstants.LABEL_VERSION)));
                final JComboBox<String> versionsBox = new ComboBoxFixVersions();
                rows.add(versionsBox);

                final JPanel commentPanel = new JPanel();
                commentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
                dialog.add(commentPanel);

                commentPanel.add(new JLabel(Resource.getString(PropertyConstants.LABEL_COMMENT)));
                final JTextArea comment = new JTextArea(1, 5);
                comment.setLineWrap(true);
                comment.setWrapStyleWord(true);
                comment.setPreferredSize(new Dimension(300, 100));
                commentPanel.add(comment);

                final JButton okButton = new JButton(Resource.getString(PropertyConstants.TEXT_OK));
                okButton.addActionListener(new TextAction(Resource.getString(PropertyConstants.TEXT_OK))
                {
                    private static final long serialVersionUID = 566865284107947772L;

                    @Override
                    public void actionPerformed(final ActionEvent e)
                    {
                        try
                        {
                            final String issueID = getIssueID(MATCHER.group(1));

                            URIBuilder builder = getCommandURIBuilder();
                            HttpPost request = new HttpPost(builder.build());
                            request.setEntity(new StringEntity(String.format(TimeTrackerConstants.ISSUE_COMMAND, TimeTrackerConstants.ISSUE_FIX_VERSIONS,
                                                                             versionsBox.getSelectedItem(), issueID), ContentType.APPLICATION_JSON));
                            HttpResponse response = executeRequest(request);
                            logResponse(response);

                            request.setEntity(new StringEntity(String.format(TimeTrackerConstants.ISSUE_COMMAND, TimeTrackerConstants.ISSUE_STATE,
                                                                             statesBox.getSelectedItem(), issueID), ContentType.APPLICATION_JSON));
                            response = executeRequest(request);
                            logResponse(response);

                            final String commentText = comment.getText();
                            if (commentText != null && !commentText.isEmpty())
                            {
                                builder = getURIBuilder(ServicePath.COMMENT, issueID);
                                request = new HttpPost(builder.build());
                                request.setEntity(new StringEntity(String.format(TimeTrackerConstants.ISSUE_COMMENT, commentText), ContentType.APPLICATION_JSON));

                                response = executeRequest(request);
                                logResponse(response);
                            }

                            dialog.dispose();
                        }
                        catch (final URISyntaxException | IOException ex)
                        {
                            Log.severe(ex.getMessage(), ex);
                        }
                    }
                });
                dialog.add(okButton);
                dialog.pack();
                dialog.setVisible(true);
            }
        }
    }

    /**
     * Liefert einen kleinen R�ckfragedialog
     * @param width Breite des Dialogs
     * @return R�ckfragedialog
     */
    private JDialog getDialog(final int width)
    {
        final Point location = getWindowLocation();
        final JDialog dialog = new JDialog(timeTracker, Resource.getString(PropertyConstants.TEXT_CONFIRMATION), true);
        dialog.setBounds(location.x, location.y, width, 200);
        dialog.setResizable(false);
        dialog.getContentPane().setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
        return dialog;
    }

    /**
     * Liefert den URI-Builder f�r Commands
     * @return URIBuilder
     */
    private URIBuilder getCommandURIBuilder()
    {
        return getURIBuilder(ServicePath.COMMAND, null);
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

            final String msg = outputStream.toString();
            if (status == HttpStatus.SC_OK)
            {
                Log.info(msg);
                Log.info("Success.");
            }
            else
            {
                Log.warning(msg);
            }
        }
    }

    /**
     * Liefet den �bergeordneten JFrame
     * @param component Komponente, dessen �bergeordneter Frame ermittelt werden soll
     * @return �bergeordneter Frame
     */
    private Frame getParentFrame(final Container component)
    {
        if(component == null)
        {
            return null;
        }
        final Container parent = component.getParent();
        if (parent instanceof Frame)
        {
            return (Frame) parent;
        }
        return getParentFrame(parent);
    }

    /**
     * Speichert die Fenstergr��e und -position
     */
    private void saveWindowPositionAndSize()
    {
        final Properties properties = new Properties();
        try (final InputStream inputStream = loadProperties(properties, TimeTrackerConstants.PROPERTIES))
        {
            if (inputStream != null)
            {
                final Point location = getLocationOnScreen();
                String value = String.format("%s,%s", Math.max(location.x, 0), Math.max(location.y, 0));
                Log.fine("Saving window position {0}", value);
                properties.setProperty(PropertyConstants.WINDOW_LOCATION, value);

                final Dimension dimension = getSize();
                value = String.format("%s,%s", dimension.width, dimension.height);
                Log.fine("Saving window dimension {0}", value);
                properties.setProperty(PropertyConstants.WINDOW_DIMENSION, value);

                saveDurations(properties);
                storeProperties(properties);
            }
        }
        catch (final IOException e)
        {
            Log.severe(e.getMessage(), e);
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
                    final String currentTime = ((JLabel) component).getText();
                    properties.put(getDurationKey(name), currentTime);
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
     * Stellt die Fenstergr��e und -position aus den Properties wieder her
     */
    private void restoreWindowPositionAndSize()
    {
        final Properties properties = new Properties();
        try (final InputStream inputStream = loadProperties(properties, TimeTrackerConstants.PROPERTIES))
        {
            if (inputStream != null)
            {
                final String windowLocation = properties.getProperty(PropertyConstants.WINDOW_LOCATION);
                if (windowLocation != null && !windowLocation.isEmpty())
                {
                    Log.fine("Restoring window position {0}", windowLocation);
                    final StringTokenizer tokenizer = new StringTokenizer(windowLocation, ",");
                    final Rectangle rectangle = new Rectangle(new Point(Integer.parseInt(tokenizer.nextToken()), Integer.parseInt(tokenizer.nextToken())));
                    setBounds(rectangle);
                }

                final String windowDimension = properties.getProperty(PropertyConstants.WINDOW_DIMENSION);
                if (windowDimension != null && !windowDimension.isEmpty())
                {
                    Log.fine("Restoring window dimension {0}", windowDimension);
                    final StringTokenizer tokenizer = new StringTokenizer(windowDimension, ",");
                    final Dimension dimension = new Dimension(Integer.parseInt(tokenizer.nextToken()), Integer.parseInt(tokenizer.nextToken()));
                    setSize(dimension);
                }
            }
        }
        catch (final IOException e)
        {
            Log.severe(e.getMessage(), e);
        }
    }

    /**
     * Liefert alle Properties in einem Objekt
     * @return alle Properties
     */
    private static Properties getProperties()
    {
        final Properties properties = new Properties();

        try (@SuppressWarnings("unused") final InputStream defaultInputStream = loadProperties(properties, TimeTrackerConstants.DEFAULT_PROPERTIES);
             @SuppressWarnings("unused") final InputStream inputStream = loadProperties(properties, TimeTrackerConstants.PROPERTIES))
        {
            return properties;
        }
        catch (final IOException e)
        {
            Log.severe(e.getMessage(), e);
        }
        return null;
    }

    /**
     * L�dt Properties. Der Stream als R�ckgabe muss selbst geschlossen werden.
     * @param properties Properties, welche erweitert werden
     * @param propertyFileName Name des Property-Files, welches ausgelesen werden soll
     * @return InputStream
     * @throws IOException Wenn vom Inputstream nicht gelesen werden konnte
     */
    private static InputStream loadProperties(final Properties properties, final String propertyFileName) throws IOException
    {
        final InputStream inputStream = getPropertiesInputStream(propertyFileName);
        if(inputStream != null)
        {
            properties.load(inputStream);
        }
        return inputStream;
    }

    private static File getPropertyFile(final String propertyFileName)
    {
        File propertyFile = new File(TimeTracker.home + propertyFileName);
        if(!propertyFile.exists())
        {
            Log.warning("Property file not found: {0}", propertyFile.getAbsolutePath());
            Log.info("Creating property file: {0}", propertyFile.getAbsolutePath());
            try
            {
                final Path file = Files.createFile(propertyFile.toPath());
                propertyFile = file.toFile();
                Log.info("Property file created: " + propertyFile.getAbsolutePath());
            }
            catch (final IOException | SecurityException | UnsupportedOperationException e)
            {
                Log.severe(e.getMessage(), e);
                return null;
            }
        }
        return propertyFile;
    }

    /**
     * Liefert das Property-File als Inputstream
     * @param propertyFileName Name des PropertyFiles
     * @return InputStream
     * @throws FileNotFoundException wenn die Property-Datei nicht gefunden wurde
     */
    private static InputStream getPropertiesInputStream(final String propertyFileName) throws FileNotFoundException
    {
        final File propertyFile = getPropertyFile(propertyFileName);
        if(propertyFile == null)
        {
            return null;
        }
        Log.fine("Property file found: {0}", propertyFile.getAbsolutePath());
        return new FileInputStream(propertyFile);
    }

    public static void main(final String[] args)
    {
        if(args != null && args.length > 0)
        {
            for(final String arg : args)
            {
                if(arg.startsWith("-h"))
                {
                    final StringBuilder sb = new StringBuilder(arg.substring(2));
                    if(!TimeTracker.home.endsWith("\\"))
                    {
                        sb.append("\\");
                    }
                    TimeTracker.home = sb.toString();
                    Log.info("Home = {0}", TimeTracker.home);
                }
            }
        }

        final Properties properties = getProperties();
        if (properties == null || properties.isEmpty())
        {
            Log.severe("Empty properties!");
            return;
        }

        try
        {
            final String lookAndFeel = properties.getProperty(PropertyConstants.LOOK_AND_FEEL, "javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.setLookAndFeel(lookAndFeel);
        }
        catch (final ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e)
        {
            Log.severe(e.getMessage(), e);
        }

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> Log.severe(e.getMessage(), e));

        final boolean callTimeTracker = handleEmptyToken(properties);
        if(callTimeTracker)
        {
            callTimeTracker(properties);
        }
    }

    private static void initClipboardObserver()
    {
        final ClipboardMonitor monitor = ClipboardMonitor.getMonitor();
        monitor.addObserver((o, arg) -> Log.info("Clipboard has been regained!"));
    }

    private static boolean handleEmptyToken(final Properties properties)
    {
        final String token = properties.getProperty(TimeTrackerConstants.YOUTRACK_TOKEN);
        if (token != null && !token.isEmpty())
        {
            return true;
        }

        final int frameWidth = 500;
        final int frameHeight = 100;
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        final JFrame dialog = new JFrame("Your token please");
        dialog.setMinimumSize(new Dimension(frameWidth, frameHeight));
        dialog.setBounds((screenSize.width / 2) - (frameWidth / 2), (screenSize.height / 2) - (frameHeight / 2), frameWidth, frameHeight);
        dialog.setResizable(false);
        dialog.setLayout(new GridLayout(2, 1));

        final JTextField tokenField = new JTextField(TimeTrackerConstants.YOUTRACK_TOKEN_PREFIX);
        tokenField.setCaretPosition(TimeTrackerConstants.YOUTRACK_TOKEN_PREFIX.length());
        tokenField.setBackground(MANDATORY);
        dialog.add(tokenField);

        final JButton button = new JButton();
        button.setAction(new TextAction(Resource.getString(PropertyConstants.TEXT_OK))
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

                if(!localToken.startsWith(TimeTrackerConstants.YOUTRACK_TOKEN_PREFIX))
                {
                    localToken = TimeTrackerConstants.YOUTRACK_TOKEN_PREFIX + localToken;
                }

                if(localToken.startsWith(TimeTrackerConstants.YOUTRACK_TOKEN_PREFIX + TimeTrackerConstants.YOUTRACK_TOKEN_PREFIX))
                {
                    localToken = localToken.substring(TimeTrackerConstants.YOUTRACK_TOKEN_PREFIX.length());
                }

                if(localToken.length() <= TimeTrackerConstants.YOUTRACK_TOKEN_PREFIX.length())
                {
                    return;
                }

                saveToken(localToken);

                properties.setProperty(TimeTrackerConstants.YOUTRACK_TOKEN, localToken);

                dialog.dispose();
                callTimeTracker(properties);
            }
        });
        dialog.add(button);

        dialog.pack();
        dialog.setVisible(true);
        return false;
    }

    /**
     * Startet den TimeTracker
     * @param properties alle Properties
     */
    private static void callTimeTracker(final Properties properties)
    {
        SwingUtilities.invokeLater(() -> {
            synchronized (syncObject)
            {
                try
                {
                    timeTracker = new TimeTracker(properties);
                    timeTracker.setVisible(true);
                    initClipboardObserver();
                }
                catch (final Exception e)
                {
                    System.err.println(Optional.ofNullable(e.getMessage()).orElse(e.getCause().getMessage()));
                    System.exit(0);
                }
            }
        });
    }

    public static TimeTracker getTimeTracker()
    {
        return timeTracker;
    }

    /**
     * Speichert den Authentifizierungstoken
     * @param token Token
     */
    private static void saveToken(final String token)
    {
        saveSetting(token, TimeTrackerConstants.YOUTRACK_TOKEN);
    }

    /**
     * Speichert einen Wert in den Properties
     * @param value Wert
     * @param key Schl�ssel
     */
    private static void saveSetting(final String value, final String key)
    {
        Log.info("Saving key = {0} with value =  {1}", new String[]{key, value});
        try (final InputStream inputStream = TimeTracker.class.getResourceAsStream(TimeTrackerConstants.PROPERTIES))
        {
            final Properties properties = new Properties();
            properties.load(inputStream);
            properties.remove(key);
            properties.put(key, value);
            storeProperties(properties);
        }
        catch (final IOException e)
        {
            Log.severe(e.getMessage(), e);
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
            Log.info("property key={0}, value={1}", new Object[]{entry.getKey(), entry.getValue()});
        }

        final File propertyFile = new File(TimeTracker.home + TimeTrackerConstants.PROPERTIES);

        //noinspection unchecked
        final Map<String, String> map = new TreeMap<>((Map) properties);
        OutputStream outputStream = null;
        try
        {
            outputStream = Files.newOutputStream(propertyFile.toPath(), StandardOpenOption.TRUNCATE_EXISTING);
            store(outputStream, map);
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
                catch (final IOException e)
                {
                    Log.severe(e.getMessage(), e);
                }
            }
        }
    }

    private static void store(final OutputStream out, final Map<String, String> map) throws IOException
    {
        store(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.ISO_8859_1)), map);
    }

    private static void store(final BufferedWriter bw, final Map<String, String> map) throws IOException
    {
        if(map.isEmpty())
        {
            return;
        }

        bw.write("#" + new Date().toString());

        for(final Map.Entry<String, String> entry : map.entrySet())
        {
            bw.newLine();
            bw.write(entry.getKey() + "=" + entry.getValue());
        }
        bw.flush();
    }
}