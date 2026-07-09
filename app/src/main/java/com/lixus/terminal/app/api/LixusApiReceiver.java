package com.lixus.terminal.app.api;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lixus.terminal.R;
import com.termux.shared.logger.Logger;
import com.termux.shared.notification.NotificationUtils;
import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Built-in equivalent of the separate "Termux:API" plugin app.
 *
 * Stock Termux:API works by having a small native helper binary (termux-api) open two
 * anonymous sockets and pass their addresses to a broadcast receiver in a *different* app
 * (com.termux.api), which must be signed with the same key as Termux itself.
 *
 * Since Lixus Terminal merges everything into one app/process, we don't need the socket
 * handshake or a second signed APK: shell scripts just call `am broadcast` against the
 * actions below, and "get"-style commands (that need to return a value) write their result
 * to a JSON file under ~/.termux/lixus-api/out/ which the calling script reads back.
 *
 * See lixus-api-scripts/ in the repo root for the matching termux-* wrapper scripts.
 * To add a new command: add an action to AndroidManifest.xml's LixusApiReceiver
 * <intent-filter>, add a case below, and drop a matching wrapper script in lixus-api-scripts/.
 */
public class LixusApiReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "LixusApiReceiver";

    private static final String ACTION_VIBRATE = TermuxConstants.TERMUX_PACKAGE_NAME + ".api.VIBRATE";
    private static final String ACTION_TOAST = TermuxConstants.TERMUX_PACKAGE_NAME + ".api.TOAST";
    private static final String ACTION_TORCH = TermuxConstants.TERMUX_PACKAGE_NAME + ".api.TORCH";
    private static final String ACTION_CLIPBOARD_GET = TermuxConstants.TERMUX_PACKAGE_NAME + ".api.CLIPBOARD_GET";
    private static final String ACTION_CLIPBOARD_SET = TermuxConstants.TERMUX_PACKAGE_NAME + ".api.CLIPBOARD_SET";
    private static final String ACTION_BATTERY_STATUS = TermuxConstants.TERMUX_PACKAGE_NAME + ".api.BATTERY_STATUS";
    private static final String ACTION_NOTIFICATION = TermuxConstants.TERMUX_PACKAGE_NAME + ".api.NOTIFICATION";

    /** Where "get"-style commands write their JSON result for the calling script to read back. */
    private static final String OUT_DIR = TermuxConstants.TERMUX_HOME_DIR_PATH + "/.termux/lixus-api/out";

    @Override
    public void onReceive(@NonNull Context context, @Nullable Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        Logger.logDebug(LOG_TAG, "Action received: " + action);

        try {
            switch (action) {
                case ACTION_VIBRATE:
                    handleVibrate(context, intent);
                    break;
                case ACTION_TOAST:
                    handleToast(context, intent);
                    break;
                case ACTION_TORCH:
                    handleTorch(context, intent);
                    break;
                case ACTION_CLIPBOARD_GET:
                    handleClipboardGet(context);
                    break;
                case ACTION_CLIPBOARD_SET:
                    handleClipboardSet(context, intent);
                    break;
                case ACTION_BATTERY_STATUS:
                    handleBatteryStatus(context);
                    break;
                case ACTION_NOTIFICATION:
                    handleNotification(context, intent);
                    break;
                default:
                    Logger.logWarn(LOG_TAG, "Unknown Lixus API action: " + action);
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Error handling " + action + ": " + e.getMessage());
        }
    }

    private void handleVibrate(@NonNull Context context, @NonNull Intent intent) {
        int durationMs = intent.getIntExtra("duration_ms", 300);
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(durationMs);
        }
    }

    private void handleToast(@NonNull Context context, @NonNull Intent intent) {
        String text = intent.getStringExtra("text");
        if (text == null || text.isEmpty()) return;
        boolean isLong = intent.getBooleanExtra("long", false);
        new Handler(Looper.getMainLooper()).post(() ->
            Toast.makeText(context, text, isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show());
    }

    private void handleTorch(@NonNull Context context, @NonNull Intent intent) {
        boolean enable = intent.getBooleanExtra("enabled", false);
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null) return;
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                Boolean hasFlash = cameraManager.getCameraCharacteristics(cameraId)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (hasFlash != null && hasFlash) {
                    cameraManager.setTorchMode(cameraId, enable);
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Logger.logError(LOG_TAG, "Torch access failed: " + e.getMessage());
        }
    }

    private void handleClipboardGet(@NonNull Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        String text = "";
        if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null
            && clipboard.getPrimaryClip().getItemCount() > 0) {
            CharSequence item = clipboard.getPrimaryClip().getItemAt(0).getText();
            text = item != null ? item.toString() : "";
        }
        // Note: Android 10+ blocks background apps from reading the clipboard in most cases;
        // this will return empty unless Lixus Terminal is the foreground/default input app.
        writeJsonResult("clipboard_get", "{\"clipboard\":" + jsonString(text) + "}");
    }

    private void handleClipboardSet(@NonNull Context context, @NonNull Intent intent) {
        String text = intent.getStringExtra("text");
        if (text == null) text = "";
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("lixus-api", text));
        }
    }

    private void handleBatteryStatus(@NonNull Context context) {
        Intent batteryIntent = context.registerReceiver(null, new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        String json = "{}";
        if (batteryIntent != null) {
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int temp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            int pct = scale > 0 ? Math.round(100f * level / scale) : -1;
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
            json = "{\"percentage\":" + pct + ",\"temperature\":" + (temp / 10.0)
                + ",\"plugged\":" + charging + "}";
        }
        writeJsonResult("battery_status", json);
    }

    private void handleNotification(@NonNull Context context, @NonNull Intent intent) {
        String title = intent.getStringExtra("title");
        String content = intent.getStringExtra("content");
        if (content == null) content = "";
        if (title == null) title = "Lixus Terminal";

        NotificationUtils.setupNotificationChannel(context, "lixus_api_notifications",
            "Lixus API Notifications", NotificationManager.IMPORTANCE_DEFAULT);
        Notification.Builder builder = NotificationUtils.geNotificationBuilder(context,
            "lixus_api_notifications", Notification.PRIORITY_DEFAULT, title, content,
            null, null, NotificationUtils.NOTIFICATION_MODE_SILENT);
        if (builder == null) return;
        builder.setSmallIcon(R.drawable.ic_service_notification);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify((int) System.currentTimeMillis(), builder.build());
    }

    /** Writes a "get"-style command's result to a JSON file for the caller script to read back. */
    private void writeJsonResult(String method, String json) {
        try {
            File dir = new File(OUT_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                Logger.logError(LOG_TAG, "Could not create output dir: " + OUT_DIR);
                return;
            }
            File outFile = new File(dir, method + ".json");
            try (FileWriter writer = new FileWriter(outFile, false)) {
                writer.write(json);
            }
        } catch (IOException e) {
            Logger.logError(LOG_TAG, "Failed writing result for " + method + ": " + e.getMessage());
        }
    }

    private String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "") + "\"";
    }

}
