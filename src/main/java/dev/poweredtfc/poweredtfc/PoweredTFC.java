package dev.poweredtfc.poweredtfc;

import com.simibubi.create.api.stress.BlockStressValues;
import dev.poweredtfc.poweredtfc.config.PoweredTFCConfig;
import dev.poweredtfc.poweredtfc.registry.PoweredTFCRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(PoweredTFC.MOD_ID)
public final class PoweredTFC {
    public static final String MOD_ID = "poweredtfc";

    public PoweredTFC(IEventBus modEventBus, ModContainer modContainer) {
        PoweredTFCRegistry.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, PoweredTFCConfig.SPEC);
        modEventBus.addListener(PoweredTFC::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BlockStressValues.IMPACTS.register(PoweredTFCRegistry.CREATE_TO_TFC_CONVERTER.get(),
                PoweredTFCConfig.CREATE_TO_TFC_STRESS_IMPACT::get);
            BlockStressValues.CAPACITIES.register(PoweredTFCRegistry.TFC_TO_CREATE_CONVERTER.get(),
                PoweredTFCConfig.TFC_TO_CREATE_STRESS_CAPACITY::get);
        });
    }
}
