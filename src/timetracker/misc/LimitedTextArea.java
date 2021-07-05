package timetracker.misc;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * Zeichenbegrenzte Textarea
 */
public class LimitedTextArea extends PlainDocument
{
    public LimitedTextArea()
    {
    }

    public LimitedTextArea(final Content c)
    {
        super(c);
    }

    @Override
    public void insertString(final int offset, final String str, final AttributeSet attr) throws BadLocationException
    {
        if (str == null)
        {
            return;
        }

        final int currentLength = getLength();
        if ((currentLength + str.length()) <= 255)
        {
            super.insertString(offset, str, attr);
        }
        else
        {
            final int max = 255 - currentLength;
            if(max > 0)
            {
                super.insertString(offset, str.substring(0, max), attr);
            }
        }
    }
}