package dev.poweredtfc.poweredtfc.gametest;

import java.util.List;

import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.mixer.CompactingRecipe;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.poweredtfc.poweredtfc.PoweredTFC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PoweredTFC.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BasinGameTests {
    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void fourFluidInputsPersistAndLoadOldData(GameTestHelper helper) {
        BasinBlockEntity basin = placeBasin(helper, new BlockPos(2, 2, 2));

        helper.runAfterDelay(1, () -> {
            IFluidHandler suppliedFluids = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                basin.getBlockPos(), null);
            helper.assertTrue(suppliedFluids != null, "A Basin must expose its block fluid capability");
            IFluidHandler input = basin.inputTank.getCapability();
            helper.assertTrue(input.getTanks() == 4, "A Basin must expose four input fluid tanks");
            for (int tank = 0; tank < 4; tank++) {
                int filled = suppliedFluids.fill(new FluidStack(testFluid(tank), 1000), FluidAction.EXECUTE);
                helper.assertTrue(filled == 1000, "Each distinct fluid must fill one 1000 mB input tank");
                helper.assertTrue(input.getTankCapacity(tank) == 1000,
                    "Every input fluid tank must retain Create's 1000 mB capacity");
                helper.assertTrue(input.getFluidInTank(tank).is(testFluid(tank))
                        && input.getFluidInTank(tank).getAmount() == 1000,
                    "Each input fluid must remain in its own full tank");
            }
            helper.assertTrue(suppliedFluids.fill(new FluidStack(testFluid(0), 1), FluidAction.EXECUTE) == 0,
                "A full fluid input tank must not accept more than 1000 mB");

            CompoundTag saved = new CompoundTag();
            basin.inputTank.write(saved, helper.getLevel().registryAccess(), false);
            BasinBlockEntity restored = placeBasin(helper, new BlockPos(4, 2, 2));
            restored.inputTank.read(saved, helper.getLevel().registryAccess(), false);
            assertFluids(helper, restored.inputTank.getCapability(), 4);

            CompoundTag oldData = saved.copy();
            ListTag oldTanks = oldData.getList("InputTanks", CompoundTag.TAG_COMPOUND);
            oldTanks.remove(3);
            oldTanks.remove(2);
            BasinBlockEntity upgraded = placeBasin(helper, new BlockPos(6, 2, 2));
            upgraded.inputTank.read(oldData, helper.getLevel().registryAccess(), false);
            assertFluids(helper, upgraded.inputTank.getCapability(), 2);
            helper.assertTrue(upgraded.inputTank.getCapability().getFluidInTank(2).isEmpty()
                    && upgraded.inputTank.getCapability().getFluidInTank(3).isEmpty(),
                "Loading old two-tank data must leave the added input tanks empty");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void threeAndFourFluidRecipesProcess(GameTestHelper helper) {
        BasinBlockEntity mixingBasin = placeBasin(helper, new BlockPos(2, 2, 2));
        BasinBlockEntity compactingBasin = placeBasin(helper, new BlockPos(5, 2, 2));

        helper.runAfterDelay(1, () -> {
            DataResult<MixingRecipe> mixingResult = decode(AllRecipeTypes.MIXING.getSerializer(), """
                {
                  "ingredients": [
                    {"type": "neoforge:single", "amount": 250, "fluid": "minecraft:water"},
                    {"type": "neoforge:single", "amount": 250, "fluid": "minecraft:lava"},
                    {"type": "neoforge:single", "amount": 250, "fluid": "tfc:salt_water"}
                  ],
                  "results": [{"id": "minecraft:brick"}]
                }
                """);
            MixingRecipe mixing = mixingResult.result().orElse(null);
            helper.assertTrue(mixing != null,
                "The Mixing serializer must decode a recipe with three fluid ingredients: "
                    + mixingResult.error().map(Object::toString).orElse("unknown error"));
            fill(mixingBasin, 3, 250);
            helper.assertTrue(BasinRecipe.apply(mixingBasin, mixing),
                "A satisfied three-fluid Mixing recipe must process");
            helper.assertTrue(mixingBasin.getOutputInventory().getStackInSlot(0).is(Items.BRICK),
                "The three-fluid Mixing recipe must create its result");

            DataResult<CompactingRecipe> compactingResult = decode(AllRecipeTypes.COMPACTING.getSerializer(), """
                {
                  "ingredients": [
                    {"type": "neoforge:single", "amount": 250, "fluid": "minecraft:water"},
                    {"type": "neoforge:single", "amount": 250, "fluid": "minecraft:lava"},
                    {"type": "neoforge:single", "amount": 250, "fluid": "tfc:salt_water"},
                    {"type": "neoforge:single", "amount": 250, "fluid": "tfc:olive_oil"}
                  ],
                  "results": [{"id": "minecraft:brick"}]
                }
                """);
            CompactingRecipe compacting = compactingResult.result().orElse(null);
            helper.assertTrue(compacting != null,
                "The Compacting serializer must decode a recipe with four fluid ingredients: "
                    + compactingResult.error().map(Object::toString).orElse("unknown error"));
            fill(compactingBasin, 4, 250);
            helper.assertTrue(BasinRecipe.apply(compactingBasin, compacting),
                "A satisfied four-fluid Compacting recipe must process");
            helper.assertTrue(compactingBasin.getOutputInventory().getStackInSlot(0).is(Items.BRICK),
                "The four-fluid Compacting recipe must create its result");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void existingRecipesAndOutputLimitsRemain(GameTestHelper helper) {
        BlockPos mixingBasinPos = new BlockPos(2, 2, 2);
        BasinBlockEntity outputLimitBasin = placeBasin(helper, new BlockPos(5, 2, 2));

        helper.runAfterDelay(1, () -> {
            helper.getLevel().setBlockAndUpdate(helper.absolutePos(mixingBasinPos).below(),
                AllBlocks.BLAZE_BURNER.getDefaultState()
                    .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED));
            BasinBlockEntity mixingBasin = placeBasin(helper, mixingBasinPos);
            RecipeHolder<?> teaHolder = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.parse("create:mixing/tea")).orElse(null);
            helper.assertTrue(teaHolder != null && teaHolder.value() instanceof MixingRecipe,
                "Create's existing tea Mixing recipe must be loaded");
            MixingRecipe tea = (MixingRecipe) teaHolder.value();
            IFluidHandler suppliedFluids = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                mixingBasin.getBlockPos(), null);
            helper.assertTrue(suppliedFluids != null, "The Mixing Basin must expose its block fluid capability");
            helper.assertTrue(suppliedFluids.fill(new FluidStack(Fluids.WATER, 250), FluidAction.EXECUTE) == 250,
                "The Mixing Basin must accept 250 mB of water");
            Fluid milk = BuiltInRegistries.FLUID.get(ResourceLocation.parse("minecraft:milk"));
            helper.assertTrue(suppliedFluids.fill(new FluidStack(milk, 250), FluidAction.EXECUTE) == 250,
                "The Mixing Basin must accept 250 mB of milk");
            mixingBasin.inputInventory.insertItem(0, new ItemStack(Items.OAK_LEAVES), false);
            helper.assertTrue(BasinRecipe.apply(mixingBasin, tea),
                "Create's existing two-fluid tea recipe must process with heat");
            IFluidHandler teaOutput = mixingBasin.getBehaviour(SmartFluidTankBehaviour.OUTPUT).getCapability();
            Fluid teaFluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse("create:tea"));
            helper.assertTrue(teaOutput.getFluidInTank(0).is(teaFluid)
                    && teaOutput.getFluidInTank(0).getAmount() == 500,
                "Create's existing tea recipe must output 500 mB of tea");

            SmartFluidTankBehaviour output = outputLimitBasin.getBehaviour(SmartFluidTankBehaviour.OUTPUT);
            IFluidHandler outputHandler = output.getCapability();
            helper.assertTrue(outputHandler.getTanks() == 2,
                "The Basin output must remain limited to two fluid tanks");
            helper.assertTrue(outputHandler.getTankCapacity(0) == 1000
                    && outputHandler.getTankCapacity(1) == 1000,
                "The Basin output tanks must remain limited to 1000 mB each");
            helper.assertTrue(outputLimitBasin.acceptOutputs(List.of(), List.of(
                    new FluidStack(testFluid(2), 1000), new FluidStack(testFluid(3), 1000)), false),
                "Two distinct 1000 mB fluid outputs must still fit");
            helper.assertTrue(!outputLimitBasin.acceptOutputs(List.of(),
                    List.of(new FluidStack(Fluids.WATER, 1)), true),
                "A third distinct fluid output must still be rejected");

            MixingRecipe threeOutputs = new StandardProcessingRecipe.Builder<>(MixingRecipe::new,
                ResourceLocation.fromNamespaceAndPath(PoweredTFC.MOD_ID, "three_fluid_outputs"))
                .output(testFluid(0), 1)
                .output(testFluid(1), 1)
                .output(testFluid(2), 1)
                .build();
            helper.assertTrue(!threeOutputs.validate().isEmpty(),
                "Recipe validation must continue to reject more than two fluid outputs");
            helper.succeed();
        });
    }

    private static <R extends BasinRecipe> DataResult<R> decode(RecipeSerializer<R> serializer, String json) {
        return serializer.codec().codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    private static BasinBlockEntity placeBasin(GameTestHelper helper, BlockPos relativePos) {
        BlockPos pos = helper.absolutePos(relativePos);
        helper.getLevel().setBlockAndUpdate(pos, AllBlocks.BASIN.getDefaultState());
        return (BasinBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static void fill(BasinBlockEntity basin, int fluidCount, int amount) {
        for (int index = 0; index < fluidCount; index++) {
            basin.inputTank.getCapability().fill(new FluidStack(testFluid(index), amount),
                FluidAction.EXECUTE);
        }
    }

    private static void assertFluids(GameTestHelper helper, IFluidHandler handler, int fluidCount) {
        for (int tank = 0; tank < fluidCount; tank++) {
            helper.assertTrue(handler.getFluidInTank(tank).is(testFluid(tank))
                    && handler.getFluidInTank(tank).getAmount() == 1000,
                "Saved Basin input fluid " + tank + " must load unchanged");
        }
    }

    private static Fluid testFluid(int index) {
        return switch (index) {
            case 0 -> Fluids.WATER;
            case 1 -> Fluids.LAVA;
            case 2 -> BuiltInRegistries.FLUID.get(ResourceLocation.parse("tfc:salt_water"));
            case 3 -> BuiltInRegistries.FLUID.get(ResourceLocation.parse("tfc:olive_oil"));
            default -> throw new IllegalArgumentException("No test fluid at index " + index);
        };
    }
}
