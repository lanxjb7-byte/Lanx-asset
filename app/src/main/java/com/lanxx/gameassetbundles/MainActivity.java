package com.lanxx.gameasset;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;
import rikka.shizuku.Shizuku;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout root;
    private TextView status, percent, log;
    private ProgressBar progress;
    private Button power;
    private Button shizukuButton;
    private TextView shizukuStatus;
    private static final int SHIZUKU_REQUEST_CODE = 2407;
    private boolean active = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Object fileLock = new Object();

    // File assets are bundled inside app/src/main/assets/gameassetbundles
    private final String[] assetFiles = {
        "optionalab_avatar_51",
        "optionalab_weapon_014",
        "optionalab_weapon_048",
        "optionalavatarres_commonab_shader",
        "optionalclothres_commonab_texture_42",
        "optionalclothres_commonab_texture_43",
        "optionalclothres_commonab_texture_44",
        "optionalclothres_commonab_texture_45",
        "optionalclothres_commonab_texture_46_a",
        "optionalclothres_commonab_texture_46_b"
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        updateShizukuStatus();
        Shizuku.addBinderReceivedListenerSticky(() -> runOnUiThread(this::updateShizukuStatus));
        Shizuku.addBinderDeadListener(() -> runOnUiThread(this::updateShizukuStatus));
        Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
            if (requestCode == SHIZUKU_REQUEST_CODE) {
                runOnUiThread(() -> {
                    updateShizukuStatus();
                    Toast.makeText(this,
                        grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ? "Izin Shizuku diberikan" : "Izin Shizuku ditolak",
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(22));
        root.setBackgroundColor(Color.rgb(12, 12, 16));

        TextView brand = label("LanxX", 28, Color.rgb(255, 55, 80), true);
        root.addView(brand);

        TextView heading = label("GAME ASSET", 23, Color.WHITE, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(8);
        root.addView(heading, hp);

        TextView sub = label("BUILT-IN ASSET MANAGER", 11, Color.LTGRAY, true);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(5);
        root.addView(sub, sp);

        LinearLayout shizukuPanel = new LinearLayout(this);
        shizukuPanel.setOrientation(LinearLayout.VERTICAL);
        shizukuPanel.setPadding(dp(15), dp(14), dp(15), dp(14));
        shizukuPanel.setBackgroundColor(Color.rgb(27, 27, 35));
        LinearLayout.LayoutParams shp = new LinearLayout.LayoutParams(-1, -2);
        shp.topMargin = dp(18);
        root.addView(shizukuPanel, shp);

        TextView shizukuTitle = label("SHIZUKU CONNECTION", 13, Color.WHITE, true);
        shizukuPanel.addView(shizukuTitle);
        shizukuStatus = label("Memeriksa Shizuku...", 11, Color.LTGRAY, false);
        LinearLayout.LayoutParams sst = new LinearLayout.LayoutParams(-1, -2);
        sst.topMargin = dp(7);
        shizukuPanel.addView(shizukuStatus, sst);
        shizukuButton = new Button(this);
        shizukuButton.setText("CONNECT SHIZUKU");
        shizukuButton.setAllCaps(false);
        shizukuButton.setOnClickListener(v -> connectShizuku());
        LinearLayout.LayoutParams sbp = new LinearLayout.LayoutParams(-1, dp(46));
        sbp.topMargin = dp(8);
        shizukuPanel.addView(shizukuButton, sbp);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(18), dp(28), dp(18), dp(28));
        panel.setBackgroundColor(Color.rgb(27, 27, 35));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, -2);
        pp.topMargin = dp(28);
        root.addView(panel, pp);

        status = label("POWER OFF", 20, Color.LTGRAY, true);
        panel.addView(status);

        power = new Button(this);
        power.setText("POWER ON");
        power.setTextColor(Color.WHITE);
        power.setAllCaps(false);
        power.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(220, 42, 65)));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(54));
        bp.topMargin = dp(20);
        panel.addView(power, bp);
        power.setOnClickListener(v -> {
            if (!active) startInstall();
            else stopFeature();
        });

        LinearLayout progressPanel = new LinearLayout(this);
        progressPanel.setOrientation(LinearLayout.VERTICAL);
        progressPanel.setPadding(dp(16), dp(18), dp(16), dp(18));
        progressPanel.setBackgroundColor(Color.rgb(27, 27, 35));
        LinearLayout.LayoutParams prp = new LinearLayout.LayoutParams(-1, -2);
        prp.topMargin = dp(18);
        root.addView(progressPanel, prp);

        TextView ptitle = label("ASSET EXTRACTION", 13, Color.WHITE, true);
        progressPanel.addView(ptitle);

        percent = label("0%", 13, Color.rgb(255, 70, 90), true);
        LinearLayout.LayoutParams pct = new LinearLayout.LayoutParams(-1, -2);
        pct.topMargin = dp(12);
        progressPanel.addView(percent, pct);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        LinearLayout.LayoutParams bar = new LinearLayout.LayoutParams(-1, dp(8));
        bar.topMargin = dp(8);
        progressPanel.addView(progress, bar);

        TextView destination = label("Lokasi: Android/data/com.dts.freefireth/files/contentcache/Optional/android/gameassetbundles", 11, Color.LTGRAY, false);
        LinearLayout.LayoutParams destp = new LinearLayout.LayoutParams(-1, -2);
        destp.topMargin = dp(12);
        progressPanel.addView(destination, destp);

        log = label("Siap. Tekan POWER ON untuk mengekstrak file bawaan APK.", 11, Color.LTGRAY, false);
        log.setPadding(dp(2), dp(16), dp(2), dp(4));
        root.addView(log);

        TextView note = label("Catatan: Android membatasi akses ke folder data aplikasi game lain. File diekstrak ke folder milik LanxX, bukan ke folder internal Free Fire.", 11, Color.rgb(190, 190, 200), false);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(12);
        root.addView(note, np);

        setContentView(root);
    }

    private void updateShizukuStatus() {
        if (shizukuStatus == null) return;
        if (!Shizuku.pingBinder()) {
            shizukuStatus.setText("Shizuku belum aktif. Jalankan layanan Shizuku terlebih dahulu.");
            shizukuButton.setText("CONNECT SHIZUKU");
            return;
        }
        if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            shizukuStatus.setText("Terhubung • izin Shizuku diberikan");
            shizukuButton.setText("SHIZUKU CONNECTED");
        } else {
            shizukuStatus.setText("Layanan aktif • aplikasi belum diberi izin");
            shizukuButton.setText("GRANT SHIZUKU PERMISSION");
        }
    }

    private void connectShizuku() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Aktifkan layanan Shizuku dahulu", Toast.LENGTH_LONG).show();
            updateShizukuStatus();
            return;
        }
        if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Shizuku sudah terhubung", Toast.LENGTH_SHORT).show();
            updateShizukuStatus();
        } else {
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE);
        }
    }

    private void startInstall() {
        active = true;
        power.setText("POWER OFF");
        power.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(70, 70, 80)));
        status.setText("POWER ON • EXTRACTING");
        log.setText("Memulai ekstraksi file bawaan APK...");
        new Thread(() -> {
            try {
                File outDir = new File(getExternalFilesDir(null), "gameassetbundles");
                if (!outDir.exists() && !outDir.mkdirs()) throw new Exception("Tidak bisa membuat folder tujuan.");
                for (int i = 0; i < assetFiles.length; i++) {
                    if (!active) return;
                    String name = assetFiles[i];
                    synchronized (fileLock) {
                        if (!active) return;
                        try (InputStream in = getAssets().open("gameassetbundles/" + name);
                             OutputStream out = new FileOutputStream(new File(outDir, name))) {
                            byte[] buffer = new byte[64 * 1024];
                            int count;
                            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                        }
                    }
                    final int p = (int)(((i + 1) * 100.0f) / assetFiles.length);
                    final String current = name;
                    handler.post(() -> {
                        progress.setProgress(p);
                        percent.setText(String.format(Locale.US, "%d%%", p));
                        log.setText("Berhasil mengekstrak: " + current);
                    });
                }
                handler.post(() -> {
                    status.setText("ASSETS READY");
                    log.setText("Selesai. File tersedia di folder penyimpanan");
                    Toast.makeText(this, "Ekstraksi selesai", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                handler.post(() -> {
                    status.setText("EXTRACTION ERROR");
                    log.setText("Gagal: " + e.getMessage());
                    Toast.makeText(this, "Ekstraksi gagal", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void stopFeature() {
        active = false;
        power.setEnabled(false);
        status.setText("POWER OFF • REMOVING FILES");
        log.setText("Menonaktifkan fitur dan menghapus file LanxX...");
        new Thread(() -> {
            int removed = 0;
            try {
                synchronized (fileLock) {
                    File outDir = new File(getExternalFilesDir(null), "gameassetbundles");
                    File[] files = outDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile() && file.delete()) removed++;
                        }
                    }
                    if (outDir.exists()) outDir.delete();
                }
                final int count = removed;
                handler.post(() -> {
                    power.setEnabled(true);
                    power.setText("POWER ON");
                    power.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(220, 42, 65)));
                    status.setText("POWER OFF");
                    progress.setProgress(0);
                    percent.setText("0%");
                    log.setText("OFF selesai. " + count + " file LanxX di penyimpanan aplikasi telah dihapus. Tekan ON untuk memasangnya lagi.");
                });
            } catch (Exception e) {
                handler.post(() -> {
                    power.setEnabled(true);
                    power.setText("POWER ON");
                    status.setText("POWER OFF • CLEANUP ERROR");
                    log.setText("Gagal menghapus file LanxX: " + e.getMessage());
                });
            }
        }).start();
    }
  }
                           
