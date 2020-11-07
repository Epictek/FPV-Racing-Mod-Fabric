package dev.lazurite.fpvracing.mixin;

import dev.lazurite.fpvracing.server.ServerTick;
import dev.lazurite.fpvracing.server.item.GogglesItem;
import dev.lazurite.fpvracing.server.entity.flyable.QuadcopterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin class modifies the values in the packet before they are sent.
 * @author Ethan Johnson
 */
@Mixin(PlayerSpawnS2CPacket.class)
public class PlayerSpawnS2CPacketMixin {
    @Shadow double x;
    @Shadow double y;
    @Shadow double z;

    /**
     * This mixin modifies the constructor of {@link PlayerSpawnS2CPacket} in
     * order to keep the player position in the same place when the given {@link PlayerEntity}
     * is flying a {@link QuadcopterEntity}.
     * @param player the given {@link PlayerEntity}
     * @param info required by every mixin injection
     */
    @Inject(method = "<init>(Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("RETURN"))
    public void init(PlayerEntity player, CallbackInfo info) {
        if (GogglesItem.isInGoggles((ServerPlayerEntity) player)) {
            Vec3d pos = ServerTick.playerPositionManager.getPos(player);
            this.x = pos.x;
            this.y = pos.y;
            this.z = pos.z;
        }
    }
}
