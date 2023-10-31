package nz.ac.canterbury.seng302.portfolio.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
    public static final String ISO_PATTERN = "yyyy-MM-dd";

    private static final String SD_PATTERN = "dd-MMMM-yyyy";

    public static String dateToFormattedString (Date date) {
        return new SimpleDateFormat (SD_PATTERN).format(date).replace('-', ' ');
    }

    /**
     * Gets the date form of the given date string
     *
     * @param dateString the string to read as a date in format 01/Jan/2000
     * @return the given date, as a date object
     */
    public static Date stringToDate(String dateString) {
        Date date = null;
        try {
            date = new SimpleDateFormat("dd/MMM/yyyy").parse(dateString);
        } catch (Exception e) {
            System.err.println("Error parsing date: " + e.getMessage());
        }
        return date;
    }

    /**
     * Gets the date form of the given ISO date string
     *
     * @param dateString the string to read as a date in format yyyy-MM-dd
     * @return the given date, as a date object, null if failed
     */
    public static Date stringToISODate(String dateString) {
        Date date = null;
        try {
            date = new SimpleDateFormat(ISO_PATTERN).parse(dateString);
        } catch (Exception e) {
            System.err.println("Error parsing date: " + e.getMessage());
            return null;
        }
        return date;
    }

    /**
     * Gets the string form of the given date in the format 01/Jan/2000
     * @param date the date to convert
     * @return the given date, as a string in format 01/Jan/2000
     */
    public static String dateToMonthString(Date date) {
        return new SimpleDateFormat("dd/MMM/yyyy").format(date);
    }

    /**
     * Gets the string form of the given date in the ISO format yyyy-MM-dd
     * @param date the date to convert
     * @return the given date, as a string in format yyyy-MM-dd
     */
    public static String dateToISOString(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat(ISO_PATTERN);
        return formatter.format(date);
    }

    /**
     * Adds a number of days to the passed date.
     * @param date - Date object to add to
     * @param days - Number of days to add
     * @return - Updated Date object
     */
    public static Date addDaysToDate(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        cal.add(Calendar.DATE, days);

        return new Date(cal.getTimeInMillis());
    }

    /**
     * Converts a 24-hour time string into a 12-hour time string.
     * @param time 24H time string to be converted into 12H time. Expected format "HH:mm"
     * @return String representation of the 24H time, in format "hh:mm a"
     */
    public static String convertTo12HourTime (String time) {
        SimpleDateFormat formatter24H = new SimpleDateFormat ("HH:mm");
        SimpleDateFormat formatter12H = new SimpleDateFormat ("hh:mm a");

        try {
            Date timeDate = formatter24H.parse(time);
            return formatter12H.format(timeDate);
        } catch (ParseException e) {
            System.out.println("Error converting string: " + time + "to 12H time");
            return "";
        }
    }

    /**
     * Remove the hours, minute, seconds and milliseconds from a date in Java
     * @param date - Date to strip
     * @return - Stripped date
     */
    public static Date stripTimeFromDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    public static Date combineDateAndTime (Date date, String time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.split(":")[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(time.split(":")[1]));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }
}
