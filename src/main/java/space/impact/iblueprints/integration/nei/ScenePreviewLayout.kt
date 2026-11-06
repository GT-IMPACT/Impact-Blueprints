package space.impact.iblueprints.integration.nei

/** All coordinates are local to the NEI recipe widget, including the click and tooltip regions. */
internal object ScenePreviewLayout {

    internal const val WIDTH: Int = 166
    internal const val HEIGHT: Int = 146
    internal const val TITLE_X: Int = 3
    internal const val TITLE_Y: Int = 3
    internal const val TITLE_WIDTH: Int = WIDTH - 2 * TITLE_X
    internal const val PREVIEW_X: Int = 2
    internal const val PREVIEW_Y: Int = 18
    internal const val PREVIEW_WIDTH: Int = 162
    internal const val PREVIEW_HEIGHT: Int = 112
    internal const val FOOTER_Y: Int = 134

    internal fun contains(
        x: Int,
        y: Int,
    ): Boolean {
        return x >= PREVIEW_X && x < PREVIEW_X + PREVIEW_WIDTH && y >= PREVIEW_Y && y < HEIGHT
    }
}
