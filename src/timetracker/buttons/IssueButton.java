package timetracker.buttons;

import timetracker.data.Issue;
import timetracker.icons.Icon;
import timetracker.menu.ContextMenu;
import timetracker.utils.LookAndFeelManager;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Stellt einen Button für ein Issue dar
 */
public class IssueButton extends BaseButton
{
    private final Issue issue;
    private static final Border BORDER_PADDING = new EmptyBorder(0,25,0,5);
    private static final Border BORDER_NO_PADDING = new EmptyBorder(0,5,0,5);
    private static final Dimension MINIMUM_SIZE = new Dimension(300, 50);

    public IssueButton(final Issue issue)
    {
        super(issue.getLabel(), issue.getIcon());
        this.issue = issue;

        setName(issue.getId());
        setMinimumSize(MINIMUM_SIZE);
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(final MouseEvent e)
            {
                showPopUp(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e)
            {
                showPopUp(e);
            }

            private void showPopUp(final MouseEvent e)
            {
                if (e.isPopupTrigger())
                {
                    final JPopupMenu menu = ContextMenu.create(IssueButton.this, issue);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    public void refresh()
    {
        repaint();
    }

    @Override
    public int getHorizontalAlignment()
    {
        return SwingConstants.LEFT;
    }

    @Override
    public Dimension getMinimumSize()
    {
        return MINIMUM_SIZE;
    }

    @Override
    public Color getBackground()
    {
        if(this.issue == null || !this.issue.isMarked())
        {
            return super.getBackground();
        }
        return LookAndFeelManager.getColorMarked();
    }

    @Override
    public void paintComponent(final Graphics g)
    {
        super.paintComponent(g);

        if(getIcon() != null && this.issue != null && (this.issue.getIcon() == null || this.issue.getIcon().isEmpty()))
        {
            setIcon((Icon) null);
        }

        if(showIcon())
        {
            final ImageIcon imageIcon = getImageIcon(getIconName(getDetailIcon().getIcon()));
            if(imageIcon != null)
            {
                final Graphics2D g2 = (Graphics2D) g;
                final RenderingHints rh = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHints(rh);
                g2.drawImage(imageIcon.getImage(), 5, (getHeight() - 16) / 2, null);
                setBorder(BORDER_PADDING);
            }
        }
        else
        {
            setBorder(BORDER_NO_PADDING);
        }
    }

    private boolean showIcon()
    {
        return this.issue != null && (this.issue.isMarked() || this.issue.isInProgress());
    }

    private Icon getDetailIcon()
    {
        if(this.issue.isMarked())
        {
            return Icon.STAR;
        }
        return Icon.CLOCK;
    }
}