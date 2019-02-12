/**
 * Icons
 *
 * @author $Author: beyera $ © forcont business technology gmbh 2001-2019
 * @version $Revision: 1.0 $$Date: 01.02.2019 14:39 $
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
    STAR("star.png");

    private String png;
    Icon(final String icon)
    {
        this.png = icon;
    }

    String getIcon()
    {
        return "icons//" + this.png;
    }
}
