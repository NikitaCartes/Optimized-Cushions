package xyz.nikitacartes.optimizedcushions.server;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;

/** Duck interface on {@code ChunkMap$TrackedEntity} (private inner class). */
public interface TrackedEntityExt {
    Entity optimizedcushions$entity();

    ServerEntity optimizedcushions$serverEntity();
}
