package timetracker.actions;

import timetracker.dialogs.ResetDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Setzt alle Time zurück
 */
public class ResetAction extends BaseAction
{
    private static final long serialVersionUID = -3128930442339113957L;

    public ResetAction(final JButton button)
    {
        super(button);
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final ResetDialog dialog = new ResetDialog(this.button);
        dialog.setVisible(true);
    }
}