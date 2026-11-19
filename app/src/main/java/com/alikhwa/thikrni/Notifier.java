package com.alikhwa.thikrni;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/** التذكير صوتي فقط (بدون إشعار). هذا الصنف يبقي قناة إشعار الخدمة الصامتة المطلوبة من أندرويد. */
public final class Notifier {
    private Notifier() {}

    public static final int ID_REMINDER = 1001;
    public static final int ID_SERVICE = 1002;
    public static final String CH_SERVICE = "thikrni_service";

    public static void ensureServiceChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            NotificationChannel ch = new NotificationChannel(CH_SERVICE, "التشغيل في الخلفية",
                    NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            ch.setSound(null, null);
            nm.createNotificationChannel(ch);
        }
    }

    /** تشغيل تذكير صوتي. */
    public static void show(Context c, String reason) {
        SoundPlayer.play(c, reason, null);
    }
}
