package space.impact.gui3dengine.api

import java.util.function.IntUnaryOperator

/** Channel bounds and normalization come from the structure definition. */
public class SceneChannel(
    public val id: String,
    public val label: String,
    public val description: String,
    public val minimum: Int,
    public val maximum: Int,
    private val normalizer: IntUnaryOperator,
) {

    public fun normalize(value: Int): Int {
        return normalizer.applyAsInt(value)
    }
}
