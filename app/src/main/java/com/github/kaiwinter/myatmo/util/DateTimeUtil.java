package com.github.kaiwinter.myatmo.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtil {
    /**
     * Converts a timestamp to a timestamp in short format, e.g. 11:10.
     *
     * @param timestamp the timestamp
     * @return the String representation
     */
    public static String getDateAsShortTimeString(long timestamp) {
        Date date = new Date(timestamp);
        DateFormat formatter = SimpleDateFormat.getTimeInstance(3);
        return formatter.format(date);
    }
}
