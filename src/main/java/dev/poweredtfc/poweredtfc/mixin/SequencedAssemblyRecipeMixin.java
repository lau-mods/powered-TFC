package dev.poweredtfc.poweredtfc.mixin;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.dries007.tfc.common.component.TFCComponents;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.heat.HeatComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SequencedAssemblyRecipe.class)
public abstract class SequencedAssemblyRecipeMixin {
    @Inject(method = "advance", at = @At("RETURN"))
    private void poweredtfc$carryHeat(ResourceLocation id, ItemStack input, RandomSource random,
        CallbackInfoReturnable<ItemStack> callback) {
        HeatComponent inputHeat = input.get(TFCComponents.HEAT);
        if (inputHeat == null) {
            return;
        }

        ItemStack output = callback.getReturnValue();
        if (output.has(AllDataComponents.SEQUENCED_ASSEMBLY)) {
            output.set(TFCComponents.HEAT,
                HeatComponent.of(HeatCapability.view(input).getHeatCapacity(),
                    HeatCapability.getTemperature(input)));
        } else if (HeatCapability.getDefinition(output) != null) {
            HeatCapability.setTemperature(output, inputHeat.getTemperature());
        }
    }
}
