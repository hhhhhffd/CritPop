# Crit Pop 1.0.0

Crit Pop adds a colorful animated phrase when a melee critical hit you land is
confirmed by the Minecraft server. Phrases scale in, rise and fade, with varied
colors and positions in the upper screen. The effect never covers the crosshair.
The built-in English phrase list and the eight placement regions are shuffled
without consecutive repeats.

## Install

Install Fabric Loader 0.18.4 or later for Minecraft Java **1.21.11** and drop the
release JAR in the client’s `mods` folder. It works with the integrated server
and ordinary vanilla multiplayer. The server does not need this mod. Crit Pop
itself does not need Fabric API.

For in-game settings, install optional Mod Menu 17 and open
**Mods → Crit Pop → Configure**. You can turn off the effect, edit/add/remove
phrases, or reset the default list. Use **Done** to save. Without Mod Menu you
can edit `config/critpop.json` while Minecraft is closed:

```json
{
  "enabled": true,
  "phrases": ["CLEAN CRIT!", "LIGHTS OUT!", "POW!"]
}
```

An empty list disables phrases. Up to 64 phrases of at most 48 characters are
stored. Text remains on your client; no chat messages or custom packets are sent.

## What counts?

The client must have actually attempted to hit that living entity. Crit Pop then
requires a vanilla damage event with your player as the direct and causing
entity, the matching critical-attack sound near your position, and the game's
critical animation on the same target. It does not show for a miss, an
ordinary hit, a magic-only critical animation, or another player's damage.
It deliberately skips ambiguous or incomplete sequences. For example, modified
servers may suppress or replace vanilla effects, and invulnerability-window
damage can be applied without sending a new damage event.

## Build

Use Java 21 and run `./gradlew build` (`gradlew.bat build` on Windows).

To run the attribution and placement checks:

```sh
javac -d build/critpop-tests src/client/java/com/critpop/CritTracker.java src/client/java/com/critpop/PopPlacement.java tests/CritTrackerTest.java
java -cp build/critpop-tests CritTrackerTest
```
