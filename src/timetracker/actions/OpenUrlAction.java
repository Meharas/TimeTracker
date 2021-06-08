package timetracker.actions;

import org.apache.http.client.utils.URIBuilder;
import timetracker.Constants;
import timetracker.ServicePath;
import timetracker.TimeTracker;
import timetracker.client.Client;
import timetracker.data.Issue;
import timetracker.log.Log;

import javax.swing.text.TextAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Action öffnet das Ticket im Browser
 */
public class OpenUrlAction extends TextAction
{
    private final Issue issue;

    public OpenUrlAction(final Issue issue)
    {
        super(Constants.STRING_EMPTY);
        this.issue = issue;
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final String ticket = this.issue.getTicket();
        if (ticket != null && !ticket.isEmpty() && TimeTracker.matches(ticket))
        {
            try
            {
                final URIBuilder builder = Client.getURIBuilder(ServicePath.URL, ticket);
                openWebpage(builder.build());
            }
            catch (final URISyntaxException ex)
            {
                Log.severe(ex.getMessage(), ex);
            }
        }
    }

    private void openWebpage(final URI uri)
    {
        final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE))
        {
            try
            {
                desktop.browse(uri);
            }
            catch (final Exception e)
            {
                Log.severe(e.getMessage(), e);
            }
        }
    }
}
