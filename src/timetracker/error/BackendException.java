package timetracker.error;

/**
 * Exception, welche im Backend auftritt
 */
public class BackendException extends Exception
{
    public BackendException(final String message)
    {
        super(message);
    }
}
