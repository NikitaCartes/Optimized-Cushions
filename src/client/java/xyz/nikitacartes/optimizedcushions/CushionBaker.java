//~ resource_location
package xyz.nikitacartes.optimizedcushions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import org.joml.Vector3f;
import org.joml.Vector3fc;
//? if >=26.1 {
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.LightLayer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
//?} else {
/*import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.BlockAndTintGetter;
*///?}
//? if >=1.21.11 {
import net.minecraft.data.AtlasIds;
//?} else {
/*import net.minecraft.client.renderer.texture.TextureAtlas;
import xyz.nikitacartes.optimizedcushions.mixin.ModelPartCubeAccessor;
*///?}

/**
 * Captures the Cushion-Backport cushion model geometry once per model bake, then emits it into chunk
 * section buffers with entity-style lighting. Sprites live in the {@code cushionbackport} namespace
 * and are stitched onto the block atlas by {@code assets/minecraft/atlases/blocks.json}.
 *
 * <p>Guarded on two axes: the emit path (26.1+ baked-quad pipeline vs the pre-rewrite vertex path)
 * and the model-introspection API (record ModelPart on 1.21.11+ vs field-based Cube/Polygon/Vertex).
 */
public final class CushionBaker {
    private record QuadTemplate(Vector3f[] positions, float[] u, float[] v, Direction face) {
    }

    private record CaptureSet(EntityModelSet source, Map<Direction, List<QuadTemplate>> byDirection) {
    }

    // The backport registers its cushion model geometry under this layer (CushionModelLayers.CUSHION).
    private static final ModelLayerLocation CUSHION_LAYER = new ModelLayerLocation(id("cushionbackport", "cushion"), "main");

    // Inlined from Lighting's private diffuse directions: the field type changed Vector3f->Vector3fc
    // at 26.2, so an accesswidener would need a per-version descriptor.
    private static final Vector3fc DIFFUSE_LIGHT_0 = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
    private static final Vector3fc DIFFUSE_LIGHT_1 = new Vector3f(-0.2F, 1.0F, 0.7F).normalize();
    private static final Vector3fc NETHER_DIFFUSE_LIGHT_0 = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
    private static final Vector3fc NETHER_DIFFUSE_LIGHT_1 = new Vector3f(-0.2F, -1.0F, 0.7F).normalize();
    private static final float[] DIFFUSE_DEFAULT = diffuseByFace(DIFFUSE_LIGHT_0, DIFFUSE_LIGHT_1);
    private static final float[] DIFFUSE_NETHER = diffuseByFace(NETHER_DIFFUSE_LIGHT_0, NETHER_DIFFUSE_LIGHT_1);
    private static final EnumMap<DyeColor, Identifier> SPRITE_IDS = buildSpriteIds();

    // ModelPart vertices store normalized (0..1) texture coordinates. TextureAtlasSprite.getU/getV
    // take that form from 1.21 on, but 1.20.1 takes 0..16 block-texture space and divides by 16.
    //? if =1.20.1 {
    /*private static final float UV_SCALE = 16.0F;
    *///?} else {
    private static final float UV_SCALE = 1.0F;
    //?}

    private static volatile CaptureSet captured;

    private CushionBaker() {
    }

