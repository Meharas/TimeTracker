package timetracker.actions;

import timetracker.Constants;
import timetracker.TimeTracker;
import timetracker.data.Issue;
import timetracker.data.WorkItemType;
import timetracker.db.Backend;
import timetracker.utils.EscapeEvent;
import timetracker.utils.Util;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Aktion zur Anzeige des Datenbankinhaltes
 */
public class ShowDatabaseContent extends AbstractAction
{
    private static final String[] COLUMN_HEADERS = {Backend.CN_ID, Backend.CN_ISSUE, Backend.CN_LABEL, Backend.CN_TYPE, Backend.CN_ICON, Backend.CN_DURATION,
                                                    Backend.CN_DURATION_SAVED, Backend.CN_DELETABLE, Backend.CN_MARKED};

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final TimeTracker timeTracker = TimeTracker.getTimeTracker();
        final List<Issue> issues;
        try
        {
            issues = Backend.getInstance().getIssues();
        }
        catch (Throwable throwable)
        {
            Util.handleException(throwable);
            return;
        }

        final Dimension size = new Dimension(1150, 500);
        final Point location = Util.getWindowLocation(size);

        final JDialog dialog = new JDialog(timeTracker, "Database content", true);
        dialog.setBounds(location.x, location.y, size.width, size.height);
        dialog.setResizable(false);
        EscapeEvent.add(dialog);

        final Object[][] rowData = issuesToData(issues);
        final JTable table = new JTable(rowData, COLUMN_HEADERS)
        {
            @Override
            public boolean isCellEditable(final int row, final int column)
            {
                return false;
            }
        };
        table.setPreferredScrollableViewportSize(size);

        final TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(50);
        columnModel.getColumn(1).setPreferredWidth(100);
        columnModel.getColumn(2).setPreferredWidth(250);
        columnModel.getColumn(3).setPreferredWidth(100);
        columnModel.getColumn(4).setPreferredWidth(250);
        columnModel.getColumn(5).setPreferredWidth(100);
        columnModel.getColumn(6).setPreferredWidth(100);
        columnModel.getColumn(7).setPreferredWidth(100);
        columnModel.getColumn(8).setPreferredWidth(100);

        final JScrollPane scrollPane = new JScrollPane(table);
        dialog.add(scrollPane);
        dialog.setVisible(true);
    }

    private Object[][] issuesToData(final List<Issue> issues)
    {
        final Object[][] result = new Object[issues.size()][9];
        int counter = 0;
        for(final Issue issue : issues)
        {
            result[counter][0] = issue.getId();
            result[counter][1] = issue.getTicket();
            result[counter][2] = issue.getLabel();
            result[counter][3] = getType(issue);
            result[counter][4] = issue.getIcon();
            result[counter][5] = issue.getDuration();
            result[counter][6] = issue.getDurationSaved();
            result[counter][7] = issue.isDeletable();
            result[counter][8] = issue.isMarked();
            counter++;
        }
        return result;
    }

    private String getType(final Issue issue)
    {
        final WorkItemType type = issue.getType();
        if(type == WorkItemType.EMPTY)
        {
            return Constants.STRING_EMPTY;
        }
        return String.format("%s (%s)", type.getLabel(), type.getId());
    }
}