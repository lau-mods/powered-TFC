package dev.poweredtfc.poweredtfc.gametest;

import java.io.File;

import javax.xml.parsers.ParserConfigurationException;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import dev.poweredtfc.poweredtfc.PoweredTFC;
import net.dries007.tfc.common.blockentities.CharcoalForgeBlockEntity;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.capabilities.BlockCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PoweredTFC.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IntegrationGameTests {
    @BeforeBatch(batch = PoweredTFC.MOD_ID)
    public static void configureReport(ServerLevel level) throws ParserConfigurationException {
        String report = System.getProperty("poweredtfc.gametest.report");
        if (report != null) {
            GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(new File(report)));
        }
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void stageTemperatures(GameTestHelper helper) {
        BlockPos burnerPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos targetPos = burnerPos.above();
        helper.getLevel().setBlockAndUpdate(burnerPos, AllBlocks.BLAZE_BURNER.getDefaultState()
            .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING));
        helper.getLevel().setBlockAndUpdate(targetPos, TFCBlocks.CRUCIBLE.get().defaultBlockState());

        helper.runAfterDelay(2, () -> {
            assertTemperature(helper, targetPos, 80, "Smouldering");
            setBurnerStage(helper, burnerPos, BlazeBurnerBlock.HeatLevel.FADING);
        });
        helper.runAfterDelay(4, () -> {
            assertTemperature(helper, targetPos, 1100, "Fading");
            setBurnerStage(helper, burnerPos, BlazeBurnerBlock.HeatLevel.KINDLED);
        });
        helper.runAfterDelay(6, () -> {
            assertTemperature(helper, targetPos, 1300, "Kindled");
            setBurnerStage(helper, burnerPos, BlazeBurnerBlock.HeatLevel.SEETHING);
        });
        helper.runAfterDelay(8, () -> {
            assertTemperature(helper, targetPos, 1800, "Seething");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID,
        timeoutTicks = 60)
    public static void aboveOnlyAndStopped(GameTestHelper helper) {
        BlockPos burnerPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos abovePos = burnerPos.above();
        BlockPos sidePos = burnerPos.east();
        BlockPos belowPos = burnerPos.below();
        helper.getLevel().setBlockAndUpdate(abovePos, TFCBlocks.CRUCIBLE.get().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(sidePos, TFCBlocks.CRUCIBLE.get().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(belowPos, TFCBlocks.CRUCIBLE.get().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(burnerPos, AllBlocks.BLAZE_BURNER.getDefaultState()
            .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED));
        setBurnerStage(helper, burnerPos, BlazeBurnerBlock.HeatLevel.KINDLED);

        helper.runAfterDelay(3, () -> {
            assertTemperature(helper, abovePos, 1300, "Block directly above");
            assertTemperature(helper, sidePos, 0, "Block beside burner");
            assertTemperature(helper, belowPos, 0, "Block below burner");
            setBurnerStage(helper, burnerPos, BlazeBurnerBlock.HeatLevel.NONE);
        });
        helper.runAfterDelay(28, () -> {
            var heat = helper.getLevel().getCapability(BlockCapabilities.HEAT, abovePos, Direction.DOWN);
            helper.assertTrue(heat != null && heat.getTemperature() < 1300,
                "The same crucible must cool after the burner stops");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID,
        timeoutTicks = 70)
    public static void horizontalFanSpeeds(GameTestHelper helper) {
        BlockPos fanPos = helper.absolutePos(new BlockPos(2, 2, 2));
        EncasedFanBlockEntity fan = placePoweredFan(helper, fanPos, Direction.EAST, 64);
        CharcoalForgeBlockEntity forge = placeForge(helper, fanPos.east());

        helper.runAfterDelay(1, () -> {
            helper.assertTrue(Math.abs(fan.getSpeed()) == 64, "Creative Motor must run the fan at 64 RPM");
            helper.assertTrue(forge.getAirTicks() >= 163 && forge.getAirTicks() <= 164,
                "A 64 RPM push must apply 164 air before the forge consumes its current tick");
            helper.getLevel().removeBlock(forge.getBlockPos(), false);
            placeForge(helper, fanPos.east());
            setMotorSpeed(helper, fanPos.west(), 128);
        });
        helper.runAfterDelay(21, () -> {
            CharcoalForgeBlockEntity fastForge = (CharcoalForgeBlockEntity) helper.getLevel()
                .getBlockEntity(fanPos.east());
            helper.assertTrue(Math.abs(fan.getSpeed()) == 128, "Creative Motor must run the fan at 128 RPM");
            helper.assertTrue(fastForge.getAirTicks() >= 227 && fastForge.getAirTicks() <= 228,
                "A 128 RPM push must apply 228 air before the forge consumes its current tick");
            helper.assertTrue(fastForge.getAirTicks() > forge.getAirTicks(),
                "128 RPM must apply more air than 64 RPM");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID,
        timeoutTicks = 100)
    public static void stoppedAndVerticalFan(GameTestHelper helper) {
        BlockPos fanPos = helper.absolutePos(new BlockPos(2, 2, 2));
        EncasedFanBlockEntity fan = placePoweredFan(helper, fanPos, Direction.EAST, 64);
        CharcoalForgeBlockEntity forge = placeForge(helper, fanPos.east());
        int[] airBeforeStop = new int[1];

        helper.runAfterDelay(1, () -> {
            helper.assertTrue(forge.getAirTicks() > 0, "The running fan must supply air before it is stopped");
            airBeforeStop[0] = forge.getAirTicks();
            helper.getLevel().removeBlock(fanPos.west(), false);
        });
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(fan.getSpeed() == 0, "Removing the kinetic source must stop the fan");
            helper.assertTrue(forge.getAirTicks() <= airBeforeStop[0] - 20,
                "A stopped fan must not replenish the forge during a full air push interval");
            helper.getLevel().removeBlock(fanPos, false);
            helper.getLevel().removeBlock(forge.getBlockPos(), false);
            placePoweredFan(helper, fanPos, Direction.UP, 128);
            placeForge(helper, fanPos.above());
        });
        helper.runAfterDelay(47, () -> {
            EncasedFanBlockEntity verticalFan = (EncasedFanBlockEntity) helper.getLevel().getBlockEntity(fanPos);
            CharcoalForgeBlockEntity verticalForge = (CharcoalForgeBlockEntity) helper.getLevel()
                .getBlockEntity(fanPos.above());
            helper.assertTrue(Math.abs(verticalFan.getSpeed()) == 128,
                "Creative Motor must run the vertical fan at 128 RPM");
            helper.assertTrue(verticalForge.getAirTicks() == 0,
                "A vertical fan must not supply air to a device in its airflow path");
            helper.succeed();
        });
    }

    private static EncasedFanBlockEntity placePoweredFan(GameTestHelper helper, BlockPos fanPos,
        Direction facing, int speed) {
        BlockPos motorPos = fanPos.relative(facing.getOpposite());
        helper.getLevel().setBlockAndUpdate(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState()
            .setValue(BlockStateProperties.FACING, facing));
        setMotorSpeed(helper, motorPos, speed);
        helper.getLevel().setBlockAndUpdate(fanPos, AllBlocks.ENCASED_FAN.getDefaultState()
            .setValue(BlockStateProperties.FACING, facing));
        return (EncasedFanBlockEntity) helper.getLevel().getBlockEntity(fanPos);
    }

    private static void setMotorSpeed(GameTestHelper helper, BlockPos motorPos, int speed) {
        CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) helper.getLevel().getBlockEntity(motorPos);
        motor.generatedSpeed.setValue(speed);
    }

    private static CharcoalForgeBlockEntity placeForge(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos, TFCBlocks.CHARCOAL_FORGE.get().defaultBlockState());
        return (CharcoalForgeBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static void setBurnerStage(GameTestHelper helper, BlockPos pos,
        BlazeBurnerBlock.HeatLevel heatLevel) {
        BlazeBurnerBlockEntity burner = (BlazeBurnerBlockEntity) helper.getLevel().getBlockEntity(pos);
        CompoundTag fuel = new CompoundTag();
        fuel.putInt("fuelLevel", switch (heatLevel) {
            case NONE -> 0;
            case SEETHING -> 2;
            default -> 1;
        });
        fuel.putInt("burnTimeRemaining", switch (heatLevel) {
            case NONE -> 0;
            case FADING -> 100;
            default -> 1000;
        });
        burner.loadWithComponents(fuel, helper.getLevel().registryAccess());
        burner.updateBlockState();
    }

    private static void assertTemperature(GameTestHelper helper, BlockPos pos, float expected,
        String stage) {
        var heat = helper.getLevel().getCapability(BlockCapabilities.HEAT, pos, Direction.DOWN);
        helper.assertTrue(heat != null && heat.getTemperature() <= expected
                && heat.getTemperature() >= expected - 1,
            stage + " must reflect a " + expected + " degree Celsius heat supply, got "
                + (heat == null ? "no heat capability" : heat.getTemperature()));
    }
}
