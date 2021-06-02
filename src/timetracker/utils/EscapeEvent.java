package timetracker.utils;

import timetracker.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Hilfsklasse zum Hinzuf�gen eines EscapeEvents
 */
public final class EscapeEvent
{
    private EscapeEvent()
    {

    }

    /**
     * F�gt an der �bergebenen Komponente ein Escape-Event hinzu
     * @param window Fenster, welchem ein Escape-Event hinzuf�gt
     */
    public static void add(final RootPaneContainer window)
    {
        window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), Constants.ACTIONMAP_KEY_CANCEL); //$NON-NLS-1$
        window.getRootPane().getActionMap().put(Constants.ACTIONMAP_KEY_CANCEL, new AbstractAction(){
            private static final long serialVersionUID = 4056930123544439411L; //$NON-NLS-1$

            @Override
            public void actionPerformed(final ActionEvent e)
            {
                ((Window) window).dispose();
            }
        });
    }
}
