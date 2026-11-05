package space.impact.gui3dengine.scene

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.item.ItemStack
import net.minecraft.util.Vec3
import net.minecraftforge.client.ForgeHooksClient
import org.lwjgl.BufferUtils
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import kotlin.math.abs

/** A reusable viewport. Hosts forward clicks and scrolling only when this view consumes them. */
public class SceneView(scene: BlockScene) {

    public var scene: BlockScene = scene
        private set

    public val camera: SceneCamera = SceneCamera()
    public val slice: SceneSlice = SceneSlice()
    public var materials: List<SceneMaterial> = buildMaterials()
        private set
    public var hovered: ScenePosition? = null
        private set
    public var selected: ScenePosition? = null
        private set

    private var highlighted: Set<ScenePosition> = emptySet()

    private var renderer: RenderBlocks = RenderBlocks(scene.world)
    private var x: Int = 0
    private var y: Int = 0
    private var width: Int = 1
    private var height: Int = 1
    private var fitted: Boolean = false
    private var dragButton: Int = -1
    private var lastMouseX: Int = 0
    private var lastMouseY: Int = 0
    private var dragDistance: Int = 0

    public fun replaceScene(replacement: BlockScene) {
        val previous = scene
        scene = replacement
        renderer = RenderBlocks(replacement.world)
        materials = buildMaterials()
        hovered = null
        selected = null
        highlighted = emptySet()
        slice.showAll()
        fitted = false
        previous.close()
    }

    public fun resetCamera() {
        camera.fit(
            bounds = scene.bounds(),
            width = width,
            height = height,
        )
        selected = null
        highlighted = emptySet()
    }

    public fun toggleMaterial(material: SceneMaterial) {
        val positions = material.positions.toSet()
        highlighted = if (highlighted == positions) {
            emptySet()
        } else {
            positions
        }
        selected = null
    }

    public fun isMaterialSelected(material: SceneMaterial): Boolean {
        return highlighted.isNotEmpty() && highlighted == material.positions.toSet()
    }

    internal fun highlightedPositions(): List<ScenePosition> {
        return scene.positions().filter {
            slice.includes(it) && (it == selected || it in highlighted)
        }
    }

    public fun contains(
        mouseX: Int,
        mouseY: Int,
    ): Boolean {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height
    }

    public fun mouseClicked(
        mouseX: Int,
        mouseY: Int,
        button: Int,
    ): Boolean {
        if (!contains(
            mouseX = mouseX,
            mouseY = mouseY,
        ) || button !in 0..1) {
            return false
        }
        dragButton = button
        lastMouseX = mouseX
        lastMouseY = mouseY
        dragDistance = 0
        return true
    }

    public fun scroll(
        mouseX: Int,
        mouseY: Int,
        steps: Int,
    ): Boolean {
        if (!contains(
            mouseX = mouseX,
            mouseY = mouseY,
        )) {
            return false
        }
        camera.zoom(steps.coerceIn(
            minimumValue = -1,
            maximumValue = 1,
        ))
        return true
    }

    public fun render(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        mouseX: Int,
        mouseY: Int,
    ) {
        require(width > 0 && height > 0)
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        camera.resize(
            width = width,
            height = height,
        )
        if (!fitted) {
            resetCamera()
            fitted = true
        }
        updateDrag(
            mouseX = mouseX,
            mouseY = mouseY,
        )
        val visible = scene.positions().filter(slice::includes)
        hovered = if (contains(
            mouseX = mouseX,
            mouseY = mouseY,
        )) {
            pick(
                visible = visible,
                mouseX = mouseX,
                mouseY = mouseY,
            )
        } else {
            null
        }
        draw(visible)
    }

    private fun updateDrag(
        mouseX: Int,
        mouseY: Int,
    ) {
        if (dragButton < 0) {
            return
        }
        if (!Mouse.isButtonDown(dragButton)) {
            if (dragButton == 0 && dragDistance < 4 && contains(
                mouseX = mouseX,
                mouseY = mouseY,
            )) {
                selected = hovered
                highlighted = emptySet()
            }
            dragButton = -1
            return
        }
        val dx = mouseX - lastMouseX
        val dy = mouseY - lastMouseY
        dragDistance += abs(dx) + abs(dy)
        if (dragButton == 0) {
            camera.rotate(
                dx = dx.toDouble(),
                dy = dy.toDouble(),
            )
        } else {
            camera.pan(
                dx = dx.toDouble(),
                dy = dy.toDouble(),
            )
        }
        lastMouseX = mouseX
        lastMouseY = mouseY
    }

