package space.impact.iblueprints.integration.structures

import net.minecraft.util.IIcon

public class HatchHintFace(
    public val base: IIcon,
    public val overlays: List<IIcon>,
) : IIcon by base
