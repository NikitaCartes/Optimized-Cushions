package xyz.nikitacartes.optimizedcushions.server;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;

/** Duck interface on either backport cushion: where the server side currently ticks it. */
public interface CushionServerExt {
    /** True for either backport cushion type (both use path {@code cushion}). */
    static boolean isCushionType(final EntityType<?> type) {
        // var covers ResourceLocation/Identifier across versions.
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null || !id.getPath().equals("cushion")) {
            return false;
        }
        String namespace = id.getNamespace();
        return namespace.equals("cushionbackport") || namespace.equals("minecraft");
    }
    /** True between onTickingStart and onTickingEnd on the server. */
    boolean optimizedcushions$isServerTicking();

    void optimizedcushions$setServerTicking(boolean ticking);

    /** True while owned by {@link CushionServerTicker} instead of the vanilla entity tick list. */
    boolean optimizedcushions$isInTicker();

    void optimizedcushions$setInTicker(boolean inTicker);

    /** Forwards to the backport entity's dye colour (no compile dependency on it). */
    DyeColor optimizedcushions$color();

    void optimizedcushions$setColor(DyeColor color);
}
