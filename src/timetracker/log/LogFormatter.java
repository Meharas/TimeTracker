package log;

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

    @Override
    public String format(final LogRecord record)
    {
        this.date.setTime(record.getMillis());

        final String message = formatMessage(record);
        String throwable = "";
        if (record.getThrown() != null)
        {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }
        return String.format(FORMAT, this.date, record.getLevel().getLocalizedName(), message, throwable);
    }
}