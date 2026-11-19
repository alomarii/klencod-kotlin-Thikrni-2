package com.alikhwa.thikrni;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** جدولة التذكير الدوري وتشغيل خدمة الشاحن/فتح القفل. */
public final class ReminderScheduler {
    private ReminderScheduler() {}

    private static PendingIntent pending(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction(ReminderReceiver.ACTION_ALARM);
        return PendingIntent.getBroadcast(c, 100, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void schedule(Context c) {
        Context app = c.getApplicationContext();
        Prefs p = new Prefs(app);
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pending(app));
        if (!p.enabled()) return;
        long at = System.currentTimeMillis() + p.intervalMin() * 60_000L;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(app));
    }

    public static void cancel(Context c) {
        Context app = c.getApplicationContext();
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pending(app));
    }

    /** يطبّق الإعدادات الحالية: المنبّه الدوري + الخدمة. */
    public static void syncAll(Context c) {
        Context app = c.getApplicationContext();
        Prefs p = new Prefs(app);
        schedule(app);
        Intent svc = new Intent(app, ReminderService.class);
        if (p.enabled()) {
            try {
                if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(svc);
                else app.startService(svc);
            } catch (Exception ignored) {
            }
        } else {
            app.stopService(svc);
        }
    }
}
