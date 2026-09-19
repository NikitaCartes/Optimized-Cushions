package xyz.nikitacartes.optimizedcushions.mixin.server;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xyz.nikitacartes.optimizedcushions.server.TrackedEntityExt;

// Accessor for ChunkMap.TrackedEntity's fields. Plain @Shadow fields are used nowhere here:
// the legacy Mixin AP emits no @Shadow-field refmap entries either, so field shadows of
// vanilla members break in obfuscated production exactly like method shadows do. Accessors
// map correctly (see the ServerEntityAccessor entries in the server refmap).
//
// The accessor method names must NOT match TrackedEntityExt's optimizedcushions$entity() /
// optimizedcushions$serverEntity(): both mixins apply to the same target, and same-named
// methods collapse into one self-recursive method at apply time (StackOverflow in prod).
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public interface TrackedEntityAccessor {
    @Accessor("serverEntity")
    ServerEntity optimizedcushions$accessServerEntity();

    @Accessor("entity")
    Entity optimizedcushions$accessEntity();
}
