package timetracker.buttons;

import timetracker.Constants;
import timetracker.TimeTracker;
import timetracker.icons.Icon;
import timetracker.log.Log;
import timetracker.utils.LookAndFeelManager;
import timetracker.utils.SmallBevelBorder;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Basisklasse für alle Buttons
 */
public class BaseButton extends JButton implements ITimeTrackerButton
{
    protected static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(5, 5, 5, 5);
    private static final Border FLAT_BORDER_RAISED = new SmallBevelBorder(BevelBorder.RAISED);
    private static final Border FLAT_BORDER_LOWERED = new SmallBevelBorder(BevelBorder.LOWERED);

    private String icon;

    public BaseButton(final Icon icon)
    {
        setIcon(icon);
        refresh();
    }

    public BaseButton(final String text, final String icon)
    {
        super(text);
        setIcon(icon);
        refresh();
    }

    public BaseButton(final String text, final Icon icon)
    {
        super(text);
        setIcon(icon);
        refresh();
    }

    private void refresh()
    {
        if(LookAndFeelManager.isFlat())
        {
            //putClientProperty("JButton.buttonType", "toolBarButton");
            setBorder(FLAT_BORDER_RAISED);
        }
        else if(isSelected())
        {
            setBorder(FLAT_BORDER_LOWERED);
        }
        else
        {
            setBorder(EMPTY_BORDER);
        }

        setButtonIcon(this, this.icon);
    }

    @Override
    public void updateUI()
    {
        super.updateUI();
        refresh();
    }

    @Override
    public void setSelected(final boolean b)
    {
        super.setSelected(b);
        refresh();
    }

    @Override
    public void setEnabled(final boolean b)
    {
        super.setEnabled(b);
        refresh();
    }

    public void setIcon(final Icon icon)
    {
        setIcon(icon == null ? null : icon.getIcon());
    }

    public void setIcon(final String icon)
    {
        setButtonIcon(this, icon);
    }

    public static void setButtonIcon(final ITimeTrackerButton button, final String icon)
    {
        if (icon == null || icon.isEmpty())
        {
            ((AbstractButton) button).setIcon(null);
            return;
        }

        if(!setButtonResource(button, getIconName(icon)))
        {
            setButtonResource(button, icon);
        }
    }

    /**
     * Setzt das Icon am Button, insofern das Icon existiert
     * @param button Button
     * @param icon Icon
     * @return {@code true}, wenn das Icon gesetzt wurde, sonst {@code false}.
     */
    private static boolean setButtonResource(final ITimeTrackerButton button, final String icon)
    {
        final ImageIcon imageIcon = getImageIcon(icon);
        final boolean hasIcon = imageIcon != null;
        if(hasIcon)
        {
            button.setIconName(icon);
            button.setIcon(imageIcon);
        }
        return hasIcon;
    }

    /**
     * Liefert das Icon in Abhängigkeit, ob gerade ein DarkMode verwendet wird oder nicht
     * @param icon Icon
     * @return Angepasster Name für das Icon
     */
    public static String getIconName(String icon)
    {
        icon = icon.replace("_dark", Constants.STRING_EMPTY);
        if(LookAndFeelManager.isDark())
        {
            return icon;
        }
        icon = icon.replace(".png", "_dark.png");
        return icon;
    }

    /**
     * Liefert den InputStream als Bytes
     * @param in Inputstream
     * @return Bytes
     * @throws IOException Fehler beim Flush
     */
    private static byte[] getBytes(final InputStream in) throws IOException
    {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream())
        {
            int next;
            while ((next = in.read()) > -1)
            {
                bos.write(next);
            }
            bos.flush();
            return bos.toByteArray();
        }
    }

    /**
     * Liefert das ImageIcon zum übergebenen Icon-Pfad
     * @param icon Icon
     * @return ImageIcon
     */
    public static ImageIcon getImageIcon(final String icon)
    {
        try (final InputStream iconStream = TimeTracker.class.getResourceAsStream(icon))
        {
            if (iconStream != null)
            {
                final byte[] bytes = getBytes(iconStream);
                return new ImageIcon(bytes);
            }
            final File file = new File(icon);
            if (file.exists())
            {
                return new ImageIcon(icon);
            }
        }
        catch (final IOException e)
        {
            Log.severe(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void setIconName(final String icon)
    {
        this.icon = icon;
    }

    @Override
    public void setIcon(final ImageIcon icon)
    {
        super.setIcon(icon);
    }
}