    private fun buildMaterials(): List<SceneMaterial> {
        val stacks = mutableListOf<ItemStack>()
        val positions = mutableListOf<MutableList<ScenePosition>>()
        for (position in scene.positions()) {
            val stack = scene.item(position) ?: continue
            var index = stacks.indexOfFirst {
                it.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(
                    it,
                    stack,
                )
            }
            if (index < 0) {
                index = stacks.size
                stacks.add(stack.copy())
                positions.add(mutableListOf())
            }
            positions[index].add(position)
        }
        return stacks.indices.map {
            SceneMaterial(
                stack = stacks[it],
                positions = positions[it].toList(),
            )
        }
    }

    private fun pick(
        visible: List<ScenePosition>,
        mouseX: Int,
        mouseY: Int,
    ): ScenePosition? {
        val near = camera.unproject(
            x = (mouseX - x).toDouble(),
            y = (mouseY - y).toDouble(),
            depth = camera.nearPlane,
        )
        val far = camera.unproject(
            x = (mouseX - x).toDouble(),
            y = (mouseY - y).toDouble(),
            depth = camera.farPlane,
        )
        val start = Vec3.createVectorHelper(
            near.x,
            near.y,
            near.z,
        )
        val end = Vec3.createVectorHelper(
            far.x,
            far.y,
            far.z,
        )
        var nearest = Double.POSITIVE_INFINITY
        var positionHit: ScenePosition? = null
        for (position in visible) {
            val block = scene.world.getBlock(
                x = position.x,
                y = position.y,
                z = position.z,
            )
            val hit = block.collisionRayTrace(
                scene.world,
                position.x,
                position.y,
                position.z,
                start,
                end,
            ) ?: continue
            val distance = start.squareDistanceTo(hit.hitVec)
            if (distance < nearest) {
                nearest = distance
                positionHit = position
            }
        }
        return positionHit
    }

