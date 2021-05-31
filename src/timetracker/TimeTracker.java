package timetracker;

import timetracker.actions.*;
import timetracker.client.Client;
import timetracker.icons.Icon;
import timetracker.log.Log;
import timetracker.menu.ContextMenu;
import timetracker.utils.ClipboardMonitor;
import timetracker.utils.TrayIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.TextAction;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;

/**
 * TimeTracking-Tool
 */
public class TimeTracker extends Frame
{
    private static final long serialVersionUID = 7225687129886672540L;

    public static final Matcher MATCHER = TimeTrackerConstants.PATTERN.matcher(TimeTrackerConstants.STRING_EMPTY);
    public static final Matcher TIME_MATCHER = TimeTrackerConstants.TIME_PATTERN.matcher(TimeTrackerConstants.STRING_EMPTY);
    public static final Matcher BURN_MATCHER = TimeTrackerConstants.BURN_PATTERN.matcher(TimeTrackerConstants.STRING_EMPTY);
    public static final Matcher DURATION_MATCHER = TimeTrackerConstants.DURATION_PATTERN.matcher(TimeTrackerConstants.STRING_EMPTY);

    public static final Color MANDATORY = new Color(200, 221, 242);
    public static final EmptyBorder BORDER = new EmptyBorder(5, 5, 5, 5);

    private static String home = TimeTrackerConstants.STRING_EMPTY;
    private static TimeTracker timeTracker;
    private static final Object syncObject = new Object();

    private int line;
    private JPanel panel;

