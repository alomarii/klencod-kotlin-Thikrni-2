package com.alikhwa.thikrni;

import android.content.Context;
import android.content.SharedPreferences;

/** تخزين إعدادات التطبيق. */
public class Prefs {
    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("thikrni", Context.MODE_PRIVATE);
    }

    public boolean enabled() { return sp.getBoolean("enabled", true); }
    public void setEnabled(boolean v) { sp.edit().putBoolean("enabled", v).apply(); }

    public int intervalMin() { return Math.max(1, sp.getInt("interval", 30)); }
    public void setIntervalMin(int v) { sp.edit().putInt("interval", Math.max(1, v)).apply(); }

    public boolean onCharge() { return sp.getBoolean("onCharge", true); }
    public void setOnCharge(boolean v) { sp.edit().putBoolean("onCharge", v).apply(); }

    public boolean onUnlock() { return sp.getBoolean("onUnlock", true); }
    public void setOnUnlock(boolean v) { sp.edit().putBoolean("onUnlock", v).apply(); }

    public boolean sound() { return sp.getBoolean("sound", true); }
    public void setSound(boolean v) { sp.edit().putBoolean("sound", v).apply(); }

    public boolean vibrate() { return sp.getBoolean("vibrate", true); }
    public void setVibrate(boolean v) { sp.edit().putBoolean("vibrate", v).apply(); }

    public long count() { return sp.getLong("count", 0); }
    public void setCount(long v) { sp.edit().putLong("count", Math.max(0, v)).apply(); }
    public long incCount() {
        long v = count() + 1;
        setCount(v);
        return v;
    }

    public int hijriAdjust() { return sp.getInt("hijriAdj", 0); }
    public void setHijriAdjust(int v) { sp.edit().putInt("hijriAdj", Math.max(-3, Math.min(3, v))).apply(); }
}
