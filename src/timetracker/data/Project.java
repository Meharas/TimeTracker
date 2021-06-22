package timetracker.data;

import timetracker.client.Client;
import timetracker.utils.Util;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Repräsentiert ein Projekt
 */
public class Project implements Serializable
{
    private static final List<Project> PROJECTS = new LinkedList<>();
    static
    {
        try
        {
            final List<Map<String, String>> projects = Client.getProjects();
            for(final Map<String, String> data : projects)
            {
                PROJECTS.add(new Project(data));
            }
        }
        catch (final Exception e)
        {
            Util.handleException(e);
        }
    }

    private String id;
    private String name;
    private String shortName;
    private String ringId;
    private boolean isDemo;
    private boolean archived;
    private boolean template;
    private boolean pinned;

    public Project(final Map<String, String> data)
    {
        for(final Map.Entry<String, String> entry : data.entrySet())
        {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if("id".equalsIgnoreCase(key))
            {
                this.id = value;
            }
            else if("name".equalsIgnoreCase(key))
            {
                this.name = value;
            }
            else if("shortName".equalsIgnoreCase(key))
            {
                this.shortName = value;
            }
            else if("ringId".equalsIgnoreCase(key))
            {
                this.ringId = value;
            }
            else if("demo".equalsIgnoreCase(key))
            {
                this.isDemo = Boolean.parseBoolean(value);
            }
            else if("archived".equalsIgnoreCase(key))
            {
                this.archived = Boolean.parseBoolean(value);
            }
            else if("template".equalsIgnoreCase(key))
            {
                this.template = Boolean.parseBoolean(value);
            }
            else if("pinned".equalsIgnoreCase(key))
            {
                this.pinned = Boolean.parseBoolean(value);
            }
        }
    }

    public Project(final String id, final String name, final String shortName, final String ringId, final boolean isDemo, final boolean archived,
                   final boolean template, final boolean pinned)
    {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.ringId = ringId;
        this.isDemo = isDemo;
        this.archived = archived;
        this.template = template;
        this.pinned = pinned;
    }

    public String getId()
    {
        return this.id;
    }

    public void setId(final String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public String getShortName()
    {
        return this.shortName;
    }

    public void setShortName(final String shortName)
    {
        this.shortName = shortName;
    }

    public String getRingId()
    {
        return this.ringId;
    }

    public void setRingId(final String ringId)
    {
        this.ringId = ringId;
    }

    public boolean isDemo()
    {
        return this.isDemo;
    }

    public void setDemo(final boolean demo)
    {
        isDemo = demo;
    }

    public boolean isArchived()
    {
        return this.archived;
    }

    public void setArchived(final boolean archived)
    {
        this.archived = archived;
    }

    public boolean isTemplate()
    {
        return this.template;
    }

    public void setTemplate(final boolean template)
    {
        this.template = template;
    }

    public boolean isPinned()
    {
        return this.pinned;
    }

    public void setPinned(final boolean pinned)
    {
        this.pinned = pinned;
    }

    public static List<Project> getProjects()
    {
        return PROJECTS;
    }

    @Override
    public String toString()
    {
        return String.format("Project[Id=%s,name=%s,shortName=%s,ringId=%s,demo=%s,archived=%s,template=%s,pinned=%s]",
                             getId(), getName(), getShortName(), getRingId(), isDemo(), isArchived(), isTemplate(), isPinned());
    }
}