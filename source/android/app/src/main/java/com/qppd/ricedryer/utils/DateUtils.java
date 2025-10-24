package com.qppd.ricedryer.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateUtils {
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }

    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    public static String formatDateTime(long timestamp) {
        return DATETIME_FORMAT.format(new Date(timestamp));
    }

    public static String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + " min ago";
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        }
    }

    public static boolean isOffline(long lastUpdate) {
        return (System.currentTimeMillis() - lastUpdate) > Constants.OFFLINE_THRESHOLD;
    }
}
