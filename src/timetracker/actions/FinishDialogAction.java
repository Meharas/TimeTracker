package timetracker.actions;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import timetracker.*;
import timetracker.client.Client;
import timetracker.log.Log;
import timetracker.menu.ComboBoxFixVersions;
import timetracker.menu.ComboBoxStates;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.TextAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Aktion um das Ticket auf "To Verify" zu setzen.
 */
public class FinishDialogAction extends BaseAction
{
    private static final long serialVersionUID = 7059526162584192854L;

    public FinishDialogAction(final JButton button)
    {
        super(button);
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        TimeTracker.MATCHER.reset(button.getText());
        if (TimeTracker.MATCHER.matches())
        {
            final JDialog dialog = this.timeTracker.getDialog(200);
            final JPanel rows = new JPanel();
            rows.setBorder(new EmptyBorder(10, 10, 10, 10));
            dialog.add(rows);

            rows.add(new JLabel(Resource.getString(PropertyConstants.LABEL_STATE)));
            final JComboBox<String> statesBox = new ComboBoxStates();
            rows.add(statesBox);

            rows.add(new JLabel(Resource.getString(PropertyConstants.LABEL_VERSION)));
            final JComboBox<String> versionsBox = new ComboBoxFixVersions();
            rows.add(versionsBox);

            final JPanel commentPanel = new JPanel();
            commentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            dialog.add(commentPanel);

            commentPanel.add(new JLabel(Resource.getString(PropertyConstants.LABEL_COMMENT)));
            final JTextArea comment = new JTextArea(1, 5);
            comment.setLineWrap(true);
            comment.setWrapStyleWord(true);
            comment.setPreferredSize(new Dimension(300, 100));
            commentPanel.add(comment);

            final JButton okButton = new JButton(Resource.getString(PropertyConstants.TEXT_OK));
            okButton.addActionListener(new TextAction(Resource.getString(PropertyConstants.TEXT_OK))
            {
                private static final long serialVersionUID = 566865284107947772L;

                @Override
                public void actionPerformed(final ActionEvent e)
                {
                    try
                    {
                        final String issueID = Client.getIssueID(TimeTracker.MATCHER.group(1));

                        URIBuilder builder = Client.getCommandURIBuilder();
                        HttpPost request = new HttpPost(builder.build());
                        request.setEntity(new StringEntity(String.format(Constants.ISSUE_COMMAND, Constants.ISSUE_FIX_VERSIONS,
                                                                         versionsBox.getSelectedItem(), issueID), ContentType.APPLICATION_JSON));
                        HttpResponse response = Client.executeRequest(request);
                        Client.logResponse(response);

                        request.setEntity(new StringEntity(String.format(Constants.ISSUE_COMMAND, Constants.ISSUE_STATE,
                                                                         statesBox.getSelectedItem(), issueID), ContentType.APPLICATION_JSON));
                        response = Client.executeRequest(request);
                        Client.logResponse(response);

                        final String commentText = comment.getText();
                        if (commentText != null && !commentText.isEmpty())
                        {
                            builder = Client.getURIBuilder(ServicePath.COMMENT, issueID);
                            request = new HttpPost(builder.build());
                            request.setEntity(new StringEntity(String.format(Constants.ISSUE_COMMENT, commentText), ContentType.APPLICATION_JSON));

                            response = Client.executeRequest(request);
                            Client.logResponse(response);
                        }

                        dialog.dispose();
                    }
                    catch (final URISyntaxException | IOException ex)
                    {
                        Log.severe(ex.getMessage(), ex);
                    }
                }
            });
            dialog.add(okButton);
            dialog.pack();
            dialog.setVisible(true);
        }
    }
}