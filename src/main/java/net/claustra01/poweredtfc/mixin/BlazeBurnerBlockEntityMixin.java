package net.claustra01.poweredtfc.mixin;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.claustra01.poweredtfc.heat.BlazeBurnerHeat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlazeBurnerBlockEntity.class)
public abstract class BlazeBurnerBlockEntityMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void poweredtfc$supplyHeat(CallbackInfo callback) {
        BlazeBurnerHeat.supplyHeat((BlazeBurnerBlockEntity) (Object) this);
    }
}
