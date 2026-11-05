package space.impact.gui3dengine.scene

public data class SceneBounds(
    public val min: ScenePosition,
    public val max: ScenePosition,
) {

    public companion object {

        public fun of(positions: Collection<ScenePosition>): SceneBounds {
            require(positions.isNotEmpty()) {
                "A scene must contain at least one block"
            }
            return SceneBounds(
                min = ScenePosition(
                    x = positions.minOf {
                        it.x
                    },
                    y = positions.minOf {
                        it.y
                    },
                    z = positions.minOf {
                        it.z
                    },
                ),
                max = ScenePosition(
                    x = positions.maxOf {
                        it.x
                    },
                    y = positions.maxOf {
                        it.y
                    },
                    z = positions.maxOf {
                        it.z
                    },
                ),
            )
        }
    }
}
