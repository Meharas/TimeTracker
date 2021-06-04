package timetracker.dialogs;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import timetracker.*;
import timetracker.actions.BaseAction;
import timetracker.client.Client;
import timetracker.log.Log;
import timetracker.menu.ComboBoxFixVersions;
import timetracker.menu.ComboBoxStates;
import timetracker.utils.EscapeEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.TextAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Dialog zum Abschlieﬂen eines Tickets
 */
public class FinishIssueDialog extends JFrame
{
    public FinishIssueDialog(final JButton button) throws HeadlessException
    {
        final Point location = TimeTracker.getTimeTracker().getWindowLocation();
        setBounds(location.x, location.y, 400, 300);
        setResizable(false);
        setAlwaysOnTop(true);
        EscapeEvent.add(this);

        final JPanel globalPanel = new JPanel();
        globalPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        globalPanel.setLayout(new BoxLayout(globalPanel, BoxLayout.Y_AXIS));

        final ComboBoxStates statesBox = new ComboBoxStates();
        statesBox.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0

        final ComboBoxFixVersions versionsBox = new ComboBoxFixVersions();
        versionsBox.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0

        final JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel(Resource.getString(PropertyConstants.LABEL_STATE)));
        panel.add(statesBox);
        panel.add(new JLabel(Resource.getString(PropertyConstants.LABEL_VERSION)));
        panel.add(versionsBox);
        globalPanel.add(panel);

        final JTextArea textArea = new JTextArea(5, 30);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0

        final JPanel commentPanel = new JPanel();
        commentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        commentPanel.setMinimumSize(new Dimension(200, 100));
        commentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0
        commentPanel.setLayout(new BoxLayout(commentPanel, BoxLayout.Y_AXIS));
        commentPanel.add(new JLabel(Resource.getString(PropertyConstants.LABEL_TEXT)));

        final JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setMaximumSize(new Dimension(525, 50));
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0
        commentPanel.add(scrollPane);
        globalPanel.add(commentPanel);

        final Action action = button.getAction();
        final BaseAction timerAction = action instanceof BaseAction ? (BaseAction) action : null;
        if (timerAction != null && timerAction.timer != null && timerAction.timer.isRunning())
        {
            timerAction.stopWithoutSave();
        }

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

                    final String commentText = textArea.getText();
                    if (commentText != null && !commentText.isEmpty())
                    {
                        builder = Client.getURIBuilder(ServicePath.COMMENT, issueID);
                        request = new HttpPost(builder.build());
                        request.setEntity(new StringEntity(String.format(Constants.ISSUE_COMMENT, commentText), ContentType.APPLICATION_JSON));

                        response = Client.executeRequest(request);
                        Client.logResponse(response);
                    }

                    dispose();
                }
                catch (final URISyntaxException | IOException ex)
                {
                    Log.severe(ex.getMessage(), ex);
                }
            }
        });

        final JPanel okBtnPanel = new JPanel();
        okBtnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);//0.0
        okBtnPanel.add(okButton);
        globalPanel.add(okBtnPanel);
        add(globalPanel);
    }
}
