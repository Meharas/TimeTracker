package timetracker.data;

import timetracker.Constants;
import timetracker.db.Backend;
import timetracker.log.Log;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Repräsentiert ein Ticket
 */
public class Issue implements Serializable
{
    private String id;
    private int order = -1;
    private String ticket;
    private String label;
    private String description;
    private WorkItemType type;
    private String duration;
    private String durationSaved;
    private String icon;
    private boolean canBeFinished;
    private boolean marked;
    private boolean inProgress;
    private boolean preventTimer;

    public Issue(final String ticket, final String label, final WorkItemType type, final String duration, final String durationSaved, final String icon)
    {
        this.ticket = getString(ticket);
        this.label = getString(label);
        this.type = type;
        this.duration = getString(duration);
        this.durationSaved = getString(durationSaved);
        this.icon = getString(icon);
    }

    public Issue(final Map<String, String> data)
    {
        this.id = getString(data.get(Backend.CN_ID));
        this.order = Optional.ofNullable(data.get(Backend.CN_ORDER)).map(Integer::parseInt).orElse(-1);
        this.ticket = getString(data.get(Backend.CN_ISSUE));
        this.label = getString(data.get(Backend.CN_LABEL));
        this.description = getString(data.get(Backend.CN_DESCRIPTION));
        this.type = WorkItemType.getType(data.get(Backend.CN_TYPE));
        this.duration = getString(data.get(Backend.CN_DURATION));
        this.durationSaved = getString(data.get(Backend.CN_DURATION_SAVED));
        this.icon = getString(data.get(Backend.CN_ICON));
        this.canBeFinished = Boolean.parseBoolean(data.get(Backend.CN_CAN_BE_FINISHED));
        this.marked = Boolean.parseBoolean(data.get(Backend.CN_MARKED));
    }

    private String getString(final String string)
    {
        return Optional.ofNullable(string).orElse(Constants.STRING_EMPTY);
    }

    public String getId()
    {
        return this.id;
    }

    public void setId(final String id)
    {
        if(this.id == null || this.id.isEmpty())
        {
            this.id = id;
        }
    }

    public int getOrder()
    {
        return this.order;
    }

    public void setOrder(final int order)
    {
        this.order = order;
    }

    public String getTicket()
    {
        return this.ticket;
    }

    public void setTicket(final String ticket)
    {
        this.ticket = getString(ticket);
    }

    public String getLabel()
    {
        return this.label;
    }

    public void setLabel(final String label)
    {
        this.label = getString(label);
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(final String description)
    {
        this.description = getString(description);
    }

    public WorkItemType getType()
    {
        return Optional.ofNullable(this.type).orElse(WorkItemType.EMPTY);
    }

    public void setType(final WorkItemType type)
    {
        this.type = Optional.ofNullable(type).orElse(WorkItemType.EMPTY);
    }

    public String getDuration()
    {
        return this.duration;
    }

    public void setDuration(final String duration)
    {
        this.duration = getString(duration);
    }

    public String getDurationSaved()
    {
        return this.durationSaved;
    }

    public void setDurationSaved(final String duration)
    {
        this.durationSaved = getString(duration);
        this.duration = Constants.STRING_EMPTY;
    }

    public String getIcon()
    {
        return this.icon;
    }

    public void setIcon(final String icon)
    {
        this.icon = getString(icon);
    }

    public boolean canBeFinished()
    {
        return this.canBeFinished;
    }

    public void setCanBeFinished(final boolean canBeFinished)
    {
        this.canBeFinished = canBeFinished;
    }

    public boolean isMarked()
    {
        return this.marked;
    }

    public void setMarked(final boolean marked)
    {
        this.marked = marked;
    }

    public boolean isInProgress()
    {
        return this.inProgress;
    }

    public void setInProgress(final boolean inProgress)
    {
        this.inProgress = inProgress;
    }

    public boolean isPreventTimer()
    {
        try
        {
            return this.preventTimer;
        }
        finally
        {
            this.preventTimer = false;
        }
    }

    public void setPreventTimer(final boolean preventTimer)
    {
        Log.log(Level.FINE, String.format("preventTimer(%s)", preventTimer));
        this.preventTimer = preventTimer;
    }

    public void putAll(final Issue other)
    {
        setMarked(other.isMarked());
        setInProgress(other.isInProgress());
    }

    @Override
    public String toString()
    {
        return String.format("Issue{Id=%s, Ticket=%s, Order=%d, Label=%s, Description=%s, Type=%s, Duration=%s, Saved duration=%s, Icon=%s, Can be finished=%s, Marked=%s}",
                             getId(), getTicket(), getOrder(), getLabel(), getDescription(), getType(), getDuration(), getDurationSaved(), getIcon(), canBeFinished(), isMarked());
    }
}