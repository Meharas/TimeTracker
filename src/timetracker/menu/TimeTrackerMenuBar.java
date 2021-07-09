package timetracker.menu;

import timetracker.PropertyConstants;
import timetracker.Resource;
import timetracker.actions.OpenLogAction;
import timetracker.actions.ShowDatabaseContent;
import timetracker.actions.ShowSettings;
import timetracker.utils.LookAndFeelManager;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Menü im Timetracker
 */
public class TimeTrackerMenuBar extends JMenuBar
{
    public TimeTrackerMenuBar(final String classname) throws HeadlessException
    {
        final JMenuItem showDatabaseContentMenuItem = new JMenuItem(Resource.getString(PropertyConstants.LABEL_SHOW_DB));
        showDatabaseContentMenuItem.addActionListener(new ShowDatabaseContent());

        final JMenuItem showLogMenuItem = new JMenuItem(Resource.getString(PropertyConstants.LABEL_SHOW_LOG));
        showLogMenuItem.addActionListener(new OpenLogAction());

        final JMenu tools = new JMenu(Resource.getString(PropertyConstants.MENU_TOOLS));
        tools.add(showDatabaseContentMenuItem);
        tools.add(showLogMenuItem);
        add(tools);

        final JMenu lookAndFeel = new JMenu("Look & Feel");
        final ButtonGroup group = new ButtonGroup();
        final Map<String, String> lookAndFeels = LookAndFeelManager.getInstance().getLookAndFeels();
        for(final Map.Entry<String, String> laf : lookAndFeels.entrySet())
        {
            final JRadioButtonMenuItem menu = LookAndFeelManager.getInstance().addListener(new JRadioButtonMenuItem(laf.getValue()));
            menu.setSelected(laf.getKey().equals(classname));
            group.add(menu);
            lookAndFeel.add(menu);
        }
        add(lookAndFeel);

        final JMenuItem settingsItem = new JMenuItem(Resource.getString(PropertyConstants.MENU_ITEM_SETTINGS));
        settingsItem.addActionListener(new ShowSettings());
        final JMenu settings = new JMenu(Resource.getString(PropertyConstants.MENU_SETTINGS));
        settings.add(settingsItem);
        add(settings);
    }
}