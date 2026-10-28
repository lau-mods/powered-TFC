package net.claustra01.poweredtfc.mixin;

import com.simibubi.create.content.processing.basin.BasinRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BasinRecipe.class)
public abstract class BasinRecipeMixin {
    @ModifyConstant(method = "getMaxFluidInputCount", constant = @Constant(intValue = 2))
    private int poweredtfc$allowFourFluidInputs(int maximum) {
        return 4;
    }
}
