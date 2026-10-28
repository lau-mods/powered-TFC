package net.claustra01.poweredtfc.mixin;

import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import net.claustra01.poweredtfc.air.EncasedFanAir;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EncasedFanBlockEntity.class)
public abstract class EncasedFanBlockEntityMixin {
    @Unique
    private int poweredtfc$airPushCooldown;

    @Inject(method = "tick", at = @At("RETURN"))
    private void poweredtfc$supplyAir(CallbackInfo callback) {
        if (poweredtfc$airPushCooldown-- <= 0) {
            poweredtfc$airPushCooldown = 19;
            EncasedFanAir.supplyAir((EncasedFanBlockEntity) (Object) this);
        }
    }
}
