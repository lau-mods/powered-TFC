package dev.poweredtfc.poweredtfc.heat;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import dev.poweredtfc.poweredtfc.PoweredTFCConfig;
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
            case SMOULDERING -> PoweredTFCConfig.SMOULDERING_HEAT;
            case FADING -> PoweredTFCConfig.FADING_HEAT;
            case KINDLED -> PoweredTFCConfig.KINDLED_HEAT;
            case SEETHING -> PoweredTFCConfig.SEETHING_HEAT;
            case NONE -> 0;
        };
        if (temperature == 0) {
            return;
        }
        HeatCapability.provideHeatTo(burner.getLevel(), burner.getBlockPos().above(), Direction.DOWN, temperature);
    }
}
