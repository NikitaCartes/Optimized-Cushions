//? if <1.21.11 {
/*package xyz.nikitacartes.optimizedcushions.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Pre-1.21.11 ModelPart.Cube keeps its polygon array private (it became a public record component
// later). The baker needs the raw quads, so widen the field with an accessor.
@Mixin(ModelPart.Cube.class)
public interface ModelPartCubeAccessor {
    @Accessor("polygons")
    ModelPart.Polygon[] optimizedcushions$polygons();
}
*///?}
