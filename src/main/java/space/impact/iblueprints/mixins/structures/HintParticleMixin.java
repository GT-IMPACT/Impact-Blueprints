package space.impact.iblueprints.mixins.structures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IconFlipped;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import space.impact.iblueprints.integration.structures.DoorHintFace;
import space.impact.iblueprints.integration.structures.HatchHintFace;

@Mixin(targets = "space.impact.structures.ClientProxy$HintParticleInfo", remap = false)
public abstract class HintParticleMixin {

    @Shadow
    @Final
    private IIcon[] icons;

    @Shadow
    @Final
    private int x;

    @Shadow
    @Final
    private int y;

    @Shadow
    @Final
    private int z;

    @Unique
    private HatchHintFace iblueprints$hatchFace;

    @Unique
    private DoorHintFace iblueprints$doorFace;

    @Unique
    private static final double[] IBLUEPRINTS_FACE_VERTICES = new double[6 * 5];

    @Unique
    private int iblueprints$vertexOffset;

    @Inject(method = "isInFrustrum", at = @At("HEAD"), cancellable = true)
    private void iblueprints$cullDoor(Frustrum frustum, CallbackInfoReturnable<Boolean> callback) {
        // NORTH is present on both halves, even when the adjoining horizontal face is omitted.
        if (icons != null && icons[2] instanceof DoorHintFace) {
            callback.setReturnValue(frustum.isBoxInFrustum(x, y, z, x + 1, y + 1, z + 1));
        }
    }

    @Redirect(
        method = "draw",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/IIcon;getMinU()F", remap = true)
    )
    private float iblueprints$readFaceIcon(IIcon icon) {
        iblueprints$hatchFace = icon instanceof HatchHintFace ? (HatchHintFace) icon : null;
        iblueprints$doorFace = icon instanceof DoorHintFace ? (DoorHintFace) icon : null;
        iblueprints$vertexOffset = 0;
        if (iblueprints$hatchFace != null) {
            iblueprints$markTextureUsed(iblueprints$hatchFace.getBase());
        } else if (iblueprints$doorFace != null) {
            iblueprints$markTextureUsed(iblueprints$doorFace.sprite());
        }
        return icon.getMinU();
    }

    @Redirect(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;addVertexWithUV(DDDDD)V",
            remap = true
        )
    )
    private void iblueprints$drawFaceVertex(
        Tessellator tessellator,
        double x,
        double y,
        double z,
        double u,
        double v
    ) {
        if (iblueprints$doorFace != null) {
            AxisAlignedBB bounds = iblueprints$doorFace.getBounds();
            tessellator.addVertexWithUV(
                iblueprints$doorCoordinate(x, bounds.minX, bounds.maxX),
                iblueprints$doorCoordinate(y, bounds.minY, bounds.maxY),
                iblueprints$doorCoordinate(z, bounds.minZ, bounds.maxZ),
                u,
                v
            );
            return;
        }
        tessellator.addVertexWithUV(x, y, z, u, v);
        HatchHintFace face = iblueprints$hatchFace;
        if (face == null || face.getOverlays().isEmpty()) {
            return;
        }

        double[] vertices = IBLUEPRINTS_FACE_VERTICES;
        vertices[iblueprints$vertexOffset++] = x;
        vertices[iblueprints$vertexOffset++] = y;
        vertices[iblueprints$vertexOffset++] = z;
        vertices[iblueprints$vertexOffset++] = u;
        vertices[iblueprints$vertexOffset++] = v;
        if (iblueprints$vertexOffset != vertices.length) {
            return;
        }

        // ImpactStructures emits two triangles per face, with UVs at the icon's corners.
        for (IIcon overlay : face.getOverlays()) {
            iblueprints$markTextureUsed(overlay);
            for (int i = 0; i < vertices.length; i += 5) {
                double overlayU = vertices[i + 3] == face.getMinU() ? overlay.getMinU() : overlay.getMaxU();
                double overlayV = vertices[i + 4] == face.getMinV() ? overlay.getMinV() : overlay.getMaxV();
                tessellator.addVertexWithUV(vertices[i], vertices[i + 1], vertices[i + 2], overlayU, overlayV);
            }
        }
    }

    @Unique
    private static double iblueprints$doorCoordinate(double coordinate, double min, double max) {
        int block = MathHelper.floor_double(coordinate);
        return block + (coordinate - block < 0.5 ? min : max);
    }

    @Unique
    private static void iblueprints$markTextureUsed(IIcon icon) {
        IIcon sprite = icon instanceof IconFlipped
            ? Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(icon.getIconName())
            : icon;
        HintClientProxyInvoker.iblueprints$markTextureUsed(sprite);
    }
}
