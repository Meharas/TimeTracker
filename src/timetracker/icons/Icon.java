package timetracker.icons;

/**
 * Icons
 */
public enum Icon
{
    ADD("add.png"),
    STOP("stop.png"),
    LOG("log.png"),
    REMOVE("delete_grey.png"),
    BURN("burn.png"),
    OPEN("link.png"),
    COPY("copy.png"),
    EDIT("edit.png"),
    PROGRESS("progress.png"),
    FINISH("ok.png"),
    STAR("star.png"),
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