package com.lixus.terminal.app.boot;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.lixus.terminal.app.TermuxService;
import com.termux.shared.file.FileUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.shell.command.ExecutionCommand.Runner;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;
import com.termux.shared.termux.file.TermuxFileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Built-in equivalent of the separate "Termux:Boot" plugin app.
 *
 * On stock Termux, running scripts on device boot requires installing a second app
 * (com.termux.boot) that listens for BOOT_COMPLETED and fires an intent back into Termux.
 * Since Lixus Terminal is a single merged app, we skip the inter-app dance entirely and
 * run the scripts directly against our own {@link TermuxService}.
 *
 * Scripts live in ~/.termux/boot/ and are executed in alphabetical order, each as a
 * background (APP_SHELL) command, mirroring termux-boot's behaviour.
 */
public class BootScriptRunner {

    private static final String LOG_TAG = "BootScriptRunner";

    /** Delay between launching consecutive boot scripts, to avoid flooding the service. */
    private static final long INTER_SCRIPT_DELAY_MS = 1000;

    public static String getBootScriptsDirectoryPath() {
        return TermuxConstants.TERMUX_HOME_DIR_PATH + "/.termux/boot";
    }

    public static synchronized void runBootScripts(@NonNull Context context) {
        String bootDirPath = getBootScriptsDirectoryPath();
        File bootDir = new File(bootDirPath);

        if (!bootDir.isDirectory()) {
            Logger.logVerbose(LOG_TAG, "Boot scripts directory does not exist, nothing to run: " + bootDirPath);
            return;
        }

        File[] files = bootDir.listFiles();
        if (files == null || files.length == 0) {
            Logger.logVerbose(LOG_TAG, "Boot scripts directory is empty: " + bootDirPath);
            return;
        }

        // Run in stable alphabetical order, same convention as termux-boot (e.g. 01-wifi.sh, 02-sshd.sh)
        Arrays.sort(files, Comparator.comparing(File::getName));

        Handler handler = new Handler(Looper.getMainLooper());
        long delay = 0;
        int scheduled = 0;

        for (File script : files) {
            if (!script.isFile()) continue;

            // Only run files that are already marked executable, same requirement as RUN_COMMAND.
            if (!script.canRead() || !script.canExecute()) {
                Logger.logWarn(LOG_TAG, "Skipping non-executable boot script: " + script.getAbsolutePath()
                    + " (run: chmod +x \"" + script.getAbsolutePath() + "\")");
                continue;
            }

            final String scriptPath = TermuxFileUtils.getCanonicalPath(script.getAbsolutePath(), null, true);
            final long thisDelay = delay;
            handler.postDelayed(() -> launchBootScript(context, scriptPath), thisDelay);
            delay += INTER_SCRIPT_DELAY_MS;
            scheduled++;
        }

        Logger.logInfo(LOG_TAG, "Scheduled " + scheduled + " boot script(s) from " + bootDirPath);
    }

    private static void launchBootScript(@NonNull Context context, @NonNull String scriptPath) {
        Logger.logInfo(LOG_TAG, "Running boot script: " + scriptPath);

        Uri executableUri = new Uri.Builder()
            .scheme(TERMUX_SERVICE.URI_SCHEME_SERVICE_EXECUTE)
            .path(scriptPath)
            .build();

        Intent execIntent = new Intent(TERMUX_SERVICE.ACTION_SERVICE_EXECUTE, executableUri);
        execIntent.setClass(context, TermuxService.class);
        execIntent.putExtra(TERMUX_SERVICE.EXTRA_RUNNER, Runner.APP_SHELL.getName());
        execIntent.putExtra(TERMUX_SERVICE.EXTRA_COMMAND_LABEL, "Boot script: " + new File(scriptPath).getName());

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(execIntent);
            } else {
                context.startService(execIntent);
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to run boot script \"" + scriptPath + "\": " + e.getMessage());
        }
    }

}
