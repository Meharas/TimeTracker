package timetracker.dnd;

import timetracker.TimeTracker;
import timetracker.buttons.IssueButton;
import timetracker.data.Issue;
import timetracker.utils.Util;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;

/**
 * Wird aufgerufen, wenn eine Button gedroppt wurde
 */
public class ButtonTransferHandler extends TransferHandler
{
    public static final DataFlavor SUPPORTED_DATA_FLAVOR = new DataFlavor(IssueButton.class, "IssueButton");

    private final Issue targetIssue;
    public ButtonTransferHandler(final Issue issue)
    {
        this.targetIssue = issue;
    }

    @Override
    public boolean canImport(final TransferHandler.TransferSupport support)
    {
        return support.isDataFlavorSupported(SUPPORTED_DATA_FLAVOR);
    }

    @Override
    public boolean importData(final TransferHandler.TransferSupport support)
    {
        boolean success = false;
        if (canImport(support))
        {
            try
            {
                final Transferable t = support.getTransferable();
                final Object source = t.getTransferData(SUPPORTED_DATA_FLAVOR);
                if (source instanceof Issue)
                {
                    final Issue sourceIssue = (Issue) source;
                    final String targetIssueId = this.targetIssue.getId();
                    final String sourceIssueId = sourceIssue.getId();
                    if(!targetIssueId.equalsIgnoreCase(sourceIssueId))
                    {
                        final TimeTracker timeTracker = TimeTracker.getInstance();
                        timeTracker.move(sourceIssue, this.targetIssue);
                        success = true;
                    }
                }
            }
            catch (final Exception e)
            {
                Util.handleException(e);
            }
        }
        return success;
    }

    @Override
    public int getSourceActions(final JComponent c)
    {
        return DnDConstants.ACTION_COPY_OR_MOVE;
    }

    @Override
    protected Transferable createTransferable(final JComponent c)
    {
        final Issue source = ((IssueButton) c).getIssue();
        return new Transferable()
        {
            @Override
            public DataFlavor[] getTransferDataFlavors()
            {
                return new DataFlavor[] {SUPPORTED_DATA_FLAVOR};
            }

            @Override
            public boolean isDataFlavorSupported(final DataFlavor flavor)
            {
                return true;
            }

            @Override
            public Object getTransferData(final DataFlavor flavor)
            {
                return source;
            }
        };
    }
}
