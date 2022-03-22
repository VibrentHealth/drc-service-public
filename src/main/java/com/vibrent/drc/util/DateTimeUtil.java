package com.vibrent.drc.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    public static final String ACADIA_STANDARD_DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIMEZONE = "UTC";

    private DateTimeUtil() {

    }

    /**
     * Get timestamp at current moment
     *
     * @return -
     */
    public static long getCurrentTimestamp() {
        return DateTime.now().getMillis();
    }

    /**
     * Get timestamp from string date in any format and in any timezone
     *
     * @param dateTime Date time in string
     * @return -
     */
    public static Long getTimestampFromStringDate(String dateTime) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(ACADIA_STANDARD_DATE_FORMAT);
        DateTimeZone timezone = DateTimeZone.forID(TIMEZONE);
        Long timestamp = null;
        if (dateTime != null) {
            timestamp = formatter.withZone(timezone).parseDateTime(dateTime).getMillis();
        }
        return timestamp;
    }


}
