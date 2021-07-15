package timetracker.db;

import timetracker.Constants;
import timetracker.TimeTracker;
import timetracker.buttons.IssueButton;
import timetracker.client.Client;
import timetracker.data.Issue;
import timetracker.data.WorkItemType;
import timetracker.error.BackendException;
import timetracker.error.ErrorCodes;
import timetracker.icons.Icon;
import timetracker.log.Log;
import timetracker.updates.IUpdateMethod;
import timetracker.updates.Updates;
import timetracker.utils.Util;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Dient dem Zugriff auf die Datenbank des TimeTrackers
 */
public class Backend
{
    private static final String FILE = TimeTracker.HOME + Constants.FOLDER_USERDATA + "/timetrackerDB";
    private static final String CONNECT_URL = "jdbc:hsqldb:" + FILE + ";hsqldb.default_table_type=CACHED;hsqldb.cache_size=60000;hsqldb.log_size=5";

    private static final String DB_DRIVER = "org.hsqldb.jdbcDriver";
    private static final String DB_SHUTDOWN = "SHUTDOWN";
    private static final String UPDATE_VALUE = "='%s',";

    private static final String STMT_CREATE_TABLE = "CREATE CACHED TABLE ";

    public static final String CN_ID = "ID";
    public static final String CN_ISSUE = "ISSUE";
    public static final String CN_LABEL = "LABEL";
    public static final String CN_DURATION = "DURATION";
    public static final String CN_DURATION_SAVED = "DURATION_SAVED";
    @Deprecated
    public static final String CN_DELETABLE = "DELETABLE";
    public static final String CN_CAN_BE_FINISHED = "CAN_BE_FINISHED";
    public static final String CN_MARKED = "MARKED";
    public static final String CN_ICON = "ICON";
    public static final String CN_TYPE = "TYPE";
    public static final String CN_ORDER = "ORDER_ID";
    public static final String CN_DESCRIPTION = "DESCRIPTION";

    public static final String CN_UPDATE_ID = "UPDATE_ID";

    public static final String CN_SETTING_NAME = "SETTING_NAME";
    public static final String CN_SETTING_VALUE = "SETTING_VALUE";

    public static final String TN_ISSUES = "ISSUES";
    public static final String TN_UPDATES = "UPDATES";
    public static final String TN_SETTNGS = "SETTINGS";
    private static final String TN_ISSUES_COLUMNS = String.format(" (%s VARCHAR(12), %s INTEGER, %s VARCHAR(12), %s NVARCHAR(255),  %s NVARCHAR(255), %s VARCHAR (12), " +
                                                                  "%s VARCHAR(16), %s VARCHAR(16), %s VARCHAR(255), %s BOOLEAN, %s BOOLEAN, PRIMARY KEY (%s));",
                                                                  CN_ID, CN_ORDER, CN_ISSUE, CN_LABEL, CN_DESCRIPTION, CN_TYPE, CN_DURATION, CN_DURATION_SAVED,
                                                                  CN_ICON, CN_CAN_BE_FINISHED, CN_MARKED, CN_ID);
    private static final String TN_UPDATES_COLUMNS = String.format(" (%s INTEGER PRIMARY KEY);", CN_UPDATE_ID);
    private static final String TN_SETTINGS_COLUMNS = String.format(" (%s VARCHAR(32) PRIMARY KEY, %s VARCHAR(32));", CN_SETTING_NAME, CN_SETTING_VALUE);

    private static final String RENAME_TABLE = "ALTER TABLE %s ALTER COLUMN %s RENAME TO %s;";
    private static final String ADD_COLUMN = "ALTER TABLE %s ADD %s %s;";

    private static final String QUERY_UPDATES = String.format("SELECT %s FROM %s;", CN_UPDATE_ID, TN_UPDATES);
    private static final String INSERT_UPDATE = String.format("INSERT INTO %s (%s) VALUES (%s);", TN_UPDATES, CN_UPDATE_ID, "%d");

