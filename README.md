# BatterActionBar

**English** | [简体中文](README_zh-CN.md)

A Minecraft **1.21.11** / Fabric client-side mod (mod id `batteractionbar`, package `com.jimmyxiao.batteractionbar`).

It splits the server-sent action bar from action bars drawn by other client mods, so the two no longer fight
for the same spot on screen — and it exposes a public API so other mods can add as many extra action bars as
they like, each with its own position.

## The problem

In vanilla, the action bar text sent by a server and the text other client mods draw through
`Gui#setOverlayMessage` are rendered in exactly the same place (`guiWidth / 2`, `guiHeight - 68`).
Many servers push an action bar every tick, so that spot stays occupied forever and client mods
(status displays, health readouts…) have nowhere to go.

This mod classifies the bar by **origin** and gives each origin its own coordinates:

- **Server bar** — shifted up 30 pixels by default, leaving the vanilla spot free
- **Client bar** — stays at the vanilla spot by default
- **Extra bars registered through the API** — vanilla spot by default, draggable anywhere in the settings screen

## How the origin is detected

Only two packet paths count as "server":

- `ClientboundSetActionBarTextPacket` → `ClientPacketListener#setActionBarText`
- `ClientboundSystemChatPacket(overlay = true)` → `ClientPacketListener#handleSystemChat`

Everything else (other mods calling `Gui#setOverlayMessage` directly, `LocalPlayer#displayClientMessage(…, true)`, …)
counts as "client".

The detection does not flip a boolean flag at the packet entry point. Instead the `Component` instance is recorded,
and a message is treated as server-originated only when `Gui#setOverlayMessage` receives that very same object —
so exceptions or delayed handling cannot misclassify a client message.

## How it renders

Vanilla has a single `overlayMessageString` slot: whoever calls `Gui#setOverlayMessage` last wins, so an action bar
sent by the server every tick keeps evicting the client mod's own bar (and vice versa). Changing coordinates alone
cannot fix that.

The mod therefore splits messages into two **independent channels** at the `Gui#setOverlayMessage` entry point.
Each channel keeps its own text, its own timer (60 ticks, fading out over the last 20) and its own rainbow flag;
vanilla's single-slot rendering is cancelled and the mod draws both bars at once, each at its own coordinates.
Server and client action bars can therefore be **visible at the same time** without evicting each other.

The styling matches vanilla (text backdrop, `nextStratum` layer, fade curve).
When the master switch is off, vanilla rendering is left untouched.

Extra registered bars are drawn at the TAIL of `Gui#render`, with their own timers as well.

## Public API for other mods

Entry point: `com.jimmyxiao.batteractionbar.api`. Register once during client initialization:

```java
import com.jimmyxiao.batteractionbar.api.ActionBarSlot;
import com.jimmyxiao.batteractionbar.api.BatterActionBarApi;
import net.minecraft.network.chat.Component;

// Register: id must be unique and must not be "server"/"client"; default offset is optional (GUI-scaled pixels)
ActionBarSlot statusBar = BatterActionBarApi.registerSlot(
    "mymod_status",                    // key used in the config file
    Component.literal("My status"),    // name shown in the settings screen
    0, 20                              // default offset X, Y
);

// Show
statusBar.show(Component.literal("Gathering"), 100);      // disappears after 100 ticks, fades over the last 20
statusBar.show(Component.literal("Ready"));               // 60 ticks by default
statusBar.showPersistent(Component.literal("AFK"));       // stays until clear()
statusBar.clear();

// Position
int x = statusBar.offsetX();
statusBar.setOffset(0, 40);   // set from code; persisted on the next config save
statusBar.resetOffset();      // back to the default offset given at registration

// Size (scale)
float s = statusBar.scale();  // current scale
statusBar.setScale(1.5f);     // clamped to 0.25 – 4.0
statusBar.resetScale();       // back to the default scale given at registration
```

## Size (scaling)

Every bar can be scaled independently: 1.0× by default (vanilla size), adjustable from 0.25× to 4.0×.
Scaling happens around the anchor `(guiWidth / 2, guiHeight - 68)`, i.e. the bar grows and shrinks in place,
and the backdrop scales with it.

- Settings screen: **scroll the mouse wheel** over any bar to resize it, 0.1× per notch
- Right-click that bar: position and size are reset together
- "Reset all positions and sizes": resets every bar at once

A default scale can also be given at registration:

```java
// id, display name, default X, default Y, default scale
ActionBarSlot statusBar = BatterActionBarApi.registerSlot(
    "mymod_status", Component.literal("My status"), 0, 20, 1.2f);
```

- The bar shows up automatically in the settings screen; players can drag and resize it, and its position and
  size are stored per id, without clashing with the two built-in bars
- Registering the same id again returns the existing bar (idempotent), which helps with hot reloading
- All extra bars are cleared when the client disconnects from a server
- When the master switch is off, extra bars are not drawn either

## Settings screen

- With **Mod Menu** installed: open it from the mod list ("Settings")
- Without Mod Menu: bind a key to "Open action bar position settings" in Options → Controls → Key Binds
  (unbound by default)

Each bar has a one-line preview: **left-drag to move**, **scroll to resize**, **right-click to reset that bar
(position + size)**; changes are saved on release.
There are also toggles for "Enable this mod", "Hide the server action bar", and a "Reset all positions and sizes" button.
Every label shows X / Y / current multiplier (e.g. `1.25x`) live.
A horizontal guide line marks the real vanilla action bar position for comparison.

## Config

File: `.minecraft/config/batteractionbar.json`

```json
{
  "enabled": true,
  "hideServer": false,
  "bars": {
    "server": { "x": 0, "y": -30, "scale": 1.0 },
    "client": { "x": 0, "y": 0, "scale": 1.0 },
    "mymod_status": { "x": 0, "y": 20, "scale": 1.2 }
  }
}
```

`x` / `y` are in GUI-scaled pixels relative to the vanilla action bar anchor `(guiWidth / 2, guiHeight - 68)`.
`scale` is the multiplier, 0.25 – 4.0; missing or ≤ 0 falls back to 1.0, so older configs keep working.
With `hideServer` enabled the server bar is not drawn at all (its messages still expire normally).

## Building

Requires JDK 21 (`build.bat` pins `C:\Program Files\Java\jdk-21.0.12.1`):

```
.\build.bat          # same as: gradlew build --offline
.\run.bat            # launch a dev client
```

Output: `build/libs/batteractionbar-1.0.0.jar`.

Dependencies: Fabric Loader ≥ 0.19.5, Fabric API, Java 21; Mod Menu is optional (settings entry point only).
Other mods that want to use this API can add the jar above as a `compileOnly` dependency.

## Download

Grab the latest jar from the [Releases](https://github.com/JimmyXiao2026/BatterActionBar/releases) page.
