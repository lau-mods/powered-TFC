package net.claustra01.poweredtfc.gametest;

import java.util.Collections;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.claustra01.poweredtfc.PoweredTFC;
import net.claustra01.poweredtfc.registry.PoweredTFCRegistry;
import net.dries007.tfc.common.blockentities.BlastFurnaceBlockEntity;
import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.devices.BlastFurnaceBlock;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PoweredTFC.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DisplaySourceGameTests {
    private static final DisplayTargetStats SINGLE_LINE = new DisplayTargetStats(1, 32, null);

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void crucibleTemperatureModes(GameTestHelper helper) {
        CrucibleBlockEntity crucible = placeCrucible(helper, 1000);
        DisplayLinkBlockEntity link = placeLinkBelow(helper, crucible.getBlockPos());
        DisplaySource source = PoweredTFCRegistry.TFC_CRUCIBLE_DISPLAY_SOURCE.get();

        helper.assertTrue(CreateBuiltInRegistries.DISPLAY_SOURCE.getKey(source).equals(
            ResourceLocation.fromNamespaceAndPath(PoweredTFC.MOD_ID, "tfc_crucible")),
            "The crucible source must retain its public ID");
        helper.assertTrue(DisplaySource.getAll(helper.getLevel(), crucible.getBlockPos()).contains(source),
            "The crucible block entity must expose the source");
        helper.assertTrue(text(source, link, 0).equals("1000°C"),
            "Temperature mode must show the current Celsius temperature");
        MutableComponent color = component(source, link, 1);
        helper.assertTrue(color.getString().startsWith("Orange"),
            "Temperature color mode must use TFC's heat name at 1000°C");
        helper.assertTrue(color.getStyle().getColor() != null,
            "Temperature color mode must use TFC's heat color");
        SignBlockEntity sign = placeSign(helper, new BlockPos(4, 2, 2));
        DisplayLinkBlockEntity linked = placeLinkedDisplay(helper, crucible.getBlockPos(),
            sign.getBlockPos(), source, 1);
        source.transferData(new DisplayLinkContext(helper.getLevel(), linked),
            DisplayTarget.get(helper.getLevel(), sign.getBlockPos()), 0);
        helper.assertTrue(sign.getFrontText().getMessage(0, false).getStyle().getColor()
            .equals(color.getStyle().getColor()), "A sign target must retain TFC's heat color");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void crucibleNoHeatAndUpdate(GameTestHelper helper) {
        CrucibleBlockEntity crucible = placeCrucible(helper, 0);
        DisplaySource source = PoweredTFCRegistry.TFC_CRUCIBLE_DISPLAY_SOURCE.get();
        SignBlockEntity sign = placeSign(helper, new BlockPos(4, 2, 2));
        placeLinkedDisplay(helper, crucible.getBlockPos(), sign.getBlockPos(), source, 0);

        helper.runAfterDelay(21, () -> {
            helper.assertTrue(signText(sign).equals("No heat"), "A cold crucible must display No heat");
            loadTemperature(crucible, helper, 500);
            helper.runAfterDelay(21, () -> {
                helper.assertTrue(signText(sign).equals("500°C"),
                    "The connected sign must display a changed temperature");
                loadTemperature(crucible, helper, 0);
                helper.runAfterDelay(21, () -> {
                    helper.assertTrue(signText(sign).equals("No heat"),
                        "The connected sign must return to No heat without replacing the link");
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void blastFurnaceModes(GameTestHelper helper) {
        BlastFurnaceBlockEntity furnace = placeBlastFurnace(helper, 1000, 2, 3, 4);
        DisplayLinkBlockEntity link = placeLinkBelow(helper, furnace.getBlockPos());
        DisplaySource source = PoweredTFCRegistry.TFC_BLAST_FURNACE_DISPLAY_SOURCE.get();

        helper.assertTrue(CreateBuiltInRegistries.DISPLAY_SOURCE.getKey(source).equals(
            ResourceLocation.fromNamespaceAndPath(PoweredTFC.MOD_ID, "tfc_blast_furnace")),
            "The blast furnace source must retain its public ID");
        helper.assertTrue(DisplaySource.getAll(helper.getLevel(), furnace.getBlockPos()).contains(source),
            "The blast furnace block entity must expose the source");
        helper.assertTrue(text(source, link, 0).equals("1000°C"), "Temperature mode must show Celsius");
        MutableComponent color = component(source, link, 1);
        helper.assertTrue(color.getString().startsWith("Orange"),
            "Temperature color mode must use TFC's heat name");
        helper.assertTrue(text(source, link, 2).equals("2"), "Input Count must show the input stack count");
        helper.assertTrue(text(source, link, 3).equals("3"),
            "Catalyst Count must show the catalyst stack count");
        helper.assertTrue(text(source, link, 4).equals("4"), "Fuel Count must show the fuel stack count");
        helper.assertTrue(text(source, link, 5).equals("1000°C | 2 | 3 | 4"),
            "Compact mode must show temperature, input, catalyst and fuel in order");
        SignBlockEntity sign = placeSign(helper, new BlockPos(4, 2, 2));
        DisplayLinkBlockEntity linked = placeLinkedDisplay(helper, furnace.getBlockPos(),
            sign.getBlockPos(), source, 1);
        source.transferData(new DisplayLinkContext(helper.getLevel(), linked),
            DisplayTarget.get(helper.getLevel(), sign.getBlockPos()), 0);
        helper.assertTrue(sign.getFrontText().getMessage(0, false).getStyle().getColor()
            .equals(color.getStyle().getColor()), "A sign target must retain TFC's heat color");
        helper.getLevel().removeBlock(furnace.getBlockPos(), false);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void blastFurnaceLiveUpdate(GameTestHelper helper) {
        BlastFurnaceBlockEntity furnace = placeBlastFurnace(helper, 500, 1, 1, 2);
        DisplaySource source = PoweredTFCRegistry.TFC_BLAST_FURNACE_DISPLAY_SOURCE.get();
        SignBlockEntity sign = placeSign(helper, new BlockPos(4, 2, 2));
        placeLinkedDisplay(helper, furnace.getBlockPos(), sign.getBlockPos(), source, 5);

        helper.runAfterDelay(21, () -> {
            helper.assertTrue(signText(sign).equals("500°C | 1 | 1 | 2"),
                "The connected sign must display the initial compact equipment state");
            loadBlastFurnace(furnace, helper, 1200, 3, 3, 1);
            helper.runAfterDelay(21, () -> {
                helper.assertTrue(signText(sign).equals("1200°C | 3 | 3 | 1"),
                    "The connected sign must update without replacing the link");
                helper.succeed();
            });
        });
    }

    private static CrucibleBlockEntity placeCrucible(GameTestHelper helper, float temperature) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, TFCBlocks.CRUCIBLE.get().defaultBlockState());
        CrucibleBlockEntity crucible = (CrucibleBlockEntity) helper.getLevel().getBlockEntity(pos);
        loadTemperature(crucible, helper, temperature);
        return crucible;
    }

    private static BlastFurnaceBlockEntity placeBlastFurnace(GameTestHelper helper, float temperature,
        int inputs, int catalysts, int fuels) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x != 0 || z != 0) {
                    helper.getLevel().setBlockAndUpdate(pos.offset(x, 1, z),
                        TFCBlocks.REINFORCED_FIRE_BRICKS.get().defaultBlockState());
                }
            }
        }
        helper.getLevel().setBlockAndUpdate(pos, TFCBlocks.BLAST_FURNACE.get().defaultBlockState()
            .setValue(BlastFurnaceBlock.LIT, true));
        BlastFurnaceBlockEntity furnace = (BlastFurnaceBlockEntity) helper.getLevel().getBlockEntity(pos);
        loadBlastFurnace(furnace, helper, temperature, inputs, catalysts, fuels);
        return furnace;
    }

    private static DisplayLinkBlockEntity placeLinkBelow(GameTestHelper helper, BlockPos sourcePos) {
        BlockPos linkPos = sourcePos.below();
        helper.getLevel().setBlockAndUpdate(linkPos, AllBlocks.DISPLAY_LINK.getDefaultState()
            .setValue(DisplayLinkBlock.FACING, Direction.DOWN));
        return (DisplayLinkBlockEntity) helper.getLevel().getBlockEntity(linkPos);
    }

    private static DisplayLinkBlockEntity placeLinkedDisplay(GameTestHelper helper, BlockPos sourcePos,
        BlockPos targetPos, DisplaySource source, int mode) {
        DisplayLinkBlockEntity link = placeLinkBelow(helper, sourcePos);
        CompoundTag sourceConfig = new CompoundTag();
        sourceConfig.putString("Id", CreateBuiltInRegistries.DISPLAY_SOURCE.getKey(source).toString());
        sourceConfig.putInt("Mode", mode);
        CompoundTag tag = new CompoundTag();
        tag.put("Source", sourceConfig);
        tag.put("TargetOffset", NbtUtils.writeBlockPos(targetPos.subtract(link.getBlockPos())));
        link.loadWithComponents(tag, helper.getLevel().registryAccess());
        return link;
    }

    private static SignBlockEntity placeSign(GameTestHelper helper, BlockPos relativePos) {
        BlockPos pos = helper.absolutePos(relativePos);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.OAK_SIGN.defaultBlockState());
        return (SignBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static String signText(SignBlockEntity sign) {
        return sign.getFrontText().getMessage(0, false).getString();
    }

    private static void loadTemperature(CrucibleBlockEntity crucible, GameTestHelper helper, float temperature) {
        CompoundTag tag = crucible.saveWithoutMetadata(helper.getLevel().registryAccess());
        tag.putFloat("temperature", temperature);
        tag.putFloat("targetTemperature", temperature);
        tag.putInt("targetTemperatureStabilityTicks", 100);
        crucible.loadWithComponents(tag, helper.getLevel().registryAccess());
    }

    private static void loadBlastFurnace(BlastFurnaceBlockEntity furnace, GameTestHelper helper,
        float temperature, int inputs, int catalysts, int fuels) {
        CompoundTag tag = furnace.saveWithoutMetadata(helper.getLevel().registryAccess());
        tag.putFloat("temperature", temperature);
        tag.putFloat("burnTemperature", temperature);
        tag.putInt("burnTicks", 10000);
        tag.put("inputStacks", itemList(helper, inputs, "tfc:metal/ingot/wrought_iron"));
        tag.put("catalystStacks", itemList(helper, catalysts, "tfc:powder/flux"));
        tag.put("fuelStacks", itemList(helper, fuels, "minecraft:charcoal"));
        furnace.loadWithComponents(tag, helper.getLevel().registryAccess());
    }

    private static ListTag itemList(GameTestHelper helper, int size, String itemId) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        return Helpers.writeItemStacksToNbt(helper.getLevel().registryAccess(),
            Collections.nCopies(size, new ItemStack(item)));
    }

    private static String text(DisplaySource source, DisplayLinkBlockEntity link, int mode) {
        return component(source, link, mode).getString();
    }

    private static MutableComponent component(DisplaySource source, DisplayLinkBlockEntity link, int mode) {
        CompoundTag config = new CompoundTag();
        config.putInt("Mode", mode);
        link.setSourceConfig(config);
        List<MutableComponent> output = source.provideText(new DisplayLinkContext(link.getLevel(), link), SINGLE_LINE);
        return output.getFirst();
    }

    private DisplaySourceGameTests() {
    }
}
