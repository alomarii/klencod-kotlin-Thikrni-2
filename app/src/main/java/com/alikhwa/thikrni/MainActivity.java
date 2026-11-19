package com.alikhwa.thikrni;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int GOLD = 0xFFE6C15A;
    private static final int GOLD_DARK = 0xFFB8912B;
    private static final int SUB = 0xFFB7D3C8;
    private static final int[] CHIP_MINS = {5, 10, 15, 30, 60, 120, 180, 0};
    private static final String[] CHIP_LABELS = {"٥ دقائق", "١٠ دقائق", "١٥ دقيقة", "٣٠ دقيقة",
            "ساعة", "ساعتان", "٣ ساعات", "مخصص"};

    private Prefs p;
    private final Handler h = new Handler(Looper.getMainLooper());
    private TextView tvDay, tvHijri, tvGreg, tvTime, tvCount, tvPhrase, tvAdj;
    private static final int REQ_AUDIO = 21;
    private TextView tvAudio;
    private CounterView counter;
    private LinearLayout content;
    private final List<TextView> chips = new ArrayList<>();

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refreshDates();
            h.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = new Prefs(this);

        FrameLayout root = new FrameLayout(this);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(new StarsView(this), new FrameLayout.LayoutParams(-1, -1));

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(12), 0, dp(32));
        sv.addView(content, new ViewGroup.LayoutParams(-1, -2));
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        buildHeader();
        buildDateCard();
        buildCounterCard();
        buildSettingsCard();
        buildFooter();

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
        ReminderScheduler.syncAll(this);
        playEntrance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCount();
        h.post(tick);
    }

    @Override
    protected void onPause() {
        h.removeCallbacks(tick);
        super.onPause();
    }

    // ------------------------------------------------------------------ UI

    private void buildHeader() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, dp(18), 0, dp(6));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo_alikhwa);
        box.addView(logo, new LinearLayout.LayoutParams(dp(112), dp(112)));
        ObjectAnimator fl = ObjectAnimator.ofFloat(logo, "translationY", 0f, -dp(9));
        fl.setDuration(2300);
        fl.setRepeatMode(ValueAnimator.REVERSE);
        fl.setRepeatCount(ValueAnimator.INFINITE);
        fl.setInterpolator(new AccelerateDecelerateInterpolator());
        fl.start();
        ObjectAnimator sc = ObjectAnimator.ofPropertyValuesHolder(logo,
                android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.06f),
                android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.06f));
        sc.setDuration(2300);
        sc.setRepeatMode(ValueAnimator.REVERSE);
        sc.setRepeatCount(ValueAnimator.INFINITE);
        sc.start();

        final TextView title = text("ذكرني", 40, GOLD, true);
        title.setPadding(0, dp(10), 0, 0);
        final LinearGradient shader = new LinearGradient(0, 0, dp(260), 0,
                new int[]{GOLD_DARK, 0xFFFFF6D0, GOLD, GOLD_DARK},
                new float[]{0f, 0.35f, 0.6f, 1f}, Shader.TileMode.REPEAT);
        title.getPaint().setShader(shader);
        final Matrix m = new Matrix();
        ValueAnimator shimmer = ValueAnimator.ofFloat(0f, dp(260));
        shimmer.setDuration(2600);
        shimmer.setRepeatCount(ValueAnimator.INFINITE);
        shimmer.setInterpolator(new android.view.animation.LinearInterpolator());
        shimmer.addUpdateListener(a -> {
            m.setTranslate((float) a.getAnimatedValue(), 0);
            shader.setLocalMatrix(m);
            title.invalidate();
        });
        shimmer.start();
        box.addView(title);

        TextView sub = text("اللهم صلِّ وسلِّم على نبينا محمد", 15, SUB, false);
        box.addView(sub);
        content.addView(box, new LinearLayout.LayoutParams(-1, -2));
    }

    private void buildDateCard() {
        LinearLayout c = card();
        tvDay = text("", 16, SUB, false);
        tvHijri = text("", 28, GOLD, true);
        tvGreg = text("", 19, Color.WHITE, true);
        tvTime = text("", 15, SUB, false);
        c.addView(tvDay);
        c.addView(tvHijri);
        c.addView(divider());
        c.addView(tvGreg);
        c.addView(tvTime);

        LinearLayout adj = new LinearLayout(this);
        adj.setGravity(Gravity.CENTER);
        adj.setPadding(0, dp(10), 0, 0);
        TextView lbl = text("تعديل الهجري:", 13, SUB, false);
        TextView minus = smallBtn("−");
        TextView plus = smallBtn("+");
        tvAdj = text("", 14, Color.WHITE, true);
        tvAdj.setMinWidth(dp(60));
        minus.setOnClickListener(v -> {
            p.setHijriAdjust(p.hijriAdjust() - 1);
            refreshDates();
        });
        plus.setOnClickListener(v -> {
            p.setHijriAdjust(p.hijriAdjust() + 1);
            refreshDates();
        });
        adj.addView(lbl);
        adj.addView(space(dp(8)));
        adj.addView(plus);
        adj.addView(tvAdj);
        adj.addView(minus);
        c.addView(adj);
        content.addView(c);
        refreshDates();
    }

    private void buildCounterCard() {
        LinearLayout c = card();
        counter = new CounterView(this);
        c.addView(counter, new LinearLayout.LayoutParams(-1, dp(250)));
        tvCount = text("", 20, GOLD, true);
        c.addView(tvCount);
        tvPhrase = text("اللهم صلِّ وسلِّم وبارك على سيدنا محمد", 15, Color.WHITE, false);
        tvPhrase.setPadding(dp(6), dp(8), dp(6), dp(4));
        tvPhrase.setLineSpacing(0, 1.25f);
        c.addView(tvPhrase);
        TextView reset = text("تصفير العدّاد", 12, SUB, false);
        reset.setPadding(dp(10), dp(10), dp(10), dp(2));
        reset.setOnClickListener(v -> {
            p.setCount(0);
            refreshCount();
        });
        c.addView(reset);

        counter.setOnClickListener(v -> {
            long n = p.incCount();
            refreshCount();
            counter.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            tvCount.setScaleX(1.25f);
            tvCount.setScaleY(1.25f);
            tvCount.animate().scaleX(1f).scaleY(1f).setDuration(260).start();
            tvPhrase.setAlpha(0f);
            tvPhrase.setText(Salawat.next());
            tvPhrase.animate().alpha(1f).setDuration(450).start();
        });
        content.addView(c);
        refreshCount();
    }

    private void buildSettingsCard() {
        LinearLayout c = card();
        TextView t = text("إعدادات التذكير", 20, GOLD, true);
        t.setPadding(0, 0, 0, dp(6));
        c.addView(t);

        c.addView(switchRow("تفعيل التذكير", p.enabled(), (b) -> {
            p.setEnabled(b);
            ReminderScheduler.syncAll(this);
        }));
        c.addView(divider());

        TextView every = text("ذكّرني كل:", 15, Color.WHITE, true);
        every.setGravity(Gravity.RIGHT);
        every.setPadding(0, dp(8), 0, dp(8));
        c.addView(every);

        LinearLayout row1 = chipRow(0, 4);
        LinearLayout row2 = chipRow(4, 8);
        c.addView(row1);
        c.addView(row2);
        refreshChips();

        c.addView(divider());
        c.addView(switchRow("عند توصيل الشاحن", p.onCharge(), (b) -> {
            p.setOnCharge(b);
            ReminderScheduler.syncAll(this);
        }));
        c.addView(switchRow("عند فتح قفل الهاتف", p.onUnlock(), (b) -> {
            p.setOnUnlock(b);
            ReminderScheduler.syncAll(this);
        }));
        c.addView(switchRow("تشغيل الصوت", p.sound(), (b) -> p.setSound(b)));
        c.addView(switchRow("الاهتزاز", p.vibrate(), (b) -> p.setVibrate(b)));

        TextView test = goldButton("جرّب الصوت الآن");
        test.setOnClickListener(v -> {
            if (!SoundPlayer.hasAudio(this)) {
                Toast.makeText(this, "اختر تسجيلًا صوتيًا أولًا", Toast.LENGTH_LONG).show();
            } else {
                SoundPlayer.play(this, "test", null);
                Toast.makeText(this, "جارٍ تشغيل الصوت...", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(50));
        lp.topMargin = dp(14);
        c.addView(test, lp);

        TextView bat = outlineButton("السماح بالعمل في الخلفية (مهم)");
        bat.setOnClickListener(v -> {
            try {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } catch (Exception e) {
                try {
                    startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                } catch (Exception ignored) {
                }
            }
        });
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(-1, dp(46));
        lp2.topMargin = dp(10);
        c.addView(bat, lp2);

        tvAudio = text("", 13, SUB, false);
        tvAudio.setPadding(0, dp(12), 0, 0);
        c.addView(tvAudio);
        TextView pick = outlineButton("اختيار صوت الصلاة على النبي من هاتفك");
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("audio/*");
            startActivityForResult(i, REQ_AUDIO);
        });
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(-1, dp(46));
        lp3.topMargin = dp(8);
        c.addView(pick, lp3);
        TextView del = text("إزالة الصوت المخصص", 12, SUB, false);
        del.setPadding(dp(10), dp(10), dp(10), dp(2));
        del.setOnClickListener(v -> {
            SoundPlayer.deleteCustom(this);
            refreshAudioStatus();
        });
        c.addView(del);
        refreshAudioStatus();

        TextView hint = text("التذكير تسجيل صوتي بشري: اختر من هاتفك تسجيلًا بصوت قارئ فصيح للصلاة على النبي، وسيُشغَّل بمستوى صوت الوسائط في الوقت المحدد وعند الشحن وفتح القفل. ولضمان العمل في الخلفية اسمح للتطبيق بذلك وعطّل توفير الطاقة له، وتبقى إشعارة صغيرة صامتة تطلبها أندرويد.", 12, SUB, false);
        hint.setPadding(dp(4), dp(12), dp(4), 0);
        hint.setLineSpacing(0, 1.2f);
        c.addView(hint);
        content.addView(c);
    }

    private void buildFooter() {
        LinearLayout c = card();
        TextView hadith = text("«من صلى عليَّ صلاةً صلى الله عليه بها عشرًا»", 15, Color.WHITE, false);
        hadith.setLineSpacing(0, 1.25f);
        c.addView(hadith);
        c.addView(divider());
        c.addView(text("برمجة وتطوير", 13, SUB, false));
        TextView dev = text("محمد العمري", 24, GOLD, true);
        c.addView(dev);
        TextView phone = text("730215825", 18, Color.WHITE, true);
        phone.setPadding(dp(10), dp(6), dp(10), dp(6));
        phone.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        phone.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:730215825")));
            } catch (Exception ignored) {
            }
        });
        c.addView(phone);
        c.addView(text("الإصدار ١٫٠", 11, SUB, false));
        content.addView(c);
    }

    // ------------------------------------------------------------ updates

    private void refreshDates() {
        if (tvHijri == null) return;
        tvDay.setText("يوم " + HijriUtil.dayName());
        tvHijri.setText(HijriUtil.hijri(p.hijriAdjust()));
        tvGreg.setText(HijriUtil.gregorian());
        tvTime.setText(HijriUtil.time());
        int a = p.hijriAdjust();
        tvAdj.setText(a == 0 ? "٠" : (a > 0 ? "+" : "−") + HijriUtil.digits(Math.abs(a)));
    }

    private void refreshAudioStatus() {
        if (tvAudio == null) return;
        if (SoundPlayer.hasCustom(this)) tvAudio.setText("الصوت الحالي: التسجيل الذي اخترته ✓");
        else if (SoundPlayer.hasRaw(this)) tvAudio.setText("الصوت الحالي: التسجيل المرفق مع التطبيق ✓");
        else tvAudio.setText("لم يُحدَّد صوت بعد، اختر تسجيلًا بصوت قارئ لتسمعه عند التذكير");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_AUDIO && res == RESULT_OK && data != null && data.getData() != null) {
            boolean ok = SoundPlayer.importCustom(this, data.getData());
            Toast.makeText(this, ok ? "تم حفظ الصوت" : "تعذّر قراءة الملف", Toast.LENGTH_SHORT).show();
            refreshAudioStatus();
            if (ok) SoundPlayer.play(this, "test", null);
        }
    }

    private void refreshCount() {
        if (tvCount != null) tvCount.setText("عدد صلواتك: " + HijriUtil.digits(p.count()));
    }

    private void refreshChips() {
        int cur = p.intervalMin();
        boolean preset = false;
        for (int i = 0; i < 7; i++) if (CHIP_MINS[i] == cur) preset = true;
        for (int i = 0; i < chips.size(); i++) {
            boolean sel = (i < 7 && CHIP_MINS[i] == cur) || (i == 7 && !preset);
            styleChip(chips.get(i), sel);
            if (i == 7) {
                chips.get(i).setText(preset ? "مخصص" : "كل " + HijriUtil.digits(cur) + " د");
            }
        }
    }

    private void playEntrance() {
        for (int i = 0; i < content.getChildCount(); i++) {
            View v = content.getChildAt(i);
            v.setAlpha(0f);
            v.setTranslationY(dp(46));
            v.animate().alpha(1f).translationY(0).setStartDelay(120L * i)
                    .setDuration(650).setInterpolator(new DecelerateInterpolator(2f)).start();
        }
    }

    // ------------------------------------------------------------ builders

    private interface OnToggle { void on(boolean b); }

    private LinearLayout switchRow(String label, boolean value, OnToggle cb) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(8), 0, dp(8));
        TextView l = text(label, 16, Color.WHITE, false);
        l.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        r.addView(l, new LinearLayout.LayoutParams(0, -2, 1f));
        Switch s = new Switch(this);
        s.setChecked(value);
        s.setThumbTintList(new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{GOLD, 0xFFB0BEC5}));
        s.setTrackTintList(new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{0x99D4AF37, 0x55FFFFFF}));
        s.setOnCheckedChangeListener((bv, checked) -> cb.on(checked));
        r.addView(s);
        return r;
    }

    private LinearLayout chipRow(int from, int to) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = from; i < to; i++) {
            final int idx = i;
            TextView c = text(CHIP_LABELS[i], 13, Color.WHITE, true);
            c.setPadding(dp(4), dp(10), dp(4), dp(10));
            c.setOnClickListener(v -> onChip(idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            r.addView(c, lp);
            chips.add(c);
        }
        return r;
    }

    private void onChip(int idx) {
        if (CHIP_MINS[idx] > 0) {
            p.setIntervalMin(CHIP_MINS[idx]);
            ReminderScheduler.syncAll(this);
            refreshChips();
            return;
        }
        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setHint("عدد الدقائق");
        et.setGravity(Gravity.CENTER);
        new AlertDialog.Builder(this)
                .setTitle("ذكّرني كل كم دقيقة؟")
                .setView(et)
                .setPositiveButton("حفظ", (d, w) -> {
                    try {
                        int v = Integer.parseInt(et.getText().toString().trim());
                        if (v < 1) v = 1;
                        if (v > 1440) v = 1440;
                        p.setIntervalMin(v);
                        ReminderScheduler.syncAll(this);
                        refreshChips();
                    } catch (Exception ignored) {
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void styleChip(TextView c, boolean sel) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                sel ? new int[]{0xFFFFE9A0, GOLD_DARK} : new int[]{0x22FFFFFF, 0x11FFFFFF});
        g.setCornerRadius(dp(14));
        if (!sel) g.setStroke(dp(1), 0x44E6C15A);
        c.setBackground(g);
        c.setTextColor(sel ? 0xFF06281D : Color.WHITE);
    }

    private TextView goldButton(String s) {
        TextView t = text(s, 17, 0xFF06281D, true);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{GOLD_DARK, 0xFFFFE9A0, GOLD_DARK});
        g.setCornerRadius(dp(16));
        t.setBackground(g);
        t.setGravity(Gravity.CENTER);
        pressEffect(t);
        return t;
    }

    private TextView outlineButton(String s) {
        TextView t = text(s, 14, GOLD, true);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(16));
        g.setColor(0x11FFFFFF);
        g.setStroke(dp(1), 0x88E6C15A);
        t.setBackground(g);
        t.setGravity(Gravity.CENTER);
        pressEffect(t);
        return t;
    }

    private TextView smallBtn(String s) {
        TextView t = text(s, 20, GOLD, true);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(0x22FFFFFF);
        g.setStroke(dp(1), 0x66E6C15A);
        t.setBackground(g);
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(34), dp(34)));
        pressEffect(t);
        return t;
    }

    private void pressEffect(final View v) {
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(160).start();
                    break;
            }
            return false;
        });
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x30FFFFFF, 0x14FFFFFF});
        g.setCornerRadius(dp(28));
        g.setStroke(dp(1), 0x55E6C15A);
        l.setBackground(g);
        l.setPadding(dp(18), dp(18), dp(18), dp(18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(16), dp(8), dp(16), dp(8));
        l.setLayoutParams(lp);
        return l;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(0x33E6C15A);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.setMargins(0, dp(10), 0, dp(10));
        v.setLayoutParams(lp);
        return v;
    }

    private View space(int w) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(w, 1));
        return v;
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return t;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
