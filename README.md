# Optimized Cushions

Fabric mod for Minecraft **26.3-snapshot-3** that optimizes [cushions](https://minecraft.wiki/w/Cushion) on both sides: the client renders them as part of the chunk mesh instead of as entity models, and the server strips their per-tick entity and network-tracker overhead.

## Why

Vanilla 26.3 added cushions as *entities* (`BlockAttachedEntity`). Every visible cushion pays the full per-frame entity cost: render state extraction, pose stack transforms, and re-submitting its model vertices every single frame. But a cushion is completely static — fixed position, one of 4 facings, one of 16 colors.

On the client this mod bakes each cushion's geometry into the chunk section mesh (the same way blocks render), so after a one-time section rebuild a cushion costs **zero per-frame CPU** — it is just terrain vertices. The approach mirrors what OptimizedBlockEntities / BetterBlockEntities do for chests, signs, etc.

## Server side

On cushion-heavy maps the server pays O(cushions) every tick in four places (profiled on a saturated 50 ms/tick world): the entity tracker's per-entity `sendChanges` + ticking-range lookup (~36% of the server thread), a full visibility re-check of every tracked entity on **every player movement packet** (~25%), the entity tick list machinery (~10%), and the natural-spawn entity scan (~11%). The mod removes most of that:

- **Tracker tick skip** — a quiescent cushion (nothing to sync: no pending teleport/velocity/data/passenger changes) skips the per-tick ticking-range hash lookup and the guaranteed-no-op `sendChanges()`. Any state change flips one of the checked flags first, so the vanilla resync path runs exactly when something actually needs sending.
- **Movement-packet throttle** — cushion visibility for a player is only re-evaluated after the player moves ≥1 block or once a second, instead of on every movement packet (vanilla re-checks every tracked entity even for look-around packets). Spawn/despawn boundary error is under one block against a ≥32-block radius; new/teleported players and entity (re)spawns still evaluate immediately through the vanilla paths.
- **Dedicated cushion ticker** — passenger-free cushions leave the vanilla entity tick list (despawn checks, per-entity ticking-range lookups, profiler scopes, vehicle checks) and are ticked by a flat loop that calls the same `Cushion.tick()`. The moment passengers are involved (someone sits, or `/ride` puts the cushion on something) it moves back to the vanilla list, so `tickPassenger`/`rideTick` semantics are vanilla-exact on the exact tick.

The natural-spawn scan is left untouched.

## What is preserved

- **All functionality** — the entity is untouched: sitting, breaking, picking, hitbox, sounds, particles, support-pop and fluid checks all work.
- **Vanilla compatibility** — no protocol or save changes. The client half works on vanilla servers; the server half serves vanilla clients (it only skips sending packets that vanilla state says would be empty).
- **Visuals** — geometry is captured from the real `CushionModel` with the exact `CushionRenderer` transforms; lighting replicates the entity pipeline (flat lightmap at the entity light probe + the entity shader's two-light diffuse per face, including the Nether variant). Light updates re-mesh sections, so baked light stays in sync exactly like terrain does.

Cushions fall back to the vanilla entity renderer whenever they are glowing (outline), on fire, or invisible; name tags, flames and F3+B hitboxes always render through the vanilla path. Add/remove/teleport/dye changes are detected each client tick and trigger a rebuild of the affected section (~ cost of placing one block).

## Known trade-offs

- **Sodium**: disabled when Sodium is present (Sodium replaces the vanilla section compiler); cushions simply render as vanilla entities there.
- Cushion textures are added to the block atlas, so they mipmap at distance like blocks (vanilla entity textures don't). Resource pack cushion textures are picked up automatically.
- Baked cushions stay visible up to full render distance instead of the vanilla ~entity render distance, and a cushion teleported off block-center may pop at screen edges (section-bounds culling).
- Server: for a cushion in a loaded but not entity-ticking chunk the support/fluid poll is deferred instead of frozen (vanilla freezes the whole tick there), and periodic zero-delta keep-alive move packets for static cushions are not sent. Both are unobservable in normal play; the poll cadence itself (up to ~5 s, staggered) is vanilla behaviour.
- Server: cushion spawn/despawn distance for a moving player is evaluated on a ≥1-block / 1-second grid instead of every packet — worst case a cushion appears one block of walking later than vanilla at the 160-block tracking edge.
