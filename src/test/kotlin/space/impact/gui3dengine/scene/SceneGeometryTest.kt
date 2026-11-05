package space.impact.gui3dengine.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

public class SceneGeometryTest {

    @Test
    public fun pickingCoordinatesInvertCameraRotation() {
        val camera = SceneCamera()
        camera.fit(
            bounds = SceneBounds(
                min = ScenePosition(
                    x = -4,
                    y = 62,
                    z = -2,
                ),
                max = ScenePosition(
                    x = 3,
                    y = 66,
                    z = 7,
                ),
            ),
            width = 162,
            height = 112,
        )
        camera.rotate(
            dx = 37.0,
            dy = -18.0,
        )
        val world = camera.fromView(
            x = 1.2,
            y = -0.7,
            z = 23.0,
        )
        val dx = world.x - camera.center.x
        val dy = world.y - camera.center.y
        val dz = world.z - camera.center.z
        val yaw = Math.toRadians(camera.yaw)
        val pitch = Math.toRadians(camera.pitch)
        val rotatedZ = -sin(yaw) * dx + cos(yaw) * dz
        assertEquals(
            1.2,
            cos(yaw) * dx + sin(yaw) * dz,
            1e-9,
        )
        assertEquals(
            -0.7,
            cos(pitch) * dy - sin(pitch) * rotatedZ,
            1e-9,
        )
        assertEquals(
            23.0,
            sin(pitch) * dy + cos(pitch) * rotatedZ,
            1e-9,
        )
    }

    @Test
    public fun perspectivePickingMatchesProjectionAcrossRotationsAndViewportSizes() {
        for ((width, height) in listOf(
            162 to 112,
            256 to 600,
            1200 to 400,
        )) {
            val camera = SceneCamera()
            camera.resize(
                width = width,
                height = height,
            )
            for (step in 0 until 24) {
                camera.rotate(
                    dx = 15.0,
                    dy = -5.0,
                )
                camera.zoom(if (step % 2 == 0) {
                    1
                } else {
                    -1
                })
                for (depth in listOf(
                    0.1,
                    camera.distance,
                    50.0,
                )) {
                    val point = camera.unproject(
                        x = width * 0.8,
                        y = height * 0.2,
                        depth = depth,
                    )
                    val projected = camera.project(point)
                    assertEquals(
                        width * 0.8,
                        projected.x,
                        1e-7,
                    )
                    assertEquals(
                        height * 0.2,
                        projected.y,
                        1e-7,
                    )
                    assertEquals(
                        depth,
                        projected.z,
                        1e-7,
                    )
                }
            }
        }
    }

    @Test
    public fun perspectiveMakesNearEdgesLargerAndPanFollowsCursor() {
        val camera = SceneCamera()
        camera.resize(
            width = 800,
            height = 600,
        )
        val near = camera.project(camera.fromView(
            x = 1.0,
            y = 0.0,
            z = 2.0,
        ))
        val far = camera.project(camera.fromView(
            x = 1.0,
            y = 0.0,
            z = -2.0,
        ))
        assertTrue(near.x - 400.0 > far.x - 400.0)
        val target = camera.center
        camera.pan(
            dx = 37.0,
            dy = -21.0,
        )
        val panned = camera.project(target)
        assertEquals(
            437.0,
            panned.x,
            1e-9,
        )
        assertEquals(
            279.0,
            panned.y,
            1e-9,
        )
    }

    @Test
    public fun fittedCubeStaysInsideViewportWhileOrbiting() {
        val camera = SceneCamera()
        camera.fit(
            bounds = SceneBounds(
                min = ScenePosition(
                    x = -1,
                    y = -1,
                    z = -1,
                ),
                max = ScenePosition(
                    x = 1,
                    y = 1,
                    z = 1,
                ),
            ),
            width = 200,
            height = 400,
        )
        for (step in 0 until 24) {
            camera.rotate(
                dx = 15.0,
                dy = -4.0,
            )
            for (x in listOf(-1.0, 2.0)) {
                for (y in listOf(-1.0, 2.0)) {
                    for (z in listOf(-1.0, 2.0)) {
                        val projected = camera.project(SceneVector(
                            x = x,
                            y = y,
                            z = z,
                        ))
                        assertTrue(projected.x in 0.0..200.0)
                        assertTrue(projected.y in 0.0..400.0)
                        assertTrue(projected.z > camera.nearPlane)
                        assertTrue(projected.z < camera.farPlane)
                    }
                }
            }
        }
    }

    @Test
    public fun sliceAndCutawayIncludeOnlyVisiblePositions() {
        val slice = SceneSlice()
        val below = ScenePosition(
            x = -2,
            y = 63,
            z = 4,
        )
        val layer = ScenePosition(
            x = -2,
            y = 64,
            z = 4,
        )
        val above = ScenePosition(
            x = -2,
            y = 65,
            z = 4,
        )
        slice.select(
            axis = 1,
            level = 64,
        )
        assertFalse(slice.includes(below))
        assertTrue(slice.includes(layer))
        assertFalse(slice.includes(above))
        slice.select(
            axis = 1,
            level = 64,
            cutaway = true,
        )
        assertTrue(slice.includes(below))
        assertTrue(slice.includes(layer))
        assertFalse(slice.includes(above))
        slice.select(
            axis = 0,
            level = -2,
        )
        assertTrue(slice.includes(above))
        slice.showAll()
        assertTrue(slice.includes(ScenePosition(
            x = 20,
            y = 100,
            z = 0,
        )))
    }

    @Test
    public fun boundsSupportNegativeCoordinatesAndRemovedExtremes() {
        val positions = mutableListOf(
            ScenePosition(
                x = -10,
                y = 64,
                z = 0,
            ),
            ScenePosition(
                x = 2,
                y = 68,
                z = -4,
            ),
        )
        assertEquals(
            -10,
            SceneBounds.of(positions).min.x,
        )
        positions.removeAt(0)
        val bounds = SceneBounds.of(positions)
        assertEquals(
            bounds.min,
            bounds.max,
        )
        assertEquals(
            2,
            bounds.min.x,
        )
    }

    @Test
    public fun cameraLimitsRemainFinite() {
        val camera = SceneCamera()
        camera.rotate(
            dx = 1e6,
            dy = 1e6,
        )
        camera.zoom(10000)
        assertEquals(
            89.0,
            camera.pitch,
            0.0,
        )
        assertEquals(
            0.2,
            camera.distance,
            0.0,
        )
        camera.zoom(-10000)
        assertEquals(
            1000.0,
            camera.distance,
            0.0,
        )
    }
}
