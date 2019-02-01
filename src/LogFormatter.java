import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formatter für die Logausschriften
 *
 * @author $Author: beyera $ &copy; forcont business technology gmbh 2001-2019
 * @version $Revision: 1.0 $ $Date: 01.02.2019 10:45 $
 * @since 7.0
 */
public class LogFormatter extends Formatter
{
    private static final String FORMAT = "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %2$s     %4$s: %5$s%6$s%n";
    private final Date date = new Date();

    @Override
    public String format(final LogRecord record)
    {
        this.date.setTime(record.getMillis());

        String source;
        if (record.getSourceClassName() != null)
        {
            source = record.getSourceClassName();
            if (record.getSourceMethodName() != null)
            {
                source += " " + record.getSourceMethodName();
            }
        }
        else
        {
            source = record.getLoggerName();
        }
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
        return String.format(FORMAT, this.date, source, record.getLoggerName(), record.getLevel().getLocalizedName(), message, throwable);
    }
}