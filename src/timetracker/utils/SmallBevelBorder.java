package timetracker.utils;

import javax.swing.border.BevelBorder;
import java.awt.*;

/**
 * Diese Klasse leitet von {@link BevelBorder} ab und ändert dabei die Rahmendicke von 2 auf 1 Pixel!
 */
public class SmallBevelBorder extends BevelBorder
{
    /**
     * Konstruktor der Klasse
     *
     * @param bevelType Typ des Rahmens (RAISED | LOWERED)
     * @see BevelBorder
     */
    public SmallBevelBorder(final int bevelType)
    {
        super(bevelType);
    }

    @Override
    protected void paintRaisedBevel(final Component c, final Graphics g, final int x, final int y, final int width, final int height)
    {
        final Color oldColor = g.getColor();
        g.translate(x, y);

        g.setColor(getHighlightOuterColor(c));
        g.drawLine(0, 0, 0, height - 1);
        g.drawLine(1, 0, width - 1, 0);

        g.setColor(getShadowOuterColor(c));
        g.drawLine(1, height - 1, width - 1, height - 1);
        g.drawLine(width - 1, 1, width - 1, height - 2);

        g.translate(-x, -y);
        g.setColor(oldColor);
    }

    @Override
    protected void paintLoweredBevel(final Component c, final Graphics g, final int x, final int y, final int width, final int height)
    {
        final Color oldColor = g.getColor();
        g.translate(x, y);

        g.setColor(getShadowInnerColor(c));
        g.drawLine(0, 0, 0, height - 1);
        g.drawLine(1, 0, width - 1, 0);

        g.setColor(getHighlightOuterColor(c));
        g.drawLine(1, height - 1, width - 1, height - 1);
        g.drawLine(width - 1, 1, width - 1, height - 2);

        g.translate(-x, -y);
        g.setColor(oldColor);
    }
}