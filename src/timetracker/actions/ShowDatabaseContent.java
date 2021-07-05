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
    private static final String[] COLUMN_HEADERS = {Backend.CN_ID, Backend.CN_ISSUE, Backend.CN_ORDER, Backend.CN_LABEL, Backend.CN_DESCRIPTION, Backend.CN_TYPE,
                                                    Backend.CN_ICON, Backend.CN_DURATION, Backend.CN_DURATION_SAVED, Backend.CN_CAN_BE_FINISHED, Backend.CN_MARKED};

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final TimeTracker timeTracker = TimeTracker.getInstance();
        final List<Issue> issues;
        try
        {
            issues = Backend.getInstance().getIssues();
        }
        catch (final Throwable t)
        {
            Util.handleException(t);
            return;
        }

        final JDialog dialog = new JDialog(timeTracker, "Database content", true);
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

        final TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(50);
        columnModel.getColumn(1).setPreferredWidth(100);
        columnModel.getColumn(2).setPreferredWidth(50);
        columnModel.getColumn(3).setPreferredWidth(100);
        columnModel.getColumn(4).setPreferredWidth(250);
        columnModel.getColumn(5).setPreferredWidth(250);
        columnModel.getColumn(6).setPreferredWidth(250);
        columnModel.getColumn(7).setPreferredWidth(100);
        columnModel.getColumn(8).setPreferredWidth(100);
        columnModel.getColumn(9).setPreferredWidth(100);
        columnModel.getColumn(10).setPreferredWidth(100);

        int width = 0;
        final int columnCount = columnModel.getColumnCount();
        for(int i = 0; i < columnCount; i++)
        {
            width += columnModel.getColumn(i).getPreferredWidth();
        }

        final Dimension size = new Dimension(width, 500);
        table.setPreferredScrollableViewportSize(size);

        final Point location = Util.getWindowLocation(size);
        dialog.setBounds(location.x, location.y, size.width, size.height);

        final JScrollPane scrollPane = new JScrollPane(table);
        dialog.add(scrollPane);
        dialog.setVisible(true);
    }

    private Object[][] issuesToData(final List<Issue> issues)
    {
        final Object[][] result = new Object[issues.size()][COLUMN_HEADERS.length];
        int counter = 0;
        for(final Issue issue : issues)
        {
            result[counter][0] = issue.getId();
            result[counter][1] = issue.getTicket();
            result[counter][2] = issue.getOrder();
            result[counter][3] = issue.getLabel();
            result[counter][4] = issue.getDescription();
            result[counter][5] = getType(issue);
            result[counter][6] = issue.getIcon();
            result[counter][7] = issue.getDuration();
            result[counter][8] = issue.getDurationSaved();
            result[counter][9] = issue.canBeFinished();
            result[counter][10] = issue.isMarked();
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