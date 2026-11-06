package space.impact.iblueprints.scene

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
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
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.BufferUtils
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import space.impact.iblueprints.planner.logPlannerTrace
import space.impact.iblueprints.planner.plannerTraceEnabled
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
    public val selected: ScenePosition?
        get() {
            return selection.keys.singleOrNull()
        }
    internal var structureValid: Boolean? = null
        private set
    internal var hasEditedStructure: Boolean = false
        private set

    private var highlighted: Set<ScenePosition> = emptySet()
    private var selection: Map<ScenePosition, ForgeDirection?> = emptyMap()
    private val replacements = linkedMapOf<ScenePosition, ItemStack>()
    private val hatchRotations = linkedMapOf<ScenePosition, Pair<ItemStack, ForgeDirection>>()

    private var renderer: RenderBlocks = RenderBlocks(scene.world)
    private var x: Int = 0
    private var y: Int = 0
    private var width: Int = 1
    private var height: Int = 1
    private var fitted: Boolean = false
    private var dragButton: Int = -1
    private var extendSelection = false
    private var lastMouseX: Int = 0
    private var lastMouseY: Int = 0
    private var dragDistance: Int = 0

    public fun replaceScene(replacement: BlockScene) {
        val previous = scene
        scene = replacement
        renderer = RenderBlocks(replacement.world)
        materials = buildMaterials()
        replacements.clear()
        hatchRotations.clear()
        structureValid = null
        hasEditedStructure = false
        hovered = null
        selection = emptyMap()
        highlighted = emptySet()
        slice.showAll()
        fitted = false
        previous.close()
    }

    internal fun replaceConfiguredScene(replacement: BlockScene) {
        val wasEdited = hasEditedStructure
        val retained = replacements.filter { (position, stack) ->
            replacement.simulation?.replace(
                position = position,
                stack = stack,
                preferredFacing = hatchRotations[position]?.second,
            ) == true && replacement.elements[position]?.matchesOriginal(stack) == false
        }
        val rotations = hatchRotations.filter { (position, rotation) ->
            replacement.item(position)?.isItemEqual(rotation.first) == true &&
                replacement.simulation?.setHatchFacing(
                    position = position,
                    facing = rotation.second,
                ) == true
        }
        replaceScene(replacement)
        replacements.putAll(retained)
        hatchRotations.putAll(rotations)
        hasEditedStructure = wasEdited
        if (hasEditedStructure) {
            validateStructure()
        }
    }

    internal fun replaceBlock(
        position: ScenePosition,
        stack: ItemStack,
        refresh: Boolean = true,
        preferredFacing: ForgeDirection? = null,
    ): Boolean {
        if (scene.item(position)?.isItemEqual(stack) == true) {
            return false
        }
        val simulation = scene.simulation ?: return false
        if (!simulation.replace(
                position = position,
                stack = stack,
                preferredFacing = preferredFacing,
            )
        ) {
            return false
        }
        recordReplacement(
            position = position,
            stack = stack,
            preferredFacing = preferredFacing,
        )
        if (refresh) {
            refreshLayout()
        }
        return true
    }

    private fun recordReplacement(
        position: ScenePosition,
        stack: ItemStack,
        preferredFacing: ForgeDirection?,
    ) {
        if (scene.elements[position]?.matchesOriginal(stack) == true) {
            replacements.remove(position)
        } else {
            replacements[position] = stack.copy()
        }
        val facing = if (preferredFacing == null) {
            null
        } else {
            scene.simulation?.hatchFacing(position)
        }
        if (facing == null) {
            hatchRotations.remove(position)
        } else {
            hatchRotations[position] = stack.copy() to facing
        }
        hasEditedStructure = true
    }

    internal fun inspectedPositions(): Set<ScenePosition> {
        if (!slice.enabled) {
            return selection.keys
        }
        return selection.keys.filterTo(
            linkedSetOf(),
            slice::includes,
        )
    }

    internal fun inspectedPosition(): ScenePosition? {
        return inspectedPositions().singleOrNull()
    }

    internal fun commonAlternatives(positions: Set<ScenePosition>): List<ItemStack> {
        val current = positions.associateWith {
            scene.item(it) ?: return emptyList()
        }
        val first = current.entries.firstOrNull() ?: return emptyList()
        val firstChoices = listOf(first.value) +
            scene.elements[first.key]?.inspectionAlternatives().orEmpty().filterNot {
                sameStack(
                    first = first.value,
                    second = it,
                )
            }
        if (current.size == 1) {
            return firstChoices
        }
        val simulation = scene.simulation ?: return emptyList()
        var common = firstChoices.filter { alternative ->
            current.values.any {
                !sameStack(
                    first = it,
                    second = alternative,
                )
            }
        }
        for ((position, occupied) in current) {
            if (common.isEmpty()) {
                break
            }
            common = common.filter { alternative ->
                sameStack(
                    first = occupied,
                    second = alternative,
                ) || simulation.canReplace(
                    position = position,
                    stack = alternative,
                ) && (position == first.key || scene.elements[position]?.hasAlternative(alternative) == true)
            }
        }
        return common
    }

    internal fun replaceSelection(
        positions: Set<ScenePosition>,
        stack: ItemStack,
    ): Boolean {
        if (positions.isEmpty() || positions != inspectedPositions()) {
            return false
        }
        if (positions.size == 1) {
            val position = positions.single()
            return replaceBlock(
                position = position,
                stack = stack,
                preferredFacing = selection[position],
            )
        }
        val targets = selection.filterKeys {
            it in positions && !sameStack(
                first = scene.item(it),
                second = stack,
            )
        }
        if (targets.isEmpty() || scene.simulation?.replaceAll(
                targets = targets,
                stack = stack,
            ) != true
        ) {
            return false
        }
        for ((position, facing) in targets) {
            recordReplacement(
                position = position,
                stack = stack,
                preferredFacing = facing,
            )
        }
        refreshLayout()
        return true
    }

    private fun sameStack(
        first: ItemStack?,
        second: ItemStack,
    ): Boolean {
        return first?.isItemEqual(second) == true && ItemStack.areItemStackTagsEqual(
            first,
            second,
        )
    }

    internal fun rotateHatch(position: ScenePosition): Boolean {
        val facing = scene.simulation?.nextHatchFacing(position) ?: return false
        return setHatchFacing(
            position = position,
            facing = facing,
        )
    }

    internal fun setHatchFacing(
        position: ScenePosition,
        facing: ForgeDirection,
        refresh: Boolean = true,
    ): Boolean {
        val simulation = scene.simulation ?: return false
        val stack = scene.item(position) ?: return false
        if (simulation.hatchFacing(position) == facing) {
            return false
        }
        if (!simulation.setHatchFacing(
                position = position,
                facing = facing,
            )
        ) {
            return false
        }
        hatchRotations[position] = stack to facing
        hasEditedStructure = true
        if (refresh) {
            validateStructure()
        }
        return true
    }

    internal fun refreshLayout() {
        materials = buildMaterials()
        if (hasEditedStructure) {
            validateStructure()
        }
    }

    internal fun validateStructure() {
        structureValid = scene.simulation?.validate()
    }

    public fun resetCamera() {
        fitCamera()
        selection = emptyMap()
        highlighted = emptySet()
    }

    private fun fitCamera() {
        camera.fit(
            bounds = scene.bounds(),
            width = width,
            height = height,
        )
    }

    internal fun highlight(positions: Set<ScenePosition>) {
        highlighted = positions
    }

    public fun toggleMaterial(material: SceneMaterial) {
        toggleMaterial(
            material = material,
            additive = false,
        )
    }

    internal fun toggleMaterial(
        material: SceneMaterial,
        additive: Boolean,
    ) {
        val positions = material.positions.toSet()
        selection = when {
            additive && isMaterialSelected(material) -> {
                selection - positions
            }
            additive -> {
                selection + (positions - selection.keys).associateWith {
                    null
                }
            }
            selection.keys == positions -> {
                emptyMap()
            }
            else -> {
                positions.associateWith {
                    null
                }
            }
        }
        highlighted = emptySet()
    }

    public fun isMaterialSelected(material: SceneMaterial): Boolean {
        return material.positions.isNotEmpty() && selection.keys.containsAll(material.positions)
    }

    internal fun highlightedPositions(): List<ScenePosition> {
        if (highlighted.isEmpty() && selection.isEmpty()) {
            return emptyList()
        }
        return (scene.positions().toSet() + scene.elements.keys).filter {
            slice.includes(it) && (it in selection || it in highlighted)
        }
    }

    internal fun selectBlock(
        hit: SceneBlockHit?,
        additive: Boolean,
    ) {
        if (additive && hit == null) {
            return
        }
        selection = when {
            hit == null -> emptyMap()
            !additive -> mapOf(hit.position to hit.face)
            hit.position in selection -> selection - hit.position
            else -> selection + (hit.position to hit.face)
        }
        highlighted = emptySet()
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
        extendSelection = button == 0 && GuiScreen.isShiftKeyDown()
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
        val traceFirstFrame = !fitted && plannerTraceEnabled
        if (traceFirstFrame) {
            logPlannerTrace("viewer.firstRender begin structure=${scene.title} viewport=${width}x$height")
        }
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
            fitCamera()
            fitted = true
        }
        val select = updateDrag(
            mouseX = mouseX,
            mouseY = mouseY,
        )
        val visible = scene.positions().filter(slice::includes)
        val hit = if (contains(
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
        hovered = hit?.position
        if (select) {
            selectBlock(
                hit = hit,
                additive = extendSelection,
            )
        }
        if (slice.enabled) {
            // Custom renderers such as GregTech cull faces using the world rather than renderAllFaces.
            scene.world.withVisiblePositions(positions = visible.toSet()) {
                draw(visible)
            }
        } else {
            draw(visible)
        }
        if (traceFirstFrame) {
            logPlannerTrace("viewer.firstRender end structure=${scene.title} visibleBlocks=${visible.size}")
        }
    }

    private fun updateDrag(
        mouseX: Int,
        mouseY: Int,
    ): Boolean {
        if (dragButton < 0) {
            return false
        }
        if (!Mouse.isButtonDown(dragButton)) {
            val select = dragButton == 0 && dragDistance < 4 && contains(
                mouseX = mouseX,
                mouseY = mouseY,
            )
            dragButton = -1
            return select
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
        return false
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

    internal fun pick(
        visible: List<ScenePosition>,
        mouseX: Int,
        mouseY: Int,
    ): SceneBlockHit? {
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
        var nearestHit: SceneBlockHit? = null
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
                val face = ForgeDirection.getOrientation(hit.sideHit)
                nearestHit = SceneBlockHit(
                    position = position,
                    face = face.takeUnless {
                        it == ForgeDirection.UNKNOWN
                    },
                )
            }
        }
        return nearestHit
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
            val selection = highlightedPositions()
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
                val offsetDepth = pass == 0 && selection.isNotEmpty()
                if (offsetDepth) {
                    // The bias follows the face's screen-space slope, including at grazing angles.
                    GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL)
                    GL11.glPolygonOffset(
                        2f,
                        2f,
                    )
                }
                try {
                    drawBlocks(
                        visible = visible,
                        pass = pass,
                    )
                } finally {
                    if (offsetDepth) {
                        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL)
                    }
                }
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
            drawSelection(selection)
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

    private fun drawSelection(positions: List<ScenePosition>) {
        if (positions.isEmpty()) {
            return
        }
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDisable(GL11.GL_ALPHA_TEST)
        GL11.glDepthFunc(GL11.GL_LEQUAL)
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
