package net.claustra01.poweredtfc.display;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import net.dries007.tfc.config.ItemTemperatureDisplayStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.SignBlockEntity;

abstract class TfcTemperatureDisplaySource extends DisplaySource {
    @Override
    public void transferData(DisplayLinkContext context, DisplayTarget activeTarget, int line) {
        super.transferData(context, activeTarget, line);
        if (context.sourceConfig().getInt("Mode") == 1
            && context.getTargetBlockEntity() instanceof SignBlockEntity sign) {
            // Create flattens sign lines to literals, so restore the TFC color on the stored component.
            MutableComponent temperature = provideText(context, activeTarget.provideStats(context)).getFirst();
            sign.setText(sign.getFrontText().setMessage(line, temperature), true);
            sign.setText(sign.getBackText().setMessage(line, temperature), false);
            context.level().sendBlockUpdated(context.getTargetPos(), sign.getBlockState(), sign.getBlockState(), 2);
        }
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }

    protected static MutableComponent formatTemperature(float temperature, boolean colorName) {
        MutableComponent formatted = (colorName ? ItemTemperatureDisplayStyle.COLOR
            : ItemTemperatureDisplayStyle.CELSIUS).formatColored(temperature);
        return formatted != null ? formatted : Component.translatable("poweredtfc.display_source.no_heat");
    }
}
