package dev.lazurite.fpvracing.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.lazurite.fpvracing.client.ClientInitializer;
import dev.lazurite.fpvracing.client.renderer.StaticRenderer;
import dev.lazurite.fpvracing.client.renderer.StaticRenderer2;
import dev.lazurite.fpvracing.server.entity.FlyableEntity;
import dev.lazurite.fpvracing.server.item.GogglesItem;
import dev.lazurite.fpvracing.server.entity.flyable.QuadcopterEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin class modifies the behavior of the on-screen HUD. For instance,
 * the {@link InGameHudMixin#renderCrosshair(MatrixStack, CallbackInfo)} injection
 * prevents the crosshair from rendering while the player is flying a drone.
 * @author Patrick Hofmann
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Shadow @Final MinecraftClient client;

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/entity/player/PlayerInventory;getArmorStack(I)Lnet/minecraft/item/ItemStack;"
            )
    )
    public void render(MatrixStack matrices, float tickDelta, CallbackInfo info) {
        if (client.options.getPerspective().isFirstPerson() && client.player.inventory.getArmorStack(3).getItem() instanceof GogglesItem) {
            if (GogglesItem.isOn(client.player) && !(client.getCameraEntity() instanceof FlyableEntity)) {
//                StaticRenderer.render(3, 8, 7, 12, tickDelta);

                // pre render setup
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.defaultBlendFunc();
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.disableAlphaTest();

                RenderSystem.matrixMode(GL11.GL_PROJECTION);
                RenderSystem.loadIdentity();
                RenderSystem.ortho(
                    0.0,
                    ClientInitializer.getStaticRenderer().calculateScaledWidth(
                        client.getWindow().getFramebufferHeight(),
                        client.getWindow().getFramebufferWidth()
                    ),
                    ClientInitializer.getStaticRenderer().calculateScaledHeight(
                        client.getWindow().getFramebufferHeight(),
                        client.getWindow().getFramebufferWidth()
                    ),
                    0.0,
                    1000.0,
                    3000.0
                );
                RenderSystem.matrixMode(GL11.GL_MODELVIEW);

                GL11.glPointSize(
                    ClientInitializer.getStaticRenderer().calculateScalar(
                        client.getWindow().getFramebufferHeight(),
                        client.getWindow().getFramebufferWidth()
                    )
                );

                // render static
                ClientInitializer.getStaticRenderer().render(
                    client.getWindow().getFramebufferHeight(),
                    client.getWindow().getFramebufferWidth(),
                    tickDelta
                );

                // post render reset
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
                RenderSystem.enableAlphaTest();
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

                RenderSystem.matrixMode(GL11.GL_PROJECTION);
                RenderSystem.loadIdentity();
                RenderSystem.ortho(
                    0.0,
                    client.getWindow().getScaledWidth(),
                    client.getWindow().getScaledHeight(),
                    0.0,
                    1000.0,
                    3000.0
                );
                RenderSystem.matrixMode(GL11.GL_MODELVIEW);

                GL11.glPointSize(1);
            }
        }
    }

    /**
     * This mixin method removes the crosshair from the player's screen whenever
     * they are flying a {@link QuadcopterEntity}.
     * @param matrices the matrix stack
     * @param info required by every mixin injection
     */
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void renderCrosshair(MatrixStack matrices, CallbackInfo info) {
        if (GogglesItem.isInGoggles()) {
            info.cancel();
        }
    }

    /**
     * This mixin method removes the experience bar from the player's screen
     * when in adventure or survival modes
     * and are flying a {@link QuadcopterEntity}
     * @param matrices the matrix stack
     * @param x the x coordinate
     * @param info required by every mixin injection
     */
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    public void renderExperienceBar(MatrixStack matrices, int x, CallbackInfo info) {
        if (GogglesItem.isInGoggles()) {
            info.cancel();
        }
    }
}