    private TimeTracker()
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
    }

    private void init(final Properties properties) throws Exception
    {

        this.panel = new JPanel(new GridLayout(0, 1));
        Client.setScheme(properties.getProperty(TimeTrackerConstants.YOUTRACK_SCHEME, TimeTrackerConstants.DEFAULT_SCHEME));
        Client.setHost(properties.getProperty(TimeTrackerConstants.YOUTRACK_HOST, TimeTrackerConstants.DEFAULT_HOST));

        final String p = properties.getProperty(TimeTrackerConstants.YOUTRACK_PORT);
        Client.setPort(p == null || p.isEmpty() ? -1 : Integer.parseInt(p));
        Client.setToken(properties.getProperty(TimeTrackerConstants.YOUTRACK_TOKEN));

        try
        {
            Client.setUserID(properties);

            final JButton add = new JButton(Resource.getString(PropertyConstants.LABEL_ADD));
            BaseAction.setButtonIcon(add, timetracker.icons.Icon.ADD);
            add.setAction(new ShowAddButtonAction(add));

            final JButton reset = new JButton(Resource.getString(PropertyConstants.LABEL_STOP));
            BaseAction.setButtonIcon(reset, timetracker.icons.Icon.STOP);
            reset.setAction(new ResetAction(reset));

            final JButton openLog = getOpenLogButton();

            final JPanel globalActionsPanel = new JPanel(new GridLayout(1, 3));
            globalActionsPanel.add(add);
            globalActionsPanel.add(reset);
            globalActionsPanel.add(openLog);
            addToPanel(globalActionsPanel);
            increaseLine();
            addToPanel(new JPanel()); //Spacer
            increaseLine();

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
                increaseLine();
            }

            add(this.panel);
            pack();
            restoreWindowPositionAndSize();
        }
        catch (final Exception e)
        {
            final String msg = getMessage(e);
            Log.severe(msg, e);
            JOptionPane.showMessageDialog(this, msg);
            throw e;
        }
    }

    public JPanel getPanel()
    {
        return this.panel;
    }

    public static String getHome()
    {
        return home;
    }

    public static void setHome(final String home)
    {
        TimeTracker.home = home;
    }

    public void increaseLine()
    {
        this.line++;
    }

    public void decreaseLine()
    {
        this.line--;
    }

    public static String getMessage(final Throwable e)
    {
        if(e == null)
        {
            return "";
        }
        String msg = e.getMessage();
        if(msg == null || msg.isEmpty())
        {
            msg = getMessage(e.getCause());
        }
        if(msg.isEmpty())
        {
            msg = e.getClass().getSimpleName();
        }
        return msg;
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
        BaseAction.setButtonIcon(openLog, timetracker.icons.Icon.LOG);
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

    public void showFrame(final MenuItem openItem, final boolean show)
    {
        setVisible(show);
        openItem.setEnabled(!show);
    }

    public JButton addButton(final String key, final String label, final String icon)
    {
        if (label == null || label.isEmpty())
        {
            return null;
        }

        final int id = Integer.parseInt(key.substring(TimeTrackerConstants.PREFIX_BUTTON.length()));

        final JButton button = new JButton(label);
        BaseAction.setButtonIcon(button, icon);
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

        final BaseAction timerAction = new BaseAction(button, key, timeLabel);
        button.setAction(timerAction);

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setMaximumSize(new Dimension(0, 30));
        buttonPanel.setLayout(new BorderLayout(0, 0));
        buttonPanel.add(labelPanel, BorderLayout.CENTER);

        final JPanel actionsPanel = new JPanel();
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.X_AXIS));

        final JButton burnAction = addAction(actionsPanel, Resource.getString(PropertyConstants.TOOLTIP_BURN), timetracker.icons.Icon.BURN);
        burnAction.addActionListener(new BurnButtonAction(button, timeLabel, key));

        final JButton action = addAction(actionsPanel, Resource.getString(PropertyConstants.LABEL_FINISH), timetracker.icons.Icon.FINISH);
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
     * F?gt den Men?eintrag "In Bearbeitung nehmen" hinzu. Dabei wird gepr?ft, ob das Ticket nicht schon in Bearbeitung ist. Ausserdem ist diese Aktion
     * f?r die Standardaktionen nicht vorgesehen
     * @param menu Men?
     * @param button Issue-Button
     * @param id Id
     */
    public void addInProgressItem(final JPopupMenu menu, final JButton button, final int id)
    {
        try
        {
            final String issueState = id > 3 ? Client.getIssueState(button.getText()) : null;
            final JMenuItem inProgressItem = new JMenuItem(Resource.getString(PropertyConstants.LABEL_IN_PROGRESS));
            inProgressItem.setBorder(BORDER);
            inProgressItem.setEnabled(issueState != null && !TimeTrackerConstants.ISSUE_VALUE_STATE_PROGRESS.equalsIgnoreCase(issueState));
            BaseAction.setButtonIcon(inProgressItem, timetracker.icons.Icon.PROGRESS);
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
     * F?gt den Men?eintrag "Zur?cksetzen" hinzu
     * @param menu Men?
     * @param button Issue-Button
     */
    public void addRedoItem(final JPopupMenu menu, final JButton button)
    {
        final JMenuItem redoItem = new JMenuItem(Resource.getString(PropertyConstants.TOOLTIP_REDO));
        redoItem.setBorder(BORDER);
        BaseAction.setButtonIcon(redoItem, timetracker.icons.Icon.STOP);
        redoItem.addActionListener((final ActionEvent event) -> {
            final Action a = button.getAction();
            ((BaseAction) a).reset();
        });
        menu.add(redoItem);
    }

    public void addStarItem(final JPopupMenu menu, final JButton button, final String key, final int id)
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
        BaseAction.setButtonIcon(starItem, timetracker.icons.Icon.STAR);
        starItem.setEnabled(id > 3);
        menu.add(starItem);
    }

    private JButton addAction(final JPanel parent, final String tooltip, final Icon icon)
    {
        final JButton button = new JButton();
        button.setBorder(new EmptyBorder(8, 8, 8, 8));
        button.setToolTipText(tooltip);
        BaseAction.setButtonIcon(button, icon);
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

    public void updateGui(final boolean removeLine)
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

    /**
     * Liefert den Schl?ssel zum Speichern der Zeitdauer
     * @param key Schl?ssel f?r das Label
     * @return Schl?ssel f?r die Zeitdauer
     */
    private String getDurationKey(final String key)
    {
        return key + TimeTrackerConstants.SUFFIX_DURATION;
    }

    /**
     * Liefert das Ticket zu einem Button. Entweder, wenn es schon gespeichert ist oder geparst aus dem Titel
     * @param key Schl?ssel mit der Button-ID
     * @param button Button mit dem Titel des Tickets
     * @return Ticket-ID zum Button
     */
    public String getTicket(final String key, final JButton button)
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
     * L?dt eine Einstellung aus den Properties
     * @param key Schl?ssel (Label des Buttons)
     * @param setting Einstellung
     * @return Wert
     */
    public String loadSetting(final String key, final String setting)
    {
        final Properties properties = getProperties();
        return properties != null ? properties.getProperty(key + setting) : null;
    }

    public void setLabelTooltip(final String savedDuration, final JLabel timeLabel)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("<html>").append(Resource.getString(PropertyConstants.TIME_SAVED)).append(TimeTrackerConstants.STRING_SPACE).append(savedDuration);
        timeLabel.setToolTipText(sb.append("</html>").toString());
    }

    /**
     * Liefert die aktuelle Position der Anwendung
     * @return aktuelle Position
     */
    public Point getWindowLocation()
    {
        if(!isShowing())
        {
            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            final int x = screenSize.width / 2 - 125;
            final int y = screenSize.height / 2 - 85;
            return new Point(x, y);
        }
        final Point location = getLocationOnScreen();
        final int x = location.x + (getWidth() / 2) - 125;
        final int y = location.y + (getHeight() / 2) - 85;
        return new Point(x, y);
    }

    /**
     * F?gt an der ?bergebenen Komponente ein Escape-Event hinzu
     * @param window Fenster, welchem ein Escape-Event hinzuf?gt
     */
    public final void addEscapeEvent(final RootPaneContainer window)
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

    public static String getTicketSummary(final JTextField textInput)
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
        return Client.getTicketSummary(text);
    }

    public void storeButtonProperties(final Properties properties, final String propertyKey, final String text, final String filePath) throws IOException
    {
        properties.setProperty(propertyKey + TimeTrackerConstants.SUFFIX_LABEL, text);
        properties.setProperty(propertyKey + TimeTrackerConstants.SUFFIX_ICON, filePath);
        storeProperties(properties);
    }

    public void showAddIssueDialog(final String text)
    {
        if(!matches(text))
        {
            return;
        }
        final AddAction action = new AddAction(Resource.getString(PropertyConstants.TEXT_OK));
        action.handleConfirmationDialog(text, true, true);
    }

    public static boolean matches(final String text)
    {
        MATCHER.reset(text);
        return MATCHER.matches();
    }

    /**
     * Liefert einen kleinen R?ckfragedialog
     * @param width Breite des Dialogs
     * @return R?ckfragedialog
     */
    public JDialog getDialog(final int width)
    {
        final Point location = getWindowLocation();
        final JDialog dialog = new JDialog(this, Resource.getString(PropertyConstants.TEXT_CONFIRMATION), true);
        dialog.setBounds(location.x, location.y, width, 200);
        dialog.setResizable(false);
        dialog.getContentPane().setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
        return dialog;
    }

    /**
     * Liefet den ?bergeordneten JFrame
     * @param component Komponente, dessen ?bergeordneter Frame ermittelt werden soll
     * @return ?bergeordneter Frame
     */
    public Frame getParentFrame(final Container component)
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
     * Speichert die Fenstergr??e und -position
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
    public void collectComponents(final Container container, final Set<Component> components)
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
     * Stellt die Fenstergr??e und -position aus den Properties wieder her
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
    public static Properties getProperties()
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
     * L?dt Properties. Der Stream als R?ckgabe muss selbst geschlossen werden.
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
    public static InputStream getPropertiesInputStream(final String propertyFileName) throws FileNotFoundException
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
                    timeTracker = new TimeTracker();
                    timeTracker.init(properties);
                    timeTracker.setVisible(true);
                    TrayIcon.addTrayIcon();
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
     * @param key Schl?ssel
     */
    public static void saveSetting(final String value, final String key)
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
    public static void storeProperties(final Properties properties) throws IOException
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