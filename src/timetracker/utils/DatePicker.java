package timetracker.utils;

import timetracker.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * DatePicker mit Eingabefeld
 */
public class DatePicker
{
    private int month = Calendar.getInstance().get(Calendar.MONTH);
    private final int year = Calendar.getInstance().get(Calendar.YEAR);

    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy");
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");
    private static final String[] HEADER = {"Mon", "Tue", "Wed", "Thur", "Fri", "Sat", "Sun"};

    private final JLabel label = new JLabel(Constants.STRING_EMPTY, SwingConstants.CENTER);
    private String day = Constants.STRING_EMPTY;
    private final JButton[] button = new JButton[49];

    public DatePicker(final JFrame parent)
    {
        final JDialog dialog = new JDialog();
        dialog.setTitle("Date Picker");
        dialog.setModal(true);
        dialog.setAlwaysOnTop(true);

        final JPanel panel = new JPanel(new GridLayout(7, 7));
        panel.setPreferredSize(new Dimension(430, 120));

        final ActionListener listener = e -> {
            final String text = ((JButton) e.getSource()).getText();
            if (!text.isEmpty())
            {
                DatePicker.this.day = text;
                dialog.dispose();
            }
        };

        for (int i = 0; i < this.button.length; i++)
        {
            final JButton btn = new JButton();
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(30,30));
            btn.setText(Constants.STRING_EMPTY);

            if (i < 7)
            {
                btn.setText(HEADER[i]);
                btn.setForeground(Color.red);
            }
            else
            {
                btn.addActionListener(listener);
            }
            this.button[i] = btn;
            panel.add(btn);
        }

        final JButton previous = new JButton("<<");
        previous.addActionListener(ae -> {
            this.month--;
            displayDate();
        });

        final JButton next = new JButton(">>");
        next.addActionListener(ae -> {
            this.month++;
            displayDate();
        });

        final JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
        controlPanel.add(previous);
        controlPanel.add(new JPanel());
        controlPanel.add(this.label);
        controlPanel.add(new JPanel());
        controlPanel.add(next);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(controlPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        displayDate();
        dialog.setVisible(true);
    }

    private void displayDate()
    {
        for (int i = 7; i < this.button.length; i++)
        {
            final JButton btn = this.button[i];
            btn.setText(Constants.STRING_EMPTY);
        }

        final Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(this.year, this.month, 1);

        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        dayOfWeek = dayOfWeek == 0 ? 7 : dayOfWeek;
        final int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 6 + dayOfWeek, d = 1; d <= daysInMonth; i++, d++)
        {
            final JButton btn = this.button[i];
            btn.setText(Constants.STRING_EMPTY + d);
        }
        this.label.setText(this.monthFormat.format(cal.getTime()));
    }

    public String getSelectedDate()
    {
        if (this.day.isEmpty())
        {
            return this.day;
        }
        final Calendar cal = Calendar.getInstance();
        cal.set(this.year, this.month, Integer.parseInt(this.day));
        return DATE_FORMAT.format(cal.getTime());
    }

    public static void main(final String[] args)
    {
        final JLabel label = new JLabel("Selected Date:");
        final JTextField text = new JTextField(20);
        final JButton b = new JButton("popup");
        final JPanel p = new JPanel();
        p.add(label);
        p.add(text);
        p.add(b);
        final JFrame f = new JFrame();
        f.getContentPane().add(p);
        EscapeEvent.add(f);
        f.pack();
        f.setVisible(true);
        b.addActionListener(ae -> text.setText(new DatePicker(f).getSelectedDate()));
    }
}