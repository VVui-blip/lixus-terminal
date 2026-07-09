# Lixus Terminal — changes in this build

## 1. Fixed the CI build (package rename mismatch)
`app/build.gradle` had `namespace`/`applicationId` set to `com.lixus.terminal`, but every
`.java` file in the `app` module still declared `package com.termux...`. That mismatch is
why the build failed with "cannot find symbol" on `import com.termux.R;` in ~10 files.

Fixed by renaming the `app` module's package throughout:
- `com.termux.app.*` → `com.lixus.terminal.app.*`
- `com.termux.filepicker.*` → `com.lixus.terminal.filepicker.*`
- Moved the actual source directories to match (`java/com/lixus/terminal/...`)
- Fixed hardcoded string references in `AndroidManifest.xml`, `res/xml/*.xml`,
  `res/layout/activity_termux.xml`, and the test sources under `app/src/test`
- Fixed `AndroidManifest.xml`'s relative reference to `ReportActivity`, which lives in the
  separate `termux-shared` module (kept as `com.termux.shared.*` — untouched, not part of
  the conflict)

**`termux-shared`, `terminal-view`, `terminal-emulator` were left as `com.termux.*`** — they're
separate library modules with no namespace conflict, and renaming them isn't needed to fix
the build. Renaming them too is possible if you want full rebrand, but it's a much bigger
change (they're shared by other Termux plugin apps' expectations too).

## 2. Built-in Termux:Boot (no second app needed)
Stock Termux needs a separate signed "Termux:Boot" APK to run scripts on device boot.
Since everything is one app now, boot scripts run in-process:

- Drop scripts into `~/.termux/boot/` (same convention as termux-boot)
- `chmod +x` each script
- On `BOOT_COMPLETED`, `SystemEventReceiver` → `BootScriptRunner` runs them in alphabetical
  order (e.g. `01-wifi.sh`, `02-sshd.sh`), 1s apart, each as a background app-shell
- Code: `app/src/main/java/com/lixus/terminal/app/boot/BootScriptRunner.java`

## 3. Built-in Termux:API (subset, no second app needed)
Stock Termux:API is a separately-signed app talking over anonymous Unix sockets — a lot of
machinery to solve a problem that doesn't exist once it's the same process. This build adds
a `LixusApiReceiver` handling a first batch of commands via plain `am broadcast`:

| Command | Script | Notes |
|---|---|---|
| Vibrate | `termux-vibrate [-d ms]` | |
| Toast | `termux-toast [-s] "text"` | `-s` = short |
| Torch | `termux-torch on\|off` | |
| Clipboard set | `echo "text" \| termux-clipboard-set` | |
| Clipboard get | `termux-clipboard-get` | Android 10+ restricts background clipboard reads; works when Lixus Terminal is foreground |
| Battery status | `termux-battery-status` | prints JSON |
| Notification | `termux-notification -t "Title" -c "text"` | |

Wrapper scripts are in `lixus-api-scripts/` at the repo root. Install them once:
```sh
cp lixus-api-scripts/* $PREFIX/bin/ && chmod +x $PREFIX/bin/termux-*
```

"Get"-style commands (clipboard-get, battery-status) can't return a value over a fire-and-
forget broadcast, so `LixusApiReceiver` writes JSON to `~/.termux/lixus-api/out/<name>.json`
and the wrapper script polls briefly then reads it back.

**This covers 7 of the ~30+ commands the real Termux:API app has** (contacts, SMS, location,
sensors, camera capture, TTS/STT, etc. aren't implemented). The architecture is meant to be
copy-paste extensible — to add a command: add an action to the `<intent-filter>` in
`AndroidManifest.xml`, add a `case` in `LixusApiReceiver.java`, add a wrapper script.
Code: `app/src/main/java/com/lixus/terminal/app/api/LixusApiReceiver.java`

## Not done / worth knowing
- No app icon/branding changes — still uses Termux's assets
- `termux-shared` etc. still say "Termux" internally in some UI strings
- Didn't touch signing config, versionName/versionCode, or CI workflow files
