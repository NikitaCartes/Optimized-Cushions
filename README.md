# Optimised Cushions

Client-side Fabric mod for Minecraft **26.3-snapshot-3** that renders [cushions](https://minecraft.wiki/w/Cushion) as part of the chunk mesh instead of as full entity models.

## Why

Vanilla 26.3 added cushions as *entities* (`BlockAttachedEntity`). Every visible cushion pays the full per-frame entity cost: render state extraction, pose stack transforms, and re-submitting its model vertices every single frame. But a cushion is completely static — fixed position, one of 4 facings, one of 16 colors.

This mod bakes each cushion's geometry into the chunk section mesh (the same way blocks render), so after a one-time section rebuild a cushion costs **zero per-frame CPU** — it is just terrain vertices. The approach mirrors what OptimisedBlockEntities / BetterBlockEntities do for chests, signs, etc.

## What is preserved

- **All functionality** — the entity is untouched: sitting, breaking, picking, hitbox, sounds, particles all work. Only the rendering path changes.
- **Vanilla compatibility** — purely client-side; works on vanilla servers, no protocol or save changes.
- **Visuals** — geometry is captured from the real `CushionModel` with the exact `CushionRenderer` transforms; lighting replicates the entity pipeline (flat lightmap at the entity light probe + the entity shader's two-light diffuse per face, including the Nether variant). Light updates re-mesh sections, so baked light stays in sync exactly like terrain does.

Cushions fall back to the vanilla entity renderer whenever they are glowing (outline), on fire, or invisible; name tags, flames and F3+B hitboxes always render through the vanilla path. Add/remove/teleport/dye changes are detected each client tick and trigger a rebuild of the affected section (~ cost of placing one block).

## Known trade-offs

- **Sodium**: disabled when Sodium is present (Sodium replaces the vanilla section compiler); cushions simply render as vanilla entities there.
- Cushion textures are added to the block atlas, so they mipmap at distance like blocks (vanilla entity textures don't). Resource pack cushion textures are picked up automatically.
- Baked cushions stay visible up to full render distance instead of the vanilla ~entity render distance, and a cushion teleported off block-center may pop at screen edges (section-bounds culling).
