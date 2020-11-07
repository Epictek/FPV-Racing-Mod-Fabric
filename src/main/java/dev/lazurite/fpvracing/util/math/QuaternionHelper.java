package dev.lazurite.fpvracing.util.math;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.Quaternion;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * A helper class for {@link Quat4f} which contains methods to perform
 * many different operations.
 * @author Ethan Johnson
 */
public class QuaternionHelper {
    /**
     * Rotate the given {@link Quat4f} by the given number of degrees on the X axis.
     * @param quat the {@link Quat4f} to perform the operation on
     * @param deg number of degrees to rotate by
     */
    public static void rotateX(Quat4f quat, double deg) {
        double radHalfAngle = Math.toRadians(deg) / 2.0;
        Quat4f rot = new Quat4f();
        rot.x = (float) Math.sin(radHalfAngle);
        rot.w = (float) Math.cos(radHalfAngle);
        quat.mul(rot);
    }

    /**
     * Rotate the given {@link Quat4f} by the given number of degrees on the Y axis.
     * @param quat the {@link Quat4f} to perform the operation on
     * @param deg number of degrees to rotate by
     */
    public static void rotateY(Quat4f quat, double deg) {
        double radHalfAngle = Math.toRadians(deg) / 2.0;
        Quat4f rot = new Quat4f();
        rot.y = (float) Math.sin(radHalfAngle);
        rot.w = (float) Math.cos(radHalfAngle);
        quat.mul(rot);
    }

    /**
     * Rotate the given {@link Quat4f} by the given number of degrees on the Z axis.
     * @param quat the {@link Quat4f} to perform the operation on
     * @param deg number of degrees to rotate by
     */
    public static void rotateZ(Quat4f quat, double deg) {
        double radHalfAngle = Math.toRadians(deg) / 2.0;
        Quat4f rot = new Quat4f();
        rot.z = (float) Math.sin(radHalfAngle);
        rot.w = (float) Math.cos(radHalfAngle);
        quat.mul(rot);
    }

    /**
     * Converts the given {@link Quat4f} to a vector containing three axes of rotation in degrees.
     * The order is (roll, pitch, yaw).
     * @param quat the {@link Quat4f} to extract the euler angles from
     * @return a new vector containing three rotations in degrees
     */
    public static Vector3f toEulerAngles(Quat4f quat) {
        Quat4f q = new Quat4f();
        q.set(quat.z, quat.x, quat.y, quat.w);

        Vector3f angles = new Vector3f();

        // roll (x-axis rotation)
        double sinr_cosp = 2 * (q.w * q.x + q.y * q.z);
        double cosr_cosp = 1 - 2 * (q.x * q.x + q.y * q.y);
        angles.x = (float) Math.atan2(sinr_cosp, cosr_cosp);

        // pitch (y-axis rotation)
        double sinp = 2 * (q.w * q.y - q.z * q.x);
        if (Math.abs(sinp) >= 1)
            angles.y = (float) Math.copySign(Math.PI / 2, sinp); // use 90 degrees if out of range
        else
            angles.y = (float) Math.asin(sinp);

        // yaw (z-axis rotation)
        double siny_cosp = 2 * (q.w * q.z + q.x * q.y);
        double cosy_cosp = 1 - 2 * (q.y * q.y + q.z * q.z);
        angles.z = (float) Math.atan2(siny_cosp, cosy_cosp);

        return angles;
    }

    /**
     * Places the given {@link Quat4f} information inside of the given {@link CompoundTag}.
     * @param quat the {@link Quat4f} to place in a {@link CompoundTag}
     * @param tag the {@link CompoundTag} to place the {@link Quat4f} inside of
     */
    public static void toTag(Quat4f quat, CompoundTag tag) {
        tag.putFloat("x", quat.x);
        tag.putFloat("y", quat.y);
        tag.putFloat("z", quat.z);
        tag.putFloat("w", quat.w);
    }