    private fun draw(visible: List<ScenePosition>) {
        val mc = Minecraft.getMinecraft()
        val resolution = ScaledResolution(
            mc,
            mc.displayWidth,
            mc.displayHeight,
        )
        val factor = resolution.scaleFactor
        val viewportX = x * factor
        val viewportY = mc.displayHeight - (y + height) * factor
        val oldMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE)
        val oldScissor = BufferUtils.createIntBuffer(4)
        GL11.glGetInteger(
            GL11.GL_SCISSOR_BOX,
            oldScissor,
        )
        val scissored = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
        GL11.glPushClientAttrib(GL11.GL_ALL_CLIENT_ATTRIB_BITS)
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        GL11.glFrustum(
            -camera.nearHalfWidth,
            camera.nearHalfWidth,
            -camera.nearHalfHeight,
            camera.nearHalfHeight,
            camera.nearPlane,
            camera.farPlane,
        )
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        try {
            GL11.glViewport(
                viewportX,
                viewportY,
                width * factor,
                height * factor,
            )
            var left = viewportX
            var bottom = viewportY
            var right = viewportX + width * factor
            var top = viewportY + height * factor
            if (scissored) {
                left = maxOf(
                    a = left,
                    b = oldScissor[0],
                )
                bottom = maxOf(
                    a = bottom,
                    b = oldScissor[1],
                )
                right = minOf(
                    a = right,
                    b = oldScissor[0] + oldScissor[2],
                )
                top = minOf(
                    a = top,
                    b = oldScissor[1] + oldScissor[3],
                )
            }
            GL11.glEnable(GL11.GL_SCISSOR_TEST)
            GL11.glScissor(
                left,
                bottom,
                maxOf(
                    a = 0,
                    b = right - left,
                ),
                maxOf(
                    a = 0,
                    b = top - bottom,
                ),
            )
            GL11.glDepthMask(true)
            GL11.glClearColor(
                0.09f,
                0.10f,
                0.12f,
                1f,
            )
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            GL11.glEnable(GL11.GL_ALPHA_TEST)
            GL11.glDisable(GL11.GL_CULL_FACE)
            GL11.glTranslated(
                0.0,
                0.0,
                -camera.distance,
            )
            GL11.glRotated(
                camera.pitch,
                1.0,
                0.0,
                0.0,
            )
            GL11.glRotated(
                camera.yaw,
                0.0,
                1.0,
                0.0,
            )
            GL11.glTranslated(
                -camera.center.x,
                -camera.center.y,
                -camera.center.z,
            )
            RenderHelper.disableStandardItemLighting()
            mc.renderEngine.bindTexture(TextureMap.locationBlocksTexture)
            renderer.renderAllFaces = slice.enabled
            for (pass in 0..1) {
                ForgeHooksClient.setRenderPass(pass)
                if (pass == 0) {
                    GL11.glDisable(GL11.GL_BLEND)
                } else {
                    GL11.glEnable(GL11.GL_BLEND)
                    GL11.glBlendFunc(
                        GL11.GL_SRC_ALPHA,
                        GL11.GL_ONE_MINUS_SRC_ALPHA,
                    )
                }
                GL11.glDepthMask(pass == 0)
                drawBlocks(
                    visible = visible,
                    pass = pass,
                )
                val dispatcher = TileEntityRendererDispatcher.instance
                for (position in visible) {
                    val tile = scene.world.getTileEntity(
                        x = position.x,
                        y = position.y,
                        z = position.z,
                    ) ?: continue
                    if (dispatcher.hasSpecialRenderer(tile) && tile.shouldRenderInPass(pass)) {
                        dispatcher.renderTileEntityAt(
                            tile,
                            position.x.toDouble(),
                            position.y.toDouble(),
                            position.z.toDouble(),
                            0f,
                        )
                    }
                }
            }
            drawSelection()
        } finally {
            ForgeHooksClient.setRenderPass(-1)
            GL11.glMatrixMode(GL11.GL_MODELVIEW)
            GL11.glPopMatrix()
            GL11.glMatrixMode(GL11.GL_PROJECTION)
            GL11.glPopMatrix()
            GL11.glPopClientAttrib()
            GL11.glPopAttrib()
            GL11.glMatrixMode(oldMatrixMode)
        }
    }

    private fun drawSelection() {
        val positions = highlightedPositions()
        if (positions.isEmpty()) {
            return
        }
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDisable(GL11.GL_ALPHA_TEST)
        GL11.glDepthMask(false)
        GL11.glLineWidth(2f)
        for (position in positions) {
            val block = scene.world.getBlock(
                x = position.x,
                y = position.y,
                z = position.z,
            )
            block.setBlockBoundsBasedOnState(
                scene.world,
                position.x,
                position.y,
                position.z,
            )
            val bounds = block.getSelectedBoundingBoxFromPool(
                scene.world,
                position.x,
                position.y,
                position.z,
            ).expand(
                0.002,
                0.002,
                0.002,
            )
            RenderGlobal.drawOutlinedBoundingBox(
                bounds,
                0xFFD166,
            )
        }
    }

    private fun drawBlocks(
        visible: List<ScenePosition>,
        pass: Int,
    ) {
        val tessellator = Tessellator.instance
        val mc = Minecraft.getMinecraft()
        val previousAo = mc.gameSettings.ambientOcclusion
        mc.gameSettings.ambientOcclusion = 0
        val eye = camera.fromView(
            x = 0.0,
            y = 0.0,
            z = camera.distance,
        )
        val ordered = if (pass == 1) {
            visible.sortedByDescending {
                val dx = it.x - eye.x
                val dy = it.y - eye.y
                val dz = it.z - eye.z
                dx * dx + dy * dy + dz * dz
            }
        } else {
            visible
        }
        tessellator.startDrawingQuads()
        try {
            tessellator.setBrightness(0xF000F0)
            for (position in ordered) {
                val block = scene.world.getBlock(
                    x = position.x,
                    y = position.y,
                    z = position.z,
                )
                if (block.canRenderInPass(pass)) {
                    renderer.setRenderBoundsFromBlock(block)
                    renderer.renderBlockByRenderType(
                        block,
                        position.x,
                        position.y,
                        position.z,
                    )
                }
            }
        } finally {
            mc.gameSettings.ambientOcclusion = previousAo
            tessellator.draw()
            tessellator.setTranslation(
                0.0,
                0.0,
                0.0,
            )
        }
    }

}
