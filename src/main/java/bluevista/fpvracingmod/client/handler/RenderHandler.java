package bluevista.fpvracingmod.client.handler;

import bluevista.fpvracingmod.client.controls.Controller;
import bluevista.fpvracingmod.client.math.QuaternionHelper;
import bluevista.fpvracingmod.server.entities.DroneEntity;
import bluevista.fpvracingmod.server.entities.ViewHandler;
import bluevista.fpvracingmod.server.items.GogglesItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class RenderHandler {

    private static MinecraftClient mc = MinecraftClient.getInstance();
    private static ViewHandler view;
    public static DroneEntity currentDrone;
    public static Vec3d playerPos;

    private static float prevX;
    private static float prevY;
    private static float prevZ;

    public static void tick(float delta) {
//        screenRotationTick();

        if (mc.player != null) {
            if (mc.player.getItemsHand().iterator().next().getItem() instanceof GogglesItem) { // if the player is holding the goggles

                if (view != null) {
                        view.clientTick(delta); // ...update the ViewHandler...
//					mc.player.move(MoverType.PLAYER, new Vec3d(playerPos.x - (mc.player.getPositionVec()).x, playerPos.y - (mc.player.getPositionVec()).y, playerPos.z - (mc.player.getPositionVec()).z));
                }

                if (!(mc.getCameraEntity() instanceof ViewHandler)) { // ...and if a ViewHandler doesn't exist, create one
                    currentDrone = DroneEntity.getNearestTo(mc.player);
                    if(currentDrone != null) {
                        playerPos = mc.player.getPos();
                        view = new ViewHandler(mc.world, currentDrone);
                        mc.setCameraEntity(view);
                    }

                } else if (((ViewHandler) mc.getCameraEntity()).getTarget() instanceof DroneEntity) {
                    DroneEntity drone = (DroneEntity) ((ViewHandler) mc.getCameraEntity()).getTarget();
                    inputTick(drone, delta);
                }

            } else if(mc.getCameraEntity() instanceof ViewHandler) {
                view = null;
                mc.setCameraEntity(mc.player); // switch back to player
                mc.player.setPos(playerPos.x, playerPos.y, playerPos.z);
            }
        }
    }

    public static void screenRotationTick() {
        Entity currentViewEntity = mc.getCameraEntity();
        if(currentViewEntity instanceof ViewHandler) {
            if (((ViewHandler) currentViewEntity).getTarget() instanceof DroneEntity) {
                DroneEntity drone = (DroneEntity) ((ViewHandler) currentViewEntity).getTarget();
                QuaternionHelper.applyRotQuat(drone.getOrientation());
            }
        }
    }

    public static void inputTick(DroneEntity drone, float delta) {
        float currX = -Controller.getAxis(2);
        float currY = -Controller.getAxis(3);
        float currZ = -Controller.getAxis(1);

        float deltaX = prevX + (currX - prevX) * delta;
        float deltaY = prevY + (currY - prevY) * delta;
        float deltaZ = prevZ + (currZ - prevZ) * delta;

        drone.setOrientation(QuaternionHelper.rotateX(drone.getOrientation(), deltaX));
        drone.setOrientation(QuaternionHelper.rotateY(drone.getOrientation(), deltaY));
        drone.setOrientation(QuaternionHelper.rotateZ(drone.getOrientation(), deltaZ));
        drone.setThrottle(Controller.getAxis(0) + 1);

        prevX = currX;
        prevY = currY;
        prevZ = currZ;
    }

}