    /**
     * Retrieves {@link Quat4f} information from the given {@link CompoundTag}.
     * @param tag the {@link CompoundTag} to retrieve the {@link Quat4f} information from
     * @return the new {@link Quat4f}
     */
    public static Quat4f fromTag(CompoundTag tag) {
        Quat4f quat = new Quat4f();
        quat.set(tag.getFloat("x"),
                 tag.getFloat("y"),
                 tag.getFloat("z"),
                 tag.getFloat("w"));
        return quat;
    }

    /**
     * Converts a {@link Quat4f} to a {@link Quaternion}.
     * @param quat the {@link Quat4f} to convert
     * @return the new {@link Quaternion}
     */
    public static Quaternion quat4fToQuaternion(Quat4f quat) {
        return new Quaternion(quat.x, quat.y, quat.z, quat.w);
    }

    /**
     * Converts a {@link Quaternion} to a {@link Quat4f}.
     * @param quat the {@link Quaternion} to convert
     * @return the new {@link Quat4f}
     */
    public static Quat4f quaternionToQuat4f(Quaternion quat) {
        Quat4f q = new Quat4f();
        q.x = quat.getX();
        q.y = quat.getY();
        q.z = quat.getZ();
        q.w = quat.getW();
        return q;
    }

    /**
     * Gets the yaw rotation from the given {@link Quat4f}.
     * @param quat the {@link Quat4f} to get the angle from
     * @return the yaw angle
     */
    public static float getYaw(Quat4f quat) {
        return -1 * (float) Math.toDegrees(QuaternionHelper.toEulerAngles(quat).z);
    }

    /**
     * Gets the pitch rotation from the given {@link Quat4f}.
     * @param quat the {@link Quat4f} to get the angle from
     * @return the pitch angle
     */
    public static float getPitch(Quat4f quat) {
        return (float) Math.toDegrees(QuaternionHelper.toEulerAngles(quat).y);
    }

//    /**
//     * Interpolates between the start {@link Quat4f} and the end {@link Quat4f}.
//     * @param start the beginning
//     * @param end the end
//     * @param tickDelta minecraft tick delta
//     * @return the new slerped {@link Quat4f}
//     */
//    public static Quat4f slerp(Quat4f start, Quat4f end, float tickDelta) {
//        Quat4f out = new Quat4f();
//        out.interpolate(start, end, tickDelta);
//        return out;
//    }

    public static Quat4f slerp(Quat4f q1, Quat4f q2, float t) {
        Quat4f out = new Quat4f();

        if (q1.x == q2.x && q1.y == q2.y && q1.z == q2.z && q1.w == q2.w) {
            out.set(q1);
            return out;
        }

        float result = (q1.x * q2.x) + (q1.y * q2.y) + (q1.z * q2.z)
                + (q1.w * q2.w);

        if (result < 0.0f) {
            // Negate the second quaternion and the result of the dot product
            q2.x = -q2.x;
            q2.y = -q2.y;
            q2.z = -q2.z;
            q2.w = -q2.w;
            result = -result;
        }

        // Set the first and second scale for the interpolation
        float scale0 = 1 - t;
        float scale1 = t;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if ((1 - result) > 0.1f) {// Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            float theta = (float) Math.acos(result);
            float invSinTheta = 1f / (float) Math.sin(theta);

            // Calculate the scale for q1 and q2, according to the angle and
            // it's sine value
            scale0 = (float) Math.sin((1 - t) * theta) * invSinTheta;
            scale1 = (float) Math.sin((t * theta)) * invSinTheta;
        }

        // Calculate the x, y, z and w values for the quaternion by using a
        // special
        // form of linear interpolation for quaternions.
        out.x = (scale0 * q1.x) + (scale1 * q2.x);
        out.y = (scale0 * q1.y) + (scale1 * q2.y);
        out.z = (scale0 * q1.z) + (scale1 * q2.z);
        out.w = (scale0 * q1.w) + (scale1 * q2.w);

        // Return the interpolated quaternion
        return out;
    }
}
