package bluevista.fpvracingmod.client;

import bluevista.fpvracingmod.client.input.InputTick;
import bluevista.fpvracingmod.server.entities.DroneEntity;
import bluevista.fpvracingmod.server.items.GogglesItem;
import bluevista.fpvracingmod.server.items.TransmitterItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public class ClientTick {
    public static DroneEntity currentDrone;
    private static boolean shouldRenderHand;

    public static void tick(MinecraftClient mc) {
        /*
         * Drone view and control logic
         */
        if (mc.player != null) {
            InputTick.setShouldTick(false);
            shouldRenderHand = !(mc.getCameraEntity() instanceof DroneEntity);
            if(isWearingGoggles(mc.player) || isHoldingTransmitter(mc.player)) {
                currentDrone = DroneEntity.getNearestTo(mc.player);

                if(currentDrone != null) {
                    if(!currentDrone.removed) {
                        if (isWearingGoggles(mc.player))
                            setView(mc, currentDrone);
                        if (isValidTransmitter(mc.player, currentDrone))
                            InputTick.setShouldTick(true);
                    } else resetView(mc);
                }
            }

            if(!isWearingGoggles(mc.player) && mc.getCameraEntity() instanceof DroneEntity)
                resetView(mc);
        } else {
            resetView(mc);
        }
    }

    /*
     * Returns whether or not the transmitter
     * the given player is holding is also
     * bound to the given drone entity.
     */
    public static boolean isValidTransmitter(ClientPlayerEntity player, DroneEntity drone) {
        ItemStack heldStack = player.inventory.getMainHandStack();

        try {
            return isHoldingTransmitter(player) &&
                    drone.getUuid().equals(heldStack.getSubTag("bind").getUuid("bind"));
        } catch (NullPointerException e) {
            return false;
        }
    }

    /*
     * Returns whether or not the given player is holding a TransmitterItem
     */
    public static boolean isHoldingTransmitter(ClientPlayerEntity player) {
        return player.inventory.getMainHandStack().getItem() instanceof TransmitterItem;
    }

    /*
     * Returns whether or not the player has a GogglesItem equipped
     * in their head armor slot.
     */
    public static boolean isWearingGoggles(ClientPlayerEntity player) {
        return player.inventory.armor.get(3).getItem() instanceof GogglesItem;
    }

    public static boolean shouldRenderHand() {
        return shouldRenderHand;
    }

    public static void setView(MinecraftClient mc, Entity entity) {
        if(!(mc.getCameraEntity() instanceof DroneEntity))
            mc.setCameraEntity(currentDrone);
    }

    public static void resetView(MinecraftClient mc) {
        mc.setCameraEntity(null);
    }

    public static void register() {
        ClientTickCallback.EVENT.register(ClientTick::tick);
    }
}
