# Optimized Cushions

Vanilla 26.3 added [cushions](https://minecraft.wiki/w/Cushion) as *entities*. A cushion never moves, yet it costs the same as any other entity, both on the client and on the server.  

This mod treats a cushion as what it is: a static thing. The client bakes it into the terrain so it renders like a block; the server drops the per-tick tracking and ticking overhead. 
Gameplay is unchanged: sitting, breaking, picking, sounds, particles, drops and saves are all vanilla.

## How it works

A cushion is rendered like any other block: baked straight into the chunk mesh and terrain is far cheaper to draw than entities.

On the server, cushions that have nobody sitting on them skip the entity tracker and tick-list and run through one lightweight loop instead. The moment someone sits down (or you `/ride` one) that cushion snaps back to the full vanilla path, so behavior stays exact.

## Features

- **More FPS** — cushions are free to render once baked; the boost scales with how many are on screen.
- **See them further** — baked cushions are visible to full render distance, just like blocks, instead of the shorter vanilla entity range.
- **Lighter servers** — big tick-time savings on cushion-heavy worlds (tracker, movement re-checks, and ticking all trimmed).
- **Nothing lost** — every cushion interaction works exactly as in vanilla.
- **Resource packs** — cushion textures from your resource pack are picked up automatically.

## Compatibility

- **Vanilla-safe** — no protocol or save changes. The client half works on vanilla servers, and the server half serves vanilla clients.
- **Sodium** — Client side are automatically disabled when Sodium is installed (will investigate it in the future with Sodium updates). The **server** optimizations still work.
- Cushions fall back to the vanilla renderer while glowing, on fire, or invisible, and name tags / F3+B hitboxes always render the vanilla way.

## Trade-offs

A few small, deliberate differences from vanilla — none affect gameplay (drops, riding, sounds, damage and saves are identical):

- **Client** — because cushions are baked into terrain, a change (place, break, teleport, dye) shows up on the next section rebuild, so it can flicker for a frame or two instead of updating instantly. Textures now mipmap at distance like blocks.
- **Server** — spawn/despawn for a moving player is checked on a ~1-block / 1-second grid instead of every packet, so a cushion at the far tracking edge can appear up to one block of walking later than vanilla. Support/fluid checks in unloaded-but-not-ticking chunks are deferred rather than frozen.

## Links

- [Discord](https://discord.gg/UY4nhvUzaK)
