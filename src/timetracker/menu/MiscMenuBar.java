package timetracker.menu;

import timetracker.actions.OpenLogAction;
import timetracker.actions.ShowDatabaseContent;

import java.awt.*;

/**
 * Menü im Timetracker
 */
public class MiscMenuBar extends MenuBar
{
    public MiscMenuBar() throws HeadlessException
    {
        final MenuItem showDatabaseContentMenuItem = new MenuItem("Show database content");
        showDatabaseContentMenuItem.addActionListener(new ShowDatabaseContent());

        final MenuItem showLogMenuItem = new MenuItem("Show log");
        showLogMenuItem.addActionListener(new OpenLogAction());

        final Menu menu = new Menu("Misc");
        menu.add(showDatabaseContentMenuItem);
        menu.add(showLogMenuItem);
        add(menu);
    }
}
