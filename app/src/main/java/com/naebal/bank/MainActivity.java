
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
