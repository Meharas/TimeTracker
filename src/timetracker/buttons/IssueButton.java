package timetracker.buttons;

import timetracker.actions.TimerAction;
import timetracker.data.Issue;
import timetracker.dnd.ButtonTransferHandler;
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

        final ToolTipManager ttm = ToolTipManager.sharedInstance();
        ttm.setInitialDelay(0);
        ttm.setDismissDelay(10000);
        ttm.registerComponent(this);

        final MouseAdapter mouseAdapter = new MouseAdapter()
        {
            @Override
            public void mousePressed(final MouseEvent e)
            {
                showPopUp(e);
                ttm.mousePressed(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e)
            {
                showPopUp(e);
                ttm.mouseReleased(e);
            }

            @Override
            public void mouseEntered(final MouseEvent e)
            {
                ttm.mouseEntered(e);
            }

            @Override
            public void mouseMoved(final MouseEvent e)
            {
                ttm.mouseMoved(e);
            }

            @Override
            public void mouseDragged(final MouseEvent e)
            {
                final JButton button = (JButton) e.getSource();
                final TransferHandler handle = button.getTransferHandler();
                handle.exportAsDrag(button, e, TransferHandler.COPY);

                ttm.mouseDragged(e);
            }

            private void showPopUp(final MouseEvent e)
            {
                if (e.isPopupTrigger())
                {
                    final JPopupMenu menu = ContextMenu.create(IssueButton.this, issue);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);

        setTransferHandler(new ButtonTransferHandler(issue));
    }

    @Override
    public String getToolTipText(final MouseEvent event)
    {
        final String description = this.issue.getDescription();
        return description == null || description.isEmpty() ? null : description;
    }

    public Issue getIssue()
    {
        return this.issue;
    }

    public void refresh(final Issue issue)
    {
        this.issue.putAll(issue);
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

    @Override
    public TimerAction getAction()
    {
        return (TimerAction) super.getAction();
    }
}