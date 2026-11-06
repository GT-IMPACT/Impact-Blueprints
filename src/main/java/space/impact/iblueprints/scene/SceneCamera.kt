package space.impact.iblueprints.scene

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.atan
import kotlin.math.tan

/** Perspective orbit camera. Projection and picking share the same field of view and eye distance. */
public class SceneCamera {

    public var yaw: Double = -45.0
        private set
    public var pitch: Double = 25.0
        private set
    public var distance: Double = 10.0
        private set
    public val scale: Double
        get() {
            return focalLength / distance
        }
    internal val nearPlane: Double = 0.05
    internal val farPlane: Double
        get() {
            return distance + radius * 4.0 + 4096.0
        }
    internal val nearHalfHeight: Double
        get() {
            return nearPlane * halfFieldOfViewTangent
        }
    internal val nearHalfWidth: Double
        get() {
            return nearHalfHeight * viewportWidth / viewportHeight
        }

    private val halfFieldOfViewTangent: Double = tan(Math.toRadians(20.0))
    private var viewportWidth: Int = 1
    private var viewportHeight: Int = 1
    private var radius: Double = 1.0
    private var maximumDistance: Double = 1000.0
    private val focalLength: Double
        get() {
            return viewportHeight / (2.0 * halfFieldOfViewTangent)
        }
    public var center: SceneVector = SceneVector(
        x = 0.0,
        y = 0.0,
        z = 0.0,
    )
        private set

    public fun fit(
        bounds: SceneBounds,
        width: Int,
        height: Int,
    ) {
        val dx = bounds.max.x - bounds.min.x + 1.0
        val dy = bounds.max.y - bounds.min.y + 1.0
        val dz = bounds.max.z - bounds.min.z + 1.0
        center = SceneVector(
            x = (bounds.min.x + bounds.max.x + 1.0) / 2,
            y = (bounds.min.y + bounds.max.y + 1.0) / 2,
            z = (bounds.min.z + bounds.max.z + 1.0) / 2,
        )
        resize(
            width = width,
            height = height,
        )
        radius = sqrt(dx * dx + dy * dy + dz * dz) / 2.0
        val limitingHalfAngle = atan(halfFieldOfViewTangent * minOf(
            a = 1.0,
            b = width.toDouble() / height,
        ))
        // Fit a bounding sphere so orbiting cannot clip a corner of the initial structure.
        distance = radius / sin(limitingHalfAngle) * 1.15
        maximumDistance = distance * 100.0
        yaw = -45.0
        pitch = 25.0
    }

    public fun rotate(
        dx: Double,
        dy: Double,
    ) {
        yaw = (yaw + dx) % 360.0
        pitch = (pitch + dy).coerceIn(
            minimumValue = -89.0,
            maximumValue = 89.0,
        )
    }

    public fun resize(
        width: Int,
        height: Int,
    ) {
        require(width > 0 && height > 0)
        viewportWidth = width
        viewportHeight = height
    }

    public fun zoom(steps: Int) {
        distance = (distance / 1.15.pow(steps)).coerceIn(
            minimumValue = nearPlane * 4.0,
            maximumValue = maximumDistance,
        )
    }

    public fun project(position: SceneVector): SceneVector {
        val ry = Math.toRadians(yaw)
        val rx = Math.toRadians(pitch)
        val dx = position.x - center.x
        val dy = position.y - center.y
        val dz = position.z - center.z
        val rotatedX = cos(ry) * dx + sin(ry) * dz
        val rotatedZ = -sin(ry) * dx + cos(ry) * dz
        val rotatedY = cos(rx) * dy - sin(rx) * rotatedZ
        val depth = distance - (sin(rx) * dy + cos(rx) * rotatedZ)
        return SceneVector(
            x = viewportWidth / 2.0 + rotatedX * focalLength / depth,
            y = viewportHeight / 2.0 - rotatedY * focalLength / depth,
            z = depth,
        )
    }

    public fun unproject(
        x: Double,
        y: Double,
        depth: Double,
    ): SceneVector {
        return fromView(
            x = (x - viewportWidth / 2.0) * depth / focalLength,
            y = (viewportHeight / 2.0 - y) * depth / focalLength,
            z = distance - depth,
        )
    }

    public fun pan(
        dx: Double,
        dy: Double,
    ) {
        center = fromView(
            x = -dx / scale,
            y = dy / scale,
            z = 0.0,
        )
    }

    public fun fromView(
        x: Double,
        y: Double,
        z: Double,
    ): SceneVector {
        val ry = Math.toRadians(yaw)
        val rx = Math.toRadians(pitch)
        val localY = cos(rx) * y + sin(rx) * z
        val localZ = -sin(rx) * y + cos(rx) * z
        return SceneVector(
            x = center.x + cos(ry) * x - sin(ry) * localZ,
            y = center.y + localY,
            z = center.z + sin(ry) * x + cos(ry) * localZ,
        )
    }
}
