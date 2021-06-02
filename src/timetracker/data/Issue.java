package timetracker.data;

import timetracker.db.Backend;

import java.util.Map;
import java.util.Optional;

/**
 * Repräsentiert ein Ticket
 */
public class Issue
{
    private final int id;
    private String ticket;
    private String label;
    private Type type;
    private String duration;
    private String durationSaved;
    private String icon;
    private final boolean deletable;
    private boolean marked;

    public Issue(final String ticket, final String label, final Type type, final String duration, final String durationSaved, final String icon,
                 final boolean deletable, final boolean marked)
    {
        this(-1, ticket, label, type, duration, durationSaved, icon, deletable, marked);
    }

    public Issue(final int id, final String ticket, final String label, final Type type, final String duration, final String durationSaved, final String icon,
                 final boolean deletable, final boolean marked)
    {
        this.id = id;
        this.ticket = ticket;
        this.label = label;
        this.type = type;
        this.duration = duration;
        this.durationSaved = durationSaved;
        this.icon = icon;
        this.deletable = deletable;
        this.marked = marked;
    }

    public Issue(final Map<String, String> data)
    {
        this.id = Optional.ofNullable(data.get(Backend.CN_ID)).map(Integer::parseInt).orElse(-1);
        this.ticket = data.get(Backend.CN_ISSUE);
        this.label = data.get(Backend.CN_LABEL);
        this.type = Type.getType(data.get(Backend.CN_TYPE));
        this.duration = data.get(Backend.CN_DURATION);
        this.durationSaved = data.get(Backend.CN_DURATION_SAVED);
        this.icon = data.get(Backend.CN_ICON);
        this.deletable = Boolean.parseBoolean(data.get(Backend.CN_DELETABLE));
        this.marked = Boolean.parseBoolean(data.get(Backend.CN_MARKED));
    }

    public int getId()
    {
        return this.id;
    }

    public String getTicket()
    {
        return this.ticket;
    }

    public void setTicket(final String ticket)
    {
        this.ticket = ticket;
    }

    public String getLabel()
    {
        return this.label;
    }

    public void setLabel(final String label)
    {
        this.label = label;
    }

    public Type getType()
    {
        return this.type;
    }

    public void setType(final Type type)
    {
        this.type = type;
    }

    public String getDuration()
    {
        return this.duration;
    }

    public void setDuration(final String duration)
    {
        this.duration = duration;
    }

    public String getDurationSaved()
    {
        return this.durationSaved;
    }

    public void setDurationSaved(final String duration)
    {
        this.durationSaved = duration;
    }

    public String getIcon()
    {
        return this.icon;
    }

    public void setIcon(final String icon)
    {
        this.icon = icon;
    }

    public boolean isDeletable()
    {
        return this.deletable;
    }

    public boolean isMarked()
    {
        return this.marked;
    }

    public void setMarked(final boolean marked)
    {
        this.marked = marked;
    }

    @Override
    public String toString()
    {
        return String.format("Issue{Id=%d, Ticket=%s, Label=%s, Type=%s, Duration=%s, Saved duration=%s, Icon=%s, Deletable=%s, Marked=%s}",
                             getId(), getTicket(), getLabel(), getType(), getDuration(), getDurationSaved(), getIcon(), isDeletable(), isMarked());
    }
}