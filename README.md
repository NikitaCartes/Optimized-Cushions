# Optimized Cushions Backport

Multiloader backport of [Optimized Cushions](https://github.com/NikitaCartes/Optimized-Cushions) — an
add-on that optimises the cushion entity from
[leclowndu93150/Cushion-Backport](https://www.curseforge.com/minecraft/mc-mods/cushion-backport) on older
Minecraft versions. The client bakes cushions into the chunk mesh so they draw like blocks; the server
drops the per-entity tracker and tick overhead. Gameplay is unchanged.

Built on [Stonecutter](https://stonecutter.kikugie.dev/) (flat multiloader) from one shared `src/`. See
[ROADMAP.md](ROADMAP.md) for design and per-version internals.

## Supported versions

| Loader   | Minecraft | Server | Client baking | Sodium baking |
|----------|-----------|:------:|:-------------:|:-------------:|
| Fabric   | 1.20.1    |   ✓    |       ✓       | — *(fallback)* |
| Fabric   | 1.21.1    |   ✓    |       ✓       |       ✓        |
| Fabric   | 1.21.11   |   ✓    |       ✓       |       ✓        |
| Fabric   | 26.1.2    |   ✓    |       ✓       |       ✓        |
| Fabric   | 26.2      |   ✓    |       ✓       |       ✓        |
| NeoForge | 1.21.1    |   ✓    |       ✓       |       ✓        |
| NeoForge | 1.21.11   |   ✓    |       ✓       |       ✓        |
| NeoForge | 26.1.2    |   ✓    |       ✓       |       ✓        |
| NeoForge | 26.2      |   ✓    |       ✓       |       ✓        |

- **Server** — cushions with nobody sitting on them skip the entity tracker and tick-list; they snap back to the full vanilla path the moment someone sits.
- **Client baking** — cushions render baked into the chunk mesh instead of as entities.
- **Sodium baking** — under Sodium, cushions bake into Sodium's own chunk meshes rather than falling back to entity rendering. On 1.20.1 the old Sodium 0.5 has no baking hook, so cushions render as normal entities there (server + non-Sodium client baking still apply).

Planned: Forge (1.18.2–1.20.1) and legacy Fabric (1.18.2, 1.19.2) — see the ROADMAP.

> Status: builds green on all 9 nodes; in-game testing is still in progress.

## Build

`./gradlew build` builds every node; jars land in `versions/<mc>-<loader>/build/libs/`.
