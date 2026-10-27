package dev.poweredtfc.poweredtfc.gametest;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import dev.poweredtfc.poweredtfc.PoweredTFC;
import dev.poweredtfc.poweredtfc.air.EncasedFanAir;
import dev.poweredtfc.poweredtfc.config.PoweredTFCConfig;
import dev.poweredtfc.poweredtfc.converter.CreateToTfcConverterBlockEntity;
import dev.poweredtfc.poweredtfc.converter.MechanicalConverterBlock;
import dev.poweredtfc.poweredtfc.converter.MechanicalPowerConversion;
import dev.poweredtfc.poweredtfc.converter.TfcToCreateConverterBlockEntity;
import dev.poweredtfc.poweredtfc.heat.BlazeBurnerHeat;
import dev.poweredtfc.poweredtfc.registry.PoweredTFCRegistry;
import java.util.List;
import java.util.Set;
import net.dries007.tfc.common.blockentities.CharcoalForgeBlockEntity;
import net.dries007.tfc.common.blockentities.rotation.CreativeRotationBlockEntity;
import net.dries007.tfc.common.blockentities.rotation.RotatingBlockEntity;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.rotation.CreativeRotationBlock;
import net.dries007.tfc.common.capabilities.BlockCapabilities;
import net.dries007.tfc.util.rotation.Rotation;
import net.dries007.tfc.util.rotation.RotationNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PoweredTFC.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConverterGameTests {
    private static final float EPSILON = 0.0001f;

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID,
        timeoutTicks = 40)
    public static void createToTfcSpeedFacesAndNetwork(GameTestHelper helper) {
        BlockPos motorPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos converterPos = motorPos.east();
        BlockPos axlePos = converterPos.east();
        BlockPos conflictingSourcePos = axlePos.east();
        BlockPos wrongConverterPos = helper.absolutePos(new BlockPos(2, 2, 3));
        BlockPos wrongMotorPos = wrongConverterPos.east();
        placeCreateMotor(helper, motorPos, Direction.EAST, 120);
        helper.getLevel().setBlockAndUpdate(converterPos,
            PoweredTFCRegistry.CREATE_TO_TFC_CONVERTER.get().defaultBlockState()
                .setValue(MechanicalConverterBlock.FACING, Direction.WEST));
        placeTfcAxle(helper, axlePos);
        helper.getLevel().setBlockAndUpdate(wrongConverterPos,
            PoweredTFCRegistry.CREATE_TO_TFC_CONVERTER.get().defaultBlockState()
                .setValue(MechanicalConverterBlock.FACING, Direction.WEST));
        placeCreateMotor(helper, wrongMotorPos, Direction.WEST, 120);

        helper.runAfterDelay(8, () -> {
            CreateToTfcConverterBlockEntity converter = blockEntity(helper, converterPos,
                CreateToTfcConverterBlockEntity.class);
            Rotation axleRotation = blockEntity(helper, axlePos, RotatingBlockEntity.class)
                .getRotationNode().rotation();
            helper.assertTrue(Math.abs(axleRotation.speed() - (float) Math.PI / 5) < EPSILON,
                "The adjacent TFC axle must receive pi/5 radians per tick");
            helper.assertTrue(axleRotation.positiveDirection() == Direction.WEST,
                "Positive Create input must turn the TFC axle toward the converter FACING direction");
            helper.assertTrue(converter.getRotationNode().connections().equals(Set.of(Direction.EAST)),
                "The TFC node must only expose the face opposite the Create input");
            helper.assertTrue(PoweredTFCRegistry.CREATE_TO_TFC_CONVERTER.get().hasShaftTowards(
                    helper.getLevel(), converterPos, converter.getBlockState(), Direction.WEST)
                    && !PoweredTFCRegistry.CREATE_TO_TFC_CONVERTER.get().hasShaftTowards(
                    helper.getLevel(), converterPos, converter.getBlockState(), Direction.EAST),
                "The Create shaft must only accept input on FACING");
            helper.assertTrue(RotationNetworkManager.get(helper.getLevel()).getNode(axlePos) != null
                    && !converter.isInvalidInNetwork(),
                "The converter and axle must coexist in TFC's normal rotation network");
            CreateToTfcConverterBlockEntity wrongConverter = blockEntity(helper, wrongConverterPos,
                CreateToTfcConverterBlockEntity.class);
            helper.assertTrue(wrongConverter.getSpeed() == 0
                    && wrongConverter.getRotationNode().rotation().speed() == 0,
                "A Create source connected to the TFC face must not convert");
            setCreateMotorSpeed(helper, motorPos, -120);
        });
        helper.runAfterDelay(11, () -> {
            Rotation axleRotation = blockEntity(helper, axlePos, RotatingBlockEntity.class)
                .getRotationNode().rotation();
            helper.assertTrue(Math.abs(axleRotation.speed() + (float) Math.PI / 5) < EPSILON
                    && axleRotation.positiveDirection() == Direction.EAST,
                "Reversing the Create input must reverse the delivered TFC axle rotation");
            helper.getLevel().setBlockAndUpdate(conflictingSourcePos,
                TFCBlocks.CREATIVE_MOTOR.get().defaultBlockState()
                    .setValue(CreativeRotationBlock.AXIS, Direction.Axis.X));
        });
        helper.runAfterDelay(17, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(conflictingSourcePos).isAir(),
                "TFC must reject and remove a second source connected to the converter network");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID,
        timeoutTicks = 55)
    public static void tfcToCreateSpeedNoInputAndLoop(GameTestHelper helper) {
        BlockPos sourcePos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos converterPos = sourcePos.east();
        BlockPos shaftPos = converterPos.east();
        placeTfcMotor(helper, sourcePos, Direction.Axis.X, 32);
        helper.getLevel().setBlockAndUpdate(converterPos,
            PoweredTFCRegistry.TFC_TO_CREATE_CONVERTER.get().defaultBlockState()
                .setValue(MechanicalConverterBlock.FACING, Direction.EAST));
        placeCreateShaft(helper, shaftPos, Direction.Axis.X);

        helper.runAfterDelay(8, () -> {
            helper.assertTrue(Math.abs(blockEntity(helper, shaftPos, KineticBlockEntity.class).getSpeed() + 120)
                    < EPSILON,
                "A steady pi/5 TFC input must turn the connected Create shaft at signed 120 RPM");
            helper.getLevel().removeBlock(sourcePos, false);
        });
        helper.runAfterDelay(12, () -> {
            TfcToCreateConverterBlockEntity converter = blockEntity(helper, converterPos,
                TfcToCreateConverterBlockEntity.class);
            helper.assertTrue(converter.getGeneratedSpeed() == 0
                    && blockEntity(helper, shaftPos, KineticBlockEntity.class).getSpeed() == 0,
                "The converter and connected shaft must stop without direct TFC input");
            placeTfcMotor(helper, sourcePos, Direction.Axis.Z, 32);
        });
        helper.runAfterDelay(16, () -> {
            helper.assertTrue(blockEntity(helper, converterPos, TfcToCreateConverterBlockEntity.class)
                    .getGeneratedSpeed() == 0,
                "A TFC node that does not output toward the input face must not convert");
            helper.getLevel().setBlockAndUpdate(sourcePos,
                PoweredTFCRegistry.CREATE_TO_TFC_CONVERTER.get().defaultBlockState()
                    .setValue(MechanicalConverterBlock.FACING, Direction.WEST));
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(blockEntity(helper, converterPos, TfcToCreateConverterBlockEntity.class)
                    .getGeneratedSpeed() == 0,
                "A directly adjacent Create-to-TFC converter must not be accepted as input");
            helper.getLevel().removeBlock(converterPos, false);
            helper.getLevel().removeBlock(shaftPos, false);
            helper.getLevel().removeBlock(sourcePos, false);
            placeConverterLoop(helper);
        });
        helper.runAfterDelay(36, () -> helper.getLevel().removeBlock(
            helper.absolutePos(new BlockPos(0, 2, 1)), false));
        helper.runAfterDelay(46, () -> {
            CreateToTfcConverterBlockEntity forward = blockEntity(helper,
                helper.absolutePos(new BlockPos(1, 2, 2)), CreateToTfcConverterBlockEntity.class);
            TfcToCreateConverterBlockEntity reverse = blockEntity(helper,
                helper.absolutePos(new BlockPos(3, 2, 2)), TfcToCreateConverterBlockEntity.class);
            helper.assertTrue(forward.getRotationNode().rotation().speed() == 0
                    && reverse.getGeneratedSpeed() == 0,
                "Converters joined through a TFC axle and a Create network must not sustain a loop");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID,
        timeoutTicks = 40)
    public static void stressAndRecipes(GameTestHelper helper) {
        BlockPos motorPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos converterPos = motorPos.east();
        BlockPos tfcSourcePos = helper.absolutePos(new BlockPos(1, 2, 3));
        BlockPos reversePos = tfcSourcePos.east();
        BlockPos shaftPos = reversePos.east();
        placeCreateMotor(helper, motorPos, Direction.EAST, 120);
        helper.getLevel().setBlockAndUpdate(converterPos,
            PoweredTFCRegistry.CREATE_TO_TFC_CONVERTER.get().defaultBlockState()
                .setValue(MechanicalConverterBlock.FACING, Direction.WEST));
        placeTfcMotor(helper, tfcSourcePos, Direction.Axis.X, 32);
        helper.getLevel().setBlockAndUpdate(reversePos,
            PoweredTFCRegistry.TFC_TO_CREATE_CONVERTER.get().defaultBlockState()
                .setValue(MechanicalConverterBlock.FACING, Direction.EAST));
        placeCreateShaft(helper, shaftPos, Direction.Axis.X);

        helper.runAfterDelay(10, () -> {
            CreateToTfcConverterBlockEntity converter = blockEntity(helper, converterPos,
                CreateToTfcConverterBlockEntity.class);
            TfcToCreateConverterBlockEntity reverse = blockEntity(helper, reversePos,
                TfcToCreateConverterBlockEntity.class);
            helper.assertTrue(converter.getSpeed() != 0
                    && Float.valueOf(64).equals(converter.getOrCreateNetwork().members.get(converter)),
                "A running Create-to-TFC converter must apply 64 SU per RPM to its Create network");
            helper.assertTrue(reverse.getGeneratedSpeed() != 0
                    && Float.valueOf(64).equals(reverse.getOrCreateNetwork().sources.get(reverse)),
                "A running TFC-to-Create converter must provide 64 SU per RPM to its Create network");
            Item oakAxle = helper.getLevel().registryAccess().registryOrThrow(Registries.ITEM)
                .get(ResourceLocation.fromNamespaceAndPath("tfc", "wood/axle/oak"));
            assertRecipe(helper, "create_to_tfc_converter",
                PoweredTFCRegistry.CREATE_TO_TFC_CONVERTER_ITEM.get(), new ItemStack(oakAxle));
            assertRecipe(helper, "tfc_to_create_converter",
                PoweredTFCRegistry.TFC_TO_CREATE_CONVERTER_ITEM.get(),
                new ItemStack(AllBlocks.SHAFT.asItem()));
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID,
        timeoutTicks = 50)
    public static void redstoneStopAndResume(GameTestHelper helper) {
        BlockPos motorPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos converterPos = motorPos.east();
        BlockPos axlePos = converterPos.east();
        BlockPos signalPos = converterPos.above();
        BlockPos tfcSourcePos = helper.absolutePos(new BlockPos(1, 2, 3));
        BlockPos reversePos = tfcSourcePos.east();
        BlockPos shaftPos = reversePos.east();
        BlockPos reverseSignalPos = reversePos.above();
        placeCreateMotor(helper, motorPos, Direction.EAST, 120);
        helper.getLevel().setBlockAndUpdate(converterPos,
            PoweredTFCRegistry.CREATE_TO_TFC_CONVERTER.get().defaultBlockState()
                .setValue(MechanicalConverterBlock.FACING, Direction.WEST));
        placeTfcAxle(helper, axlePos);
        placeTfcMotor(helper, tfcSourcePos, Direction.Axis.X, 32);
        helper.getLevel().setBlockAndUpdate(reversePos,
            PoweredTFCRegistry.TFC_TO_CREATE_CONVERTER.get().defaultBlockState()
                .setValue(MechanicalConverterBlock.FACING, Direction.EAST));
        placeCreateShaft(helper, shaftPos, Direction.Axis.X);

        helper.runAfterDelay(9, () -> {
            CreateToTfcConverterBlockEntity converter = blockEntity(helper, converterPos,
                CreateToTfcConverterBlockEntity.class);
            TfcToCreateConverterBlockEntity reverse = blockEntity(helper, reversePos,
                TfcToCreateConverterBlockEntity.class);
            helper.assertTrue(blockEntity(helper, axlePos, RotatingBlockEntity.class)
                    .getRotationNode().rotation().speed() != 0
                    && Float.valueOf(64).equals(converter.getOrCreateNetwork().members.get(converter)),
                "Create-to-TFC output and its network stress must initially be active");
            helper.assertTrue(blockEntity(helper, shaftPos, KineticBlockEntity.class).getSpeed() != 0
                    && Float.valueOf(64).equals(reverse.getOrCreateNetwork().sources.get(reverse)),
                "TFC-to-Create output and its network capacity must initially be active");
            helper.getLevel().setBlockAndUpdate(signalPos, Blocks.REDSTONE_BLOCK.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(reverseSignalPos, Blocks.REDSTONE_BLOCK.defaultBlockState());
        });
        helper.runAfterDelay(14, () -> {
            CreateToTfcConverterBlockEntity converter = blockEntity(helper, converterPos,
                CreateToTfcConverterBlockEntity.class);
            TfcToCreateConverterBlockEntity reverse = blockEntity(helper, reversePos,
                TfcToCreateConverterBlockEntity.class);
            helper.assertTrue(converter.getBlockState().getValue(MechanicalConverterBlock.POWERED)
                    && blockEntity(helper, axlePos, RotatingBlockEntity.class)
                        .getRotationNode().rotation().speed() == 0
                    && Float.valueOf(0).equals(converter.getOrCreateNetwork().members.get(converter)),
                "Redstone must stop TFC output and remove actual Create network stress");
            helper.assertTrue(reverse.getBlockState().getValue(MechanicalConverterBlock.POWERED)
                    && reverse.getGeneratedSpeed() == 0
                    && blockEntity(helper, shaftPos, KineticBlockEntity.class).getSpeed() == 0,
                "Redstone must stop Create output and its supplied network capacity");
            helper.getLevel().removeBlock(signalPos, false);
            helper.getLevel().removeBlock(reverseSignalPos, false);
        });
        helper.runAfterDelay(21, () -> {
            CreateToTfcConverterBlockEntity converter = blockEntity(helper, converterPos,
                CreateToTfcConverterBlockEntity.class);
            TfcToCreateConverterBlockEntity reverse = blockEntity(helper, reversePos,
                TfcToCreateConverterBlockEntity.class);
            helper.assertTrue(!converter.getBlockState().getValue(MechanicalConverterBlock.POWERED)
                    && Math.abs(Math.abs(blockEntity(helper, axlePos, RotatingBlockEntity.class)
                        .getRotationNode().rotation().speed()) - (float) Math.PI / 5) < EPSILON
                    && Float.valueOf(64).equals(converter.getOrCreateNetwork().members.get(converter)),
                "Create-to-TFC conversion and stress must resume without replacement");
            helper.assertTrue(!reverse.getBlockState().getValue(MechanicalConverterBlock.POWERED)
                    && Math.abs(Math.abs(blockEntity(helper, shaftPos, KineticBlockEntity.class).getSpeed())
                        - 120) < EPSILON
                    && Float.valueOf(64).equals(reverse.getOrCreateNetwork().sources.get(reverse)),
                "TFC-to-Create conversion and capacity must resume without replacement");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID,
        timeoutTicks = 100)
    public static void configKeysDefaultsBoundsBehaviorAndPersistence(GameTestHelper helper) {
        List<ModConfigSpec.ConfigValue<?>> values = configValues();
        helper.assertTrue(values.stream().map(value -> String.join(".", value.getPath())).toList().equals(List.of(
                "blaze_burner.smoulderingHeat", "blaze_burner.fadingHeat",
                "blaze_burner.kindledHeat", "blaze_burner.seethingHeat",
                "encased_fan.baseAir", "encased_fan.speedMultiplier",
                "mechanical_converters.createToTfcStressImpact",
                "mechanical_converters.tfcToCreateStressCapacity")),
            "The common config must contain exactly the eight specified integration keys");
        for (ModConfigSpec.ConfigValue<?> value : values) {
            ModConfigSpec.ValueSpec spec = value.getSpec();
            Object below = value == PoweredTFCConfig.SPEED_MULTIPLIER ? -0.1 : -1;
            Object above = value == PoweredTFCConfig.SPEED_MULTIPLIER ? 100.1 : 20000;
            helper.assertTrue(spec.test(value.getDefault()) && !spec.test(below) && !spec.test(above),
                value.getPath() + " must accept its default and reject values outside its range");
            helper.assertTrue(spec.test(spec.correct(below)) && spec.test(spec.correct(above)),
                value.getPath() + " must safely correct invalid values");
        }
        Object[] originalValues = values.stream().map(ModConfigSpec.ConfigValue::get).toArray();

        helper.runAfterDelay(60, () -> {
            setConfigValues(81, 1101, 1301, 1801, 101, 2.0, 65, 66);
            placeConfigFixtures(helper);
        });
        helper.runAfterDelay(72, () -> {
            try {
                assertConfiguredHeat(helper);
                CharcoalForgeBlockEntity forge = blockEntity(helper,
                    helper.absolutePos(new BlockPos(4, 2, 0)), CharcoalForgeBlockEntity.class);
                int airBeforePush = forge.getAirTicks();
                EncasedFanAir.supplyAir(blockEntity(helper,
                    helper.absolutePos(new BlockPos(3, 2, 0)), EncasedFanBlockEntity.class));
                helper.assertTrue(forge.getAirTicks() - airBeforePush == 229,
                    "The fan must use reloaded baseAir and speedMultiplier values");
                CreateToTfcConverterBlockEntity converter = blockEntity(helper,
                    helper.absolutePos(new BlockPos(1, 2, 4)), CreateToTfcConverterBlockEntity.class);
                TfcToCreateConverterBlockEntity reverse = blockEntity(helper,
                    helper.absolutePos(new BlockPos(3, 2, 2)), TfcToCreateConverterBlockEntity.class);
                helper.assertTrue(Float.valueOf(65).equals(
                        converter.getOrCreateNetwork().members.get(converter)),
                    "The running converter must use the reloaded stress impact");
                helper.assertTrue(Float.valueOf(66).equals(
                        reverse.getOrCreateNetwork().sources.get(reverse)),
                    "The running reverse converter must use the reloaded stress capacity");
                PoweredTFCConfig.SPEC.save();
                PoweredTFCConfig.SPEC.afterReload();
                helper.assertTrue(PoweredTFCConfig.SMOULDERING_HEAT.get() == 81
                        && PoweredTFCConfig.FADING_HEAT.get() == 1101
                        && PoweredTFCConfig.KINDLED_HEAT.get() == 1301
                        && PoweredTFCConfig.SEETHING_HEAT.get() == 1801
                        && PoweredTFCConfig.BASE_AIR.get() == 101
                        && PoweredTFCConfig.SPEED_MULTIPLIER.get() == 2.0
                        && PoweredTFCConfig.CREATE_TO_TFC_STRESS_IMPACT.get() == 65
                        && PoweredTFCConfig.TFC_TO_CREATE_STRESS_CAPACITY.get() == 66,
                    "All integration values must remain active after a config reload");
                try (CommentedFileConfig saved = CommentedFileConfig.of(
                        FMLPaths.CONFIGDIR.get().resolve("poweredtfc-common.toml"))) {
                    saved.load();
                    helper.assertTrue(saved.<Integer>get("blaze_burner.smoulderingHeat") == 81
                            && saved.<Integer>get("mechanical_converters.createToTfcStressImpact") == 65,
                        "Saved integration values must persist in the common config file");
                }
                helper.assertTrue(Math.abs(MechanicalPowerConversion.rpmToRadiansPerTick(120)
                        - (float) Math.PI / 5) < EPSILON,
                    "The converter calculation must preserve physical rotation units");
                helper.succeed();
            } finally {
                restoreConfigValues(originalValues);
                PoweredTFCConfig.SPEC.save();
            }
        });
    }

    private static void placeConverterLoop(GameTestHelper helper) {
        BlockPos forwardPos = helper.absolutePos(new BlockPos(1, 2, 2));
        helper.getLevel().setBlockAndUpdate(forwardPos,
            PoweredTFCRegistry.CREATE_TO_TFC_CONVERTER.get().defaultBlockState()
                .setValue(MechanicalConverterBlock.FACING, Direction.WEST));
        placeTfcAxle(helper, forwardPos.east());
        helper.getLevel().setBlockAndUpdate(forwardPos.east(2),
            PoweredTFCRegistry.TFC_TO_CREATE_CONVERTER.get().defaultBlockState()
                .setValue(MechanicalConverterBlock.FACING, Direction.EAST));
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(0, 2, 1)),
            AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING, Direction.SOUTH));
        setCreateMotorSpeed(helper, helper.absolutePos(new BlockPos(0, 2, 1)), 120);
        placeCreateGearbox(helper, new BlockPos(0, 2, 2));
        placeCreateGearbox(helper, new BlockPos(0, 2, 4));
        placeCreateGearbox(helper, new BlockPos(4, 2, 4));
        placeCreateGearbox(helper, new BlockPos(4, 2, 2));
        placeCreateShaft(helper, helper.absolutePos(new BlockPos(0, 2, 3)), Direction.Axis.Z);
        placeCreateShaft(helper, helper.absolutePos(new BlockPos(4, 2, 3)), Direction.Axis.Z);
        for (int x = 1; x < 4; x++) {
            placeCreateShaft(helper, helper.absolutePos(new BlockPos(x, 2, 4)), Direction.Axis.X);
        }
    }

    private static void placeCreateGearbox(GameTestHelper helper, BlockPos relativePos) {
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(relativePos),
            AllBlocks.GEARBOX.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
    }

    private static void placeConfigFixtures(GameTestHelper helper) {
        BlockPos burnerPos = helper.absolutePos(new BlockPos(1, 1, 0));
        helper.getLevel().setBlockAndUpdate(burnerPos, AllBlocks.BLAZE_BURNER.getDefaultState()
            .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING));
        helper.getLevel().setBlockAndUpdate(burnerPos.above(), TFCBlocks.CRUCIBLE.get().defaultBlockState());
        BlockPos fanPos = helper.absolutePos(new BlockPos(3, 2, 0));
        placeCreateMotor(helper, fanPos.west(), Direction.EAST, 64);
        helper.getLevel().setBlockAndUpdate(fanPos, AllBlocks.ENCASED_FAN.getDefaultState()
            .setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.getLevel().setBlockAndUpdate(fanPos.east(), TFCBlocks.CHARCOAL_FORGE.get().defaultBlockState());
        BlockPos converterPos = helper.absolutePos(new BlockPos(1, 2, 4));
        placeCreateMotor(helper, converterPos.west(), Direction.EAST, 120);
        helper.getLevel().setBlockAndUpdate(converterPos,
            PoweredTFCRegistry.CREATE_TO_TFC_CONVERTER.get().defaultBlockState()
                .setValue(MechanicalConverterBlock.FACING, Direction.WEST));
        BlockPos reversePos = helper.absolutePos(new BlockPos(3, 2, 2));
        placeTfcMotor(helper, reversePos.west(), Direction.Axis.X, 32);
        helper.getLevel().setBlockAndUpdate(reversePos,
            PoweredTFCRegistry.TFC_TO_CREATE_CONVERTER.get().defaultBlockState()
                .setValue(MechanicalConverterBlock.FACING, Direction.EAST));
        placeCreateShaft(helper, reversePos.east(), Direction.Axis.X);
    }

    private static void assertConfiguredHeat(GameTestHelper helper) {
        BlockPos burnerPos = helper.absolutePos(new BlockPos(1, 1, 0));
        BlazeBurnerBlockEntity burner = blockEntity(helper, burnerPos, BlazeBurnerBlockEntity.class);
        int[] expected = {81, 1101, 1301, 1801};
        BlazeBurnerBlock.HeatLevel[] levels = {
            BlazeBurnerBlock.HeatLevel.SMOULDERING, BlazeBurnerBlock.HeatLevel.FADING,
            BlazeBurnerBlock.HeatLevel.KINDLED, BlazeBurnerBlock.HeatLevel.SEETHING
        };
        for (int index = 0; index < levels.length; index++) {
            helper.getLevel().setBlockAndUpdate(burnerPos,
                burner.getBlockState().setValue(BlazeBurnerBlock.HEAT_LEVEL, levels[index]));
            BlazeBurnerHeat.supplyHeat(burner);
            var heat = helper.getLevel().getCapability(BlockCapabilities.HEAT, burnerPos.above(), Direction.DOWN);
            helper.assertTrue(heat != null && heat.getTemperature() <= expected[index]
                    && heat.getTemperature() >= expected[index] - 1,
                levels[index] + " must use its reloaded heat value");
        }
    }

    private static List<ModConfigSpec.ConfigValue<?>> configValues() {
        return List.of(PoweredTFCConfig.SMOULDERING_HEAT, PoweredTFCConfig.FADING_HEAT,
            PoweredTFCConfig.KINDLED_HEAT, PoweredTFCConfig.SEETHING_HEAT,
            PoweredTFCConfig.BASE_AIR, PoweredTFCConfig.SPEED_MULTIPLIER,
            PoweredTFCConfig.CREATE_TO_TFC_STRESS_IMPACT,
            PoweredTFCConfig.TFC_TO_CREATE_STRESS_CAPACITY);
    }

    private static void setConfigValues(int smouldering, int fading, int kindled, int seething,
        int baseAir, double speedMultiplier, int impact, int capacity) {
        PoweredTFCConfig.SMOULDERING_HEAT.set(smouldering);
        PoweredTFCConfig.FADING_HEAT.set(fading);
        PoweredTFCConfig.KINDLED_HEAT.set(kindled);
        PoweredTFCConfig.SEETHING_HEAT.set(seething);
        PoweredTFCConfig.BASE_AIR.set(baseAir);
        PoweredTFCConfig.SPEED_MULTIPLIER.set(speedMultiplier);
        PoweredTFCConfig.CREATE_TO_TFC_STRESS_IMPACT.set(impact);
        PoweredTFCConfig.TFC_TO_CREATE_STRESS_CAPACITY.set(capacity);
        PoweredTFCConfig.SPEC.afterReload();
    }

    private static void restoreConfigValues(Object[] values) {
        setConfigValues((Integer) values[0], (Integer) values[1], (Integer) values[2],
            (Integer) values[3], (Integer) values[4], (Double) values[5],
            (Integer) values[6], (Integer) values[7]);
    }

    @SuppressWarnings("unchecked")
    private static void assertRecipe(GameTestHelper helper, String id, Item item,
        ItemStack thirdIngredient) {
        RecipeHolder<?> recipe = helper.getLevel().getRecipeManager()
            .byKey(ResourceLocation.fromNamespaceAndPath(PoweredTFC.MOD_ID, id)).orElse(null);
        helper.assertTrue(recipe != null, id + " recipe must be loaded");
        helper.assertTrue(recipe.value().getIngredients().size() == 3,
            id + " must have three shapeless ingredients");
        Recipe<CraftingInput> craftingRecipe = (Recipe<CraftingInput>) recipe.value();
        CraftingInput input = CraftingInput.of(3, 1, List.of(
            new ItemStack(AllBlocks.CLUTCH.asItem()), new ItemStack(AllBlocks.COGWHEEL.asItem()), thirdIngredient));
        helper.assertTrue(craftingRecipe.matches(input, helper.getLevel()),
            id + " must accept its specified three ingredients");
        ItemStack result = craftingRecipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(item) && result.getCount() == 1,
            id + " must craft one registered converter");
    }

    private static void placeCreateMotor(GameTestHelper helper, BlockPos pos, Direction facing, int speed) {
        helper.getLevel().setBlockAndUpdate(pos, AllBlocks.CREATIVE_MOTOR.getDefaultState()
            .setValue(BlockStateProperties.FACING, facing));
        setCreateMotorSpeed(helper, pos, speed);
    }

    private static void setCreateMotorSpeed(GameTestHelper helper, BlockPos pos, int speed) {
        blockEntity(helper, pos, CreativeMotorBlockEntity.class).generatedSpeed.setValue(speed);
    }

    private static void placeTfcMotor(GameTestHelper helper, BlockPos pos, Direction.Axis axis, int step) {
        helper.getLevel().setBlockAndUpdate(pos, TFCBlocks.CREATIVE_MOTOR.get().defaultBlockState()
            .setValue(CreativeRotationBlock.AXIS, axis));
        CreativeRotationBlockEntity motor = blockEntity(helper, pos, CreativeRotationBlockEntity.class);
        CompoundTag tag = motor.saveWithoutMetadata(helper.getLevel().registryAccess());
        tag.putInt("step", step);
        motor.loadWithComponents(tag, helper.getLevel().registryAccess());
    }

    private static void placeTfcAxle(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos,
            helper.getLevel().registryAccess().registryOrThrow(Registries.BLOCK)
                .get(ResourceLocation.fromNamespaceAndPath("tfc", "wood/axle/oak"))
                .defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
    }

    private static void placeCreateShaft(GameTestHelper helper, BlockPos pos, Direction.Axis axis) {
        helper.getLevel().setBlockAndUpdate(pos, AllBlocks.SHAFT.getDefaultState()
            .setValue(BlockStateProperties.AXIS, axis));
    }

    private static <T> T blockEntity(GameTestHelper helper, BlockPos pos, Class<T> type) {
        return type.cast(helper.getLevel().getBlockEntity(pos));
    }

    private ConverterGameTests() {
    }
}
