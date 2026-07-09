package xyz.nikitacartes.optimizedcushions;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Util;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.LightLayer;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Captures the vanilla cushion entity model geometry (with the exact PoseStack transforms
 * {@code CushionRenderer.submit} applies) once per model bake, and emits it into chunk
 * section buffers with entity-style lighting: a flat lightmap sampled at the entity's
 * light probe position and the fixed two-directional diffuse the entity shader would apply.
 */
public final class CushionBaker {
    private record QuadTemplate(Vector3f[] positions, float[] u, float[] v, Direction face) {
    }

    private record CaptureSet(EntityModelSet source, Map<Direction, List<QuadTemplate>> byDirection) {
    }

    // Vanilla's diffuse light directions entity shader's mix-light formula per world-space face.
    private static final float[] DIFFUSE_DEFAULT = diffuseByFace(Lighting.DIFFUSE_LIGHT_0, Lighting.DIFFUSE_LIGHT_1);
    private static final float[] DIFFUSE_NETHER = diffuseByFace(Lighting.NETHER_DIFFUSE_LIGHT_0, Lighting.NETHER_DIFFUSE_LIGHT_1);
    private static final EnumMap<DyeColor, Identifier> SPRITE_IDS = Util.make(new EnumMap<>(DyeColor.class), sprites -> {
        for (DyeColor color : DyeColor.values()) {
            sprites.put(color, Identifier.withDefaultNamespace("entity/cushion/" + color.getName() + "_cushion"));
        }
    });

    private static volatile CaptureSet captured;

    private CushionBaker() {
    }

    /** Called on section meshing worker threads. */
    public static void emit(final VertexConsumer buffer, final CushionTracker.Snapshot cushion, final SectionPos sectionPos, final RenderSectionRegion region) {
        List<QuadTemplate> quads = templates(cushion.dir());
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(SPRITE_IDS.get(cushion.color()));
        int light = LightCoordsUtil.pack(
            region.getBrightness(LightLayer.BLOCK, cushion.lightPos()),
            region.getBrightness(LightLayer.SKY, cushion.lightPos())
        );
        float[] diffuse = CardinalLighting.NETHER.equals(region.cardinalLighting()) ? DIFFUSE_NETHER : DIFFUSE_DEFAULT;
        float offsetX = (float)(cushion.x() - sectionPos.minBlockX());
        float offsetY = (float)(cushion.y() - sectionPos.minBlockY());
        float offsetZ = (float)(cushion.z() - sectionPos.minBlockZ());
        BakedQuad.MaterialInfo materialInfo = new BakedQuad.MaterialInfo(sprite, ChunkSectionLayer.CUTOUT, null, null, null, -1, null, 0);
        QuadInstance instance = new QuadInstance();
        instance.setLightCoords(light);

        for (QuadTemplate template : quads) {
            instance.setColor(ARGB.gray(diffuse[template.face().get3DDataValue()]));
            BakedQuad quad = new BakedQuad(
                template.positions()[0],
                template.positions()[1],
                template.positions()[2],
                template.positions()[3],
                UVPair.pack(sprite.getU(template.u()[0]), sprite.getV(template.v()[0])),
                UVPair.pack(sprite.getU(template.u()[1]), sprite.getV(template.v()[1])),
                UVPair.pack(sprite.getU(template.u()[2]), sprite.getV(template.v()[2])),
                UVPair.pack(sprite.getU(template.u()[3]), sprite.getV(template.v()[3])),
                template.face(),
                materialInfo
            );
            buffer.putBlockBakedQuad(offsetX, offsetY, offsetZ, quad, instance);
        }
    }

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

    /** Replays the transforms of CushionRenderer.submit for each horizontal facing. */
    private static Map<Direction, List<QuadTemplate>> capture(final EntityModelSet models) {
        ModelPart root = models.bakeLayer(ModelLayers.CUSHION);
        Map<Direction, List<QuadTemplate>> byDirection = new EnumMap<>(Direction.class);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            List<QuadTemplate> quads = new ArrayList<>();
            PoseStack poseStack = new PoseStack();
            poseStack.mulPose(Axis.YP.rotationDegrees(direction.toYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.translate(0.0F, -0.25F, 0.0F);
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

    /** Entity shader diffuse: min(1, 0.4 + 0.6 * (max(0, L0·N) + max(0, L1·N))) per axis face. */
    private static float[] diffuseByFace(final Vector3fc light0, final Vector3fc light1) {
        float[] byFace = new float[6];
        for (Direction direction : Direction.values()) {
            Vector3f normal = new Vector3f(direction.getUnitVec3f());
            float accum = Math.max(0.0F, light0.dot(normal)) + Math.max(0.0F, light1.dot(normal));
            byFace[direction.get3DDataValue()] = Math.min(1.0F, 0.4F + 0.6F * accum);
        }

        return byFace;
    }
}