    private static EnumMap<DyeColor, Identifier> buildSpriteIds() {
        EnumMap<DyeColor, Identifier> ids = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            ids.put(color, id("cushionbackport", "entity/cushion/" + color.getName() + "_cushion"));
        }
        return ids;
    }

    // fromNamespaceAndPath was added in 1.20.5; 1.20.1 still uses the two-arg constructor.
    private static Identifier id(final String namespace, final String path) {
        //? if =1.20.1 {
        /*return new Identifier(namespace, path);
        *///?} else {
        return Identifier.fromNamespaceAndPath(namespace, path);
        //?}
    }

    // The cushion sprites are stitched onto the block atlas; look them up there. The atlas handle
    // moved from ModelManager to a dedicated AtlasManager at 1.21.11.
    private static TextureAtlasSprite sprite(final DyeColor color) {
        //? if >=1.21.11 {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(SPRITE_IDS.get(color));
        //?} else
        /*return Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(SPRITE_IDS.get(color));*/
    }

    //? if >=26.1 {
    /** Called on section meshing worker threads. Emits via the 26.1+ baked-quad section pipeline. */
    public static void emit(final VertexConsumer buffer, final CushionTracker.Snapshot cushion, final SectionPos sectionPos, final RenderSectionRegion region) {
        List<QuadTemplate> quads = templates(cushion.dir());
        TextureAtlasSprite sprite = sprite(cushion.color());
        float offsetX = (float)(cushion.x() - sectionPos.minBlockX());
        float offsetY = (float)(cushion.y() - sectionPos.minBlockY());
        float offsetZ = (float)(cushion.z() - sectionPos.minBlockZ());
        int light = LightCoordsUtil.pack(
            region.getBrightness(LightLayer.BLOCK, cushion.lightPos()),
            region.getBrightness(LightLayer.SKY, cushion.lightPos())
        );
        float[] diffuse = CardinalLighting.NETHER.equals(region.cardinalLighting()) ? DIFFUSE_NETHER : DIFFUSE_DEFAULT;
        // shade=false: entity-style diffuse is already baked into the vertex colour, so block
        // face-shading must not be applied on top.
        BakedQuad.MaterialInfo materialInfo = new BakedQuad.MaterialInfo(sprite, ChunkSectionLayer.CUTOUT, null, -1, false, 0);
        QuadInstance instance = new QuadInstance();
        instance.setLightCoords(light);

        for (QuadTemplate template : quads) {
            instance.setColor(ARGB.gray(diffuse[template.face().get3DDataValue()]));
            BakedQuad quad = new BakedQuad(
                template.positions()[0],
                template.positions()[1],
                template.positions()[2],
                template.positions()[3],
                UVPair.pack(sprite.getU(template.u()[0] * UV_SCALE), sprite.getV(template.v()[0] * UV_SCALE)),
                UVPair.pack(sprite.getU(template.u()[1] * UV_SCALE), sprite.getV(template.v()[1] * UV_SCALE)),
                UVPair.pack(sprite.getU(template.u()[2] * UV_SCALE), sprite.getV(template.v()[2] * UV_SCALE)),
                UVPair.pack(sprite.getU(template.u()[3] * UV_SCALE), sprite.getV(template.v()[3] * UV_SCALE)),
                template.face(),
                materialInfo
            );
            buffer.putBlockBakedQuad(offsetX, offsetY, offsetZ, quad, instance);
        }
    }
    //?} else {
    /*public static void emit(final VertexConsumer buffer, final CushionTracker.Snapshot cushion, final SectionPos sectionPos, final BlockAndTintGetter region) {
        List<QuadTemplate> quads = templates(cushion.dir());
        TextureAtlasSprite sprite = sprite(cushion.color());
        float offsetX = (float)(cushion.x() - sectionPos.minBlockX());
        float offsetY = (float)(cushion.y() - sectionPos.minBlockY());
        float offsetZ = (float)(cushion.z() - sectionPos.minBlockZ());
        int light = LevelRenderer.getLightColor(region, cushion.lightPos());
        // No CardinalLighting pre-26.1; the overworld diffuse table is used everywhere (the nether
        // variant only differs on the down-face and cushions sit flush, so the difference is minor).
        float[] diffuse = DIFFUSE_DEFAULT;

        for (QuadTemplate template : quads) {
            Direction face = template.face();
            int channel = (int)(diffuse[face.get3DDataValue()] * 255.0F) & 0xFF;
            int color = 0xFF000000 | (channel << 16) | (channel << 8) | channel;
            for (int i = 0; i < 4; i++) {
                Vector3f pos = template.positions()[i];
                emitVertex(buffer, offsetX + pos.x(), offsetY + pos.y(), offsetZ + pos.z(), color, sprite.getU(template.u()[i] * UV_SCALE), sprite.getV(template.v()[i] * UV_SCALE), light, face);
            }
        }
    }
    *///?}

    //? if >=1.21.1 {
    /**
     * Sodium path: emit into Sodium's fallback chunk {@link VertexConsumer} for the CUTOUT layer.
     * Sodium derives the cull-facing and sprite from the UVs itself, so only position, colour, uv,
     * light and normal are supplied per vertex. Call shape is identical across 1.21.1..26.2; only
     * packed light differs by era.
     */
    public static void emitFallback(final VertexConsumer buffer, final CushionTracker.Snapshot cushion, final SectionPos sectionPos, final BlockAndTintGetter region) {
        List<QuadTemplate> quads = templates(cushion.dir());
        TextureAtlasSprite sprite = sprite(cushion.color());
        float offsetX = (float)(cushion.x() - sectionPos.minBlockX());
        float offsetY = (float)(cushion.y() - sectionPos.minBlockY());
        float offsetZ = (float)(cushion.z() - sectionPos.minBlockZ());
        //? if >=26.1 {
        int light = LightCoordsUtil.pack(
            region.getBrightness(LightLayer.BLOCK, cushion.lightPos()),
            region.getBrightness(LightLayer.SKY, cushion.lightPos())
        );
        //?} else {
        /*int light = LevelRenderer.getLightColor(region, cushion.lightPos());
        *///?}
        // Overworld diffuse table only; the nether variant differs only on the down-face and cushions
        // sit flush, matching emit() <26.1.
        for (QuadTemplate template : quads) {
            Direction face = template.face();
            int channel = (int)(DIFFUSE_DEFAULT[face.get3DDataValue()] * 255.0F) & 0xFF;
            int color = 0xFF000000 | (channel << 16) | (channel << 8) | channel;
            for (int i = 0; i < 4; i++) {
                Vector3f pos = template.positions()[i];
                buffer.addVertex(offsetX + pos.x(), offsetY + pos.y(), offsetZ + pos.z())
                    .setColor(color)
                    .setUv(sprite.getU(template.u()[i] * UV_SCALE), sprite.getV(template.v()[i] * UV_SCALE))
                    .setLight(light)
                    .setNormal((float)face.getStepX(), (float)face.getStepY(), (float)face.getStepZ());
            }
        }
    }
    //?}

    // The chunk BLOCK vertex layout: position, color, uv0, uv2(light), normal. The builder API was
    // rewritten to addVertex(...) at 1.21; 1.20.1 still uses the vertex()...endVertex() chain.
    //? if >=1.21 <26.1 {
    /*private static void emitVertex(final VertexConsumer buffer, final float x, final float y, final float z, final int color, final float u, final float v, final int light, final Direction face) {
        buffer.addVertex(x, y, z, color, u, v, OverlayTexture.NO_OVERLAY, light, face.getStepX(), face.getStepY(), face.getStepZ());
    }
    *///?}
    //? if <1.21 {
    /*private static void emitVertex(final VertexConsumer buffer, final float x, final float y, final float z, final int color, final float u, final float v, final int light, final Direction face) {
        buffer.vertex(x, y, z).color(color).uv(u, v).uv2(light).normal(face.getStepX(), face.getStepY(), face.getStepZ()).endVertex();
    }
    *///?}

    private static List<QuadTemplate> templates(final Direction direction) {
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        CaptureSet set = captured;
        if (set == null || set.source() != models) {
            synchronized (CushionBaker.class) {
                set = captured;
                if (set == null || set.source() != models) {
                    set = new CaptureSet(models, capture(models));
                    captured = set;
                }
            }
        }

        return set.byDirection().get(direction);
    }

    //? if >=1.21.11 {
    /** Replays the transforms of CushionRenderer for each horizontal facing (record ModelPart API). */
    private static Map<Direction, List<QuadTemplate>> capture(final EntityModelSet models) {
        ModelPart root = models.bakeLayer(CUSHION_LAYER);
        Map<Direction, List<QuadTemplate>> byDirection = new EnumMap<>(Direction.class);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            List<QuadTemplate> quads = new ArrayList<>();
            PoseStack poseStack = poseFor(direction);
            root.visit(poseStack, (pose, path, cubeIndex, cube) -> {
                for (ModelPart.Polygon polygon : cube.polygons) {
                    if (polygon.vertices().length != 4) {
                        continue;
                    }

                    Vector3f normal = pose.transformNormal(polygon.normal(), new Vector3f());
                    Direction face = Direction.getApproximateNearest(normal.x(), normal.y(), normal.z());
                    Vector3f[] positions = new Vector3f[4];
                    float[] u = new float[4];
                    float[] v = new float[4];

                    for (int i = 0; i < 4; i++) {
                        ModelPart.Vertex vertex = polygon.vertices()[i];
                        positions[i] = pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f());
                        u[i] = vertex.u();
                        v[i] = vertex.v();
                    }

                    quads.add(new QuadTemplate(positions, u, v, face));
                }
            });
            byDirection.put(direction, quads);
        }

        return byDirection;
    }
    //?} else {
    /*private static Map<Direction, List<QuadTemplate>> capture(final EntityModelSet models) {
        ModelPart root = models.bakeLayer(CUSHION_LAYER);
        Map<Direction, List<QuadTemplate>> byDirection = new EnumMap<>(Direction.class);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            List<QuadTemplate> quads = new ArrayList<>();
            PoseStack poseStack = poseFor(direction);
            root.visit(poseStack, (pose, path, cubeIndex, cube) -> {
                for (ModelPart.Polygon polygon : ((ModelPartCubeAccessor) (Object) cube).optimizedcushions$polygons()) {
                    if (polygon.vertices.length != 4) {
                        continue;
                    }

                    // transformDirection (not Pose.transformNormal, absent on 1.20.1): our pose is
                    // pure rotation, so the direction transform equals the normal transform.
                    Vector3f normal = pose.pose().transformDirection(polygon.normal, new Vector3f());
                    Direction face = Direction.getNearest(normal.x(), normal.y(), normal.z());
                    Vector3f[] positions = new Vector3f[4];
                    float[] u = new float[4];
                    float[] v = new float[4];

                    for (int i = 0; i < 4; i++) {
                        ModelPart.Vertex vertex = polygon.vertices[i];
                        // Vertex.pos is in model units; Cube.compile divides by 16 before the
                        // transform (the record's worldX/worldY/worldZ on 1.21.11+ do it instead).
                        positions[i] = pose.pose().transformPosition(vertex.pos.x() / 16.0F, vertex.pos.y() / 16.0F, vertex.pos.z() / 16.0F, new Vector3f());
                        u[i] = vertex.u;
                        v[i] = vertex.v;
                    }

                    quads.add(new QuadTemplate(positions, u, v, face));
                }
            });
            byDirection.put(direction, quads);
        }

        return byDirection;
    }
    *///?}

    /** The transform CushionRenderer applies before drawing the model, per horizontal facing. */
    private static PoseStack poseFor(final Direction direction) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(Axis.YP.rotationDegrees(direction.toYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -0.25F, 0.0F);
        return poseStack;
    }

    /** Entity shader diffuse: min(1, 0.4 + 0.6 * (max(0, L0·N) + max(0, L1·N))) per axis face. */
    private static float[] diffuseByFace(final Vector3fc light0, final Vector3fc light1) {
        float[] byFace = new float[6];
        for (Direction direction : Direction.values()) {
            //? if >=1.21.11 {
            Vector3f normal = new Vector3f(direction.getUnitVec3f());
            //?} else
            /*Vector3f normal = new Vector3f(direction.step());*/
            float accum = Math.max(0.0F, light0.dot(normal)) + Math.max(0.0F, light1.dot(normal));
            byFace[direction.get3DDataValue()] = Math.min(1.0F, 0.4F + 0.6F * accum);
        }

        return byFace;
    }
}
