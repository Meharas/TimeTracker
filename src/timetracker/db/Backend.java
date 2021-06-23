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

    public static final String CN_UPDATE_ID = "UPDATE_ID";

    public static final String TN_ISSUES = "ISSUES";
    public static final String TN_UPDATES = "UPDATES";
    private static final String TN_ISSUES_COLUMNS = String.format(" (%s VARCHAR(12), %s INTEGER, %s VARCHAR(12), %s NVARCHAR(255), %s VARCHAR (12), %s VARCHAR(16), %s VARCHAR(16), " +
                                                                  "%s VARCHAR(255), %s BOOLEAN, %s BOOLEAN, PRIMARY KEY (%s));",
                                                                  CN_ID, CN_ORDER, CN_ISSUE, CN_LABEL, CN_TYPE, CN_DURATION, CN_DURATION_SAVED, CN_ICON,
                                                                  CN_CAN_BE_FINISHED, CN_MARKED, CN_ID);
    private static final String TN_UPDATES_COLUMNS = String.format(" (%s INTEGER PRIMARY KEY);", CN_UPDATE_ID);

    private static final String RENAME_TABLE = "ALTER TABLE %s ALTER COLUMN %s RENAME TO %s;";
    private static final String ADD_COLUMN = "ALTER TABLE %s ADD %s %s;";

    private static final String QUERY_UPDATES = String.format("SELECT %s FROM %s;", CN_UPDATE_ID, TN_UPDATES);
    private static final String INSERT_UPDATE = String.format("INSERT INTO %s (%s) VALUES (%s);", TN_UPDATES, CN_UPDATE_ID, "%d");

    private static final String QUERY_STMT = String.format("SELECT * FROM %s WHERE %s = ", TN_ISSUES, CN_ID) + "'%s';";
    private static final String QUERY_ALL_STMT = String.format("SELECT * FROM %s ORDER BY %s;", TN_ISSUES, CN_ORDER);
    private static final String QUERY_FIRST_ISSUE = String.format("SELECT * FROM %s LIMIT 1;", TN_ISSUES);
    private static final String INSERT_STMT = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES ",
                                                            TN_ISSUES, CN_ID, CN_ORDER, CN_ISSUE, CN_LABEL, CN_TYPE, CN_DURATION, CN_DURATION_SAVED, CN_ICON,
                                                            CN_CAN_BE_FINISHED, CN_MARKED) +
                                              "('%s', %d, '%s', '%s', '%s', '%s', '%s', '%s', %s, %s);";
    private static final String DELETE_STMT = String.format("DELETE FROM %s WHERE %s = ", TN_ISSUES, CN_ID) + "'%s';";
    private static final String UPDATE_STMT = "UPDATE " + TN_ISSUES + " SET " + CN_ISSUE + UPDATE_VALUE + CN_ORDER + "=%d," + CN_LABEL + UPDATE_VALUE + CN_TYPE + UPDATE_VALUE +
                                              CN_DURATION + UPDATE_VALUE + CN_DURATION_SAVED + UPDATE_VALUE + CN_ICON + UPDATE_VALUE +
                                              CN_CAN_BE_FINISHED + UPDATE_VALUE + CN_MARKED + "=%s WHERE " + CN_ID + "='%s';";

    private static final String QUERY_MAX_ORDER = String.format("SELECT MAX(%s) FROM %s;", CN_ORDER, TN_ISSUES);

    private static final String COMMIT_STMT = "COMMIT;";
    private static final String ROLLBACK_STMT = "ROLLBACK WORK;";

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

        final String file = TimeTracker.HOME + Constants.FOLDER_USERDATA + "/timetrackerDB";
        try
        {
            Log.info("Connect to DB...");
            Log.info("Database file: " + file);
            this.conn = DriverManager.getConnection("jdbc:hsqldb:" + file + ";hsqldb.default_table_type=CACHED;hsqldb.cache_size=60000;hsqldb.log_size=5",
                                                    "sa", null);
            Log.info("Connected successfully to DB");

            try
            {
                this.statement = this.conn.createStatement();
            }
            catch (final SQLException e)
            {
                Log.severe("Could not create statement. Can not handle issues in DB!", e);
            }

            initTables();

            //Verhindert das automatische Committen. Es können sonst keine Updates zurückgerollt werden.
            executeUpdate("SET AUTOCOMMIT FALSE;", true);

            Runtime.getRuntime().addShutdownHook(new DBShutDownThread());
        }
        catch (final SQLException e)
        {
            Log.severe("Can't connect to " + file, e);
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
            boolean columnFound = false;
            try (final ResultSet resultSet = this.statement.executeQuery(QUERY_FIRST_ISSUE))
            {
                final ResultSetMetaData rsMetaData = resultSet.getMetaData();
                final int columnCount = rsMetaData.getColumnCount();
                while (resultSet.next())
                {
                    for(int i = 1; i <= columnCount; i++)
                    {
                        columnFound = CN_ORDER.equalsIgnoreCase(rsMetaData.getColumnName(i));
                        if(columnFound)
                        {
                            break;
                        }
                    }
                    if(columnFound)
                    {
                        break;
                    }
                }
            }
            if(!columnFound)
            {
                addColum(TN_ISSUES, CN_ORDER, "INTEGER");
            }
        }

        rs = metaData.getTables(null, null, TN_UPDATES, null);
        if (!rs.next())
        {
            insertInitialUpdates = initUpdateTable();
        }
        if(insertInitialUpdates)
        {
            Updates.insertInitialUpdates();
        }
    }

    /**
     * Erzeugt die Tabelle für die Issues und fügt die Standard-Issues ein
     */
    private boolean initIssueTable()
    {
        final StringBuilder sqlTable = new StringBuilder(STMT_CREATE_TABLE);
        sqlTable.append(TN_ISSUES);
        sqlTable.append(TN_ISSUES_COLUMNS);

        try (final Statement stmt = this.conn.createStatement())
        {
            stmt.executeUpdate(sqlTable.toString());
            Log.info("Table created: " + TN_ISSUES);

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
    private boolean initUpdateTable()
    {
        final StringBuilder sqlTable = new StringBuilder(STMT_CREATE_TABLE);
        sqlTable.append(TN_UPDATES);
        sqlTable.append(TN_UPDATES_COLUMNS);

        try (final Statement stmt = this.conn.createStatement())
        {
            stmt.executeUpdate(sqlTable.toString());
            Log.info("Table created: " + TN_UPDATES);
            return true;
        }
        catch (final Exception e)
        {
            Util.handleException(e);
        }
        return false;
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

        executeUpdate(String.format(INSERT_STMT, id, issue.getOrder(), ticket, issue.getLabel(), Optional.ofNullable(issue.getType()).map(WorkItemType::getId).orElse(null),
                                    issue.getDuration(), issue.getDurationSaved(), issue.getIcon(), issue.canBeFinished(), issue.isMarked()), true);
        issue.setId(id);
        Log.info("Issue inserted: " +  issue);
    }

    private void setOrder(final Issue issue) throws SQLException
    {
        int order = issue.getOrder();
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
     * @return Anzahl der betroffenen Datensätze
     * @throws SQLException database access error
     */
    private int executeUpdate(final String query, final boolean commit) throws SQLException
    {
        SQLException ex = null;
        int i = -1;
        try
        {
             i = this.statement.executeUpdate(query);
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
            final String sb = String.format("Could not execute statement for insert/delete in db-table: %s%n%s", TN_ISSUES, query);
            Log.severe(sb);
        }
        if(ex != null)
        {
            throw ex;
        }
        return i;
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
     * @param issueId Id des Issues
     * @param duration Zeitdauer
     * @throws Throwable database access error or other errors
     */
    public void saveCurrentDuration(final String issueId, final String duration) throws Throwable
    {
        final Issue issue = getIssue(issueId);
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
        executeUpdate(String.format(UPDATE_STMT, issue.getTicket(), issue.getOrder(), issue.getLabel(), issue.getType().getId(), issue.getDuration(), issue.getDurationSaved(),
                                    issue.getIcon(), issue.canBeFinished(), issue.isMarked(), issue.getId()), commit);
        Log.info("Issue updated: " +  issue);

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
        try (final ResultSet rs = this.statement.executeQuery(query))
        {
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