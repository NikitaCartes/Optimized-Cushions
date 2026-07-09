package xyz.nikitacartes.optimisedcushions.server;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;

/** Duck interface on {@code ChunkMap$TrackedEntity} (private inner class). */
public interface TrackedEntityExt {
    Entity optimisedcushions$entity();

    ServerEntity optimisedcushions$serverEntity();
}
