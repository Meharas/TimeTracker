package timetracker.log;

import timetracker.Constants;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formatter für die Logausschriften
 */
public class LogFormatter extends Formatter
{
    private static final String FORMAT = "[%1$tF %1$tT.%1$tQ] %2$-15.13s %3$s%4$s%n";
    private final Date date = new Date();
    private static final StringWriter STRING_WRITER = new StringWriter();

    @Override
    public String format(final LogRecord record)
    {
        this.date.setTime(record.getMillis());

        final String message = formatMessage(record);
        String throwable = Constants.STRING_EMPTY;
        final Throwable cause = record.getThrown();
        if (cause != null)
        {
            final PrintWriter pw = new PrintWriter(STRING_WRITER);
            pw.println();
            cause.printStackTrace(pw);
            pw.close();
            throwable = STRING_WRITER.toString();
        }
        return String.format(FORMAT, this.date, record.getLevel().getName(), message, throwable);
    }
}