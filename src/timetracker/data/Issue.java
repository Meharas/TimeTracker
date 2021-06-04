package timetracker.data;

import timetracker.Constants;
import timetracker.db.Backend;

import java.util.Map;
import java.util.Optional;

/**
 * Repräsentiert ein Ticket
 */
public class Issue
{
    private String id;
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
        this(Constants.STRING_EMPTY, ticket, label, type, duration, durationSaved, icon, deletable, marked);
    }

    public Issue(final String id, final String ticket, final String label, final Type type, final String duration, final String durationSaved, final String icon,
                 final boolean deletable, final boolean marked)
    {
        this.id = id;
        this.ticket = getString(ticket);
        this.label = getString(label);
        this.type = type;
        this.duration = getString(duration);
        this.durationSaved = getString(durationSaved);
        this.icon = getString(icon);
        this.deletable = deletable;
        this.marked = marked;
    }

    public Issue(final Map<String, String> data)
    {
        this.id = getString(data.get(Backend.CN_ID));
        this.ticket = getString(data.get(Backend.CN_ISSUE));
        this.label = getString(data.get(Backend.CN_LABEL));
        this.type = Type.getType(data.get(Backend.CN_TYPE));
        this.duration = getString(data.get(Backend.CN_DURATION));
        this.durationSaved = getString(data.get(Backend.CN_DURATION_SAVED));
        this.icon = getString(data.get(Backend.CN_ICON));
        this.deletable = Boolean.parseBoolean(data.get(Backend.CN_DELETABLE));
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

    public Type getType()
    {
        return this.type;
    }

    public void setType(final Type type)
    {
        this.type = Optional.ofNullable(type).orElse(Type.EMPTY);
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
    }

    public String getIcon()
    {
        return this.icon;
    }

    public void setIcon(final String icon)
    {
        this.icon = getString(icon);
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