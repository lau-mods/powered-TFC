package net.claustra01.poweredtfc.air;

import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import net.claustra01.poweredtfc.config.PoweredTFCConfig;
import net.dries007.tfc.common.blocks.devices.IBellowsConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public final class EncasedFanAir {
    private EncasedFanAir() {
    }

    public static void supplyAir(EncasedFanBlockEntity fan) {
        if (fan.getLevel().isClientSide() || fan.getSpeed() == 0) {
            return;
        }
        Direction airflow = fan.getAirFlowDirection();
        if (airflow.getAxis().isVertical()) {
            return;
        }
        for (IBellowsConsumer.Offset offset : IBellowsConsumer.offsets()) {
            BlockPos targetPos = fan.getBlockPos().above(offset.up())
                .relative(airflow, offset.out())
                .relative(airflow.getClockWise(), offset.side());
            BlockState targetState = fan.getLevel().getBlockState(targetPos);
            if (targetState.getBlock() instanceof IBellowsConsumer consumer
                && consumer.canAcceptAir(fan.getLevel(), targetPos, targetState)) {
                consumer.intakeAir(fan.getLevel(), targetPos, targetState,
                    (int) (PoweredTFCConfig.BASE_AIR.get()
                        + Math.abs(fan.getSpeed()) * PoweredTFCConfig.SPEED_MULTIPLIER.get()));
            }
        }
    }
}
