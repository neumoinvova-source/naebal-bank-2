package com.naebal.bank;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Vibrator;
import android.os.Build;
import android.os.VibrationEffect;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.media.AudioManager;
import android.media.ToneGenerator;
import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends Activity {
    private NfcAdapter adapter;
    private LinearLayout root;
    private EditText amountInput;
    private TextView status;
    private boolean waiting = false;
    private String currentAmount = "0";

    private final int bg = Color.rgb(12, 17, 28);
    private final int panel = Color.rgb(24, 32, 48);
    private final int green = Color.rgb(63, 224, 157);
    private final int muted = Color.rgb(157, 170, 190);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        adapter = NfcAdapter.getDefaultAdapter(this);
        showEntry();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(8, 8, 8, 8);
        return t;
    }

    private void base(String subtitle) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(26, 24, 26, 24);
        root.setBackgroundColor(bg);
        setContentView(root);
        root.addView(label("НАЕБАЛ БАНК", 27, Color.WHITE, true));
        root.addView(label("NFC • ШУТОЧНЫЙ СИМУЛЯТОР", 11, muted, true));
        TextView line = label(subtitle, 13, muted, false);
        line.setPadding(8, 22, 8, 12);
        root.addView(line);
    }

    private Button actionButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.rgb(8, 24, 20));
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setBackground(shape(green, 28));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 58);
        p.setMargins(0, 18, 0, 10);
        b.setLayoutParams(p);
        return b;
    }

    private void showEntry() {
        waiting = false;
        base("Введите сумму вымышленной операции");
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18, 18, 18, 18);
        card.setBackground(shape(panel, 28));
        root.addView(card, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        card.addView(label("СУММА, ₽", 12, muted, true));
        amountInput = new EditText(this);
        amountInput.setSingleLine(true);
        amountInput.setTextColor(Color.WHITE);
        amountInput.setHintTextColor(muted);
        amountInput.setHint("Например, 1500");
        amountInput.setTextSize(30);
        amountInput.setGravity(Gravity.CENTER);
        amountInput.setInputType(8194);
        card.addView(amountInput);

        Button confirm = actionButton("✓  ПОДТВЕРДИТЬ");
        root.addView(confirm);
        status = label("Терминал готов к шуточной демонстрации", 13, muted, false);
        root.addView(status);

        confirm.setOnClickListener(v -> {
            String raw = amountInput.getText().toString().trim().replace(',', '.');
            try {
                double n = Double.parseDouble(raw);
                if (!Double.isFinite(n) || n <= 0 || n > 999999999) throw new Exception();
                currentAmount = NumberFormat.getNumberInstance(new Locale("ru", "RU")).format(n);
                showWaiting();
            } catch (Exception ex) {
                status.setText("Введите корректную сумму");
            }
        });

        root.addView(label("ДЕМОНСТРАЦИЯ • НЕ ПРИНИМАЕТ И НЕ ПЕРЕДАЁТ ДЕНЬГИ",
            10, muted, false));
    }

    private void showWaiting() {
        waiting = true;
        base("ОЖИДАНИЕ NFC");
        root.addView(label(currentAmount + " ₽", 42, Color.WHITE, true));
        TextView symbol = label("◉", 88, green, true);
        root.addView(symbol);
        Animation pulse = new AlphaAnimation(0.35f, 1f);
        pulse.setDuration(850);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        symbol.startAnimation(pulse);

        root.addView(label("ПРИЛОЖИТЕ NFC-МЕТКУ", 19, Color.WHITE, true));
        root.addView(label("Телефон обнаружит только совместимые NFC-метки/устройства",
            12, muted, false));
        Button cancel = actionButton("Отмена");
        root.addView(cancel);
        cancel.setOnClickListener(v -> showEntry());

        if (adapter == null) {
            root.addView(label("В этом телефоне NFC не обнаружен", 12,
                Color.rgb(255, 190, 90), true));
        } else if (!adapter.isEnabled()) {
            root.addView(label("Включите NFC в настройках телефона", 12,
                Color.rgb(255, 190, 90), true));
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (adapter != null && adapter.isEnabled()) {
            try {
                adapter.enableReaderMode(this, this::onTagDetected,
                    NfcAdapter.FLAG_READER_NFC_A |
                    NfcAdapter.FLAG_READER_NFC_B |
                    NfcAdapter.FLAG_READER_NFC_F |
                    NfcAdapter.FLAG_READER_NFC_V,
                    null);
            } catch (Exception ignored) { }
        }
    }

    private void onTagDetected(Tag tag) {
        runOnUiThread(() -> {
            if (!waiting) return;
            waiting = false;
            try {
                // Генерируем стандартный системный звуковой пик высокого тона
                ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                toneG.startTone(ToneGenerator.TONE_CDMA_PIP, 150);

                Vibrator vib = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                if (vib != null && vib.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= 26)
                        vib.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE));
                    else
                        vib.vibrate(180);
                }
            } catch (Exception ignored) { }
            showSuccess();
        });
    }

    private void showSuccess() {
        base("РЕЗУЛЬТАТ СЧИТЫВАНИЯ");
        root.addView(label("✓", 108, green, true));
        root.addView(label("NFC ОБНАРУЖЕН", 25, green, true));
        root.addView(label(currentAmount + " ₽", 35, Color.WHITE, true));
        root.addView(label("Это демонстрация обнаружения NFC.\nПлатёж не совершался.",
            13, muted, false));
        root.postDelayed(this::showEntry, 2400);
    }

    @Override protected void onPause() {
        super.onPause();
        if (adapter != null) {
            try { adapter.disableReaderMode(this); } catch (Exception ignored) { }
        }
    }
}
