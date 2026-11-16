package xyz.nikitacartes.optimizedcushions.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import xyz.nikitacartes.optimizedcushions.CushionExt;

/**
 * Registry lookups for either backport ({@code cushionbackport:cushion} or
 * {@code minecraft:cushion}). No compile dependency on either mod.
 */
final class BackportCushionIds {
    private BackportCushionIds() {
    }

    /** Entity id for /summon and /execute selectors. */
    static String entityId() {
        return cushionNamespace() + ":cushion";
    }

    static EntityType<Entity> entityType() {
        Identifier id = Identifier.fromNamespaceAndPath(cushionNamespace(), "cushion");
        @SuppressWarnings("unchecked")
        EntityType<Entity> type = (EntityType<Entity>) BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (type == null) {
            throw new AssertionError(id + " is not registered");
        }
        return type;
    }

    static Item whiteItem() {
        Identifier id = Identifier.fromNamespaceAndPath(cushionNamespace(), "white_cushion");
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null) {
            throw new AssertionError(id + " is not registered");
        }
        return item;
    }

    static boolean isCushion(final Entity entity) {
        return entity instanceof CushionExt;
    }

    private static String cushionNamespace() {
        if (registered("cushionbackport")) {
            return "cushionbackport";
        }
        if (registered("minecraft")) {
            return "minecraft";
        }
        throw new AssertionError("no cushion entity registered (need Cushion-Backport or VanillaBackport)");
    }

    private static boolean registered(final String namespace) {
        return BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.fromNamespaceAndPath(namespace, "cushion"));
    }
}
