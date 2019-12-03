import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * Auswahldialog für Icons
 */
public class IconFileFilter extends FileFilter
{
    @Override
    public boolean accept(final File f)
    {
        if (f == null)
        {
            return false;
        }
        if (f.isDirectory())
        {
            return true;
        }
        final String fileName = f.getName();
        final int dot = fileName.lastIndexOf('.');
        if (dot < 0)
        {
            return false;
        }
        final String suffix = fileName.substring(dot);
        return ".png".equalsIgnoreCase(suffix) || ".jpg".equalsIgnoreCase(suffix) || ".jpeg".equalsIgnoreCase(suffix);
    }

    @Override
    public String getDescription()
    {
        return ".jpg, .jpeg, .png";
    }
}