package com.alikhwa.thikrni;

import android.icu.util.Calendar;
import android.icu.util.IslamicCalendar;

import java.util.Date;

/** التاريخ الهجري (أم القرى) والميلادي بالعربية. */
public final class HijriUtil {
    private HijriUtil() {}

    private static final String[] HIJRI_MONTHS = {
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"};

    private static final String[] GREG_MONTHS = {
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};

    private static final String[] DAYS = {
            "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"};

    public static String digits(long n) {
        String s = String.valueOf(n);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') sb.append((char) ('٠' + (ch - '0')));
            else sb.append(ch);
        }
        return sb.toString();
    }

    public static String dayName() {
        Calendar c = Calendar.getInstance();
        return DAYS[c.get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static String hijri(int adjustDays) {
        IslamicCalendar ic = new IslamicCalendar();
        ic.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
        ic.setTime(new Date());
        if (adjustDays != 0) ic.add(Calendar.DATE, adjustDays);
        int d = ic.get(Calendar.DAY_OF_MONTH);
        int m = ic.get(Calendar.MONTH);
        int y = ic.get(Calendar.YEAR);
        return digits(d) + " " + HIJRI_MONTHS[Math.max(0, Math.min(11, m))] + " " + digits(y) + " هـ";
    }

    public static boolean isFriday() {
        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
    }

    public static String gregorian() {
        Calendar c = Calendar.getInstance();
        return digits(c.get(Calendar.DAY_OF_MONTH)) + " " + GREG_MONTHS[c.get(Calendar.MONTH)]
                + " " + digits(c.get(Calendar.YEAR)) + " م";
    }

    public static String time() {
        Calendar c = Calendar.getInstance();
        int h = c.get(Calendar.HOUR);
        if (h == 0) h = 12;
        int m = c.get(Calendar.MINUTE);
        int s = c.get(Calendar.SECOND);
        boolean pm = c.get(Calendar.AM_PM) == Calendar.PM;
        return digits(h) + ":" + digits(m / 10) + digits(m % 10) + ":" + digits(s / 10) + digits(s % 10)
                + (pm ? " م" : " ص");
    }
}
