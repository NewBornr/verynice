# MyClient 0.3.0 for Minecraft 26.2 (Fabric)

Minecraft 26.2 is unobfuscated, so this project uses Mojang's own class names (no Yarn),
Java 25, Gradle 9.5.1, Fabric Loom 1.17 (`net.fabricmc.fabric-loom`), Fabric Loader 0.19.5 and Fabric API 0.161.0+26.2.
The older 1.21.4 version is in `myclient-1.21.4.zip`.

## Easiest way to get the jar: GitHub builds it for you
1. Make a free GitHub repository and upload the contents of this folder (including the hidden `.github` folder).
2. Open the repository's Actions tab, run "build" (it also runs on upload).
3. When it finishes, download "myclient-jar" under Artifacts. The jar is inside.
If the build fails, copy the error lines from the Actions log and send them over.

## Build on your own PC
1. Install JDK 25 and Gradle 9.5.1 (or generate the wrapper once: `gradle wrapper --gradle-version 9.5.1`).
2. `./gradlew build` (Windows: `gradlew.bat build`)
3. Jar: `build/libs/myclient-0.3.0.jar` (not the `-sources` one).

## Install
Fabric Loader for 26.2 + Fabric API 0.161.0+26.2 + this jar in `.minecraft/mods`.

## Controls
- Right Shift: open/close the menu
- V: Freecam, B: Base Finder, G: Donut Wheel (rebind in the menu: open a module, use the Bind row)
- Settings and keybinds save to `.minecraft/config/myclient.json` when you close the menu

## Modules
Visual: Freecam, Block ESP
Donut: Ancient Debris Finder, Auto Base Hunt, Auto Dig Down, Auto Elytra Fly, Auto Rtp, Auto Sell,
       Base ESP, Base Finder, Donut Wheel, Farm FPS Optimizer

## Known risk areas (first build)
Not compiled by the author. Most likely to need a one-line fix after a build error:
  render/Render3D.java, mixin/*.java, util/Mc.java, module/modules/AutoSell.java (inventory click call)
