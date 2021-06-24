package timetracker.utils;

import timetracker.PropertyConstants;
import timetracker.TimeTracker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manager für Look&Feels
 */
public final class LookAndFeelManager
{
    private static String selectedLafClassName;

    // Innere private Klasse, die erst beim Zugriff durch die umgebende Klasse initialisiert wird
    private static final class InstanceHolder
    {
        // Die Initialisierung von Klassenvariablen geschieht nur einmal und wird vom ClassLoader implizit synchronisiert
        static final LookAndFeelManager instance = new LookAndFeelManager();
    }

    public static LookAndFeelManager getInstance()
    {
        return LookAndFeelManager.InstanceHolder.instance;
    }

    private static final ToggleUiListener LISTENER = new ToggleUiListener();
    private static final Color IN_PROGRESS = new Color(163, 255, 145);
    private static final Color MARKED = new Color(255, 205, 28);
    private static final Color BLACK = new Color(34, 34, 34);

    private LookAndFeelManager()
    {
        installLookAndFeel(new LafInfo("Metal", "javax.swing.plaf.metal.MetalLookAndFeel"));
        installLookAndFeel(new LafInfo("Nimbus", "javax.swing.plaf.nimbus.NimbusLookAndFeel"));
        installLookAndFeel(new LafInfo("FlatLaf Light", "com.formdev.flatlaf.FlatLightLaf"));
        //installLookAndFeel(new LafInfo("FlatLaf IntelliJ", "com.formdev.flatlaf.FlatIntelliJLaf"));
        //installLookAndFeel(new LafInfo("FlatLaf Dark", "com.formdev.flatlaf.FlatDarkLaf", true));
        //installLookAndFeel(new LafInfo("FlatLaf Darcula", "com.formdev.flatlaf.FlatDarculaLaf", true));
    }

    /**
     * Installiert das LookAndFeel des Info-Objektes <tt>lfInfo</tt>.
     *
     * @param lfInfo das Info-Objekt des zu installierenden LookAndFeels.
     */
    private void installLookAndFeel(final UIManager.LookAndFeelInfo lfInfo)
    {
        final String clsname = lfInfo.getClassName();
        final UIManager.LookAndFeelInfo[] feels = UIManager.getInstalledLookAndFeels();
        boolean found = false;
        for (int i = 0; i < feels.length && !found; i++)
        {
            final UIManager.LookAndFeelInfo info = feels[i];
            if (info.getName().equals(lfInfo.getName()) || info.getClassName().equals(clsname))
            {
                found = true;
            }
        }
        if (!found && Arrays.stream(UIManager.getInstalledLookAndFeels()).noneMatch(info -> clsname.equals(info.getClassName())))
        {
            // Workaround für: KunststoffLookAndFeel installiert sich im Konstruktor
            UIManager.installLookAndFeel(lfInfo);
        }
    }

    public Map<String, String> getLookAndFeels()
    {
        final UIManager.LookAndFeelInfo[] lookAndFeels = UIManager.getInstalledLookAndFeels();
        return Util.sortByValue(Arrays.stream(lookAndFeels).filter(laf -> !"CDE/Motif".equalsIgnoreCase(laf.getName()))
                                                           .collect(Collectors.toMap(UIManager.LookAndFeelInfo::getClassName,
                                                                                     UIManager.LookAndFeelInfo::getName)));
    }

    public JRadioButtonMenuItem addListener(final JRadioButtonMenuItem item)
    {
        item.addActionListener(LISTENER);
        return item;
    }

    private static class ToggleUiListener implements ActionListener
    {
        @Override
        public void actionPerformed(final ActionEvent e)
        {
            final JMenuItem item = (JMenuItem) e.getSource();
            final String name = item.getText();

            final UIManager.LookAndFeelInfo[] lookAndFeels = UIManager.getInstalledLookAndFeels();
            Arrays.stream(lookAndFeels).filter(laf -> laf.getName().equalsIgnoreCase(name)).findFirst()
                                       .ifPresent(info -> LookAndFeelManager.setLookAndFeel(info.getClassName()));
        }
    }

    public static void setLookAndFeel(final String classname)
    {
        try
        {
            LookAndFeelManager.selectedLafClassName = classname;
            UIManager.setLookAndFeel(classname);
            SwingUtilities.updateComponentTreeUI(TimeTracker.getTimeTracker());
            TimeTracker.saveSetting(PropertyConstants.LOOK_AND_FEEL, classname);
        }
        catch (final Exception ex)
        {
            Util.handleException(ex);
        }
    }

    public static boolean isDark()
    {
        return getSelectedLafInfoStream().anyMatch(LafInfo::isDark);
    }

    public static boolean isFlat()
    {
        return getSelectedLafInfoStream().anyMatch(LafInfo::isFlat);
    }

    private static Stream<LafInfo> getSelectedLafInfoStream()
    {
        final UIManager.LookAndFeelInfo[] lookAndFeels = UIManager.getInstalledLookAndFeels();
        return Arrays.stream(lookAndFeels).filter(LafInfo.class::isInstance)
                                          .filter(laf -> laf.getClassName().equalsIgnoreCase(LookAndFeelManager.selectedLafClassName))
                                          .map(LafInfo.class::cast);
    }

    /**
     * Liefert die Farbe für ein Issue, welches in Bearbeitung genommen wurde
     * @return Farbe für ein Issue in Bearbeitung
     */
    public static Color getColorInProgress()
    {
        /*final boolean darkMode = LookAndFeelManager.isDarkMode();
        if(darkMode)
        {
            return new Color(163, 255, 145);
        }
        return Color.GREEN;*/
        return IN_PROGRESS;
    }

    /**
     * Liefert die Farbe für ein Issue, welches markiert wurde
     * @return Farbe für ein markiertes Issue
     */
    public static Color getColorMarked()
    {
        /*final boolean darkMode = LookAndFeelManager.isDarkMode();
        if(darkMode)
        {
            return new Color(244, 151, 148);
        }
        return Color.YELLOW;*/
        return MARKED;
    }

    /**
     * Liefert die Schriftfarbe für ein markiertes Issue
     * @return Schriftfarbe für ein markiertes Issue
     */
    public static Color getFontColor()
    {
        /*final boolean darkMode = LookAndFeelManager.isDarkMode();
        if(darkMode)
        {
            return new Color(34, 34, 34);
        }
        return Color.BLACK;*/
        return BLACK;
    }

    private static class LafInfo extends UIManager.LookAndFeelInfo
    {
        private final boolean isDark;
        private final boolean isFlat;

        /**
         * Constructs a <code>UIManager</code>s
         * <code>LookAndFeelInfo</code> object.
         *
         * @param name      a <code>String</code> specifying the name of
         *                  the look and feel
         * @param className a <code>String</code> specifying the name of
         */
        public LafInfo(final String name, final String className)
        {
            this(name, className, false);
        }
        /**
         * Constructs a <code>UIManager</code>s
         * <code>LookAndFeelInfo</code> object.
         *
         * @param name      a <code>String</code> specifying the name of
         *                  the look and feel
         * @param className a <code>String</code> specifying the name of
         */
        public LafInfo(final String name, final String className, final boolean isDarkMode)
        {
            super(name, className);
            this.isDark = isDarkMode;
            this.isFlat = name.startsWith("Flat");
        }

        public boolean isDark()
        {
            return this.isDark;
        }

        public boolean isFlat()
        {
            return this.isFlat;
        }
    }
}
