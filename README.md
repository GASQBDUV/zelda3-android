# zelda3-android

> **Which branch is which**
>
> | Branch | Target | What it is |
> |---|---|---|
> | `dual-screen` | **Android** | The APK (Android Studio / `./gradlew assembleDebug`), second screen as a second Android display. |
> | `bananaos-linux` (this one) | **Linux** (BananaOS on the Anbernic RG DS, aarch64 SDL2; also desktop Linux) | The `make zelda3` build with the second screen in the right half of one window over both panels. |

A port of Zelda3 to Android with a second screen mod for dual screen devices like the AYN Thor.

Original Repository: https://github.com/snesrev/zelda3 <br>
Based on: https://github.com/Waterdish/zelda3-android

The dual-screen mod was made with the help of Claude Code and opencode.

The bottom screen shows a live world map, a dungeon map with the rooms you visited, and a touch inventory where you can tap an item to equip it. On the title screen and during cutscenes it just shows a triforce.

The second screen graphics are made from your ROM while the game runs, so there is no extra setup and the app contains no game assets.

This branch also builds the second screen for desktop/handheld Linux (SDL2), so the same UI runs on dual-screen Linux handhelds.

![](showcase.png)

# Instructions

1. Install the APK from the releases page. Android 13 users: check the releases tab for the Android 13 version of the app.
2. Launch the app and tap "Select ROM" when asked, then pick your "Legend of Zelda, The - A Link to the Past (USA)" rom file.
3. That's it. The app extracts the game assets once and boots straight into the game. Every launch after that goes directly to the game.

The rom is only read to build the assets (zelda3_assets.dat) — it is never copied or kept, and the app ships no game assets of its own.

The game itself is controller only, but the bottom screen has full touch controls (map, inventory, equipping items).

## Settings

Common options can be changed from the settings screen on the bottom panel. Everything else is in Android/data/com.dishii.zelda3/files/zelda3.ini — edit it with a text editor.

Default settings:
- L3 Turbo button
- 16:9 aspect ratio
- Fullscreen (no android on-screen controls)

## Optional: providing zelda3_assets.dat yourself

If you'd rather extract the assets on a computer, drop your zelda3_assets.dat into Android/data/com.dishii.zelda3/files and the app will use it and skip the ROM prompt. You can create it with the manual instructions on the original repository, or on-device as follows if you don't have access to a computer:

1. Download PyDroid: https://play.google.com/store/apps/details?id=ru.iiec.pydroid3&hl=en_US. Choose to skip any options that ask for money, you can do all of the following steps without paying.
2. Open the hamburger menu at the top left of the app and select Pip.
3. Type in "Pillow" without the quotes and it will have you install the repository app from the app store.
4. Once the repository app is installed, you can install "Pillow" and "pyyaml"
5. Download the **source code** zip file for zelda3 at https://github.com/snesrev/zelda3/releases/tag/v0.3. The zip file with the exe file in it will not work.
6. Extract the zip file.
7. Place your rom file in the main zelda3 directory that you extracted, the same one as extract_assets.bat, and rename it to zelda3.sfc
8. Open PyDroid again, open the hamburger menu, and select Terminal.
9. Navigate to where you placed the rom file. (If you are unfamiliar with terminal commands, "ls" lists the folders and files and "cd Foldername" changes the directory. An example using the 0.3 release of zelda3 above would be "cd Download" "cd zelda3-0.3" "cd zelda3-0.3" or simply "cd Download/zelda3-0.3/zelda3-0.3")
10. Paste in this command `python3 assets/restool.py --extract-from-rom`
11. It should pause for a while and when it finishes you should be able to see zelda3_assets.dat in the same folder as your rom. You can go ahead and copy that to the Android/data/com.dishii.zelda3/files location.

# Building

The native code lives in `app/jni/src` and builds two ways; pick the one for your target. `second_screen.c` is the shared, platform-free core (game-state reads + art generation); each target compiles only its own frontend from `src/platform/`:

- `src/platform/android/` — JNI bridge (`second_screen_jni.c`) + no-op SDL stubs
- `src/platform/linux/` — the SDL UI (`second_screen_sdl.c`) + its generated tables

**Android:** open the project in Android Studio and build/run, or `./gradlew assembleDebug`. The NDK build (`jni/Android.mk`) compiles `src/*.c` + `src/platform/android/*.c`.

**Linux (desktop / handheld):**
```
cd app/jni/src
make zelda3          # needs SDL2 dev headers (libsdl2-dev / SDL2-devel / sdl2)
```
This compiles `src/*.c` + `src/platform/linux/*.c` into the `zelda3` binary. Enable the second screen with `ZELDA3_SECOND_SCREEN=1` (env knobs are documented at the top of `second_screen_sdl.c`).

The generated tables (`src/platform/linux/ss_sheets.h`, `ss_textures.h`) come from `app/src/main/assets/secondscreen/` and are committed, so a normal build doesn't regenerate them. If those assets change, run `python tools/secondscreen/gen_linux_tables.py` from the repo root (needs Pillow).

# Linux on BananaOS (Anbernic RG DS): one window over both panels

This branch (`bananaos-linux`) is the Linux build, not the Android app. On BananaOS the SDL KMSDRM driver only
drives one panel, so the second screen is not a second window: the game runs in a single 2048x768 window that
BananaOS's SDL puts on both panels (`SDL_BANANA_PANELS=side`, the top panel the left half, the bottom one the right)
and the UI is drawn into the right half. Taps there reach the UI as touch events of the one window.

```
ZELDA3_SECOND_SCREEN=side            # draw the UI in the right half of the game's window
ZELDA3_SS_DELAY=40                   # ms the UI thread waits between redraws (default 40)
ZELDA3_SS_DEBUG=1                    # print touches and per-frame timings
```

With `ZELDA3_SECOND_SCREEN=1` the old behaviour is unchanged (a second window on the second display).

Needed in `zelda3.ini`: `WindowSize = 2048x768`, `Fullscreen = 0`, `OutputMethod = SDL-Software`.
The UI is drawn by a thread of its own and handed over through a second buffer, so the game never waits for it
and never shows a half-drawn frame.

`bananaos/Zelda 3 (fork, 2 screens).sh` is the PortMaster-style launcher used on the device (it sets
`SDL_VIDEODRIVER=evdev`, `SDL_BANANA_PANELS=side` and switches the bottom panel's backlight on).

Cross-building for the device (aarch64, glibc 2.41): a Debian trixie chroot with `crossbuild-essential-arm64` and
`libsdl2-dev:arm64`, then `make zelda3 CC=aarch64-linux-gnu-gcc CFLAGS="-O2 -I . -I/usr/include/SDL2 -D_REENTRANT"`
in `app/jni/src`. `zelda3_assets.dat` comes from the original port.

# MSU-1 music (custom soundtracks)

To use an MSU-1 audio pack instead of the built-in music:

1. Copy the pack files (`alttp_msu-1.pcm`, `alttp_msu-2.pcm`, ...) into `Android/data/com.dishii.zelda3/files/msu/`.
2. In `Android/data/com.dishii.zelda3/files/zelda3.ini`, set `EnableMSU = true`, `MSUPath = msu/alttp_msu-`, and `AudioFreq = 44100` (use `48000` for OPUZ packs).
3. Relaunch the game.
