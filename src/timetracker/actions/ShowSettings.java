package timetracker.actions;

import timetracker.dialogs.ShowSettingsDialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Zeigt das Einstellungspanel an
 */
public class ShowSettings implements ActionListener
{
    @Override
    public void actionPerformed(final ActionEvent e)
    {
        final ShowSettingsDialog dialog = new ShowSettingsDialog();
        dialog.setVisible(true);
    }
}