package timetracker.db;

import timetracker.Constants;
import timetracker.data.Issue;
import timetracker.data.Type;
import timetracker.icons.Icon;
import timetracker.log.Log;

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
    private static final String TN_ISSUES_COLUMNS = String.format(" (%s INTEGER, %s VARCHAR(12), %s NVARCHAR(255), %s VARCHAR (12), %s VARCHAR(16), %s VARCHAR(16), " +
                                                                  "%s VARCHAR(255), %s BOOLEAN, %s BOOLEAN, PRIMARY KEY (%s))",
                                                                  CN_ID, CN_ISSUE, CN_LABEL, CN_TYPE, CN_DURATION, CN_DURATION_SAVED, CN_ICON, CN_DELETABLE, CN_MARKED, CN_ID);

    private static final String QUERY_STMT = String.format("SELECT * FROM %s WHERE ID = ", TN_ISSUES) + "%s";
    private static final String QUERY_ALL_STMT = String.format("SELECT * FROM %s", TN_ISSUES);
    private static final String INSERT_STMT = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES ",
                                                            TN_ISSUES, CN_ID, CN_ISSUE, CN_LABEL, CN_TYPE, CN_DURATION, CN_DURATION_SAVED, CN_ICON, CN_DELETABLE, CN_MARKED) +
                                                            "(%d, '%s', '%s', '%s', '%s', '%s', '%s', %s, %s)";
    private static final String DELETE_STMT = String.format("DELETE FROM %s WHERE %s = ", TN_ISSUES, CN_ID) + "'%s'";
    private static final String MAX_ID_STMT = String.format("SELECT MAX(%s) FROM %s", CN_ID, TN_ISSUES);
    private static final String UPDATE_STMT = "UPDATE " + TN_ISSUES + " SET " + CN_ISSUE + UPDATE_VALUE + CN_LABEL + UPDATE_VALUE + CN_TYPE + UPDATE_VALUE +
                                              CN_DURATION + UPDATE_VALUE + CN_DURATION_SAVED + UPDATE_VALUE + CN_ICON + UPDATE_VALUE +
                                              CN_DELETABLE + UPDATE_VALUE + CN_MARKED + "=%s WHERE " + CN_ID + "=%d";

    
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
            this.conn = DriverManager.getConnection("jdbc:hsqldb:db/timetrackerDB;hsqldb.default_table_type=CACHED;hsqldb.cache_size=60000;hsqldb.log_size=5",
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
     * Generiert eine Tabelle mit dem NAmen
     * @return {@code true}, wenn die Tabelle schon existierte, {@code false} sonst.
     * @throws SQLException Wenn Tabelle schon vorhanden ist
     */
    public boolean initTable() throws Throwable
    {
        final ResultSet rs = this.conn.getMetaData().getTables(null, null, TN_ISSUES, null);
        if (rs.next())
        {
            return true;
        }

        //noinspection StringBufferReplaceableByString
        final StringBuilder sqlTable = new StringBuilder(STMT_CREATE_TABLE);
        sqlTable.append(TN_ISSUES);
        sqlTable.append(TN_ISSUES_COLUMNS);

        final Statement stmt = this.conn.createStatement();
        stmt.executeUpdate(sqlTable.toString());
        Log.info("Table created: " + TN_ISSUES);

        insertIssue(new Issue(Constants.STRING_EMPTY, "Support", Type.SUPPORT, Constants.STRING_EMPTY, Constants.STRING_EMPTY, Icon.SUPPORT.getIcon(), false, false));
        insertIssue(new Issue(Constants.STRING_EMPTY, "Telefonat", Type.EMPTY, Constants.STRING_EMPTY, Constants.STRING_EMPTY, "\\icons\\phone.png", false, false));
        insertIssue(new Issue(Constants.STRING_EMPTY, "Meeting", Type.MEETING, Constants.STRING_EMPTY, Constants.STRING_EMPTY, Icon.MEETING.getIcon(), false, false));
        insertIssue(new Issue(Constants.STRING_EMPTY, "Pause", Type.EMPTY, Constants.STRING_EMPTY, Constants.STRING_EMPTY, "\\icons\\pause.png", false, false));

        return false;
    }

    /**
     * Fügt das übergebene Issue als Datensatz ein
     * @param issue Issue
     * @throws Throwable database access error or other errors
     */
    public void insertIssue(final Issue issue) throws Throwable
    {
        initTable();
        final int id = getId();
        executeUpdate(String.format(INSERT_STMT, id, issue.getTicket(), issue.getLabel(), Optional.ofNullable(issue.getType()).map(Type::getId).orElse(null),
                                    issue.getDuration(), issue.getDurationSaved(), issue.getIcon(), issue.isDeletable(), issue.isMarked()));
        issue.setId(id);
    }

    private int getId() throws Throwable
    {
        final int max = getMaxId();
        return max + 1;
    }

    /**
     * Liefert das Maximum über die Id
     * @return Maximum über die Id
     * @throws Throwable database access error or other errors
     */
    private int getMaxId() throws Throwable
    {
        ResultSet rs = null;
        try
        {
            rs = this.statement.executeQuery(MAX_ID_STMT);
            if (rs.next())
            {
                return rs.getInt(1);
            }
        }
        catch (final Throwable t)
        {
            Log.warning("Could not execute query: " + MAX_ID_STMT, t);
            throw t;
        }
        finally
        {
            close(rs);
        }
        return 0;
    }

    /**
     * Löscht die Issues zu einem Benutzer
     * @param issue Ticket
     * @return Anzahl gelöschter Datensätze
     * @throws Throwable database access error or other errors
     */
    public int deleteIssue(final Issue issue) throws Throwable
    {
        if(issue == null)
        {
            Log.severe("Issue is null or empty");
            return -1;
        }
        try
        {
            initTable();
        }
        catch (final SQLException e)
        {
            Log.severe("Could not create table", e);
        }
        return executeUpdate(String.format(DELETE_STMT, issue.getId()));
    }

    /**
     * Führt ein Update oder Delete aus
     * @param query Statement
     * @return Anzahl der betroffenen Datensätze
     * @throws Throwable database access error or other errors
     */
    private int executeUpdate(final String query) throws Throwable
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
        if(!initTable())
        {
            return Collections.emptyList();
        }
        return executeSelect(QUERY_ALL_STMT);
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
    public void saveDuration(final String issueId, final String duration) throws Throwable
    {
        final List<Issue> issues = executeSelect(String.format(QUERY_STMT, issueId));
        final Issue issue = issues.iterator().next();
        issue.setDurationSaved(duration);
        issue.setDuration(Constants.STRING_EMPTY);
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