package net.claustra01.poweredtfc.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BasinBlockEntity.class)
public abstract class BasinBlockEntityMixin {
    @ModifyArg(method = "addBehaviours",
        at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/fluid/SmartFluidTankBehaviour;"
                + "<init>(Lcom/simibubi/create/foundation/blockEntity/behaviour/BehaviourType;"
                + "Lcom/simibubi/create/foundation/blockEntity/SmartBlockEntity;IIZ)V",
            ordinal = 0),
        index = 2)
    private int poweredtfc$fourInputTanks(int tankCount) {
        return 4;
    }
}
