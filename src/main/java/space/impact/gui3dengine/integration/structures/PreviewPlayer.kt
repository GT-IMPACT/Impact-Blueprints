package space.impact.gui3dengine.integration.structures

import com.mojang.authlib.GameProfile
import java.util.UUID
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ChunkCoordinates
import net.minecraft.util.IChatComponent
import net.minecraft.world.World

/** Placement actor confined to the preview world; messages are retained as construction diagnostics. */
internal class PreviewPlayer(world: World) : EntityPlayer(
    world,
    GameProfile(
        UUID.fromString("5277fffa-50da-4ff5-87b6-b4bf59f22d24"),
        "SceneBuilder",
    ),
) {

    internal val messages: MutableSet<String> = linkedSetOf()

    init {
        capabilities.isCreativeMode = true
    }

    override fun addChatComponentMessage(message: IChatComponent) {
        messages.add(message.unformattedText)
    }

    override fun addChatMessage(message: IChatComponent) {
        addChatComponentMessage(message)
    }

    override fun getPlayerCoordinates(): ChunkCoordinates {
        return ChunkCoordinates(
            posX.toInt(),
            posY.toInt(),
            posZ.toInt(),
        )
    }

    override fun canCommandSenderUseCommand(
        permission: Int,
        command: String,
    ): Boolean {
        return false
    }
}
