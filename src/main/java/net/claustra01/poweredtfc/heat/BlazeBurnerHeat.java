package net.claustra01.poweredtfc.heat;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.claustra01.poweredtfc.config.PoweredTFCConfig;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.minecraft.core.Direction;

public final class BlazeBurnerHeat {
    private BlazeBurnerHeat() {
    }

    public static void supplyHeat(BlazeBurnerBlockEntity burner) {
        if (burner.getLevel().isClientSide()) {
            return;
        }
        float temperature = switch (burner.getBlockState().getValue(BlazeBurnerBlock.HEAT_LEVEL)) {
            case SMOULDERING -> PoweredTFCConfig.SMOULDERING_HEAT.get();
            case FADING -> PoweredTFCConfig.FADING_HEAT.get();
            case KINDLED -> PoweredTFCConfig.KINDLED_HEAT.get();
            case SEETHING -> PoweredTFCConfig.SEETHING_HEAT.get();
            case NONE -> 0;
        };
        if (temperature == 0) {
            return;
        }
        HeatCapability.provideHeatTo(burner.getLevel(), burner.getBlockPos().above(), Direction.DOWN, temperature);
    }
}
