package dev.poweredtfc.poweredtfc.display;

import java.util.List;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.CreateLang;
import net.dries007.tfc.common.blockentities.BlastFurnaceBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public final class BlastFurnaceDisplaySource extends TfcTemperatureDisplaySource {
    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof BlastFurnaceBlockEntity furnace)) {
            return EMPTY;
        }

        MutableComponent text = switch (context.sourceConfig().getInt("Mode")) {
            case 1 -> formatTemperature(furnace.getTemperature(), true);
            case 2 -> Component.literal(String.valueOf(furnace.getInputCount()));
            case 3 -> Component.literal(String.valueOf(furnace.getCatalystCount()));
            case 4 -> Component.literal(String.valueOf(furnace.getFuelCount()));
            case 5 -> formatTemperature(furnace.getTemperature(), false)
                .append(" | " + furnace.getInputCount() + " | " + furnace.getCatalystCount()
                    + " | " + furnace.getFuelCount());
            default -> formatTemperature(furnace.getTemperature(), false);
        };
        return List.of(text);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder,
        boolean isFirstLine) {
        if (isFirstLine) {
            builder.addSelectionScrollInput(0, 137, (input, label) -> input.forOptions(
                CreateLang.translatedOptions("display_source.tfc_blast_furnace", "temperature", "color",
                    "input_count", "catalyst_count", "fuel_count", "compact")), "Mode");
        }
    }
}
