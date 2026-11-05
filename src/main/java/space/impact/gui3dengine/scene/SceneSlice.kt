package space.impact.gui3dengine.scene

/** Visibility is shared by rendering, picking and material highlighting. */
public class SceneSlice {

    public var axis: Int = 1
        private set
    public var level: Int = 0
        private set
    public var enabled: Boolean = false
        private set
    public var cutaway: Boolean = false
        private set

    public fun showAll() {
        enabled = false
    }

    public fun select(
        axis: Int,
        level: Int,
        cutaway: Boolean = false,
    ) {
        require(axis in 0..2)
        this.axis = axis
        this.level = level
        this.cutaway = cutaway
        enabled = true
    }

    public fun includes(position: ScenePosition): Boolean {
        if (!enabled) {
            return true
        }
        val coordinate = when (axis) {
            0 -> {
                position.x
            }
            1 -> {
                position.y
            }
            else -> {
                position.z
            }
        }
        return if (cutaway) {
            coordinate <= level
        } else {
            coordinate == level
        }
    }
}
