package net.claustra01.poweredtfc.display;

import java.util.List;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.CreateLang;
import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public final class CrucibleDisplaySource extends TfcTemperatureDisplaySource {
    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof CrucibleBlockEntity crucible)) {
            return EMPTY;
        }
        return List.of(formatTemperature(crucible.getTemperature(), context.sourceConfig().getInt("Mode") == 1));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder,
        boolean isFirstLine) {
        if (isFirstLine) {
            builder.addSelectionScrollInput(0, 137, (input, label) -> input.forOptions(
                CreateLang.translatedOptions("display_source.tfc_crucible", "temperature", "color")), "Mode");
        }
    }
}
