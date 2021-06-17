package timetracker.icons;

/**
 * Icons
 */
public enum Icon
{
    ADD("add.png"),
    BURN("burn.png"),
    CALENDAR("calendar.png"),
    CLIPBOARD("clipboard.png"),
    CLOCK("clock.png"),
    COPY("copy.png"),
    EDIT("edit.png"),
    FINISH("ok.png"),
    LOG("log.png"),
    MEETING("meeting.png"),
    OPEN("link.png"),
    PAUSE("pause.png"),
    PHONE("phone.png"),
    PROGRESS("progress.png"),
    REMOVE("delete_grey.png"),
    REMOVE_ICON("removeicon.png"),
    STAR("star.png"),
    UNSTAR("unstar.png"),
    STOP("stop.png"),
    SUPPORT("support.png"),
    TIMETRACKER("timetracker.png");

    private final String png;
    Icon(final String icon)
    {
        this.png = icon;
    }

    public String getIcon()
    {
        return "icons//" + this.png;
    }
}