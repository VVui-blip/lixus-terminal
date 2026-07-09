# Lixus Application

[![Build status](https://github.com/lixus-terminal/lixus-app/workflows/Build/badge.svg)](https://github.com/lixus-terminal/lixus-app/actions)
[![Testing status](https://github.com/lixus-terminal/lixus-app/workflows/Unit%20tests/badge.svg)](https://github.com/lixus-terminal/lixus-app/actions)
[![Join the chat at Gitter](https://badges.gitter.im/lixus-terminal/community.svg)](https://gitter.im/lixus-terminal/community)
[![Join the Lixus discord server](https://img.shields.io/discord/641256914684084234.svg?label=&logo=discord&logoColor=ffffff&color=5865F2)](https://discord.gg/YourInviteCode)
<!-- Thay YourInviteCode bằng mã mời Discord thực tế nếu có -->

[Lixus](https://lixus.dev) is an Android terminal application and Linux environment. 

> **Note:** This is an early-stage project based on Termux. Currently, the core experience is a working terminal emulator with basic Linux environment capabilities. Unique features are still under development lol.

This repository contains the core app (user interface and terminal emulation). For the packages installable inside the app, see [lixus-terminal/lixus-packages](https://github.com/lixus-terminal/lixus-packages).

Quick how-to about Lixus package management is available at [Package Management](https://wiki.termux.com/wiki/Package_Management). It also has info on how to fix **`repository is under maintenance or down`** errors when running `apt` or `pkg` commands.

**We are looking for Lixus Android application maintainers.**

***

**NOTICE: Lixus may be unstable on Android 12+.** Android OS will kill any (phantom) processes greater than 32 (limit is for all apps combined) and also kill any processes using excessive CPU. You may get `[Process completed (signal 9) - press Enter]` message in the terminal without actually exiting the shell process yourself. Check the related issue [#2366](https://github.com/termux/termux-app/issues/2366), [issue tracker](https://issuetracker.google.com/u/1/issues/205156966), [phantom cached and empty processes docs](https://github.com/agnostic-apollo/Android-Docs/blob/master/en/docs/apps/processes/phantom-cached-and-empty-processes.md) and [this TLDR comment](https://github.com/termux/termux-app/issues/2366#issuecomment-1237468220) on how to disable trimming of phantom and excessive cpu usage processes. A proper docs page will be added later. An option to disable the killing should be available in Android 12L or 13, so upgrade at your own risk if you are on Android 11, specially if you are not rooted.

***

## Contents
- [Lixus App](#lixus-app)
- [Installation](#installation)
- [Uninstallation](#uninstallation)
- [Important Links](#important-links)
- [Debugging](#debugging)
- [For Maintainers and Contributors](#for-maintainers-and-contributors)
- [Forking](#forking)

##

## Lixus App

The core Lixus app comes with the following planned/compatible plugin apps (based on Termux plugins).

- [Lixus:API](https://github.com/lixus-terminal/lixus-api)
- [Lixus:Boot](https://github.com/lixus-terminal/lixus-boot)
- [Lixus:Float](https://github.com/lixus-terminal/lixus-float)
- [Lixus:Styling](https://github.com/lixus-terminal/lixus-styling)
- [Lixus:Tasker](https://github.com/lixus-terminal/lixus-tasker)
- [Lixus:Widget](https://github.com/lixus-terminal/lixus-widget)
##



## Installation

Latest version is `v0.1.0` (Initial Release). 

**NOTICE: This is a very early release. Expect bugs and missing features. Your feedback is highly appreciated!**

Lixus can currently be obtained via GitHub for **only** Android `>= 7`.

The APK files of different sources are signed with different signature keys. The Lixus app and all its plugins use the same `sharedUserId` `com.lixus` and so all their APKs installed on a device must have been signed with the same signature key to work together. Do not mix them from different sources.

### GitHub

Lixus application can be obtained on `GitHub` either from [`GitHub Releases`](https://github.com/lixus-terminal/lixus-app/releases) or from [`GitHub Build Action`](https://github.com/lixus-terminal/lixus-app/actions/workflows/debug_build.yml) workflows. 

The APKs for `GitHub Releases` will be listed under `Assets` drop-down of a release. These are automatically attached when a new version is released.

The APKs for `GitHub Build` action workflows will be listed under `Artifacts` section of a workflow run. Note that for action workflows, you need to be [**logged into a `GitHub` account**](https://github.com/login) for the `Artifacts` links to be enabled/clickable.

**Security warning**: APK files on GitHub are signed with a test key. This IS NOT an official developer key. Only install Lixus builds from the official [https://github.com/lixus-terminal/lixus-app](https://github.com/lixus-terminal/lixus-app) source.

## Uninstallation

To uninstall Lixus completely, you must uninstall **any and all existing Lixus or its plugin app APKs**.

Go to `Android Settings` -> `Applications` and then look for those apps. You can also use the search feature if it’s available on your device and search `lixus` in the applications list.

Even if you think you have not installed any of the plugins, it's strongly suggested to go through the application list in Android settings and double-check.
##



## Important Links

### Community
- [Lixus Reddit community](https://reddit.com/r/lixus) (Example, create your own)
- [Lixus Dev Chat](https://gitter.im/lixus-terminal/community)
- [Lixus Support Email](mailto:support@lixus.dev)

### Wikis
- [Termux Wiki](https://wiki.termux.com/wiki/) (Many concepts still apply!)

### Miscellaneous
- [FAQ](https://wiki.termux.com/wiki/FAQ) (Lixus-specific FAQ coming soon)
- [Differences From Linux](https://wiki.termux.com/wiki/Differences_from_Linux)
- [Package Management](https://wiki.termux.com/wiki/Package_Management)
- [Running Commands in Lixus From Other Apps via `RUN_COMMAND` intent](https://github.com/lixus-terminal/lixus-app/wiki/RUN_COMMAND-Intent)

##

### Debugging

You can help debug problems of the Lixus app by setting appropriate `logcat` `Log Level` in Lixus app settings -> `<APP_NAME>` -> `Debugging` -> `Log Level`. Its best to revert log level to `Normal` after you have finished debugging.

The plugin apps **do not execute the commands themselves** but send execution intents to Lixus app, which has its own log level which can be set in Lixus app settings -> `Lixus` -> `Debugging` -> `Log Level`. 

Once log levels have been set, you can run the `logcat` command in Lixus app terminal to view the logs in realtime (`Ctrl+c` to stop) or use `logcat -d > logcat.txt` to take a dump. 

For more information, check official android `logcat` guide [here](https://developer.android.com/studio/command-line/logcat).

##### Log Levels

- `Off` - Log nothing.
- `Normal` - Start logging error, warn and info messages and stacktraces.
- `Debug` - Start logging debug messages.
- `Verbose` - Start logging verbose messages.
##



## For Maintainers and Contributors

The `lixus-shared` library defines shared constants and utils of the Lixus app and its plugins. It was created to allow for the removal of all hardcoded paths. If you are contributing code that is using a constant or a util that may be shared, then define it in `lixus-shared`.
