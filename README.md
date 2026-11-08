# Chess Clock

A simple digital chess clock for Android: two clocks — press the clock of
the side to move to complete its move, like a real chess clock.

## Time controls

Official categories, same lineup as on chess.com, plus your own custom control:

| Category  | Presets                                  |
|-----------|------------------------------------------|
| Bullet    | 1                                        |
| Blitz     | 3 + 2, 5, 5 + 1                          |
| Rapid     | 10 + 5, 15 + 5, 15 + 10                  |
| Classical | 30, 30 + 20, 60 + 30                     |

Bonus mechanic: **increment (Fischer)** — the player who just moved gains it.
With zero increment you get a plain countdown (1 min bullet-style).

Editing any field turns the control into a custom one (preset selection is
dropped automatically). Your last setup is saved and restored.

## Behaviour

- `m:ss.t` display, driven by a main-thread ticker that measures real elapsed
  time — no drift, no lost time across pauses.
- Clock turns red below 10 s; when a clock hits zero the opponent wins.
- Pause/Resume. Restart asks for confirmation first (it stops the running
game). The settings button (gear) opens a small menu with the two in-game
  options, both remembered across launches:
  - **Layout** — toggles which color sits on top (the texts are always
    rotated for the phone lying on its side).
  - **Design** — *Default* (both boxes dark) or *Contrast* (the white box is
    white with dark text, the black box is black with light text, so each side
    reads at a glance).
- Screen stays on while the app is open. If you leave the app, the ticker is
  stopped (the clock resumes where it left off when you return).
- Zero permissions; the app is fully offline.

## Building & testing

```sh
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:testDebugUnitTest    # JVM unit tests of the clock engine
```

The clock engine (`app/src/main/java/com/barth/chessclock/model/`) is pure
Kotlin with no Android imports, so the whole time-control domain (ticking, increment, flagging) is tested
on the JVM in seconds.

The package is `com.barth.chessclock`, so the new build can be installed
side-by-side with the old one for A/B comparison.
