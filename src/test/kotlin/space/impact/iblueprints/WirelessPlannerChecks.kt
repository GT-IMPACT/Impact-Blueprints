package space.impact.iblueprints

import appeng.api.features.IWirelessTermHandler
import io.netty.buffer.Unpooled
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import space.impact.iblueprints.integration.ae2.MePlannerInventory
import space.impact.iblueprints.integration.ae2.WirelessBlueprintPlannerHost
import space.impact.iblueprints.integration.ae2.WirelessPlannerIntegration
import space.impact.iblueprints.integration.ae2.WirelessPlannerMode
import space.impact.iblueprints.integration.structures.PreviewPlayer
import space.impact.iblueprints.item.BlankBlueprintItem
import space.impact.iblueprints.item.StructureBlueprintItem
import space.impact.iblueprints.planner.PlannerState
import space.impact.iblueprints.planner.PlannerSession
import space.impact.iblueprints.scene.world.SceneWorld
import java.lang.reflect.Proxy

internal object WirelessPlannerChecks {

    internal fun check(plan: NBTTagCompound) {
        val world = SceneWorld().apply {
            isRemote = false
        }
        try {
            val player = PreviewPlayer(world)
            val handler = Proxy.newProxyInstance(
                IWirelessTermHandler::class.java.classLoader,
                arrayOf(IWirelessTermHandler::class.java),
            ) { _, method, _ ->
                when (method.name) {
                    "getEncryptionKey" -> ""
                    "hasInfinityRange", "hasInfinityPower", "hasPower" -> false
                    else -> error("Unexpected wireless call: ${method.name}")
                }
            } as IWirelessTermHandler
            val stack = ItemStack(Items.paper).apply {
                tagCompound = NBTTagCompound().apply {
                    setString(
                        "encryptionKey",
                        "network-key",
                    )
                    setInteger(
                        "mode",
                        2,
                    )
                }
            }
            player.inventory.mainInventory[4] = stack
            val host = WirelessBlueprintPlannerHost(
                handler = handler,
                stack = stack,
                player = player,
                slot = 4,
            )
            assertEquals(
                5,
                host.planner.inventory.slots,
            )
            assertFalse(host.accessible(player))
            assertFalse(WirelessPlannerIntegration.supports(stack))
            assertTrue(host.planner.select(
                id = "test:wireless",
                snapshot = plan,
            ))
            val inventory = MePlannerInventory(host)
            inventory.setInventorySlotContents(
                PlannerState.BLUEPRINT_INPUT,
                ItemStack(BlankBlueprintItem),
            )
            val stored = stack.tagCompound.copy()
            val reopenedStack = stack.copy()
            player.inventory.mainInventory[4] = reopenedStack
            val reopened = WirelessBlueprintPlannerHost(
                handler = handler,
                stack = reopenedStack,
                player = player,
                slot = 4,
            )
            assertEquals(
                stored,
                reopenedStack.tagCompound,
            )
            assertEquals(
                "test:wireless",
                reopened.planner.recipe,
            )
            assertEquals(
                plan,
                reopened.planner.plan,
            )
            assertTrue(reopened.planner.inventory.getStackInSlot(PlannerState.BLUEPRINT_INPUT)?.item === BlankBlueprintItem)
            assertEquals(
                "network-key",
                reopenedStack.tagCompound.getString("encryptionKey"),
            )
            assertEquals(
                2,
                reopenedStack.tagCompound.getInteger("mode"),
            )
            val reopenedInventory = MePlannerInventory(reopened)
            assertTrue(reopenedInventory.decrStackSize(
                PlannerState.BLUEPRINT_INPUT,
                1,
            )?.item === BlankBlueprintItem)
            assertEquals(
                0,
                reopenedStack.tagCompound.getCompoundTag(WirelessBlueprintPlannerHost.PLANNER_TAG)
                    .getCompoundTag("Inventory").getTagList("Items", 10).tagCount(),
            )
            WirelessPlannerMode.select(reopenedStack)
            assertTrue(WirelessPlannerMode.isSelected(reopenedStack.copy()))
            WirelessPlannerMode.clear(reopenedStack)
            assertFalse(WirelessPlannerMode.isSelected(reopenedStack))
            assertEquals(
                "test:wireless",
                reopened.planner.recipe,
            )
            reopened.planner.reset(reopened.planner.selection)
            host.planner.reset(host.planner.selection)
            val server = PlannerSession(
                state = reopened.planner,
                player = {
                    player
                },
                isOpen = {
                    true
                },
                sendToServer = { _, _ -> },
                sendToClient = { _, _ -> },
            )
            val client = PlannerSession(
                state = host.planner,
                player = {
                    player
                },
                isOpen = {
                    host.accessible(player)
                },
                sendToServer = { id, write ->
                    val packet = PacketBuffer(Unpooled.buffer())
                    try {
                        write(packet)
                        world.isRemote = false
                        server.readOnServer(
                            id = id,
                            buf = packet,
                        )
                    } finally {
                        world.isRemote = true
                        packet.release()
                    }
                },
                sendToClient = { _, _ -> },
            )
            world.isRemote = true
            assertTrue(reopened.accessible(player))
            assertTrue(host.accessible(player))
            assertTrue(client.select(
                id = "test:wireless_nei",
                plan = plan,
            ))
            assertEquals(
                "test:wireless_nei",
                reopenedStack.tagCompound.getCompoundTag(WirelessBlueprintPlannerHost.PLANNER_TAG).getString("Recipe"),
            )
            host.planner.readState(NBTTagCompound().also(reopened.planner::writeState))
            reopened.planner.inventory.setStackInSlot(
                PlannerState.BLUEPRINT_INPUT,
                ItemStack(BlankBlueprintItem),
            )
            client.action(PlannerSession.BLUEPRINT)
            assertTrue(reopened.planner.inventory.getStackInSlot(PlannerState.BLUEPRINT)?.item === StructureBlueprintItem)
            val savedOutput = WirelessBlueprintPlannerHost(
                handler = handler,
                stack = reopenedStack.copy(),
                player = player,
                slot = 4,
            )
            assertTrue(savedOutput.planner.inventory.getStackInSlot(PlannerState.BLUEPRINT)?.item === StructureBlueprintItem)
            player.inventory.mainInventory[4] = null
            assertFalse(reopened.accessible(player))
        } finally {
            world.clear()
        }
    }
}
