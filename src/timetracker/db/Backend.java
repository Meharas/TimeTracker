package timetracker.db;

import timetracker.Constants;
import timetracker.TimeTracker;
import timetracker.client.Client;
import timetracker.data.Issue;
import timetracker.data.Type;
import timetracker.error.BackendException;
import timetracker.error.ErrorCodes;
import timetracker.icons.Icon;
import timetracker.log.Log;
import timetracker.utils.Util;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

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
    public static final String CN_DELETABLE = "DELETABLE";
    public static final String CN_MARKED = "MARKED";
    public static final String CN_ICON = "ICON";
    public static final String CN_TYPE = "TYPE";

    private static final String TN_ISSUES = "ISSUES";
    private static final String TN_ISSUES_COLUMNS = String.format(" (%s VARCHAR(12), %s VARCHAR(12), %s NVARCHAR(255), %s VARCHAR (12), %s VARCHAR(16), %s VARCHAR(16), " +
                                                                  "%s VARCHAR(255), %s BOOLEAN, %s BOOLEAN, PRIMARY KEY (%s))",
                                                                  CN_ID, CN_ISSUE, CN_LABEL, CN_TYPE, CN_DURATION, CN_DURATION_SAVED, CN_ICON, CN_DELETABLE, CN_MARKED, CN_ID);

    private static final String QUERY_STMT = String.format("SELECT * FROM %s WHERE %s = ", TN_ISSUES, CN_ID) + "'%s'";
    private static final String QUERY_ALL_STMT = String.format("SELECT * FROM %s", TN_ISSUES);
    private static final String INSERT_STMT = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES ",
                                                            TN_ISSUES, CN_ID, CN_ISSUE, CN_LABEL, CN_TYPE, CN_DURATION, CN_DURATION_SAVED, CN_ICON, CN_DELETABLE, CN_MARKED) +
                                                            "('%s', '%s', '%s', '%s', '%s', '%s', '%s', %s, %s)";
    private static final String DELETE_STMT = String.format("DELETE FROM %s WHERE %s = ", TN_ISSUES, CN_ID) + "'%s'";
    private static final String UPDATE_STMT = "UPDATE " + TN_ISSUES + " SET " + CN_ISSUE + UPDATE_VALUE + CN_LABEL + UPDATE_VALUE + CN_TYPE + UPDATE_VALUE +
                                              CN_DURATION + UPDATE_VALUE + CN_DURATION_SAVED + UPDATE_VALUE + CN_ICON + UPDATE_VALUE +
                                              CN_DELETABLE + UPDATE_VALUE + CN_MARKED + "=%s WHERE " + CN_ID + "='%s'";

    
    // Innere private Klasse, die erst beim Zugriff durch die umgebende Klasse initialisiert wird
    private static final class InstanceHolder
    {
        // Die Initialisierung von Klassenvariablen geschieht nur einmal und wird vom ClassLoader implizit synchronisiert
        static final Backend instance = new Backend();
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

        try
        {
            Log.info("Connect to DB...");
            final String file = TimeTracker.home + "db/timetrackerDB";
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

            initTable();

            Runtime.getRuntime().addShutdownHook(new DBShutDownThread());
        }
        catch (final SQLException e)
        {
            Log.severe("Can't connect to TaskPrinter DB", e);
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
    private void initTable() throws SQLException
    {
        final ResultSet rs = this.conn.getMetaData().getTables(null, null, TN_ISSUES, null);
        if (rs.next())
        {
            return;
        }

        final StringBuilder sqlTable = new StringBuilder(STMT_CREATE_TABLE);
        sqlTable.append(TN_ISSUES);
        sqlTable.append(TN_ISSUES_COLUMNS);

        try (final Statement stmt = this.conn.createStatement())
        {
            stmt.executeUpdate(sqlTable.toString());
            Log.info("Table created: " + TN_ISSUES);

            insertIssue(new Issue("1", Constants.STRING_EMPTY, "Support", Type.SUPPORT, Constants.STRING_EMPTY, Constants.STRING_EMPTY, Icon.SUPPORT.getIcon(), false, false));
            insertIssue(new Issue("2", Constants.STRING_EMPTY, "Telefonat", Type.EMPTY, Constants.STRING_EMPTY, Constants.STRING_EMPTY, Icon.PHONE.getIcon(), false, false));
            insertIssue(new Issue("3", Constants.STRING_EMPTY, "Meeting", Type.MEETING, Constants.STRING_EMPTY, Constants.STRING_EMPTY, Icon.MEETING.getIcon(), false, false));
            insertIssue(new Issue("4", Constants.STRING_EMPTY, "Pause", Type.EMPTY, Constants.STRING_EMPTY, Constants.STRING_EMPTY, Icon.PAUSE.getIcon(), false, false));
        }
        catch (final Throwable t)
        {
            Util.handleException(t);
        }
    }

    /**
     * Fügt das übergebene Issue als Datensatz ein
     * @param issue Issue
     * @throws Throwable database access error or other errors
     */
    public void insertIssue(final Issue issue) throws Throwable
    {
        initTable();

        final String ticket = issue.getTicket();
        String id = issue.getId();
        if(id == null || id.isEmpty())
        {
            id = Client.getIssueID(ticket);
            issue.setId(id);
        }

        final Issue result = getIssue(id);
        if(result != null)
        {
            throw new BackendException(ErrorCodes.getString(ErrorCodes.ERROR_ISSUE_EXISTS));
        }

        executeUpdate(String.format(INSERT_STMT, id, ticket, issue.getLabel(), Optional.ofNullable(issue.getType()).map(Type::getId).orElse(null),
                                    issue.getDuration(), issue.getDurationSaved(), issue.getIcon(), issue.isDeletable(), issue.isMarked()));
        issue.setId(id);
        Log.info("Issue inserted: " +  issue);
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
        try
        {
            initTable();
        }
        catch (final SQLException e)
        {
            Log.severe("Could not create table", e);
        }
        executeUpdate(String.format(DELETE_STMT, issue.getId()));
        Log.info("Issue updated: " +  issue);
    }

    /**
     * Führt ein Update oder Delete aus
     * @param query Statement
     * @return Anzahl der betroffenen Datensätze
     * @throws SQLException database access error
     */
    private int executeUpdate(final String query) throws SQLException
    {
        SQLException ex = null;
        int i = -1;
        try
        {
             i = this.statement.executeUpdate(query);
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
        executeUpdate(String.format(UPDATE_STMT, issue.getTicket(), issue.getLabel(), Optional.ofNullable(issue.getType()).map(Type::getId).orElse(Constants.STRING_EMPTY),
                                    issue.getDuration(), issue.getDurationSaved(), issue.getIcon(), issue.isDeletable(), issue.isMarked(), issue.getId()));
        Log.info("Issue updated: " +  issue);
    }

    /**
     * Liefert Issues als Ergebnis einer Select-Anfrage
     * @param query Select-Anfrage
     * @return Liste von Issues passend zur Anfrage
     * @throws Throwable database access error or other errors
     */
    private List<Issue> executeSelect(final String query) throws Throwable
    {
        ResultSet rs = null;
        try
        {
            rs = this.statement.executeQuery(query);
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
        finally
        {
            close(rs);
        }
    }

    /**
     * Gibt die Ressource wieder frei
     * @param rs ResultSet
     */
    private void close(final ResultSet rs)
    {
        if (rs == null)
        {
            return;
        }
        try
        {
            rs.close();
        }
        catch (final SQLException e)
        {
            Log.log(Level.FINE, String.format("Could not close resultset: %s", rs.toString()), e);
        }
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