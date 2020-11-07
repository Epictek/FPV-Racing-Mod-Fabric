package dev.lazurite.fpvracing.mixin;

import dev.lazurite.fpvracing.util.Matrix4fInject;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.vecmath.Vector3f;

@Mixin(Matrix4f.class)
public class Matrix4fMixin implements Matrix4fInject {
    @Shadow float a02;
    @Shadow float a12;
    @Shadow float a22;

    public Vector3f matrixToVector() {
        return new Vector3f(a02, a12, a22);
    }

    public void fromQuaternion(Quaternion q) {
        double tmp1;
        double tmp2;

        double sqw = q.getW()*q.getW();
        double sqx = q.getX()*q.getX();
        double sqy = q.getY()*q.getY();
        double sqz = q.getZ()*q.getZ();

        double invs = 1 / (sqx + sqy + sqz + sqw);
        a22 = (float) ((-sqx - sqy + sqz + sqw)*invs);

        tmp1 = q.getX()*q.getZ();
        tmp2 = q.getY()*q.getW();
        a02 = (float) (2.0 * (tmp1 + tmp2)*invs);

        tmp1 = q.getY()*q.getZ();
        tmp2 = q.getX()*q.getW();
        a12 = (float) (2.0 * (tmp1 - tmp2)*invs);
    }
}
