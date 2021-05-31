package timetracker.actions;

import timetracker.TimeTracker;
import timetracker.TimeTrackerConstants;
import timetracker.log.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * L?scht eine komplette Zeile.
 */
public class DeleteButtonAction extends BaseAction
{
    private static final long serialVersionUID = -2092965435624779543L;

    public DeleteButtonAction(final JButton button, final String key)
    {
        super(button, key, null);
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        try (final InputStream inputStream = TimeTracker.class.getResourceAsStream(TimeTrackerConstants.PROPERTIES))
        {
            final Properties properties = new Properties();
            properties.load(inputStream);

            final String prefix = this.key + ".";
            for (final Iterator<Map.Entry<Object, Object>> iter = properties.entrySet().iterator(); iter.hasNext(); )
            {
                final Map.Entry<Object, Object> entry = iter.next();
                final String propKey = (String) entry.getKey();
                if (propKey.startsWith(prefix))
                {
                    iter.remove();
                }
            }
            TimeTracker.storeProperties(properties);

            remove();
            this.timeTracker.updateGui(true);
            this.timeTracker.decreaseLine();
        }
        catch (final IOException ex)
        {
            Log.severe(ex.getMessage(), ex);
        }
    }

    /**
     * Entfernt eine komplette Zeile
     */
    private void remove()
    {
        final Container parent = this.button.getParent().getParent();
        if (parent == null)
        {
            return;
        }
        final Component[] components = parent.getComponents();
        for (final Component child : components)
        {
            remove(parent, child);
        }
        this.timeTracker.getPanel().remove(parent);
    }

    /**
     * Entfernt die Komponente und deren Kinder vom Parent
     * @param parent Parent
     * @param component Komponente
     */
    private void remove(final Container parent, final Component component)
    {
        if (component instanceof JPanel)
        {
            final Component[] components = ((JPanel) component).getComponents();
            for (final Component child : components)
            {
                remove((Container) component, child);
            }
        }
        parent.remove(component);
    }
}