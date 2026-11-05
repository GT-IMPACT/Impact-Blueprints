package space.impact.gui3dengine.integration.modularui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.widgets.ButtonWidget

internal class SceneActionButton(
    label: IKey,
    action: () -> Unit,
) : ButtonWidget<SceneActionButton>() {

    init {
        overlay(label)
        onMousePressed {
            if (it == 0) {
                action()
                true
            } else {
                false
            }
        }
    }
}