    private static final String QUERY_STMT = String.format("SELECT * FROM %s WHERE %s = ", TN_ISSUES, CN_ID) + "'%s';";
    private static final String QUERY_ALL_STMT = String.format("SELECT * FROM %s ORDER BY %s;", TN_ISSUES, CN_ORDER);
    private static final String QUERY_FIRST_ISSUE = String.format("SELECT * FROM %s LIMIT 1;", TN_ISSUES);
    private static final String INSERT_STMT = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES ",
                                                            TN_ISSUES, CN_ID, CN_ORDER, CN_ISSUE, CN_LABEL, CN_DESCRIPTION, CN_TYPE, CN_DURATION, CN_DURATION_SAVED,
                                                            CN_ICON, CN_CAN_BE_FINISHED, CN_MARKED) +
                                              "('%s', %d, '%s', '%s', '%s', '%s', '%s', '%s', '%s', %s, %s);";
    private static final String DELETE_STMT = String.format("DELETE FROM %s WHERE %s = ", TN_ISSUES, CN_ID) + "'%s';";
    private static final String UPDATE_STMT = "UPDATE " + TN_ISSUES + " SET " + CN_ISSUE + UPDATE_VALUE + CN_ORDER + "=%d," + CN_LABEL + UPDATE_VALUE +
                                              CN_DESCRIPTION + UPDATE_VALUE + CN_TYPE + UPDATE_VALUE +
                                              CN_DURATION + UPDATE_VALUE + CN_DURATION_SAVED + UPDATE_VALUE + CN_ICON + UPDATE_VALUE +
                                              CN_CAN_BE_FINISHED + UPDATE_VALUE + CN_MARKED + "=%s WHERE " + CN_ID + "='%s';";

    private static final String QUERY_MAX_ORDER = String.format("SELECT MAX(%s) FROM %s;", CN_ORDER, TN_ISSUES);

    private static final String QUERY_SETTINGS = String.format("SELECT * FROM %s ORDER BY %s;", TN_SETTNGS, CN_SETTING_NAME);
    private static final String INSERT_SETTINGS = String.format("INSERT INTO %s (%s, %s) VALUES", TN_SETTNGS, CN_SETTING_NAME, CN_SETTING_VALUE) + " ('%s', '%s');";
    private static final String UPDATE_SETTINGS = String.format("UPDATE %s SET %s=%s WHERE %s=%s", TN_SETTNGS, CN_SETTING_VALUE, "'%s'", CN_SETTING_NAME, "'%s'");

    private static final String COMMIT_STMT = "COMMIT;";
    private static final String ROLLBACK_STMT = "ROLLBACK WORK;";

    public static final String SETTING_ALWAYS_ON_TOP = "ALWAYS_ON_TOP";
    public static final String SETTING_START_MINIMIZED = "START_MINIMIZED";
    public static final String SETTING_DEFAULT_WORKTYPE = "DEFAULT_WORKTYPE";
    public static final String SETTING_LOG_LEVEL = "LOG_LEVEL";
    public static final String SETTING_ENABLE_DRAGNDROP = "ENABLE_DRAGNDROP";

    // Innere private Klasse, die erst beim Zugriff durch die umgebende Klasse initialisiert wird
    private static final class InstanceHolder
    {
        // Die Initialisierung von Klassenvariablen geschieht nur einmal und wird vom ClassLoader implizit synchronisiert
        private static final Backend instance = new Backend();
        static
        {
            Updates.getInstance().executeUpdates();
        }
    }

    public static Backend getInstance()
    {
        return InstanceHolder.instance;
    }

    /** Verbindung zur DB */
    private Connection conn;
    /** Insert-/Delete-/Select-Statement oft gebraucht */
    private Statement statement;

    private Backend()
    {
        try
        {
            Log.info("Load JDBC driver");
            Class.forName(DB_DRIVER);
        }
        catch (final ClassNotFoundException e)
        {
            Log.severe("Can't Load JDBC-Driver.", e);
            System.exit(1);
        }

        String reason = "Can't connect to " + FILE;
        try
        {
            Log.info("Connect to DB...");
            Log.info("Database file: " + FILE);
            this.conn = DriverManager.getConnection(CONNECT_URL, "sa", null);
            Log.info("Connected successfully to DB");

            reason = "Failed adding shutdown hook";
            Runtime.getRuntime().addShutdownHook(new DBShutDownThread());

            reason = "Could not create statement. Can not handle issues in DB!";
            this.statement = this.conn.createStatement();

            reason = "Initialising tables";
            initTables();

            //Verhindert das automatische Committen. Es können sonst keine Updates zurückgerollt werden.
            reason = "Setting auto commit to false";
            executeUpdate("SET AUTOCOMMIT FALSE;", true);
        }
        catch (final SQLException e)
        {
            Log.severe(reason, e);
            System.exit(1);
        }
    }

    /**
     * Faehrt die DB ordnungsgemaess herunter
     */
    protected void shutdown()
    {
        try
        {
            if (this.statement != null)
            {
                this.statement.close();
            }
        }
        catch (final SQLException e)
        {
            Log.log(Level.FINE, "Could not close statement", e);
        }

        //Sauberer Shutdown
        Statement st = null;
        try
        {
            if (this.conn != null)
            {
                st = this.conn.createStatement();
                st.execute(DB_SHUTDOWN);
                Log.info("Shutdown database successfully");
            }
        }
        catch (final SQLException e)
        {
            Log.warning("Could not execute SHUTDOWN", e);
        }
        finally
        {
            close(st);
        }

        try
        {
            if (this.conn != null)
            {
                this.conn.close();
            }
        }
        catch (final SQLException e)
        {
            Log.warning("Could not close db-connection", e);
        }
    }

    /**
     * Generiert eine Tabelle für die Issues
     * @throws SQLException Wenn Tabelle schon vorhanden ist
     */
    private void initTables() throws SQLException
    {
        boolean insertInitialUpdates = false;
        final DatabaseMetaData metaData = this.conn.getMetaData();
        ResultSet rs = metaData.getTables(null, null, TN_ISSUES, null);
        if (!rs.next())
        {
            insertInitialUpdates = initIssueTable();
        }
        else
        {
            boolean orderColumnFound = false;
            boolean descColumnFound = false;
            try (final ResultSet resultSet = this.statement.executeQuery(QUERY_FIRST_ISSUE))
            {
                final ResultSetMetaData rsMetaData = resultSet.getMetaData();
                final int columnCount = rsMetaData.getColumnCount();
                while (resultSet.next())
                {
                    for(int i = 1; i <= columnCount; i++)
                    {
                        if(CN_ORDER.equalsIgnoreCase(rsMetaData.getColumnName(i)))
                        {
                            orderColumnFound = true;
                        }
                        else if(CN_DESCRIPTION.equalsIgnoreCase(rsMetaData.getColumnName(i)))
                        {
                            descColumnFound = true;
                        }
                    }
                    if(orderColumnFound && descColumnFound)
                    {
                        break;
                    }
                }
            }
            if(!orderColumnFound)
            {
                addColum(TN_ISSUES, CN_ORDER, "INTEGER");
            }
            if(!descColumnFound)
            {
                addColum(TN_ISSUES, CN_DESCRIPTION, "NVARCHAR(255)");
            }
        }

        rs = metaData.getTables(null, null, TN_UPDATES, null);
        if (!rs.next())
        {
            initUpdateTable();
        }
        if(insertInitialUpdates)
        {
            Updates.insertInitialUpdates(this);
        }

        rs = metaData.getTables(null, null, TN_SETTNGS, null);
        if(!rs.next())
        {
            initSettingsTable();
        }
    }

    /**
     * Erzeugt die Tabelle für die Issues und fügt die Standard-Issues ein
     */
    private boolean initIssueTable()
    {
        try
        {
            initTable(TN_ISSUES, TN_ISSUES_COLUMNS);

            insertIssue(new Issue(Constants.STRING_EMPTY, "Support", WorkItemType.EMPTY, Constants.STRING_EMPTY, Constants.STRING_EMPTY, Icon.SUPPORT.getIcon()));
            insertIssue(new Issue(Constants.STRING_EMPTY, "Telefonat", WorkItemType.EMPTY, Constants.STRING_EMPTY, Constants.STRING_EMPTY, Icon.PHONE.getIcon()));
            insertIssue(new Issue(Constants.STRING_EMPTY, "Meeting", WorkItemType.EMPTY, Constants.STRING_EMPTY, Constants.STRING_EMPTY, Icon.MEETING.getIcon()));
            insertIssue(new Issue(Constants.STRING_EMPTY, "Pause", WorkItemType.EMPTY, Constants.STRING_EMPTY, Constants.STRING_EMPTY, Icon.PAUSE.getIcon()));
            return true;
        }
        catch (final Throwable t)
        {
            Util.handleException(t);
        }
        return false;
    }

    /**
     * Erzeugt die Tabelle für die UpdateIds
     */
    private void initUpdateTable()
    {
        initTable(TN_UPDATES, TN_UPDATES_COLUMNS);
    }

    /**
     * Erzeugt die Tabelle für die Einstellungen
     */
    private void initSettingsTable()
    {
        initTable(TN_SETTNGS, TN_SETTINGS_COLUMNS);

        try
        {
            executeUpdate(String.format(INSERT_SETTINGS, SETTING_ALWAYS_ON_TOP, true), false);
            executeUpdate(String.format(INSERT_SETTINGS, SETTING_START_MINIMIZED, false), false);
            executeUpdate(String.format(INSERT_SETTINGS, SETTING_DEFAULT_WORKTYPE, Constants.STRING_EMPTY), false);
            executeUpdate(String.format(INSERT_SETTINGS, SETTING_LOG_LEVEL, Level.INFO), false);
            executeUpdate(String.format(INSERT_SETTINGS, SETTING_ENABLE_DRAGNDROP, true), false);
            commit();
        }
        catch (final SQLException e)
        {
            Util.handleException(e);
        }
    }

    private void initTable(final String tableName, final String tableColumns)
    {
        final StringBuilder sqlTable = new StringBuilder(STMT_CREATE_TABLE);
        sqlTable.append(tableName);
        sqlTable.append(tableColumns);

        try
        {
            this.statement.executeUpdate(sqlTable.toString());
            Log.info("Table created: " + tableName);
        }
        catch (final Exception e)
        {
            Util.handleException(e);
        }
    }

    /**
     * Fügt das übergebene Issue als Datensatz ein
     * @param issue Issue
     * @throws Throwable database access error or other errors
     */
    public void insertIssue(final Issue issue) throws Throwable
    {
        final String ticket = issue.getTicket();
        String id = Client.getIssueID(ticket);
        //Wenn eine Id ermittelt werden konnte, dann kann das Ticket auch abgeschlossen werden. Ansonsten ist es was eigenes ohne Entsprechung im Youtrack
        issue.setCanBeFinished(id != null);

        if(id == null)
        {
            id = UUID.randomUUID().toString();
            id = id.substring(id.lastIndexOf("-") + 1); //Die letzten Zeichen sind die max. zulässige Anzahl Zeichen der Spalte
        }
        issue.setId(id);

        final Issue result = getIssue(id);
        if(result != null)
        {
            result.setMarked(true);
            updateIssue(result);
            throw new BackendException(ErrorCodes.getString(ErrorCodes.ERROR_ISSUE_EXISTS));
        }

        setOrder(issue);

        executeUpdate(String.format(INSERT_STMT, id, issue.getOrder(), ticket, issue.getLabel(), issue.getDescription(), issue.getType().getId(),
                                    issue.getDuration(), issue.getDurationSaved(), issue.getIcon(), issue.canBeFinished(), issue.isMarked()), true);
        issue.setId(id);
        Log.info("Issue inserted: " +  issue);
    }

    private void setOrder(final Issue issue) throws SQLException
    {
        final int order = issue.getOrder();
        if(order == -1)
        {
            getNewOrder(issue);
        }
    }

    private void getNewOrder(final Issue issue) throws SQLException
    {
        try (final ResultSet rs = this.statement.executeQuery(QUERY_MAX_ORDER))
        {
            int order = 0;
            if(rs.next())
            {
                order = rs.getInt(1) + 1;
            }
            issue.setOrder(order);
        }
    }

    /**
     * Löscht die Issues zu einem Benutzer
     * @param issue Ticket
     * @throws Throwable database access error or other errors
     */
    public void deleteIssue(final Issue issue) throws Throwable
    {
        if(issue == null)
        {
            Log.severe("Issue is null or empty");
            return;
        }
        executeUpdate(String.format(DELETE_STMT, issue.getId()), false);
        Log.info("Issue updated: " +  issue);

        final int currentOrder = issue.getOrder();
        final List<Issue> issues = getIssues().stream().filter(iss -> iss.getOrder() > currentOrder).collect(Collectors.toList());
        for(final Issue iss : issues)
        {
            iss.setOrder(iss.getOrder() - 1);
            updateIssue(iss, false);
        }
    }

    /**
     * Führt ein Update oder Delete aus
     * @param query Statement
     * @param commit Apply changes
     * @return Anzahl geänderter Datensätze
     * @throws SQLException database access error
     */
    private int executeUpdate(final String query, final boolean commit) throws SQLException
    {
        Log.finest("Executing " + query);

        SQLException ex = null;
        int i = -1;
        try
        {
             final long start = System.currentTimeMillis();
             i = this.statement.executeUpdate(query);
             Util.logDuration(start);
             if(commit)
             {
                 commit();
             }
        }
        catch (final SQLException e)
        {
            Log.severe("Could not execute query: " + query, e);
            ex = e;
        }
        if (i == -1)
        {
            final String sb = String.format("Could not execute statement for insert/update/delete: %n%s", query);
            Log.severe(sb);
        }
        if(ex != null)
        {
            throw ex;
        }
        return i;
    }

    public Map<String, String> getSettings()
    {
        try
        {
            try (final ResultSet rs = this.statement.executeQuery(QUERY_SETTINGS))
            {
                final Map<String, String> result = new LinkedHashMap<>();
                while (rs.next())
                {
                    result.put(rs.getString(1), rs.getString(2));
                }
                return result;
            }
        }
        catch (final SQLException t)
        {
            Util.handleException(t);
        }
        return Collections.emptyMap();
    }

    /**
     * Specichert die Einstellungen
     * @param settings Einstellungen
     * @throws SQLException Wenn beim Speichern der Einstellungen ein Fehler aufgetreten ist.
     */
    public void saveSettings(final Map<String, String> settings) throws SQLException
    {
        if(settings != null && !settings.isEmpty())
        {
            for(final Map.Entry<String, String> setting : settings.entrySet())
            {
                final int updated = executeUpdate(String.format(UPDATE_SETTINGS, setting.getValue(), setting.getKey()), true);
                if(updated < 1)
                {
                    executeUpdate(String.format(INSERT_SETTINGS, setting.getKey(), setting.getValue()), true);
                }
            }
            TimeTracker.getInstance().setAlwaysOnTop(Boolean.parseBoolean(settings.get(Backend.SETTING_ALWAYS_ON_TOP)));
            Log.setLevel(Level.parse(settings.get(Backend.SETTING_LOG_LEVEL)));
        }
    }

    /**
     * Liefert die Issues
     * @return Liste mit Issues
     * @throws Throwable database access error or other errors
     */
    public List<Issue> getIssues() throws Throwable
    {
        return executeSelect(QUERY_ALL_STMT);
    }

    /**
     * Liefert das Issue zur übergebenen Id
     * @param id Id des Issues
     * @return Issue oder {@code null}
     * @throws Throwable database access error or other errors
     */
    public Issue getIssue(final String id) throws Throwable
    {
        final List<Issue> issues = executeSelect(String.format(QUERY_STMT, id));
        return !issues.isEmpty() ? issues.iterator().next() : null;
    }

    /**
     * Setzt die Zeitdauer zurück
     * @param issue Issue
     * @throws Throwable database access error or other errors
     */
    public void removeDuration(final Issue issue) throws Throwable
    {
        issue.setDuration(Constants.STRING_EMPTY);
        updateIssue(issue);
    }

    /**
     * Speichert die Zeitdauer für ein bestimmtes Issue
     * @param issue Issue
     * @param duration Zeitdauer
     * @throws Throwable database access error or other errors
     */
    public void saveCurrentDuration(final Issue issue, final String duration) throws Throwable
    {
        issue.setDuration(duration);
        updateIssue(issue);
    }

    /**
     * Aktualisiert das übergebene Issue in der Datenbank
     * @param issue Issue
     * @throws Throwable database access error or other errors
     */
    public void updateIssue(final Issue issue) throws Throwable
    {
        updateIssue(issue, true);
    }

    /**
     * Aktualisiert das übergebene Issue in der Datenbank
     * @param issue Issue
     * @throws Throwable database access error or other errors
     */
    public void updateIssue(final Issue issue, final boolean commit) throws Throwable
    {
        setOrder(issue);
        executeUpdate(String.format(UPDATE_STMT, issue.getTicket(), issue.getOrder(), issue.getLabel(), issue.getDescription(), issue.getType().getId(),
                                    issue.getDuration(), issue.getDurationSaved(), issue.getIcon(), issue.canBeFinished(), issue.isMarked(), issue.getId()), commit);
        Log.log(Level.FINE, "Issue updated: " +  issue);

        final IssueButton button = Util.getButton(issue);
        Optional.ofNullable(button).ifPresent(btn -> btn.refresh(issue));
    }

    /**
     * Liefert Issues als Ergebnis einer Select-Anfrage
     * @param query Select-Anfrage
     * @return Liste von Issues passend zur Anfrage
     * @throws Throwable database access error or other errors
     */
    private List<Issue> executeSelect(final String query) throws Throwable
    {
        Log.finest("Executing query " + query);
        final long start = System.currentTimeMillis();
        try (final ResultSet rs = this.statement.executeQuery(query))
        {
            Util.logDuration("Query", start);

            final ResultSetMetaData metaData = rs.getMetaData();
            final int columnCount = metaData.getColumnCount();
            final List<Issue> issues = new ArrayList<>();
            while (rs.next())
            {
                final Map<String, String> record = new HashMap<>(columnCount);
                for(int i = 1; i <= columnCount; i++)
                {
                    record.put(metaData.getColumnName(i), rs.getString(i));
                }
                issues.add(new Issue(record));
            }
            Log.finest(String.format("%d issue(s) found", issues.size()));
            return issues;
        }
    }

    public void insertUpdate(final IUpdateMethod update) throws SQLException
    {
        executeUpdate(String.format(INSERT_UPDATE, update.getUpdateId()), false);
    }

    public void commit() throws SQLException
    {
        this.statement.executeUpdate(COMMIT_STMT);
    }

    public void rollback() throws SQLException
    {
        this.statement.executeUpdate(ROLLBACK_STMT);
    }

    public Set<Integer> getUpdateIds()
    {
        try
        {
            try (final ResultSet rs = this.statement.executeQuery(QUERY_UPDATES))
            {
                final Set<Integer> updateIds = new HashSet<>();
                while (rs.next())
                {
                    updateIds.add(rs.getInt(1));
                }
                return updateIds;
            }
        }
        catch (final SQLException t)
        {
            Util.handleException(t);
        }
        return Collections.emptySet();
    }

    public void renameColumn(final String tableName, final String oldColumnName, final String newColumnName) throws SQLException
    {
        this.statement.executeUpdate(String.format(RENAME_TABLE, tableName, oldColumnName, newColumnName));
    }

    public void addColum(final String tableName, final String columnName, final String columnType) throws SQLException
    {
        this.statement.executeUpdate(String.format(ADD_COLUMN, tableName, columnName, columnType));
    }

    /**
     * Gibt die Ressource wieder frei
     * @param st Statement
     */
    private void close(final Statement st)
    {
        if (st == null)
        {
            return;
        }
        try
        {
            st.close();
        }
        catch (final SQLException e)
        {
            Log.log(Level.FINE, String.format("Could not close statement: %s", st.toString()), e);
        }
    }

    /**
     * Dieser Thread wird als ein  VM shutdown-hook registriert, um sicherzustellen, dass die DB ordnungsgemaess
     * beendet wird.
     */
    private static class DBShutDownThread extends Thread
    {
        private DBShutDownThread()
        {
            super();
        }

        @Override
        public void run()
        {
            Backend.getInstance().shutdown();
        }
    }
}