package net.claustra01.poweredtfc.client;

import net.claustra01.poweredtfc.PoweredTFC;
import net.claustra01.poweredtfc.registry.PoweredTFCRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = PoweredTFC.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class PoweredTFCClientEvents {
    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PoweredTFCRegistry.CREATE_TO_TFC_CONVERTER_BLOCK_ENTITY.get(),
            MechanicalConverterRenderer::new);
        event.registerBlockEntityRenderer(PoweredTFCRegistry.TFC_TO_CREATE_CONVERTER_BLOCK_ENTITY.get(),
            MechanicalConverterRenderer::new);
    }

    private PoweredTFCClientEvents() {
    }
}
