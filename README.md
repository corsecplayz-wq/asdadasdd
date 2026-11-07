# Key Macro (Fabric, Minecraft 1.21.11)

Press Right Shift in game to open the menu (rebind it with the "Menu key" button
in the menu, or under Options > Controls > Key Macro).

Three macro slots. Each has:
- Enabled on/off
- Key: click the button, then press any key (or mouse button 3+). Esc clears it.
- Slots: hotbar slots (1-9) to swap through in order, e.g. `2,1,3`
- Delay (ms): time between swaps. 50 ms = 1 game tick, the fastest the game applies.
- Use item: right-click after each swap
- Loop: repeat the sequence while the key is held

Settings are saved to `config/keymacro.json` when you close the menu.
Commands: `/keymacro reload`, `/keymacro stop`.

## Build
JDK 21 + Gradle 8.10+:

    gradle wrapper
    ./gradlew build

Jar: `build/libs/keymacro-1.21.11-1.0.0.jar` (not the -sources jar). Put it in `mods/`
with Fabric Loader and Fabric API.

Many multiplayer servers prohibit input automation. Check the rules first.

## Build without installing anything
1. Create a free GitHub repo and upload everything in this folder (keep the `.github` folder).
2. Open the repo's Actions tab, wait for "Build mod" to finish (about 2-3 minutes).
3. Download the `keymacro-1.21.11-jar` artifact. It contains the ready-to-use jar.
