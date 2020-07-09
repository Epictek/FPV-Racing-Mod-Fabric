package bluevista.fpvracingmod.client.input;

import bluevista.fpvracingmod.client.controller.Controller;
import bluevista.fpvracingmod.client.math.helper.QuaternionHelper;
import bluevista.fpvracingmod.server.entities.DroneEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class InputTick {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static boolean shouldTick;

    private static float prevX;
    private static float prevY;
    private static float prevZ;
    private static float prevT;

    public static void tick(DroneEntity drone, float delta) {
        if(shouldTick()) {

            // Note: There's probably a better way of doing this, but yeah... it ignores input within the deadzone range

            // order: axis, rate, expo, superRate

            float currX = -Controller.getBetaflightAxis(Controller.PITCH_NUM, Controller.RATE, Controller.EXPO, Controller.SUPER_RATE);
            float currY = -Controller.getBetaflightAxis(Controller.YAW_NUM, Controller.RATE, Controller.EXPO, Controller.SUPER_RATE);
            float currZ = -Controller.getBetaflightAxis(Controller.ROLL_NUM, Controller.RATE, Controller.EXPO, Controller.SUPER_RATE);
            float currT = Controller.getBetaflightAxis(Controller.THROTTLE_NUM, Controller.RATE, Controller.EXPO, Controller.SUPER_RATE) + 1;

            if (Controller.DEADZONE != 0) {
                float halfDeadzone = Controller.DEADZONE / 2.0f;

                if (currX < halfDeadzone && currX > -halfDeadzone) {
                    currX = 0.0f;
                }

                if (currY < halfDeadzone && currY > -halfDeadzone) {
                    currY = 0.0f;
                }

                if (currZ < halfDeadzone && currZ > -halfDeadzone) {
                    currZ = 0.0f;
                }

                if (currT < halfDeadzone && currT > -halfDeadzone) {
                    currT = 0.0f;
                }
            }

            float deltaX = prevX + (currX - prevX) * delta;
            float deltaY = prevY + (currY - prevY) * delta;
            float deltaZ = prevZ + (currZ - prevZ) * delta;
            float deltaT = prevT + (currT - prevT) * delta;

            drone.setOrientation(QuaternionHelper.rotateX(drone.getOrientation(), deltaX));
            drone.setOrientation(QuaternionHelper.rotateY(drone.getOrientation(), deltaY));
            drone.setOrientation(QuaternionHelper.rotateZ(drone.getOrientation(), deltaZ));
            drone.setThrottle(deltaT);

            prevX = currX;
            prevY = currY;
            prevZ = currZ;
            prevT = currT;
        }
    }

    public static void setShouldTick(boolean should) {
        shouldTick = should;
    }

    public static boolean shouldTick() {
        return false;//shouldTick && !mc.isPaused();
    }

}