package com.alikhwa.thikrni;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * تشغيل التذكير كتسجيل صوتي بشري للصلاة على النبي (بدون صوت آلي).
 * الأولوية: ملف اختاره المستخدم من هاتفه، ثم ملف res/raw/salawat إن وُجد.
 */
public final class SoundPlayer {
    private SoundPlayer() {}

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String CUSTOM = "salawat_custom";

    // ------------------------------------------------------------ الملفات

    private static File customFile(Context c) {
        return new File(c.getApplicationContext().getFilesDir(), CUSTOM);
    }

    public static boolean hasCustom(Context c) {
        File f = customFile(c);
        return f.exists() && f.length() > 0;
    }

    private static int rawId(Context c) {
        Context app = c.getApplicationContext();
        return app.getResources().getIdentifier("salawat", "raw", app.getPackageName());
    }

    public static boolean hasRaw(Context c) {
        return rawId(c) != 0;
    }

    public static boolean hasAudio(Context c) {
        return hasCustom(c) || hasRaw(c);
    }

    /** ينسخ التسجيل الذي اختاره المستخدم إلى مخزن التطبيق. */
    public static boolean importCustom(Context c, Uri uri) {
        try (InputStream in = c.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(customFile(c))) {
            if (in == null) return false;
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void deleteCustom(Context c) {
        customFile(c).delete();
    }

    // -------------------------------------------------------------- تشغيل

    public static void play(Context c, String reason, final Runnable done) {
        final Context app = c.getApplicationContext();
        Prefs p = new Prefs(app);

        final PowerManager.WakeLock wl = ((PowerManager) app.getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "thikrni:play");
        try {
            wl.acquire(65000);
        } catch (Exception ignored) {
        }
        final AtomicBoolean finished = new AtomicBoolean(false);
        final AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        final AudioFocusRequest[] focus = new AudioFocusRequest[1];
        final Runnable finish = () -> {
            if (!finished.compareAndSet(false, true)) return;
            try {
                if (Build.VERSION.SDK_INT >= 26) {
                    if (focus[0] != null) am.abandonAudioFocusRequest(focus[0]);
                } else {
                    am.abandonAudioFocus(null);
                }
            } catch (Exception ignored) {
            }
            try {
                if (wl.isHeld()) wl.release();
            } catch (Exception ignored) {
            }
            if (done != null) done.run();
        };
        MAIN.postDelayed(finish, 60000);

        if (p.vibrate()) vibrate(app);
        if (!p.sound() || !hasAudio(app)) {
            finish.run();
            return;
        }

        final AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                focus[0] = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(attrs).build();
                am.requestAudioFocus(focus[0]);
            } else {
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            }
        } catch (Exception ignored) {
        }

        new Thread(() -> {
            try {
                MediaPlayer mp = new MediaPlayer();
                mp.setAudioAttributes(attrs);
                if (hasCustom(app)) {
                    mp.setDataSource(customFile(app).getAbsolutePath());
                } else {
                    android.content.res.AssetFileDescriptor afd = app.getResources().openRawResourceFd(rawId(app));
                    mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    afd.close();
                }
                mp.setOnCompletionListener(m -> {
                    m.release();
                    finish.run();
                });
                mp.setOnErrorListener((m, w, e) -> {
                    m.release();
                    finish.run();
                    return true;
                });
                mp.prepare();
                mp.start();
            } catch (Exception e) {
                finish.run();
            }
        }).start();
    }

    private static void vibrate(Context app) {
        try {
            Vibrator v = (Vibrator) app.getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createWaveform(new long[]{0, 200, 100, 200}, -1));
            } else {
                v.vibrate(new long[]{0, 200, 100, 200}, -1);
            }
        } catch (Exception ignored) {
        }
    }
}
