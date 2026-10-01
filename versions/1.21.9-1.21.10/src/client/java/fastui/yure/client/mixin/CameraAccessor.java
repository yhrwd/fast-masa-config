package fastui.yure.client.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Camera.getPos() was removed from vanilla in 1.21.11, so the indicator reads
 * the backing pos field directly; yarn names it "pos" on 1.21.9 through 1.21.11.
 */
@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("pos")
    Vec3d fastui$getPos();
}
