package timetracker.updates;

import org.hsqldb.HsqlException;
import org.hsqldb.error.ErrorCode;
import timetracker.db.Backend;
import timetracker.utils.Util;

import java.sql.SQLException;

/**
 * Benennt die Spalte "IS_DELETABLE" um
 */
public class RenameColumnDeletable implements IUpdateMethod
{
    @Override
    public int getUpdateId()
    {
        return 1;
    }

    @Override
    public boolean isPreBackendUpdate()
    {
        return false;
    }

    @Override
    public boolean doUpdate()
    {
        boolean success = false;
        try
        {
            Backend.getInstance().renameColumn(Backend.TN_ISSUES, Backend.CN_DELETABLE, Backend.CN_CAN_BE_FINISHED);
            success = true;
        }
        catch (final SQLException e)
        {
            final Throwable cause = e.getCause();
            if((cause instanceof HsqlException) && (ErrorCode.X_42504 == Math.abs(((HsqlException) cause).getErrorCode())))
            {
                //Spalte existiert schon
                success = true;
            }
            else
            {
                Util.handleException(e);
            }
        }
        return success;
    }
}
